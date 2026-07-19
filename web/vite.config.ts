import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig({
  base: './',
  plugins: [
    react(),
    VitePWA({
      registerType: 'prompt',
      includeAssets: ['icons/*.png'],
      manifest: {
        name: 'Onomatopoeia Detector',
        short_name: 'オノマトペ判定',
        description: 'Spoken Japanese onomatopoeia detector',
        start_url: '.',
        scope: '.',
        display: 'standalone',
        orientation: 'portrait',
        background_color: '#fbfaf6',
        theme_color: '#fbfaf6',
        categories: ['education', 'utilities'],
        lang: 'en',
        icons: [
          { src: 'icons/icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: 'icons/icon-512.png', sizes: '512x512', type: 'image/png' },
          { src: 'icons/icon-maskable-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' }
        ]
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,json,png,bin,gz,dat}'],
        maximumFileSizeToCacheInBytes: 25 * 1024 * 1024,
        navigateFallback: 'index.html',
        cleanupOutdatedCaches: true
      }
    })
  ],
  server: {
    fs: { allow: ['..'] }
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    include: ['src/**/*.test.{ts,tsx}'],
    coverage: { reporter: ['text', 'html'] }
  }
})
