import type { SpeechCallbacks } from '../types'
import { SpeechServiceError } from '../types'

const MAX_SECONDS = 10

export class WebSpeechService {
  private recognition?: BrowserSpeechRecognition
  private timeout?: number
  private latestText = ''
  private finished = false
  private stopping = false

  constructor(private readonly callbacks: SpeechCallbacks) {}

  get supported(): boolean {
    return Boolean(window.SpeechRecognition ?? window.webkitSpeechRecognition)
  }

  async start(): Promise<void> {
    this.abort()
    const Constructor = window.SpeechRecognition ?? window.webkitSpeechRecognition
    if (!Constructor) throw new SpeechServiceError('unsupported')

    const recognition = new Constructor()
    recognition.lang = 'ja-JP'
    recognition.continuous = false
    recognition.interimResults = true
    recognition.maxAlternatives = 5

    if ('processLocally' in recognition && Constructor.available) {
      try {
        const availability = await Constructor.available({ langs: ['ja-JP'], processLocally: true })
        recognition.processLocally = availability === 'available'
      } catch {
        recognition.processLocally = false
      }
    }

    this.recognition = recognition
    this.latestText = ''
    this.finished = false
    this.stopping = false

    recognition.onresult = (event) => {
      let combined = ''
      let allFinal = true
      for (let index = 0; index < event.results.length; index += 1) {
        combined += event.results[index][0]?.transcript ?? ''
        allFinal &&= event.results[index].isFinal
      }
      this.latestText = combined.trim()
      if (this.latestText) this.callbacks.onPartial(this.latestText)
      if (allFinal && this.latestText) this.finish(this.latestText)
    }
    recognition.onerror = (event) => {
      if (this.finished || (this.stopping && event.error === 'aborted')) return
      if (event.error === 'no-speech' || event.error === 'aborted') this.cancel()
      else if (event.error === 'not-allowed' || event.error === 'service-not-allowed') {
        this.fail(new SpeechServiceError('permission-denied', event.message))
      } else if (event.error === 'language-not-supported') {
        this.fail(new SpeechServiceError('unavailable', event.message))
      } else {
        this.fail(new SpeechServiceError('recognition-failed', event.message))
      }
    }
    recognition.onend = () => {
      if (this.finished) return
      if (this.latestText) this.finish(this.latestText)
      else this.cancel()
    }

    try {
      recognition.start()
      this.timeout = window.setTimeout(() => this.stop(), MAX_SECONDS * 1000)
    } catch (error) {
      this.clear()
      throw new SpeechServiceError('unavailable', error instanceof Error ? error.message : undefined)
    }
  }

  stop(): void {
    if (!this.recognition || this.finished) return
    this.stopping = true
    if (this.latestText) this.finish(this.latestText)
    else this.recognition.stop()
  }

  abort(): void {
    if (this.recognition && !this.finished) {
      this.finished = true
      this.recognition.abort()
    }
    this.clear()
  }

  private finish(text: string): void {
    if (this.finished) return
    this.finished = true
    this.clearTimer()
    this.recognition?.stop()
    this.callbacks.onFinal(text)
  }

  private cancel(): void {
    if (this.finished) return
    this.finished = true
    this.clear()
    this.callbacks.onCancelled()
  }

  private fail(error: SpeechServiceError): void {
    if (this.finished) return
    this.finished = true
    this.clear()
    this.callbacks.onError(error)
  }

  private clearTimer(): void {
    if (this.timeout !== undefined) window.clearTimeout(this.timeout)
    this.timeout = undefined
  }

  private clear(): void {
    this.clearTimer()
    if (this.recognition) {
      this.recognition.onresult = null
      this.recognition.onerror = null
      this.recognition.onend = null
    }
    this.recognition = undefined
  }
}

