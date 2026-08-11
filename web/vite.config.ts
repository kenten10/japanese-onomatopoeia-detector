import { defineConfig, type Plugin } from 'vitest/config'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'
import { buildHeadersFile, securityHeaders } from './security-headers'

/** ホスティングが読む _headers を、上のヘッダー定義から書き出す。 */
function emitHeadersFile(): Plugin {
  return {
    name: 'emit-headers-file',
    apply: 'build',
    generateBundle() {
      this.emitFile({ type: 'asset', fileName: '_headers', source: buildHeadersFile() })
    }
  }
}

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
        // 単体で開くプライバシーポリシーはアプリ画面に差し替えない
        navigateFallbackDenylist: [/^\/privacy\//],
        cleanupOutdatedCaches: true
      }
    }),
    emitHeadersFile()
  ],
  server: {
    // 共有辞書だけを web の外から読む。リポジトリ全体は開放しない
    fs: { allow: ['.', '../shared'] }
  },
  preview: {
    headers: securityHeaders
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    include: ['src/**/*.test.{ts,tsx}'],
    coverage: { reporter: ['text', 'html'] },
    // ブラウザ版の kuromoji は XHR で辞書を取りに行き jsdom では解決できないため、
    // テストのときだけ Node 版と実辞書に差し替えて形態素解析を通す
    alias: { 'kuromoji/build/kuromoji.js': 'kuromoji' },
    env: { VITE_KUROMOJI_DICT: 'node_modules/kuromoji/dict/' }
  }
})
