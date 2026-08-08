import { describe, expect, it } from 'vitest'
import { hiraganaText, kanaOnlyText, katakanaToHiragana, morphemeScore } from './japaneseText'

describe('Japanese text conversion', () => {
  it('converts katakana to hiragana', () => {
    expect(katakanaToHiragana('ドキドキ')).toBe('どきどき')
  })

  it('preserves hiragana pronunciation', async () => {
    expect(await hiraganaText('にほんご')).toBe('にほんご')
  })

  it('corrects the known long-sound transcription', () => {
    expect(kanaOnlyText('シーン')).toBe('しーん')
    expect(kanaOnlyText('しいん')).toBe('しーん')
  })

  it('reads kanji as its hiragana pronunciation', async () => {
    const converted = await hiraganaText('日本語を入力')
    expect(converted).not.toMatch(/\p{Script=Han}/u)
    expect(converted).toMatch(/にほんご|にっぽんご/)
    expect(converted).toContain('にゅうりょく')
  })

  it('scores morphemes by independence', async () => {
    expect(await morphemeScore('ふわふわ')).toBe(1)
    expect(await morphemeScore('わくわくする')).toBe(0.3)
    expect(await morphemeScore('ごはんをたべる')).toBe(0)
    expect(await morphemeScore('ふわふわと')).toBe(0)
  })

  // 撥音止めのオノマトペは「ん」を助詞と解析されるが、そこで減点しない
  it('ignores the syllabic nasal parsed as a particle', async () => {
    expect(await morphemeScore('どーん')).toBe(1)
    expect(await morphemeScore('がーん')).toBe(1)
  })
})

