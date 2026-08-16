import Foundation
import Observation

@MainActor
@Observable
final class AppViewModel {

    // MARK: - Observable state
    var recordingState: RecordingState = .idle
    var history: [HistoryItem] = []
    var appLanguage: AppLanguage = .defaultLanguage
    var persistenceErrorMessage: String?

    // MARK: - Sub-managers
    let speech = SpeechManager()
    private let persistence: PersistenceController
    private let engine = OnoEngine.shared

    // MARK: - Init

    /// - Parameter persistence: 既定は共有ストア。テストではメモリ上のストアを渡す。
    init(persistence: PersistenceController = .shared) {
        self.persistence = persistence
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
        speech.onFailed = { [weak self] in
            Task { @MainActor in
                guard let self else { return }
                switch self.recordingState {
                case .recording, .recognizing:
                    self.recordingState = .error(String(localized: "error.recognition.failed"))
                default:
                    break
                }
            }
        }
    }

    // MARK: - Permissions

    func startRecording() {
        Task { [weak self] in
            guard let self else { return }
            if speech.authStatus == .notDetermined || speech.micAuthStatus == .notDetermined {
                await speech.requestPermissions()
            }
            beginRecordingIfAuthorized()
        }
    }

    private func beginRecordingIfAuthorized() {
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
        // 句読点だけの認識結果は空白除去では落ちないため、正規化して中身の有無を見る
        guard !OnoEngine.normalize(text).isEmpty else {
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

    /// 同じ結果を二重に保存しない。保存済みの判定 ID を覚えておく。
    private var savedResultID: UUID?

    @discardableResult
    func saveCurrentResult() -> Bool {
        guard case .result(let r) = recordingState else { return false }
        guard savedResultID != r.id else { return false }
        do {
            try persistence.addHistory(inputText: r.inputText, score: r.score)
            savedResultID = r.id
            loadHistory()
            return true
        } catch {
            showPersistenceError(error)
            return false
        }
    }

    // MARK: - History

    func loadHistory() {
        do {
            history = try persistence.fetchHistory()
        } catch {
            history = []
            showPersistenceError(error)
        }
    }

    func deleteHistoryItem(_ item: HistoryItem) {
        do {
            try persistence.delete(item: item)
            loadHistory()
        } catch {
            showPersistenceError(error)
        }
    }

    func clearAllHistory() {
        do {
            try persistence.deleteAll()
            loadHistory()
        } catch {
            showPersistenceError(error)
        }
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

    func dismissPersistenceError() {
        persistenceErrorMessage = nil
    }

    private func showPersistenceError(_ error: Error) {
        persistenceErrorMessage = (error as? LocalizedError)?.errorDescription
            ?? String(localized: "error.persistence.load")
    }
}
