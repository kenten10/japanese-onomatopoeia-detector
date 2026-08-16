package com.kensukeyoshida.onomatopoeiadetector

import android.speech.SpeechRecognizer
import com.kensukeyoshida.onomatopoeiadetector.speech.ErrorAction
import com.kensukeyoshida.onomatopoeiadetector.speech.RecognitionErrorPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class RecognitionErrorPolicyTest {

    private fun decide(
        error: Int,
        usingOnDevice: Boolean = true,
        isStopping: Boolean = false,
        didFallBackToService: Boolean = false,
        hasPartialText: Boolean = false
    ) = RecognitionErrorPolicy.decide(
        error, usingOnDevice, isStopping, didFallBackToService, hasPartialText
    )

    /** 端末上に日本語モデルが無いだけなら、通常の音声認識サービスで録り直す。 */
    @Test
    fun `switches to the service when the on-device language is missing`() {
        assertEquals(ErrorAction.SwitchToService, decide(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE))
        assertEquals(ErrorAction.SwitchToService, decide(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED))
    }

    /**
     * 停止操作の後に切り替えると、利用者は止めたつもりなのに録音がやり直され、
     * 画面は「認識中」のまま最大で上限時間ぶん待たされる。
     */
    @Test
    fun `does not switch after the user has stopped`() {
        assertEquals(
            ErrorAction.FinalizeWithPartial,
            decide(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE, isStopping = true, hasPartialText = true)
        )
        assertEquals(
            ErrorAction.Fail,
            decide(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE, isStopping = true, hasPartialText = false)
        )
    }

    /** 切り替えは一度だけ。二度目は素直に失敗として扱う。 */
    @Test
    fun `switches only once`() {
        assertEquals(
            ErrorAction.Fail,
            decide(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE, didFallBackToService = true)
        )
    }

    /** 端末上のエンジンを使っていないなら切り替え先が無い。 */
    @Test
    fun `does not switch when already using the service`() {
        assertEquals(
            ErrorAction.Fail,
            decide(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE, usingOnDevice = false)
        )
    }

    /** 無音や打ち切りは異常ではないので、確定処理へ進める。 */
    @Test
    fun `treats silence and cancellation as a normal ending`() {
        for (code in listOf(
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
            SpeechRecognizer.ERROR_CLIENT
        )) {
            assertEquals("code=$code", ErrorAction.FinalizeWithPartial, decide(code))
        }
    }

    /** 途中まで聞き取れていれば、それを活かして結果を出す。 */
    @Test
    fun `keeps what was heard when an error interrupts recognition`() {
        assertEquals(
            ErrorAction.FinalizeWithPartial,
            decide(SpeechRecognizer.ERROR_NETWORK, hasPartialText = true)
        )
        assertEquals(
            ErrorAction.Fail,
            decide(SpeechRecognizer.ERROR_NETWORK, hasPartialText = false)
        )
    }

    /** 権限が無いなど、続けようのないものは失敗として伝える。 */
    @Test
    fun `fails on errors that cannot be recovered from`() {
        assertEquals(
            ErrorAction.Fail,
            decide(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
        )
        assertEquals(
            ErrorAction.Fail,
            decide(SpeechRecognizer.ERROR_AUDIO)
        )
    }
}
