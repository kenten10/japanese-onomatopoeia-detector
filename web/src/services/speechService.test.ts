import { afterEach, describe, expect, it, vi } from 'vitest'
import { WebSpeechService } from './speechService'

class FakeRecognition extends EventTarget implements BrowserSpeechRecognition {
  static latest?: FakeRecognition
  lang = ''; continuous = false; interimResults = false; maxAlternatives = 1
  onresult: ((event: BrowserSpeechRecognitionEvent) => void) | null = null
  onerror: ((event: BrowserSpeechRecognitionErrorEvent) => void) | null = null
  onend: (() => void) | null = null
  constructor() { super(); FakeRecognition.latest = this }
  start() {}
  stop() { this.onend?.() }
  abort() {}
  result(text: string, isFinal: boolean) {
    this.onresult?.({ results: { 0: { 0: { transcript: text, confidence: 1 }, isFinal, length: 1 }, length: 1 }, resultIndex: 0 } as unknown as BrowserSpeechRecognitionEvent)
  }
  error(error: string) {
    this.onerror?.({ error, message: error } as BrowserSpeechRecognitionErrorEvent)
  }
}

describe('WebSpeechService', () => {
  afterEach(() => { vi.useRealTimers(); delete window.SpeechRecognition; FakeRecognition.latest = undefined })

  it('delivers partial and final text once', async () => {
    window.SpeechRecognition = FakeRecognition
    const onPartial = vi.fn(), onFinal = vi.fn()
    const service = new WebSpeechService({ onPartial, onFinal, onCancelled: vi.fn(), onError: vi.fn() })
    await service.start()
    FakeRecognition.latest?.result('ふわふわ', false)
    service.stop()
    FakeRecognition.latest?.result('ignored', true)
    expect(onPartial).toHaveBeenCalledWith('ふわふわ')
    expect(onFinal).toHaveBeenCalledOnce()
    expect(onFinal).toHaveBeenCalledWith('ふわふわ')
  })

  it('automatically stops after ten seconds', async () => {
    vi.useFakeTimers()
    window.SpeechRecognition = FakeRecognition
    const onCancelled = vi.fn()
    const service = new WebSpeechService({ onPartial: vi.fn(), onFinal: vi.fn(), onCancelled, onError: vi.fn() })
    await service.start()
    vi.advanceTimersByTime(10_000)
    expect(onCancelled).toHaveBeenCalledOnce()
  })

  it('distinguishes a disabled speech service from microphone denial', async () => {
    window.SpeechRecognition = FakeRecognition
    const onError = vi.fn()
    const service = new WebSpeechService({ onPartial: vi.fn(), onFinal: vi.fn(), onCancelled: vi.fn(), onError })
    await service.start()
    FakeRecognition.latest?.error('service-not-allowed')
    expect(onError.mock.calls[0][0].code).toBe('speech-service-disabled')
  })

  it('keeps only one recognition when start is called twice while awaiting availability', async () => {
    // processLocally の判定は await を挟むため、その間に 2 回目の start が走ると
    // 認識が二重に動いてしまう。後から始めた側だけが残ることを確かめる。
    const started: FakeRecognition[] = []
    let release: (() => void) | undefined
    class SlowRecognition extends FakeRecognition {
      processLocally = false
      static available = () => new Promise<string>((resolve) => { release = () => resolve('unavailable') })
      override start() { started.push(this) }
    }
    window.SpeechRecognition = SlowRecognition as unknown as typeof window.SpeechRecognition

    const service = new WebSpeechService({ onPartial: vi.fn(), onFinal: vi.fn(), onCancelled: vi.fn(), onError: vi.fn() })
    const first = service.start()
    const firstRelease = release
    const second = service.start()
    firstRelease?.()
    release?.()
    await Promise.all([first, second])

    expect(started).toHaveLength(1)
  })
})
