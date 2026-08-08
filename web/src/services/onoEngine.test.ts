import { describe, expect, it } from 'vitest'
import { evaluate, findSimilar, normalize } from './onoEngine'

describe('OnoEngine parity', () => {
  it('normalizes katakana, spaces and punctuation', () => {
    expect(normalize(' ドキ、ドキ！ ')).toBe('どきどき')
  })

  it('awards an exact dictionary match five stars', async () => {
    const result = await evaluate('しーん')
    expect(result.score).toBe(5)
    expect(result.similarEntries).toHaveLength(3)
  })

  it('normalizes katakana dictionary entries before matching', async () => {
    const result = await evaluate('ドキドキ')
    expect(result.score).toBe(5)
    expect(result.similarEntries[0].entry.word).toBe('ドキドキ')
  })

  it('scores a known repeating word above a plain phrase', async () => {
    const onomatopoeia = await evaluate('ふわふわ')
    const phrase = await evaluate('ごはんをたべる')
    expect(onomatopoeia.score).toBe(5)
    expect(onomatopoeia.score).toBeGreaterThan(phrase.score)
  })

  it('returns similarity results in descending order', () => {
    const results = findSimilar('ふわふわ')
    expect(results).toHaveLength(3)
    expect(results[0].entry.word).toBe('ふわふわ')
    expect(results[0].similarity).toBeGreaterThanOrEqual(results[1].similarity)
  })

  // iOS 版・Android 版と同じ表。3 実装で同じ点数になることを保証し、
  // どれかの評価だけが動いたときに気付けるようにしている。
  const sharedScores: Array<[string, number]> = [
    ['ふわふわ', 5], ['しーん', 5], ['ドキドキ', 5], ['どきどき', 5],
    ['キラキラ', 5], ['きらきら', 5], ['ぐるぐる', 5],
    ['ざーざー', 3], ['どーん', 3],
    ['ふわふわと', 2], ['わくわくする', 2], ['ごはんをたべる', 2]
  ]

  it.each(sharedScores)('matches the score shared with the other platforms: %s', async (input, score) => {
    expect((await evaluate(input)).score).toBe(score)
  })

  it('does not penalise onomatopoeia ending with a syllabic nasal', async () => {
    expect((await evaluate('どーん')).score).toBe(3)
    expect((await evaluate('がーん')).score).toBe(3)
  })
})
