import { type Page } from '@playwright/test'

/**
 * 音声認識を差し替える。
 *
 * ブラウザの SpeechRecognition は実際のマイク入力を要するため、テストからも
 * スクリーンショット撮影からも動かせない。決まったことばを返す偽物を入れて、
 * 認識後の画面を再現できるようにしている。
 */
export async function installFakeSpeech(
  page: Page,
  mode: 'automatic' | 'on-stop' | 'denied' | 'standalone-error' = 'automatic'
) {
  await page.addInitScript((selectedMode) => {
    class FakeSpeechRecognition {
      lang = ''; continuous = false; interimResults = true; maxAlternatives = 1
      onresult: ((event: unknown) => void) | null = null
      onerror: ((event: unknown) => void) | null = null
      onend: (() => void) | null = null
      start() {
        if (selectedMode === 'automatic') window.setTimeout(() => this.emitResult(), 30)
        if (selectedMode === 'denied') window.setTimeout(() => this.onerror?.({ error: 'not-allowed', message: '' }), 30)
        if (selectedMode === 'standalone-error') window.setTimeout(() => this.onerror?.({ error: 'service-not-allowed', message: '' }), 30)
      }
      stop() { if (selectedMode === 'on-stop') this.emitResult(); this.onend?.() }
      abort() {}
      emitResult() {
        this.onresult?.({ resultIndex: 0, results: { 0: { 0: { transcript: 'ふわふわ', confidence: 1 }, isFinal: true, length: 1 }, length: 1 } })
      }
    }
    Object.defineProperty(window, 'SpeechRecognition', { configurable: true, value: FakeSpeechRecognition })
    Object.defineProperty(window, 'webkitSpeechRecognition', { configurable: true, value: undefined })
    if (selectedMode === 'standalone-error') {
      Object.defineProperty(navigator, 'userAgent', { configurable: true, value: 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X)' })
      Object.defineProperty(navigator, 'standalone', { configurable: true, value: true })
    }
  }, mode)
}
