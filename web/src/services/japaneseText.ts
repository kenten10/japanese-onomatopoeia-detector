import kuromoji from 'kuromoji/build/kuromoji.js'
import type { IpadicFeatures, Tokenizer } from 'kuromoji'

let tokenizerPromise: Promise<Tokenizer<IpadicFeatures>> | undefined

function tokenizer(): Promise<Tokenizer<IpadicFeatures>> {
  tokenizerPromise ??= new Promise((resolve, reject) => {
    kuromoji.builder({ dicPath: `${import.meta.env.BASE_URL}dict/` }).build((error, built) => {
      if (error) reject(error)
      else resolve(built)
    })
  })
  return tokenizerPromise
}

export function katakanaToHiragana(text: string): string {
  return Array.from(text, (character) => {
    const value = character.codePointAt(0) ?? 0
    return value >= 0x30a1 && value <= 0x30f6
      ? String.fromCodePoint(value - 0x60)
      : character
  }).join('')
}

export function correctLongSounds(text: string): string {
  return text.replaceAll('しいん', 'しーん')
}

export function kanaOnlyText(text: string): string {
  return correctLongSounds(katakanaToHiragana(text).replace(/\s/gu, ''))
}

export async function hiraganaText(text: string): Promise<string> {
  if (!/[\p{Script=Han}]/u.test(text)) return kanaOnlyText(text)
  try {
    const built = await tokenizer()
    const reading = built.tokenize(text).map((token) => token.reading && token.reading !== '*'
      ? token.reading
      : token.surface_form).join('')
    return kanaOnlyText(reading)
  } catch {
    return kanaOnlyText(text)
  }
}

export async function morphemeScore(text: string): Promise<number> {
  try {
    const built = await tokenizer()
    const tokens = built.tokenize(text)
    if (tokens.some((token) => token.pos === '助詞')) return 0
    if (tokens.some((token) => token.pos === '動詞')) return 0.3
    return 1
  } catch {
    return 1
  }
}
