import Foundation
import Combine
import SwiftUI

@MainActor
final class AppViewModel: ObservableObject {

    // MARK: - Published
    @Published var recordingState: RecordingState = .idle
    @Published var history: [HistoryItem] = []
    @Published var appLanguage: AppLanguage = .system

    // MARK: - Sub-managers
    let speech = SpeechManager()
    private let persistence = PersistenceController.shared
    private let engine = OnoEngine.shared

    // MARK: - Init

    init() {
        loadHistory()
        setupSpeechCallback()

        // Load saved language preference
        if let saved = UserDefaults.standard.string(forKey: "appLanguage"),
           let lang = AppLanguage(rawValue: saved) {
            appLanguage = lang
        }
    }

    // MARK: - Speech Setup

    private func setupSpeechCallback() {
        speech.onFinalResult = { [weak self] text in
            guard let self else { return }
            Task { @MainActor in
                self.evaluateText(text)
            }
        }
    }

    // MARK: - Permissions

    func requestPermissionsIfNeeded() async {
        await speech.requestPermissions()
    }

    // MARK: - Recording

    func startRecording() {
        guard speech.canRecord else { return }
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
        // Run on background then hop back to main
        Task.detached(priority: .userInitiated) { [weak self] in
            guard let self else { return }
            let result = self.engine.evaluate(text: text)
            await MainActor.run {
                self.recordingState = .result(result)
            }
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
        UserDefaults.standard.set(lang.rawValue, forKey: "appLanguage")
    }
}
