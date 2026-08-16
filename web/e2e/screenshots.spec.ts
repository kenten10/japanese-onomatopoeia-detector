import { mkdirSync } from 'node:fs'
import { expect, test, type Page } from '@playwright/test'
import { messages } from '../src/i18n/messages'
import { installFakeSpeech } from './fakeSpeech'

/**
 * README に載せる画面の写しを撮る。
 *
 * 通常のテストと違い、これは成果物を作るための実行なので CI では走らせない。
 * `npm run screenshots` から、iPhone 幅・ライト/ダーク・日英の組み合わせで撮る。
 * 音声認識は e2e と同じ偽物に差し替えるため、実機もマイクも要らない。
 */

const OUTPUT_DIR = '../docs/screenshots'

test.describe('screenshots', () => {
  // 撮影は明示したときだけ。既定の e2e 実行に混ぜると、CI が成果物を作りにいってしまう
  // 撮るのは端末の幅だけ。デスクトップ幅は README に載せる絵として横に間延びする。
  // 対象の絞り込みは npm script 側の --project=iPhone で行う
  test.skip(!process.env.CAPTURE_SCREENSHOTS, 'set CAPTURE_SCREENSHOTS=1 to capture')

  for (const locale of ['ja', 'en'] as const) {
    for (const theme of ['light', 'dark'] as const) {
      test(`${locale}-${theme}`, async ({ page }) => {
        mkdirSync(OUTPUT_DIR, { recursive: true })
        const t = (key: keyof typeof messages.en) => messages[locale][key]
        const pathFor = (name: string) => `${OUTPUT_DIR}/${name}-${locale}-${theme}.png`
        const shoot = (name: string) => page.screenshot({ path: pathFor(name), fullPage: true })

        await page.emulateMedia({ colorScheme: theme })
        await installFakeSpeech(page)
        await page.goto('/')

        // 表示言語を決める。既定は英語なので、日本語のときだけ切り替える
        if (locale === 'ja') {
          await page.getByRole('button', { name: messages.en['settings.title'] }).click()
          await page.getByLabel(messages.en['settings.language']).selectOption('ja')
          await page.getByRole('button', { name: t('home.tab') }).click()
        }

        // 1. ホーム（録音待機）
        await expect(page.getByRole('button', { name: t('home.record.button') })).toBeVisible()
        await shoot('1-home')

        // 2. 判定結果。偽の音声認識が「ふわふわ」を返すため、評価 5 の画面になる
        await page.getByRole('button', { name: t('home.record.button') }).click()
        const result = page.getByRole('dialog', { name: t('result.title') })
        await expect(result.getByText('5', { exact: true })).toBeVisible()
        // 星は 120ms 間隔で 1 つずつ灯る。撮るのは 5 つ目が灯りきってから
        await expect(result.locator('.stars svg[fill="currentColor"]')).toHaveCount(5)
        // この画面だけは端末の高さに収まらない。ダイアログは画面の高さに合わせて伸縮し、
        // あふれたぶんは内側でスクロールするため、そのまま撮ると下端が切れる。
        // 画面をいったん大きく広げて中身を出しきり、ダイアログの矩形だけを切り出す。
        const viewport = page.viewportSize()!
        await page.setViewportSize({ ...viewport, height: 3200 })
        const box = await result.boundingBox()
        await page.screenshot({ path: pathFor('2-result'), clip: box ?? undefined })
        await page.setViewportSize(viewport)

        // 3. 履歴。1 件保存してから開く
        await result.getByRole('button', { name: t('result.save') }).click()
        await expect(result.getByRole('button', { name: t('result.saved') })).toBeVisible()
        await result.getByRole('button', { name: t('result.again') }).last().click()
        await page.getByRole('button', { name: t('history.title') }).click()
        await expect(page.getByText('ふわふわ')).toBeVisible()
        await shoot('3-history')

        // 4. 設定
        await page.getByRole('button', { name: t('settings.title') }).click()
        await expect(page.getByLabel(t('settings.language'))).toBeVisible()
        await shoot('4-settings')
      })
    }
  }
})
