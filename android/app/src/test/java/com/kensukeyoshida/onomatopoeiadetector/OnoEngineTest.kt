package com.kensukeyoshida.onomatopoeiadetector

import com.kensukeyoshida.onomatopoeiadetector.engine.OnoEngine
import com.kensukeyoshida.onomatopoeiadetector.model.OnomatopoeiaEntry
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * iOS 版 `OnoEngineTests` / Web 版 `onoEngine.test.ts` と同じ期待値で評価の一致を確かめる。
 */
class OnoEngineTest {

    private val engine: OnoEngine by lazy {
        val path = System.getProperty("dictionary.path")
            ?: "../../shared/onomatopoeia_dict.json"
        val entries = Json { ignoreUnknownKeys = true }
            .decodeFromString<List<OnomatopoeiaEntry>>(File(path).readText())
        OnoEngine(entries)
    }

    @Test
    fun `normalizes katakana, spaces and punctuation`() {
        assertEquals("どきどき", OnoEngine.normalize(" ドキ、ドキ！ "))
    }

    @Test
    fun `awards an exact dictionary match five stars`() = runBlocking {
        val result = engine.evaluate("しーん")
        assertEquals(5, result.score)
        assertEquals(3, result.similarEntries.size)
    }

    @Test
    fun `normalizes katakana dictionary entries before matching`() = runBlocking {
        val result = engine.evaluate("ドキドキ")
        assertEquals(5, result.score)
        assertEquals("ドキドキ", result.similarEntries.first().entry.word)
    }

    @Test
    fun `scores a known repeating word above a plain phrase`() = runBlocking {
        val onomatopoeia = engine.evaluate("ふわふわ")
        val phrase = engine.evaluate("ごはんをたべる")
        assertEquals(5, onomatopoeia.score)
        assertTrue(onomatopoeia.score > phrase.score)
    }

    @Test
    fun `returns similarity results in descending order`() {
        val results = engine.findSimilar(OnoEngine.normalize("ふわふわ"))
        assertEquals(3, results.size)
        assertEquals("ふわふわ", results.first().entry.word)
        assertTrue(results[0].similarity >= results[1].similarity)
    }

    @Test
    fun `keeps similar entries hidden for a low score`() = runBlocking {
        val result = engine.evaluate("ごはんをたべる")
        assertTrue(result.score < 3)
        assertTrue(result.similarEntries.isEmpty())
    }

    /**
     * iOS 版・Web 版と同じ点数になることを確かめる。3 実装で同じ表を持ち、
     * どれかの評価だけが動いたときに気付けるようにしている。
     */
    @Test
    fun `matches the score table shared with the other platforms`() = runBlocking {
        val expected = mapOf(
            "ふわふわ" to 5, "しーん" to 5, "ドキドキ" to 5, "どきどき" to 5,
            "キラキラ" to 5, "きらきら" to 5, "ぐるぐる" to 5,
            "ざーざー" to 3, "どーん" to 3,
            "ふわふわと" to 2, "わくわくする" to 2, "ごはんをたべる" to 2
        )
        for ((input, score) in expected) {
            assertEquals("input=$input", score, engine.evaluate(input).score)
        }
    }

    /** 撥音止めのオノマトペは「ん」を助詞と解析されるが、そこで減点しない。 */
    @Test
    fun `does not penalise onomatopoeia ending with a syllabic nasal`() = runBlocking {
        assertEquals(3, engine.evaluate("どーん").score)
        assertEquals(3, engine.evaluate("がーん").score)
    }

    /**
     * 記号だけの認識結果は正規化すると空になり、あらゆる見出しの部分文字列として
     * 一致してしまっていた。1文字も辞書のどこかに必ず含まれる。
     */
    @Test
    fun `does not reward empty or single character input`() = runBlocking {
        for (input in listOf("。", "、。", "ん", "あ", "っ")) {
            val result = engine.evaluate(input)
            assertTrue("input=$input score=${result.score}", result.score < 3)
            assertTrue("input=$input", result.similarEntries.isEmpty())
        }
    }
}
