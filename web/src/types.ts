export interface OnomatopoeiaEntry {
  word: string
  reading: string
  meaning_ja: string
  meaning_en: string
  example_ja: string
  example_en: string
  category: string
}

export interface SimilarEntry {
  entry: OnomatopoeiaEntry
  similarity: number
}

export interface EvaluationResult {
  id: string
  inputText: string
  score: number
  similarEntries: SimilarEntry[]
  date: Date
}

export interface HistoryItem {
  id: string
  inputText: string
  score: number
  date: Date
}

export type RecordingState =
  | { kind: 'idle' }
  | { kind: 'recording' }
  | { kind: 'recognizing' }
  | { kind: 'evaluating' }
  | { kind: 'result'; result: EvaluationResult }
  | { kind: 'error'; message: string; permissionDenied?: boolean }

export type AppLanguage = 'system' | 'ja' | 'en'

export interface SpeechCallbacks {
  onPartial(text: string): void
  onFinal(text: string): void
  onCancelled(): void
  onError(error: SpeechServiceError): void
}

export class SpeechServiceError extends Error {
  constructor(
    public readonly code: 'unsupported' | 'permission-denied' | 'unavailable' | 'recognition-failed',
    message?: string
  ) {
    super(message ?? code)
  }
}

