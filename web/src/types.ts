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
  | { kind: 'error'; message: string; code: SpeechServiceErrorCode }

export type AppLanguage = 'system' | 'ja' | 'en'

export interface SpeechCallbacks {
  onPartial(text: string): void
  onFinal(text: string): void
  onCancelled(): void
  onError(error: SpeechServiceError): void
}

export type SpeechServiceErrorCode =
  | 'unsupported'
  | 'permission-denied'
  | 'standalone-unsupported'
  | 'speech-service-disabled'
  | 'unavailable'
  | 'recognition-failed'

export class SpeechServiceError extends Error {
  constructor(
    public readonly code: SpeechServiceErrorCode,
    message?: string
  ) {
    super(message ?? code)
  }
}
