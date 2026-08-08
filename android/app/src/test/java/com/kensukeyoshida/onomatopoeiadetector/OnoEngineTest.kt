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
            ?: "../../OnomatopoeiaDetector/Resources/onomatopoeia_dict.json"
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
}
