import Foundation
import AVFoundation
import Speech
import Combine

// MARK: - SpeechManager

@MainActor
final class SpeechManager: NSObject, ObservableObject {

    // MARK: Published
    @Published var partialText: String = ""
    @Published var authStatus: SFSpeechRecognizerAuthorizationStatus = .notDetermined
    @Published var micAuthStatus: AVAuthorizationStatus = .notDetermined

    // MARK: Private
    private let recognizer = SFSpeechRecognizer(locale: Locale(identifier: "ja-JP"))!
    private var audioEngine = AVAudioEngine()
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?

    // MARK: Completion callback
    var onFinalResult: ((String) -> Void)?

    // MARK: - Auth

    func requestPermissions() async {
        // Speech
        let speechStatus = await withCheckedContinuation { cont in
            SFSpeechRecognizer.requestAuthorization { status in
                cont.resume(returning: status)
            }
        }
        authStatus = speechStatus

        // Microphone
        let micStatus = await AVAudioApplication.requestRecordPermission()
        micAuthStatus = micStatus ? .authorized : .denied
    }

    var canRecord: Bool {
        authStatus == .authorized && micAuthStatus == .authorized
    }

    // MARK: - Recording

    func startRecording() throws {
        stopRecording()
        partialText = ""

        let session = AVAudioSession.sharedInstance()
        try session.setCategory(.record, mode: .measurement, options: .duckOthers)
        try session.setActive(true, options: .notifyOthersOnDeactivation)

        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        guard let request = recognitionRequest else { return }
        request.shouldReportPartialResults = true
        request.requiresOnDeviceRecognition = true // offline first

        let inputNode = audioEngine.inputNode
        let format = inputNode.outputFormat(forBus: 0)

        recognitionTask = recognizer.recognitionTask(with: request) { [weak self] result, error in
            guard let self else { return }
            Task { @MainActor in
                if let result {
                    self.partialText = result.bestTranscription.formattedString
                    if result.isFinal {
                        self.onFinalResult?(result.bestTranscription.formattedString)
                        self.stopRecording()
                    }
                }
                if let error {
                    let nsError = error as NSError
                    // Ignore cancellation errors
                    if nsError.domain != "kAFAssistantErrorDomain" || nsError.code != 216 {
                        self.stopRecording()
                    }
                }
            }
        }

        inputNode.installTap(onBus: 0, bufferSize: 1024, format: format) { [weak self] buffer, _ in
            self?.recognitionRequest?.append(buffer)
        }

        audioEngine.prepare()
        try audioEngine.start()
    }

    func stopRecording() {
        audioEngine.stop()
        audioEngine.inputNode.removeTap(onBus: 0)
        recognitionRequest?.endAudio()
        recognitionRequest = nil
        recognitionTask?.cancel()
        recognitionTask = nil

        try? AVAudioSession.sharedInstance().setActive(false)
    }

    // MARK: - Auto-stop after max duration

    private var autoStopTask: Task<Void, Never>?

    func startRecordingWithAutoStop(maxSeconds: Double = 10) throws {
        try startRecording()
        autoStopTask?.cancel()
        autoStopTask = Task {
            try? await Task.sleep(nanoseconds: UInt64(maxSeconds * 1_000_000_000))
            if !Task.isCancelled {
                await MainActor.run {
                    self.stopAndFinalize()
                }
            }
        }
    }

    func stopAndFinalize() {
        autoStopTask?.cancel()
        let text = partialText
        stopRecording()
        if !text.isEmpty {
            onFinalResult?(text)
        }
    }
}
