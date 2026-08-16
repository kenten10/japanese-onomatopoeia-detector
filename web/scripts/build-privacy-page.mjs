// アプリ内のプライバシーポリシーと同じ文言で、単体で開ける HTML を書き出す。
//
// ストアの審査や配布ページからは「アプリの外から読めるURL」を求められる。
// 文言を書き写すと必ずずれるので、アプリが使っている翻訳から生成する。
import { mkdir, writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

// messages.ts は TypeScript なので、値だけを取り出して評価する
const source = await import(path.join(root, 'src/i18n/messages.ts'))
  .then((module) => module.messages)
  .catch(async () => {
    const text = await import('node:fs/promises').then((fs) =>
      fs.readFile(path.join(root, 'src/i18n/messages.ts'), 'utf8'))
    const body = text.slice(text.indexOf('{'), text.lastIndexOf('} as const') + 1)
    return Function(`"use strict"; return (${body})`)()
  })

const escape = (value) => value
  .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

function section(messages, key) {
  return `      <section>
        <h2>${escape(messages[`privacy.${key}.title`])}</h2>
        <p>${escape(messages[`privacy.${key}.body`])}</p>
      </section>`
}

function page(messages, lang, otherLang, otherLabel) {
  return `<!doctype html>
<html lang="${lang}">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <meta name="robots" content="index, follow" />
    <title>${escape(messages['settings.privacy'])} — ${escape(messages['app.title'])}</title>
    <style>
      :root { color-scheme: light dark; --paper: #fbfaf6; --ink: #141414; --vermilion: #d63523; }
      @media (prefers-color-scheme: dark) { :root { --paper: #17150f; --ink: #f2eee4; --vermilion: #e8412e; } }
      body {
        margin: 0 auto; padding: 40px 20px 80px; max-width: 42rem;
        background: var(--paper); color: var(--ink); line-height: 1.7;
        font-family: -apple-system, BlinkMacSystemFont, "Hiragino Sans", "Yu Gothic", sans-serif;
      }
      h1 { font-size: 1.6rem; margin: 0 0 4px; }
      h2 { font-size: 1.1rem; margin: 32px 0 8px; }
      p { margin: 0; }
      .lead { margin: 24px 0 0; }
      .app { font-size: .9rem; opacity: .6; margin: 0 0 24px; }
      a { color: var(--vermilion); }
      footer { margin-top: 48px; font-size: .85rem; opacity: .6; }
    </style>
  </head>
  <body>
    <main>
      <h1>${escape(messages['settings.privacy'])}</h1>
      <p class="app">${escape(messages['app.title'])}</p>
      <p class="lead">${escape(messages['privacy.summary'])}</p>
${section(messages, 'audio')}
${section(messages, 'storage')}
${section(messages, 'diagnostics')}
${section(messages, 'feedback')}
${section(messages, 'control')}
    </main>
    <footer>
      <p><a href="./${otherLang}.html">${otherLabel}</a> · <a href="../">${escape(messages['app.title'])}</a></p>
    </footer>
  </body>
</html>
`
}

const outDir = path.join(root, 'public', 'privacy')
await mkdir(outDir, { recursive: true })
await writeFile(path.join(outDir, 'en.html'), page(source.en, 'en', 'ja', '日本語'))
await writeFile(path.join(outDir, 'ja.html'), page(source.ja, 'ja', 'en', 'English'))

// 言語を選ばずに開いたときは、ブラウザの言語で振り分ける
await writeFile(path.join(outDir, 'index.html'), `<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>${escape(source.en['settings.privacy'])}</title>
    <script>
      location.replace((navigator.language || '').startsWith('ja') ? './ja.html' : './en.html')
    </script>
  </head>
  <body>
    <p><a href="./en.html">English</a> · <a href="./ja.html">日本語</a></p>
  </body>
</html>
`)

console.log('public/privacy/ にプライバシーポリシーを書き出しました')
