import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import type { HistoryItem, RecordingState } from './types'
import { evaluate } from './services/onoEngine'
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
  stateRef.current = recordingState

  const loadHistory = useCallback(async () => {
    try { setHistory(await repository.fetch()) }
    catch { setHistory([]); setPersistenceError(t('error.persistence.load')) }
  }, [t])

  useEffect(() => { void loadHistory() }, [loadHistory])

  const speech = useMemo(() => new WebSpeechService({
    onPartial(raw) {
      void hiraganaText(raw).then(setPartialText)
    },
    onFinal(raw) {
      setRecordingState({ kind: 'recognizing' })
      void hiraganaText(raw).then(async (text) => {
        if (!text.trim()) { setRecordingState({ kind: 'idle' }); return }
        setPartialText(text)
        setRecordingState({ kind: 'evaluating' })
        setRecordingState({ kind: 'result', result: await evaluate(text) })
      })
    },
    onCancelled() {
      if (stateRef.current.kind === 'recording' || stateRef.current.kind === 'recognizing') {
        setRecordingState({ kind: 'idle' })
      }
    },
    onError(error) {
      const denied = error.code === 'permission-denied'
      const message = denied ? t('permission.mic.deny')
        : error.code === 'recognition-failed' ? t('error.recognition.failed')
          : t('error.recognizer.unavailable')
      setRecordingState({ kind: 'error', message, permissionDenied: denied })
    }
  }), [t])

  useEffect(() => () => speech.abort(), [speech])

  const value = useMemo<AppValue>(() => ({
    recordingState, partialText, history, persistenceError,
    async startRecording() {
      setPartialText('')
      try {
        await speech.start()
        setRecordingState({ kind: 'recording' })
      } catch (error) {
        const denied = error instanceof SpeechServiceError && error.code === 'permission-denied'
        setRecordingState({
          kind: 'error', permissionDenied: denied,
          message: denied ? t('permission.mic.deny') : t('error.recognizer.unavailable')
        })
      }
    },
    stopRecording() {
      if (recordingState.kind !== 'recording') return
      setRecordingState({ kind: 'recognizing' })
      speech.stop()
    },
    resetToIdle() { speech.abort(); setPartialText(''); setRecordingState({ kind: 'idle' }) },
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

