package com.kensukeyoshida.onomatopoeiadetector.speech

import android.annotation.SuppressLint
import android.speech.SpeechRecognizer

/** 認識エラーを受けたときに何をするか。 */
enum class ErrorAction {
    /** 端末上のエンジンをあきらめ、通常の音声認識サービスで録り直す。 */
    SwitchToService,

    /** 認識途中のテキストを確定として扱う（無ければキャンセル扱い）。 */
    FinalizeWithPartial,

    /** 続けられないエラーとして利用者に伝える。 */
    Fail
}

/**
 * エラーコードと録音の状況から次の動きを決める。
 *
 * 実際の録音を伴わずに確かめられるよう、判断だけを切り出している。
 */
object RecognitionErrorPolicy {

    // API 31 以降の定数だが、値の比較にしか使わないため古い端末でも安全
    @SuppressLint("InlinedApi")
    private val languageUnavailableCodes = setOf(
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED
    )

    /** 無音や打ち切りとして扱うコード。異常ではないので確定処理へ進める。 */
    private val benignCodes = setOf(
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
        SpeechRecognizer.ERROR_CLIENT
    )

    fun decide(
        error: Int,
        usingOnDevice: Boolean,
        isStopping: Boolean,
        didFallBackToService: Boolean,
        hasPartialText: Boolean
    ): ErrorAction {
        // 端末上の日本語モデルが無いだけなら、通常のサービスへ一度だけ切り替える。
        // ただし停止操作の後は録り直さず、そのまま確定処理へ進む。
        if (usingOnDevice && error in languageUnavailableCodes && !didFallBackToService && !isStopping) {
            return ErrorAction.SwitchToService
        }
        if (error in benignCodes) return ErrorAction.FinalizeWithPartial
        return if (hasPartialText) ErrorAction.FinalizeWithPartial else ErrorAction.Fail
    }
}
