import kuromoji from 'kuromoji/build/kuromoji.js'
import type { IpadicFeatures, Tokenizer } from 'kuromoji'

let tokenizerPromise: Promise<Tokenizer<IpadicFeatures>> | undefined

// 配信時は public/dict/ を読む。ユニットテストだけは node 側の実辞書を指すよう
// VITE_KUROMOJI_DICT で差し替える（形態素解析を素通しさせずに検証するため）。
const dicPath = import.meta.env.VITE_KUROMOJI_DICT ?? `${import.meta.env.BASE_URL}dict/`

function tokenizer(): Promise<Tokenizer<IpadicFeatures>> {
  tokenizerPromise ??= new Promise((resolve, reject) => {
    kuromoji.builder({ dicPath }).build((error, built) => {
      if (error) {
        // 失敗した Promise を握り続けると以後ずっと再試行できなくなる
        tokenizerPromise = undefined
        reject(error)
      } else {
        resolve(built)
      }
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
    // 撥音止めのオノマトペ（どーん・がーん など）は「ん」を助詞と解析されるため除く。
    // 音象徴スコア側では撥音止めを加点しており、ここで減点すると評価が食い違う。
    if (tokens.some((token) => token.pos === '助詞' && token.surface_form !== 'ん')) return 0
    if (tokens.some((token) => token.pos === '動詞')) return 0.3
    return 1
  } catch {
    return 1
  }
}
