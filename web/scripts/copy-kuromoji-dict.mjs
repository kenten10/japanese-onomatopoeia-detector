import { cp, mkdir } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
await mkdir(path.join(root, 'public', 'dict'), { recursive: true })
await cp(path.join(root, 'node_modules', 'kuromoji', 'dict'), path.join(root, 'public', 'dict'), {
  recursive: true,
  force: true
})
