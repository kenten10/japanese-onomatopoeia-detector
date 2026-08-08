package com.kensukeyoshida.onomatopoeiadetector.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kensukeyoshida.onomatopoeiadetector.R
import com.kensukeyoshida.onomatopoeiadetector.data.DictionaryLoader
import com.kensukeyoshida.onomatopoeiadetector.data.HistoryRepository
import com.kensukeyoshida.onomatopoeiadetector.data.LanguageSettings
import com.kensukeyoshida.onomatopoeiadetector.data.PersistenceException
import com.kensukeyoshida.onomatopoeiadetector.engine.OnoEngine
import com.kensukeyoshida.onomatopoeiadetector.model.AppLanguage
import com.kensukeyoshida.onomatopoeiadetector.model.HistoryItem
import com.kensukeyoshida.onomatopoeiadetector.model.RecordingState
import com.kensukeyoshida.onomatopoeiadetector.speech.SpeechManager
import com.kensukeyoshida.onomatopoeiadetector.text.JapaneseAnalyzer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AppViewModel(application: Application) : AndroidViewModel(application) {

    // MARK: - Observable state

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    private val _appLanguage = MutableStateFlow(AppLanguage.defaultLanguage)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _persistenceErrorMessage = MutableStateFlow<Int?>(null)
    val persistenceErrorMessage: StateFlow<Int?> = _persistenceErrorMessage.asStateFlow()

    private val _savedFeedback = MutableStateFlow(false)
    val savedFeedback: StateFlow<Boolean> = _savedFeedback.asStateFlow()

    // MARK: - Sub-managers

    val speech = SpeechManager(application)
    private val persistence = HistoryRepository(application)
    private val engine = OnoEngine(DictionaryLoader.load(application))

    private var savedFeedbackJob: Job? = null
    /** 認識途中のテキスト変換は最新の 1 件だけを走らせ、表示が古い結果に巻き戻らないようにする。 */
    private var partialJob: Job? = null
    /** 履歴の読み書きを直列化し、削除直後に古い一覧で上書きされないようにする。 */
    private val historyMutex = Mutex()
    /** 同じ結果を二重に保存しない。 */
    private var savedResultId: String? = null

    // MARK: - Init

    init {
        loadHistory()
        setupSpeechCallback()

        // 保存済みの言語設定（なければ既定＝英語）を反映
        _appLanguage.value = LanguageSettings.stored(application)

        // 形態素解析の辞書は初回だけ読み込みに時間がかかるため、先に暖機しておく
        viewModelScope.launch { JapaneseAnalyzer.warmUp() }
    }

    // MARK: - Speech Setup

    private fun setupSpeechCallback() {
        speech.onPartial = { raw ->
            partialJob?.cancel()
            partialJob = viewModelScope.launch {
                val hiragana = JapaneseAnalyzer.hiraganaText(raw)
                if (_recordingState.value is RecordingState.Recording) _partialText.value = hiragana
            }
        }
        speech.onAutoStopped = {
            // 上限時間で自動停止したときも、手動停止と同じく認識中の表示へ切り替える
            if (_recordingState.value is RecordingState.Recording) {
                _recordingState.value = RecordingState.Recognizing
            }
        }
        speech.onFinalResult = { raw ->
            partialJob?.cancel()
            partialJob = null
            viewModelScope.launch {
                val hiragana = JapaneseAnalyzer.hiraganaText(raw)
                _partialText.value = hiragana
                evaluateText(hiragana)
            }
        }
        speech.onCancelled = {
            // 評価中／結果表示中を上書きしないよう、録音・認識中のときだけ待機に戻す
            when (_recordingState.value) {
                is RecordingState.Recording, is RecordingState.Recognizing -> resetToIdle()
                else -> Unit
            }
        }
        speech.onError = { messageRes ->
            _recordingState.value = RecordingState.Error(messageRes)
        }
    }

    // MARK: - Recording

    /** マイク権限が許可されている前提で録音を始める。 */
    fun startRecording() {
        if (!speech.isRecognitionAvailable) {
            _recordingState.value = RecordingState.Error(R.string.error_recognizer_unavailable)
            return
        }
        _partialText.value = ""
        _recordingState.value = RecordingState.Recording
        speech.startRecordingWithAutoStop(maxSeconds = 10.0)
    }

    fun showPermissionDenied() {
        _recordingState.value = RecordingState.Error(
            messageRes = R.string.permission_mic_deny,
            permissionsDenied = true
        )
    }

    fun stopRecording() {
        if (_recordingState.value !is RecordingState.Recording) return
        _recordingState.value = RecordingState.Recognizing
        speech.stopAndFinalize()
    }

    /** 画面が背面に回ったら録音を畳む（マイクを掴んだままにしない）。 */
    fun stopRecordingOnBackground() {
        when (_recordingState.value) {
            is RecordingState.Recording -> {
                _recordingState.value = RecordingState.Recognizing
                speech.stopAndFinalize()
            }
            else -> Unit
        }
    }

    // MARK: - Evaluation

    private suspend fun evaluateText(text: String) {
        if (text.isBlank()) {
            resetToIdle()
            return
        }
        _recordingState.value = RecordingState.Evaluating
        val result = engine.evaluate(text)
        _recordingState.value = RecordingState.Result(result)
    }

    // MARK: - Save to History

    fun saveCurrentResult() {
        val state = _recordingState.value
        if (state !is RecordingState.Result) return
        if (savedResultId == state.result.id) return
        savedResultId = state.result.id
        viewModelScope.launch {
            try {
                historyMutex.withLock {
                    persistence.add(state.result.inputText, state.result.score)
                    _history.value = persistence.fetch()
                }
                _savedFeedback.value = true
                savedFeedbackJob?.cancel()
                savedFeedbackJob = viewModelScope.launch {
                    delay(1500)
                    _savedFeedback.value = false
                }
            } catch (error: PersistenceException) {
                savedResultId = null
                _persistenceErrorMessage.value = error.messageRes
            }
        }
    }

    // MARK: - History

    fun loadHistory() {
        viewModelScope.launch { reloadHistory() }
    }

    fun deleteHistoryItem(item: HistoryItem) {
        viewModelScope.launch {
            runHistoryUpdate { persistence.delete(item) }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            runHistoryUpdate { persistence.deleteAll() }
        }
    }

    /** 更新と再取得を同じロックの中で行い、一覧が古いスナップショットに戻らないようにする。 */
    private suspend fun runHistoryUpdate(update: suspend () -> Unit) {
        try {
            historyMutex.withLock {
                update()
                _history.value = persistence.fetch()
            }
        } catch (error: PersistenceException) {
            _persistenceErrorMessage.value = error.messageRes
        }
    }

    private suspend fun reloadHistory() {
        try {
            historyMutex.withLock { _history.value = persistence.fetch() }
        } catch (error: PersistenceException) {
            _history.value = emptyList()
            _persistenceErrorMessage.value = error.messageRes
        }
    }

    // MARK: - Reset

    fun resetToIdle() {
        partialJob?.cancel()
        partialJob = null
        _recordingState.value = RecordingState.Idle
        _partialText.value = ""
        _savedFeedback.value = false
    }

    // MARK: - Language

    fun setLanguage(language: AppLanguage) {
        _appLanguage.value = language
        LanguageSettings.persist(getApplication(), language)
    }

    fun dismissPersistenceError() {
        _persistenceErrorMessage.value = null
    }

    override fun onCleared() {
        speech.release()
        super.onCleared()
    }
}
