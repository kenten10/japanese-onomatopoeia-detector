package com.kensukeyoshida.onomatopoeiadetector.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

// MARK: - Dictionary Entry

@Serializable
data class OnomatopoeiaEntry(
    val word: String,
    val reading: String,
    @SerialName("meaning_ja") val meaningJa: String,
    @SerialName("meaning_en") val meaningEn: String,
    @SerialName("example_ja") val exampleJa: String,
    @SerialName("example_en") val exampleEn: String,
    val category: String
)

// MARK: - Evaluation Result

data class SimilarEntry(
    val entry: OnomatopoeiaEntry,
    val similarity: Double
)

data class EvaluationResult(
    val id: String = UUID.randomUUID().toString(),
    val inputText: String,
    /** 1〜5 */
    val score: Int,
    val similarEntries: List<SimilarEntry>,
    val date: Long
)

// MARK: - History Item (Room mapped)

data class HistoryItem(
    val id: String,
    val inputText: String,
    val score: Int,
    val date: Long
)

// MARK: - Recording State

sealed interface RecordingState {
    data object Idle : RecordingState
    data object Recording : RecordingState
    data object Recognizing : RecordingState
    data object Evaluating : RecordingState
    data class Result(val result: EvaluationResult) : RecordingState

    /** 表示中に言語が切り替わっても追従できるよう、文字列は解決せず ID のまま持つ。 */
    data class Error(val messageRes: Int, val permissionsDenied: Boolean = false) : RecordingState
}

// MARK: - App Language

enum class AppLanguage(val code: String) {
    SYSTEM("system"),
    JAPANESE("ja"),
    ENGLISH("en");

    companion object {
        /** 英語学習者向けアプリのため、既定の表示言語は英語。 */
        val defaultLanguage: AppLanguage = ENGLISH

        fun from(raw: String?): AppLanguage =
            entries.firstOrNull { it.code == raw } ?: defaultLanguage
    }
}
