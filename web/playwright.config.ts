import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  webServer: { command: 'npm run dev -- --host 127.0.0.1 --port 4173', port: 4173, reuseExistingServer: true },
  use: { baseURL: 'http://127.0.0.1:4173', trace: 'on-first-retry' },
  projects: [{ name: 'iPhone', use: { ...devices['iPhone 13'] } }, { name: 'Desktop Chrome', use: { ...devices['Desktop Chrome'] } }]
})
