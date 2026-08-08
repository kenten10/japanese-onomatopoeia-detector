import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import type { HistoryItem, RecordingState } from './types'
import { evaluate, normalize } from './services/onoEngine'
import { hiraganaText } from './services/japaneseText'
import { HistoryRepository } from './services/historyRepository'
import { WebSpeechService } from './services/speechService'
import { SpeechServiceError } from './types'
import { useI18n } from './i18n/I18nContext'

interface AppValue {
  recordingState: RecordingState
  partialText: string
  history: HistoryItem[]
  persistenceError?: string
  startRecording(): Promise<void>
  stopRecording(): void
  resetToIdle(): void
  saveCurrentResult(): Promise<boolean>
  deleteHistory(id: string): Promise<void>
  clearHistory(): Promise<void>
  dismissPersistenceError(): void
}

const AppContext = createContext<AppValue | null>(null)
const repository = new HistoryRepository()

export function AppProvider({ children }: { children: ReactNode }) {
  const { t } = useI18n()
  const [recordingState, setRecordingState] = useState<RecordingState>({ kind: 'idle' })
  const [partialText, setPartialText] = useState('')
  const [history, setHistory] = useState<HistoryItem[]>([])
  const [persistenceError, setPersistenceError] = useState<string>()
  const stateRef = useRef(recordingState)
  const tRef = useRef(t)
  const sessionGeneration = useRef(0)
  const partialGeneration = useRef(0)
  stateRef.current = recordingState
  tRef.current = t

  const loadHistory = useCallback(async () => {
    try { setHistory(await repository.fetch()) }
    catch { setHistory([]); setPersistenceError(t('error.persistence.load')) }
  }, [t])

  useEffect(() => { void loadHistory() }, [loadHistory])

  const speechRef = useRef<WebSpeechService | null>(null)
  if (!speechRef.current) {
    speechRef.current = new WebSpeechService({
      onPartial(raw) {
        const generation = ++partialGeneration.current
        const session = sessionGeneration.current
        void hiraganaText(raw).then((text) => {
          if (generation === partialGeneration.current
            && session === sessionGeneration.current
            && stateRef.current.kind === 'recording') setPartialText(text)
        })
      },
      onFinal(raw) {
        const session = sessionGeneration.current
        ++partialGeneration.current
        setRecordingState({ kind: 'recognizing' })
        void hiraganaText(raw).then(async (text) => {
          if (session !== sessionGeneration.current) return
          // 句読点だけの認識結果は trim では落ちないため、正規化して中身の有無を見る
          if (!normalize(text)) { setRecordingState({ kind: 'idle' }); return }
          setPartialText(text)
          setRecordingState({ kind: 'evaluating' })
          const result = await evaluate(text)
          if (session === sessionGeneration.current) setRecordingState({ kind: 'result', result })
        })
      },
      onCancelled() {
        if (stateRef.current.kind === 'recording' || stateRef.current.kind === 'recognizing') {
          setRecordingState({ kind: 'idle' })
        }
      },
      onError(error) {
        const translate = tRef.current
        const message = error.code === 'permission-denied' ? translate('permission.mic.deny')
          : error.code === 'standalone-unsupported' ? translate('permission.pwa.unsupported')
            : error.code === 'speech-service-disabled' ? translate('permission.speech.disabled')
              : error.code === 'recognition-failed' ? translate('error.recognition.failed')
                : translate('error.recognizer.unavailable')
        setRecordingState({ kind: 'error', message, code: error.code })
      }
    })
  }
  const speech = speechRef.current

  useEffect(() => () => speech.abort(), [speech])

  const value = useMemo<AppValue>(() => ({
    recordingState, partialText, history, persistenceError,
    async startRecording() {
      sessionGeneration.current += 1
      partialGeneration.current += 1
      setPartialText('')
      try {
        await speech.start()
        setRecordingState({ kind: 'recording' })
      } catch (error) {
        const code = error instanceof SpeechServiceError ? error.code : 'unavailable'
        setRecordingState({
          kind: 'error', code,
          message: code === 'permission-denied' ? t('permission.mic.deny')
            : code === 'standalone-unsupported' ? t('permission.pwa.unsupported')
              : code === 'speech-service-disabled' ? t('permission.speech.disabled')
                : t('error.recognizer.unavailable')
        })
      }
    },
    stopRecording() {
      if (recordingState.kind !== 'recording') return
      setRecordingState({ kind: 'recognizing' })
      speech.stop()
    },
    resetToIdle() {
      sessionGeneration.current += 1
      partialGeneration.current += 1
      speech.abort(); setPartialText(''); setRecordingState({ kind: 'idle' })
    },
    async saveCurrentResult() {
      if (recordingState.kind !== 'result') return false
      try { await repository.add(recordingState.result.inputText, recordingState.result.score); await loadHistory(); return true }
      catch { setPersistenceError(t('error.persistence.save')); return false }
    },
    async deleteHistory(id) {
      try { await repository.delete(id); await loadHistory() }
      catch { setPersistenceError(t('error.persistence.delete')) }
    },
    async clearHistory() {
      try { await repository.clear(); await loadHistory() }
      catch { setPersistenceError(t('error.persistence.delete')) }
    },
    dismissPersistenceError() { setPersistenceError(undefined) }
  }), [recordingState, partialText, history, persistenceError, speech, t, loadHistory])

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>
}

export function useApp(): AppValue {
  const value = useContext(AppContext)
  if (!value) throw new Error('useApp must be used inside AppProvider')
  return value
}
