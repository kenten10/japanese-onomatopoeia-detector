import { describe, expect, it } from 'vitest'
import { hiraganaText, kanaOnlyText, katakanaToHiragana } from './japaneseText'

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
})

