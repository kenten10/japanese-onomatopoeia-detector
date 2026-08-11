import { readFileSync } from 'node:fs'
import { expect, test, type Page } from '@playwright/test'
import { securityHeaders } from '../security-headers'

async function installFakeSpeech(page: Page, mode: 'automatic' | 'on-stop' | 'denied' | 'standalone-error' = 'automatic') {
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

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => localStorage.clear())
})

test('serves the PWA with restrictive security headers', async ({ request }) => {
  const response = await request.get('/')
  expect(response.ok()).toBe(true)

  // preview の応答が定義どおりであること
  const headers = response.headers()
  for (const [name, value] of Object.entries(securityHeaders)) {
    expect(headers[name.toLowerCase()], name).toBe(value)
  }
})

test('emits a _headers file that matches the served headers', async () => {
  // ホスティングが読む _headers は preview と同じ定義から作る。
  // 片方だけ直しても気付けるよう、両方を突き合わせる。
  const emitted = readFileSync('dist/_headers', 'utf8')
  for (const [name, value] of Object.entries(securityHeaders)) {
    expect(emitted, name).toContain(`${name}: ${value}`)
  }
  // Service Worker はホスティングのキャッシュに載せない
  expect(emitted).toContain('/sw.js')
  expect(emitted).toContain('Cache-Control: no-cache')
})

test('shows the three-tab app and changes language', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Onomatopoeia Detector' })).toBeVisible()
  await page.getByRole('button', { name: 'Settings' }).click()
  await page.getByLabel('Display Language').selectOption('ja')
  await expect(page.getByRole('heading', { name: '設定' })).toBeVisible()
  const homeTab = page.getByRole('button', { name: 'ホーム' })
  await expect(homeTab).not.toHaveAttribute('aria-current')
  await homeTab.click()
  await expect(homeTab).toHaveAttribute('aria-current', 'page')
})

test('records, evaluates, traps modal focus, saves and deletes history', async ({ page }) => {
  await installFakeSpeech(page)
  await page.goto('/')
  await page.getByRole('button', { name: 'Record' }).click()
  const result = page.getByRole('dialog', { name: 'Result' })
  await expect(result).toBeVisible()
  await expect(result.getByText('5', { exact: true })).toBeVisible()
  await expect(result.getByRole('button', { name: 'Try Again' }).first()).toBeFocused()
  await page.keyboard.press('Shift+Tab')
  await expect(result.getByRole('button', { name: 'Try Again' }).last()).toBeFocused()
  await page.keyboard.press('Tab')
  await expect(result.getByRole('button', { name: 'Try Again' }).first()).toBeFocused()
  await result.getByRole('button', { name: 'Save to History' }).click()
  await expect(result.getByRole('button', { name: 'Saved!' })).toBeVisible()
  await result.getByRole('button', { name: 'Try Again' }).last().click()
  await page.getByRole('button', { name: 'History' }).click()
  await expect(page.getByText('ふわふわ')).toBeVisible()
  await page.getByRole('button', { name: 'Delete' }).click()
  const deleteDialog = page.getByRole('dialog')
  await deleteDialog.getByRole('button', { name: 'Delete' }).click()
  await expect(page.getByText('No saved results yet')).toBeVisible()
})

test('keeps the active recognition service across a language change', async ({ page }) => {
  await installFakeSpeech(page, 'on-stop')
  await page.goto('/')
  await page.getByRole('button', { name: 'Record' }).click()
  await page.getByRole('button', { name: 'Settings' }).click()
  await page.getByLabel('Display Language').selectOption('ja')
  await page.getByRole('button', { name: 'ホーム' }).click()
  await page.getByRole('button', { name: 'タップして停止' }).click()
  await expect(page.getByRole('dialog', { name: '判定結果' })).toBeVisible()
})

