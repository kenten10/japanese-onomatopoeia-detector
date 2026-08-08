package com.kensukeyoshida.onomatopoeiadetector

import com.kensukeyoshida.onomatopoeiadetector.text.JapaneseAnalyzer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * iOS 版 `SpeechTextConverterTests` と同じ期待値で、音声認識結果のひらがな化を確かめる。
 */
class JapaneseAnalyzerTest {

    private fun String.containsKanji(): Boolean = any { character ->
        val code = character.code
        code in 0x4E00..0x9FFF || code in 0x3400..0x4DBF || code in 0xF900..0xFAFF
    }

    @Test
    fun `converts katakana to hiragana`() = runBlocking {
        assertEquals("どきどき", JapaneseAnalyzer.hiraganaText("ドキドキ"))
    }

    @Test
    fun `converts recognized japanese text to a hiragana reading`() = runBlocking {
        val converted = JapaneseAnalyzer.hiraganaText("日本語を入力")

        assertFalse(converted.containsKanji())
        assertTrue(converted.contains("にほんご") || converted.contains("にっぽんご"))
        assertTrue(converted.contains("にゅうりょく"))
    }

    @Test
    fun `preserves received hiragana pronunciation`() = runBlocking {
        assertEquals("にほんご", JapaneseAnalyzer.hiraganaText("にほんご"))
        assertEquals("にっぽんご", JapaneseAnalyzer.hiraganaText("にっぽんご"))
    }

    @Test
    fun `corrects onomatopoeia long sound written as vowel kana`() = runBlocking {
        assertEquals("しーん", JapaneseAnalyzer.hiraganaText("しいん"))
        assertEquals("しーん", JapaneseAnalyzer.hiraganaText("シーン"))
    }

    @Test
    fun `scores morphemes by independence`() = runBlocking {
        assertEquals(0.0, JapaneseAnalyzer.morphemeScore("ごはんをたべる"), 0.0001)
        assertEquals(1.0, JapaneseAnalyzer.morphemeScore("ふわふわ"), 0.0001)
    }
}
