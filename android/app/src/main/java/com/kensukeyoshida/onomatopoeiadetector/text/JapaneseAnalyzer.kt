package com.kensukeyoshida.onomatopoeiadetector.text

import android.util.Log
import com.atilika.kuromoji.ipadic.Token
import com.atilika.kuromoji.ipadic.Tokenizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 日本語の読み推定と品詞判定。
 *
 * 音声認識の結果は漢字混じりで返るため、表示・評価・履歴に渡す前にひらがなへ変換する。
 * iOS 版の `SpeechTextConverter` / `NLTagger` に対応し、辞書は Web 版と同じ IPADIC を使う。
 */
object JapaneseAnalyzer {

    private const val TAG = "OnoJapanese"

    private val tokenizer: Tokenizer? by lazy {
        try {
            Tokenizer()
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to build tokenizer", error)
            null
        }
    }

    /** 辞書の読み込みは数百ミリ秒かかるため、録音開始前に暖機しておく。 */
    suspend fun warmUp() = withContext(Dispatchers.Default) {
        tokenizer
        Unit
    }

    // MARK: - Kana helpers

    fun katakanaToHiragana(text: String): String = buildString(text.length) {
        for (character in text) {
            val code = character.code
            if (code in 0x30A1..0x30F6) append((code - 0x60).toChar()) else append(character)
        }
    }

    /** オノマトペとして明確な長音誤表記を補正する（例: しいん → しーん）。 */
    fun correctLongSounds(text: String): String = text.replace("しいん", "しーん")

    fun kanaOnlyText(text: String): String =
        correctLongSounds(katakanaToHiragana(text).filterNot { it.isWhitespace() })

    private fun containsKanji(text: String): Boolean =
        text.any { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN }

    // MARK: - Reading

    /** 漢字を含む場合は形態素解析の読みに置き換えたうえで、全体をひらがなへ揃える。 */
    suspend fun hiraganaText(text: String): String = withContext(Dispatchers.Default) {
        if (!containsKanji(text)) return@withContext kanaOnlyText(text)
        val tokens = tokenize(text) ?: return@withContext kanaOnlyText(text)
        val reading = tokens.joinToString("") { token ->
            token.reading.takeIf { !it.isNullOrEmpty() && it != "*" } ?: token.surface
        }
        kanaOnlyText(reading)
    }

    // MARK: - Morpheme score

    /** 助詞を含めば 0、動詞を含めば 0.3、どちらも無い独立語なら 1.0。 */
    suspend fun morphemeScore(text: String): Double = withContext(Dispatchers.Default) {
        val tokens = tokenize(text) ?: return@withContext 1.0
        when {
            // 撥音止めのオノマトペ（どーん・がーん など）は「ん」を助詞と解析されるため除く。
            // 音象徴スコア側では撥音止めを加点しており、ここで減点すると評価が食い違う。
            tokens.any { it.partOfSpeechLevel1 == "助詞" && it.surface != "ん" } -> 0.0
            tokens.any { it.partOfSpeechLevel1 == "動詞" } -> 0.3
            else -> 1.0
        }
    }

    private fun tokenize(text: String): List<Token>? = try {
        tokenizer?.tokenize(text)
    } catch (error: Throwable) {
        Log.e(TAG, "tokenize failed", error)
        null
    }
}
