// kuromoji の辞書と告知類を public/ へ配置する（postinstall で実行）。
//
// IPADIC（mecab-ipadic-2.7.0-20070801）は再頒布時に著作権表示と免責条項の同梱を求め、
// kuromoji 本体は Apache-2.0 のため NOTICE の再頒布義務がある。辞書だけでなく
// 告知ファイルも配信物へ含めることで、dist/ をそのまま公開しても表示義務を満たす。
import { cp, mkdir } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const kuromoji = path.join(root, 'node_modules', 'kuromoji')

await mkdir(path.join(root, 'public', 'dict'), { recursive: true })
await cp(path.join(kuromoji, 'dict'), path.join(root, 'public', 'dict'), {
  recursive: true,
  force: true
})

const licenses = path.join(root, 'public', 'licenses')
await mkdir(licenses, { recursive: true })
await cp(path.join(kuromoji, 'NOTICE.md'), path.join(licenses, 'kuromoji-NOTICE.md'), { force: true })
await cp(path.join(kuromoji, 'LICENSE-2.0.txt'), path.join(licenses, 'kuromoji-LICENSE-2.0.txt'), {
  force: true
})