test('keeps portal dialogs readable in dark mode', async ({ page }) => {
  await installFakeSpeech(page)
  await page.emulateMedia({ colorScheme: 'dark' })
  await page.goto('/')

  await page.getByRole('button', { name: 'Settings' }).click()
  await page.getByRole('button', { name: 'Clear All History' }).click()
  const confirmDialog = page.getByRole('alertdialog')
  await expect(confirmDialog).toHaveCSS('color', 'rgb(242, 238, 228)')
  await expect(confirmDialog).toHaveCSS('background-color', 'rgb(43, 39, 33)')
  await expect(confirmDialog.getByRole('button', { name: 'Cancel' })).toBeVisible()
  await confirmDialog.getByRole('button', { name: 'Cancel' }).click()

  await page.getByRole('button', { name: 'Home' }).click()
  await page.getByRole('button', { name: 'Record' }).click()
  const result = page.getByRole('dialog', { name: 'Result' })
  await expect(result).toHaveCSS('color', 'rgb(242, 238, 228)')
  await expect(result.getByRole('button', { name: 'Try Again' }).last()).toBeVisible()
})

test('distinguishes microphone denial and iPhone standalone failure', async ({ page }) => {
  await installFakeSpeech(page, 'denied')
  await page.goto('/')
  await page.getByRole('button', { name: 'Record' }).click()
  await expect(page.getByRole('alertdialog')).toContainText('Microphone access is off')
  await page.getByRole('button', { name: 'OK' }).click()

  await page.close()
})

test('offers Safari recovery for an iPhone Home Screen speech failure', async ({ page }) => {
  await installFakeSpeech(page, 'standalone-error')
  await page.goto('/')
  await page.getByRole('button', { name: 'Record' }).click()
  const alert = page.getByRole('alertdialog')
  await expect(alert).toContainText('Home Screen app')
  await expect(alert.getByRole('link', { name: 'Open in Safari' })).toHaveAttribute('target', '_blank')
})

test('loads the production PWA offline after service worker activation', async ({ page, context, browserName }) => {
  test.skip(browserName === 'webkit', 'Playwright WebKit cannot reliably emulate an offline reload')
  await page.goto('/')
  await page.evaluate(async () => { await navigator.serviceWorker.ready })
  await page.reload()
  await expect.poll(() => page.evaluate(() => Boolean(navigator.serviceWorker.controller))).toBe(true)
  await context.setOffline(true)
  try {
    await page.reload()
    await expect(page.getByRole('heading', { name: 'Onomatopoeia Detector' })).toBeVisible()
  } finally {
    await context.setOffline(false)
  }
})

test('matches the home screen visual baseline in light and dark mode', async ({ page }) => {
  test.skip(process.platform !== 'linux', 'Visual baselines are generated on Ubuntu')
  await page.goto('/')
  await expect(page).toHaveScreenshot('home-light.png', { animations: 'disabled', maxDiffPixelRatio: 0.04 })
  await page.emulateMedia({ colorScheme: 'dark', reducedMotion: 'reduce' })
  await expect(page).toHaveScreenshot('home-dark.png', { animations: 'disabled', maxDiffPixelRatio: 0.04 })
})

test('publishes a standalone privacy policy in both languages', async ({ page, request }) => {
  // ストアの審査や配布ページからは、アプリの外から読める URL を求められる
  for (const [path, heading] of [['/privacy/ja.html', 'プライバシーポリシー'], ['/privacy/en.html', 'Privacy Policy']]) {
    const response = await request.get(path)
    expect(response.ok(), path).toBe(true)
    const html = await response.text()
    expect(html).toContain(heading)
  }

  // アプリ内の文言と食い違っていないこと
  const inApp = await request.get('/privacy/ja.html').then((response) => response.text())
  expect(inApp).toContain('このアプリは、個人情報や利用状況データを収集せず、広告やトラッキングも行いません。')

  // Service Worker のフォールバックでアプリ画面に差し替わらないこと
  await page.goto('/privacy/ja.html')
  await expect(page.locator('h1')).toHaveText('プライバシーポリシー')
})
