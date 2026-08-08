package com.kensukeyoshida.onomatopoeiadetector.data

import android.content.Context
import android.util.Log
import com.kensukeyoshida.onomatopoeiadetector.model.OnomatopoeiaEntry
import kotlinx.serialization.json.Json

/**
 * オノマトペ辞書のローダー。辞書は iOS 版と共有している `onomatopoeia_dict.json` を
 * ビルド時に assets へ取り込んだもので、起動時に一度だけ読み込む不変データ。
 */
object DictionaryLoader {

    private const val TAG = "OnoDictionary"
    private const val ASSET_NAME = "onomatopoeia_dict.json"

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cached: List<OnomatopoeiaEntry>? = null

    fun load(context: Context): List<OnomatopoeiaEntry> {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: loadFromAssets(context).also { cached = it }
        }
    }

    private fun loadFromAssets(context: Context): List<OnomatopoeiaEntry> = try {
        val text = context.applicationContext.assets.open(ASSET_NAME)
            .bufferedReader()
            .use { it.readText() }
        json.decodeFromString<List<OnomatopoeiaEntry>>(text).also {
            Log.i(TAG, "Loaded ${it.size} dictionary entries")
        }
    } catch (error: Exception) {
        Log.e(TAG, "Failed to load $ASSET_NAME", error)
        emptyList()
    }
}
