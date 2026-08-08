import dictionaryJson from '../../../OnomatopoeiaDetector/Resources/onomatopoeia_dict.json'
import type { EvaluationResult, OnomatopoeiaEntry, SimilarEntry } from '../types'
import { morphemeScore } from './japaneseText'

const dictionary = dictionaryJson as OnomatopoeiaEntry[]

export function normalize(text: string): string {
  return Array.from(text, (character) => {
    const value = character.codePointAt(0) ?? 0
    return value >= 0x30a1 && value <= 0x30f6 ? String.fromCodePoint(value - 0x60) : character
  }).join('').replace(/[\p{White_Space}\p{P}]/gu, '')
}

/** 辞書と部分一致で照合する最小の文字数。 */
const MIN_MATCH_LENGTH = 2

const normalizedDictionary = dictionary.map((entry) => ({
  entry,
  word: normalize(entry.word),
  reading: normalize(entry.reading)
}))

function exactMatch(text: string): boolean {
  return normalizedDictionary.some(({ word, reading }) => word === text || reading === text)
}

function dictionaryScore(text: string): number {
  if (exactMatch(text)) return 1
  // 1文字以下は語として照合しない。空文字はあらゆる見出しの部分文字列になり、
  // 「ん」「ー」のような1文字も辞書のどこかに必ず含まれてしまうため。
  if (text.length < MIN_MATCH_LENGTH) return 0
  if (normalizedDictionary.some(({ word }) => text.includes(word) || word.includes(text))) return 0.7
  return normalizedDictionary.some(({ reading }) => text.includes(reading) || reading.includes(text)) ? 0.4 : 0
}

function phoneticPatternScore(text: string): number {
  const chars = Array.from(text)
  const length = chars.length
  if (length < 2) return 0
  if (length >= 4 && length % 2 === 0 && chars.slice(0, length / 2).join('') === chars.slice(length / 2).join('')) return 1
  if (length === 3 && chars[1] === chars[2]) return 0.8
  if (chars.some((character) => 'っーん'.includes(character))) return 0.5
  if (length === 4 && chars[1] === chars[3]) return 0.6
  return 0.1
}

function soundSymbolScore(text: string): number {
  const chars = Array.from(text)
  const voiced = new Set(Array.from('がぎぐげござじずぜぞだぢづでどばびぶべぼ'))
  let score = Math.min((chars.filter((character) => voiced.has(character)).length / Math.max(chars.length, 1)) * 1.5, 0.4)
  if (/[ぱぴぷぺぽ]/u.test(text)) score += 0.3
  if (text.includes('ー')) score += 0.2
  if (text.includes('っ')) score += 0.2
  if (text.endsWith('ん')) score += 0.1
  return Math.min(score, 1)
}

function levenshtein(a: string[], b: string[]): number {
  const previous = Array.from({ length: b.length + 1 }, (_, index) => index)
  for (let i = 1; i <= a.length; i += 1) {
    const current = [i]
    for (let j = 1; j <= b.length; j += 1) {
      current[j] = a[i - 1] === b[j - 1]
        ? previous[j - 1]
        : 1 + Math.min(previous[j], current[j - 1], previous[j - 1])
    }
    previous.splice(0, previous.length, ...current)
  }
  return previous[b.length]
}

function similarity(a: string, b: string): number {
  const charsA = Array.from(a)
  const charsB = Array.from(b)
  const maxLength = Math.max(charsA.length, charsB.length)
  const edit = maxLength === 0 ? 1 : 1 - levenshtein(charsA, charsB) / maxLength
  const setA = new Set(charsA)
  const setB = new Set(charsB)
  const union = new Set([...setA, ...setB]).size
  const overlap = union === 0 ? 0 : [...setA].filter((character) => setB.has(character)).length / union
  return edit * 0.6 + overlap * 0.4
}

export function findSimilar(text: string, top = 3): SimilarEntry[] {
  return normalizedDictionary.map(({ entry, word }) => ({ entry, similarity: similarity(text, word) }))
    .sort((a, b) => b.similarity - a.similarity)
    .slice(0, top)
}

export async function evaluate(text: string): Promise<EvaluationResult> {
  const normalized = normalize(text)
  let score: number
  if (exactMatch(normalized)) {
    score = 5
  } else {
    const raw = dictionaryScore(normalized) * 0.4
      + phoneticPatternScore(normalized) * 0.3
      + soundSymbolScore(normalized) * 0.2
      + await morphemeScore(normalized) * 0.1
    score = Math.max(1, Math.min(5, Math.round(raw * 4) + 1))
  }
  return {
    id: crypto.randomUUID(), inputText: text, score,
    similarEntries: score >= 3 ? findSimilar(normalized) : [], date: new Date()
  }
}
