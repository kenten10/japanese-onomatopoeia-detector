import Foundation
import Observation

@MainActor
@Observable
final class AppViewModel {

    // MARK: - Observable state
    var recordingState: RecordingState = .idle
    var history: [HistoryItem] = []
    var appLanguage: AppLanguage = .defaultLanguage

    // MARK: - Sub-managers
    let speech = SpeechManager()
    private let persistence = PersistenceController.shared
    private let engine = OnoEngine.shared

    // MARK: - Init

    init() {
        loadHistory()
        setupSpeechCallback()

        // 保存済みの言語設定（なければ既定＝英語）を反映
        appLanguage = AppLanguage.stored
    }

    // MARK: - Speech Setup

    private func setupSpeechCallback() {
        speech.onFinalResult = { [weak self] text in
            guard let self else { return }
            Task { @MainActor in
                self.evaluateText(text)
            }
        }
        speech.onCancelled = { [weak self] in
            Task { @MainActor in
                guard let self else { return }
                // 評価中／結果表示中を上書きしないよう、録音・認識中のときだけ待機に戻す
                switch self.recordingState {
                case .recording, .recognizing:
                    self.recordingState = .idle
                default:
                    break
                }
            }
        }
    }

    // MARK: - Permissions

    func requestPermissionsIfNeeded() async {
        await speech.requestPermissions()
    }

    // MARK: - Recording

    func startRecording() {
        guard speech.canRecord else {
            recordingState = .error(String(localized: "permission.mic.deny"))
            return
        }
        do {
            recordingState = .recording
            try speech.startRecordingWithAutoStop(maxSeconds: 10)
        } catch {
            recordingState = .error(error.localizedDescription)
        }
    }

    func stopRecording() {
        guard case .recording = recordingState else { return }
        recordingState = .recognizing
        speech.stopAndFinalize()
    }

    // MARK: - Evaluation

    private func evaluateText(_ text: String) {
        guard !text.trimmingCharacters(in: .whitespaces).isEmpty else {
            recordingState = .idle
            return
        }
        recordingState = .evaluating
        // 評価は actor 上（メインスレッド外）で実行し、完了後にメインへ戻して反映する
        Task { [weak self] in
            guard let self else { return }
            let result = await self.engine.evaluate(text: text)
            self.recordingState = .result(result)
        }
    }

    // MARK: - Save to History

    func saveCurrentResult() {
        guard case .result(let r) = recordingState else { return }
        persistence.addHistory(inputText: r.inputText, score: r.score)
        loadHistory()
    }

    // MARK: - History

    func loadHistory() {
        history = persistence.fetchHistory()
    }

    func deleteHistoryItem(_ item: HistoryItem) {
        persistence.delete(item: item)
        loadHistory()
    }

    func clearAllHistory() {
        persistence.deleteAll()
        loadHistory()
    }

    // MARK: - Reset

    func resetToIdle() {
        recordingState = .idle
        speech.partialText = ""
    }

    // MARK: - Language

    func setLanguage(_ lang: AppLanguage) {
        appLanguage = lang
        lang.persist() // AppleLanguages に反映（切り替えは次回起動時に反映される）
    }
}
