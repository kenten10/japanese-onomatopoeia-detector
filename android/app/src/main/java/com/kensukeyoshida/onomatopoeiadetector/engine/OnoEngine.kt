package com.kensukeyoshida.onomatopoeiadetector.engine

import com.kensukeyoshida.onomatopoeiadetector.model.EvaluationResult
import com.kensukeyoshida.onomatopoeiadetector.model.OnomatopoeiaEntry
import com.kensukeyoshida.onomatopoeiadetector.model.SimilarEntry
import com.kensukeyoshida.onomatopoeiadetector.text.JapaneseAnalyzer
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * オノマトペらしさを評価するエンジン。
 *
 * 辞書は初期化時に一度だけ読み込む不変データ。入力は評価前にカタカナをひらがなへ正規化し、
 * 空白・記号を除去する。辞書側の見出し語・読みも同じ正規化を通してから照合する。
 */
class OnoEngine(
    dictionary: List<OnomatopoeiaEntry>,
    private val morphemeScore: suspend (String) -> Double = JapaneseAnalyzer::morphemeScore
) {

    private class NormalizedEntry(
        val entry: OnomatopoeiaEntry,
        val word: String,
        val reading: String
    )

    private val normalizedDictionary = dictionary.map {
        NormalizedEntry(it, normalize(it.word), normalize(it.reading))
    }

    // MARK: - Public API

    suspend fun evaluate(text: String): EvaluationResult {
        val normalized = normalize(text)

        val score = if (isExactMatch(normalized)) {
            5
        } else {
            // 重み付き合計: 40 / 30 / 20 / 10
            val raw = dictionaryScore(normalized) * 0.40 +
                phoneticPatternScore(normalized) * 0.30 +
                soundSymbolScore(normalized) * 0.20 +
                morphemeScore(normalized) * 0.10
            // 1〜5 に正規化。iOS/Web と同じく 0.5 は切り上げる（JVM の round は偶数丸めのため使わない）
            max(1, min(5, floor(raw * 4 + 0.5).toInt() + 1))
        }

        return EvaluationResult(
            inputText = text,
            score = score,
            similarEntries = if (score >= 3) findSimilar(normalized) else emptyList(),
            date = System.currentTimeMillis()
        )
    }

    fun findSimilar(text: String, top: Int = 3): List<SimilarEntry> =
        normalizedDictionary
            .map { SimilarEntry(it.entry, similarity(text, it.word)) }
            .sortedByDescending { it.similarity }
            .take(top)

    // MARK: - Dictionary Score

    private fun isExactMatch(text: String): Boolean =
        normalizedDictionary.any { it.word == text || it.reading == text }

    private fun dictionaryScore(text: String): Double = when {
        isExactMatch(text) -> 1.0
        // 部分一致
        normalizedDictionary.any { text.contains(it.word) || it.word.contains(text) } -> 0.7
        // 読みレベルの部分一致
        normalizedDictionary.any { text.contains(it.reading) || it.reading.contains(text) } -> 0.4
        else -> 0.0
    }

    // MARK: - Phonetic Pattern Score

    /** オノマトペに典型的な ABAB / 畳語などの反復パターンを検出する。 */
    private fun phoneticPatternScore(text: String): Double {
        val chars = text.toList()
        val length = chars.size
        if (length < 2) return 0.0

        // ABAB パターン（例: ふわふわ）
        if (length >= 4 && length % 2 == 0 &&
            chars.subList(0, length / 2) == chars.subList(length / 2, length)
        ) {
            return 1.0
        }

        // 3文字で後半2文字が畳語（例: ぐるる）
        if (length == 3 && chars[1] == chars[2]) return 0.8

        // 促音・長音・撥音（オノマトペの目印）
        if (chars.any { it in "っーん" }) return 0.5

        // 4文字で 2・4文字目が一致（子音反復のヒューリスティック）
        if (length == 4 && chars[1] == chars[3]) return 0.6

        return 0.1
    }

    // MARK: - Sound Symbol Score

    /** 濁音・半濁音・特殊モーラの比率で音象徴性を採点する。 */
    private fun soundSymbolScore(text: String): Double {
        val chars = text.toList()

        // 濁音（重い・丸い印象）
        val voicedRatio = chars.count { it in VOICED }.toDouble() / max(chars.size, 1)
        var score = min(voicedRatio * 1.5, 0.4)

        // 半濁音（軽い・鋭い印象）
        if (chars.any { it in PLOSIVE }) score += 0.3

        if (text.contains("ー")) score += 0.2 // 長音
        if (text.contains("っ")) score += 0.2 // 促音
        if (text.endsWith("ん")) score += 0.1 // 撥音止め

        return min(score, 1.0)
    }

    // MARK: - Similarity Search

    /** 複合類似度: レーベンシュタイン(60%) + 文字集合の重なり(40%) */
    private fun similarity(a: String, b: String): Double =
        levenshteinSimilarity(a, b) * 0.6 + phoneticOverlap(a, b) * 0.4

    private fun levenshteinSimilarity(a: String, b: String): Double {
        val maxLength = max(a.length, b.length)
        if (maxLength == 0) return 1.0
        return 1.0 - levenshtein(a, b).toDouble() / maxLength
    }

    private fun levenshtein(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        var previous = IntArray(n + 1) { it }
        var current = IntArray(n + 1)
        for (i in 1..m) {
            current[0] = i
            for (j in 1..n) {
                current[j] = if (a[i - 1] == b[j - 1]) {
                    previous[j - 1]
                } else {
                    1 + min(min(previous[j], current[j - 1]), previous[j - 1])
                }
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[n]
    }

    private fun phoneticOverlap(a: String, b: String): Double {
        val setA = a.toSet()
        val setB = b.toSet()
        val union = (setA + setB).size
        if (union == 0) return 0.0
        return setA.intersect(setB).size.toDouble() / union
    }

    companion object {
        private const val VOICED = "がぎぐげござじずぜぞだぢづでどばびぶべぼ"
        private const val PLOSIVE = "ぱぴぷぺぽ"

        /** カタカナ→ひらがな正規化＋空白・記号除去 */
        fun normalize(text: String): String = buildString(text.length) {
            for (character in text) {
                val code = character.code
                val normalized = if (code in 0x30A1..0x30F6) (code - 0x60).toChar() else character
                if (!normalized.isWhitespace() && !normalized.isPunctuation()) append(normalized)
            }
        }

        private fun Char.isPunctuation(): Boolean = when (Character.getType(this).toByte()) {
            Character.CONNECTOR_PUNCTUATION,
            Character.DASH_PUNCTUATION,
            Character.START_PUNCTUATION,
            Character.END_PUNCTUATION,
            Character.INITIAL_QUOTE_PUNCTUATION,
            Character.FINAL_QUOTE_PUNCTUATION,
            Character.OTHER_PUNCTUATION -> true
            else -> false
        }
    }
}
