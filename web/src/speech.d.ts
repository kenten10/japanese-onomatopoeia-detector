interface BrowserSpeechRecognitionAlternative { transcript: string; confidence: number }
interface BrowserSpeechRecognitionResult { readonly isFinal: boolean; readonly length: number; [index: number]: BrowserSpeechRecognitionAlternative }
interface BrowserSpeechRecognitionResultList { readonly length: number; [index: number]: BrowserSpeechRecognitionResult }
interface BrowserSpeechRecognitionEvent extends Event { readonly resultIndex: number; readonly results: BrowserSpeechRecognitionResultList }
interface BrowserSpeechRecognitionErrorEvent extends Event { readonly error: string; readonly message: string }
interface BrowserSpeechRecognition extends EventTarget {
  lang: string
  continuous: boolean
  interimResults: boolean
  maxAlternatives: number
  processLocally?: boolean
  start(): void
  stop(): void
  abort(): void
  onresult: ((event: BrowserSpeechRecognitionEvent) => void) | null
  onerror: ((event: BrowserSpeechRecognitionErrorEvent) => void) | null
  onend: (() => void) | null
}
interface BrowserSpeechRecognitionConstructor {
  new(): BrowserSpeechRecognition
  available?(options: { langs: string[]; processLocally: boolean }): Promise<'available' | 'downloadable' | 'downloading' | 'unavailable'>
}
interface Window {
  SpeechRecognition?: BrowserSpeechRecognitionConstructor
  webkitSpeechRecognition?: BrowserSpeechRecognitionConstructor
}

interface Navigator {
  readonly standalone?: boolean
}
