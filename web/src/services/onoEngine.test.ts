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
})

