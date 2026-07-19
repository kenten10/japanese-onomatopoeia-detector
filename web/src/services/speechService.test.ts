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
})

