import { expect, test } from '@playwright/test'

test('shows the three-tab app and changes language', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Onomatopoeia Detector' })).toBeVisible()
  await page.getByRole('button', { name: 'Settings' }).click()
  await page.getByLabel('Display Language').selectOption('ja')
  await expect(page.getByRole('heading', { name: '設定' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'ホーム' })).toBeVisible()
})

test('shows a useful error when speech recognition is unavailable', async ({ page }) => {
  await page.addInitScript(() => {
    Object.defineProperty(window, 'SpeechRecognition', { value: undefined })
    Object.defineProperty(window, 'webkitSpeechRecognition', { value: undefined })
  })
  await page.goto('/')
  await page.getByRole('button', { name: 'Record' }).click()
  await expect(page.getByRole('alertdialog')).toContainText('Speech recognition is unavailable')
})

