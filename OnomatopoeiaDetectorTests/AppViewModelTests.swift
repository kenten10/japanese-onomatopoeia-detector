import XCTest
@testable import OnomatopoeiaDetector

@MainActor
final class AppViewModelTests: XCTestCase {

    private var viewModel: AppViewModel!

    override func setUp() async throws {
        try await super.setUp()
        // 実際の履歴を汚さないよう、メモリ上のストアを使う
        viewModel = AppViewModel(persistence: PersistenceController(inMemory: true))
    }

    override func tearDown() async throws {
        viewModel = nil
        try await super.tearDown()
    }

    // MARK: - 状態遷移

    func testFinalResultLeadsToResultState() async throws {
        viewModel.speech.onFinalResult?("ふわふわ")

        try await waitUntil { if case .result = self.viewModel.recordingState { return true } else { return false } }

        guard case .result(let result) = viewModel.recordingState else {
            return XCTFail("結果状態にならなかった")
        }
        XCTAssertEqual(result.inputText, "ふわふわ")
        XCTAssertEqual(result.score, 5)
    }

    func testBlankRecognitionReturnsToIdle() async throws {
        viewModel.recordingState = .recognizing
        viewModel.speech.onFinalResult?("   ")

        try await waitUntil { self.viewModel.recordingState == .idle }
    }

    /// 記号だけの認識結果は正規化すると空になる。評価へ進めず待機に戻す。
    func testPunctuationOnlyRecognitionReturnsToIdle() async throws {
        viewModel.recordingState = .recognizing
        viewModel.speech.onFinalResult?("。、")

        try await waitUntil { self.viewModel.recordingState == .idle }
    }

    func testStopRecordingMovesToRecognizing() {
        viewModel.recordingState = .recording

        viewModel.stopRecording()

        XCTAssertEqual(viewModel.recordingState, .recognizing)
    }

    func testStopRecordingIsIgnoredWhenNotRecording() {
        viewModel.recordingState = .idle

        viewModel.stopRecording()

        XCTAssertEqual(viewModel.recordingState, .idle)
    }

    func testCancellationReturnsToIdleOnlyWhileListening() async throws {
        viewModel.recordingState = .recording
        viewModel.speech.onCancelled?()
        try await waitUntil { self.viewModel.recordingState == .idle }

        // 結果を見ている最中に遅れて届いたキャンセルで、結果を消さない
        let result = EvaluationResult(inputText: "ふわふわ", score: 5, similarEntries: [], date: Date())
        viewModel.recordingState = .result(result)
        viewModel.speech.onCancelled?()
        try await Task.sleep(for: .milliseconds(50))

        guard case .result = viewModel.recordingState else {
            return XCTFail("結果表示がキャンセルで消えた")
        }
    }

    /// 認識の失敗は無音と区別してエラーで伝える。
    func testRecognitionFailureSurfacesAsError() async throws {
        viewModel.recordingState = .recording
        viewModel.speech.onFailed?()

        try await waitUntil { if case .error = self.viewModel.recordingState { return true } else { return false } }
    }

    func testResetToIdleClearsPartialText() {
        viewModel.speech.partialText = "ふわ"
        viewModel.recordingState = .recording

        viewModel.resetToIdle()

        XCTAssertEqual(viewModel.recordingState, .idle)
        XCTAssertEqual(viewModel.speech.partialText, "")
    }

    // MARK: - 履歴

    func testSaveCurrentResultStoresOnlyOnce() {
        let result = EvaluationResult(inputText: "ふわふわ", score: 5, similarEntries: [], date: Date())
        viewModel.recordingState = .result(result)

        XCTAssertTrue(viewModel.saveCurrentResult())
        XCTAssertFalse(viewModel.saveCurrentResult(), "同じ結果は二度保存しない")
        XCTAssertEqual(viewModel.history.count, 1)
        XCTAssertEqual(viewModel.history.first?.inputText, "ふわふわ")
    }

    func testSaveIsIgnoredWithoutResult() {
        viewModel.recordingState = .idle

        XCTAssertFalse(viewModel.saveCurrentResult())
        XCTAssertTrue(viewModel.history.isEmpty)
    }

    func testDeleteAndClearHistory() throws {
        for text in ["ふわふわ", "きらきら"] {
            viewModel.recordingState = .result(
                EvaluationResult(inputText: text, score: 5, similarEntries: [], date: Date())
            )
            XCTAssertTrue(viewModel.saveCurrentResult())
        }
        XCTAssertEqual(viewModel.history.count, 2)

        let removed = try XCTUnwrap(viewModel.history.first)
        viewModel.deleteHistoryItem(removed)
        XCTAssertEqual(viewModel.history.count, 1)
        XCTAssertFalse(viewModel.history.contains { $0.id == removed.id })

        viewModel.clearAllHistory()
        XCTAssertTrue(viewModel.history.isEmpty)
    }

    // MARK: - 表示言語

    func testSetLanguagePersistsSelection() {
        viewModel.setLanguage(.japanese)

        XCTAssertEqual(viewModel.appLanguage, .japanese)
        XCTAssertEqual(AppLanguage.stored, .japanese)

        viewModel.setLanguage(.english)
        XCTAssertEqual(AppLanguage.stored, .english)
    }

    // MARK: - Helper

    /// 非同期に進む状態変化を待つ。
    private func waitUntil(
        timeout: TimeInterval = 5,
        _ condition: @escaping () -> Bool,
        file: StaticString = #filePath,
        line: UInt = #line
    ) async throws {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            if condition() { return }
            try await Task.sleep(for: .milliseconds(10))
        }
        XCTFail("条件が満たされないまま時間切れになった", file: file, line: line)
    }
}
