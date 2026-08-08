package com.kensukeyoshida.onomatopoeiadetector.speech

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import com.kensukeyoshida.onomatopoeiadetector.R

/**
 * 音声認識のラッパー。1 回の録音につき、確定テキストかキャンセルのどちらかを一度だけ配送する。
 *
 * 端末上の認識エンジンを優先し、日本語モデルが無い端末では通常の認識サービスへ切り替える。
 * すべてのメソッドはメインスレッドから呼ぶ。
 */
class SpeechManager(private val context: Context) {

    /** 認識途中のテキスト（未変換の生テキスト）。 */
    var onPartial: ((String) -> Unit)? = null

    /** 確定テキストが得られたとき、1 録音につき一度だけ呼ばれる。 */
    var onFinalResult: ((String) -> Unit)? = null

    /** 結果を得ずに録音が終了したとき（無音・キャンセル）、一度だけ呼ばれる。 */
    var onCancelled: (() -> Unit)? = null

    /** 上限時間に達して自動停止したとき、確定を待ち始める前に呼ばれる。 */
    var onAutoStopped: (() -> Unit)? = null

    /** 録音を継続できないエラー。引数は表示する文字列リソース。 */
    var onError: ((Int) -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var latestPartial: String = ""
    private var hasFinished = false
    /** 停止操作の後は、認識サービスの切り替えではなく確定処理へ進む。 */
    private var isStopping = false
    private var usingOnDevice = false
    private var didFallBackToService = false
    private var maxMillis: Long = (DEFAULT_MAX_SECONDS * 1000).toLong()
    private var startedAtUptime: Long = 0

    private val autoStop = Runnable {
        if (!hasFinished && !isStopping) onAutoStopped?.invoke()
        stopAndFinalize()
    }
    private val finalizeFallback = Runnable { finalizeWithPartial() }

    val isRecognitionAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context) ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context))

    val hasMicrophonePermission: Boolean
        get() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    // MARK: - Recording

    fun startRecordingWithAutoStop(maxSeconds: Double = DEFAULT_MAX_SECONDS) {
        teardown()
        latestPartial = ""
        hasFinished = false
        isStopping = false
        didFallBackToService = false
        maxMillis = (maxSeconds * 1000).toLong()
        startedAtUptime = SystemClock.uptimeMillis()
        startListening(preferOnDevice = true)
    }

    private fun startListening(preferOnDevice: Boolean) {
        val onDevice = preferOnDevice &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

        val created = try {
            if (onDevice) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }
        } catch (error: Exception) {
            Log.e(TAG, "createSpeechRecognizer failed", error)
            null
        }

        if (created == null) {
            deliverError()
            return
        }

        usingOnDevice = onDevice
        recognizer = created
        try {
            created.setRecognitionListener(listener)
            created.startListening(recognitionIntent(preferOffline = onDevice))
        } catch (error: Exception) {
            Log.e(TAG, "startListening failed", error)
            deliverError()
            return
        }

        // 認識サービスを切り替えた場合も、録音全体の上限は最初の開始時刻から数える
        val remaining = maxMillis - (SystemClock.uptimeMillis() - startedAtUptime)
        handler.postDelayed(autoStop, remaining.coerceAtLeast(MIN_REMAINING_MS))
    }

    private fun recognitionIntent(preferOffline: Boolean): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, JAPANESE_TAG)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, JAPANESE_TAG)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // 端末上のモデルが無くて切り替えた後は、オフライン優先を外して再試行する
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

    /** 手動／自動停止。確定結果が届かない場合は認識途中のテキストを確定として扱う。 */
    fun stopAndFinalize() {
        if (hasFinished) return
        isStopping = true
        handler.removeCallbacks(autoStop)
        try {
            recognizer?.stopListening()
        } catch (error: Exception) {
            Log.e(TAG, "stopListening failed", error)
        }
        handler.removeCallbacks(finalizeFallback)
        handler.postDelayed(finalizeFallback, FINALIZE_TIMEOUT_MS)
    }

    fun release() {
        hasFinished = true
        teardown()
    }

    // MARK: - Listener

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onPartialResults(partialResults: Bundle?) {
            firstResult(partialResults)?.let {
                latestPartial = it
                onPartial?.invoke(it)
            }
        }

        override fun onResults(results: Bundle?) {
            val text = firstResult(results) ?: latestPartial
            if (text.isEmpty()) finishCancelled() else finish(text)
        }

        // API 31 以降の定数だが、値の比較にしか使わないため古い端末でも安全（該当コードが来ないだけ）
        @SuppressLint("InlinedApi")
        override fun onError(error: Int) {
            Log.w(TAG, "recognition error: $error")

            // 端末上の日本語モデルが無い場合は、通常の認識サービスへ一度だけ切り替える。
            // 停止操作の後は録音を再開せず、そのまま確定処理へ進む。
            val languageUnavailable = error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ||
                error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED
            if (usingOnDevice && languageUnavailable && !didFallBackToService &&
                !hasFinished && !isStopping
            ) {
                didFallBackToService = true
                teardown()
                startListening(preferOnDevice = false)
                return
            }

            when (error) {
                // 無音・打ち切りは正常終了として扱う
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                SpeechRecognizer.ERROR_CLIENT -> finalizeWithPartial()

                else -> {
                    if (latestPartial.isNotEmpty()) finalizeWithPartial() else deliverError()
                }
            }
        }
    }

    private fun firstResult(bundle: Bundle?): String? =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }

    // MARK: - Single delivery

    private fun finalizeWithPartial() {
        if (latestPartial.isEmpty()) finishCancelled() else finish(latestPartial)
    }

    private fun finish(text: String) {
        if (hasFinished) return
        hasFinished = true
        teardown()
        onFinalResult?.invoke(text)
    }

    private fun finishCancelled() {
        if (hasFinished) return
        hasFinished = true
        teardown()
        onCancelled?.invoke()
    }

    private fun deliverError() {
        if (hasFinished) return
        hasFinished = true
        teardown()
        onError?.invoke(R.string.error_recognizer_unavailable)
    }

    // MARK: - Teardown（冪等・コールバックを発火しない）

    private fun teardown() {
        handler.removeCallbacks(autoStop)
        handler.removeCallbacks(finalizeFallback)
        recognizer?.let {
            try {
                it.setRecognitionListener(null)
                it.cancel()
                it.destroy()
            } catch (error: Exception) {
                Log.e(TAG, "destroy failed", error)
            }
        }
        recognizer = null
    }

    private companion object {
        const val TAG = "OnoSpeech"
        const val JAPANESE_TAG = "ja-JP"
        const val DEFAULT_MAX_SECONDS = 10.0
        const val FINALIZE_TIMEOUT_MS = 1500L
        const val MIN_REMAINING_MS = 500L
    }
}
