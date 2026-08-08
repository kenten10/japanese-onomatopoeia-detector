import Foundation
import AVFoundation
import Speech
import Observation
import os

// MARK: - SpeechManager

@MainActor
@Observable
final class SpeechManager: NSObject {

    // MARK: Observable state
    var partialText: String = ""
    var authStatus: SFSpeechRecognizerAuthorizationStatus = .notDetermined
    var micAuthStatus: AVAuthorizationStatus = .notDetermined

    // MARK: Private
    private let recognizer = SFSpeechRecognizer(locale: Locale(identifier: "ja-JP"))
    private let audioEngine = AVAudioEngine()
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    private var autoStopTask: Task<Void, Never>?
    /// 1回の録音につき最終結果（確定 or キャンセル）を一度だけ配送するためのフラグ。
    private var hasFinished = false
    private let log = Logger(subsystem: "OnomatopoeiaDetector", category: "Speech")

    // MARK: Callbacks
    /// 確定テキストが得られたとき、1録音につき一度だけ呼ばれる。
    var onFinalResult: ((String) -> Void)?
    /// 結果を得ずに録音が終了したとき（無音・エラー・キャンセル）、一度だけ呼ばれる。
    var onCancelled: (() -> Void)?

    // MARK: - Auth

    func requestPermissions() async {
        let speechStatus = await withCheckedContinuation { cont in
            SFSpeechRecognizer.requestAuthorization { cont.resume(returning: $0) }
        }
        authStatus = speechStatus

        let micGranted = await AVAudioApplication.requestRecordPermission()
        micAuthStatus = micGranted ? .authorized : .denied
    }

    var canRecord: Bool {
        authStatus == .authorized && micAuthStatus == .authorized
    }

    var permissionsDenied: Bool {
        authStatus == .denied || authStatus == .restricted || micAuthStatus == .denied
    }

    // MARK: - Recording

    func startRecording() throws {
        teardownAudio()
        partialText = ""
        hasFinished = false

        guard let recognizer, recognizer.isAvailable else {
            throw SpeechError.recognizerUnavailable
        }

        let session = AVAudioSession.sharedInstance()
        try session.setCategory(.record, mode: .measurement, options: .duckOthers)
        try session.setActive(true)

        let request = SFSpeechAudioBufferRecognitionRequest()
        request.shouldReportPartialResults = true
        request.requiresOnDeviceRecognition = true // オフライン優先
        recognitionRequest = request

        recognitionTask = recognizer.recognitionTask(with: request) { [weak self] result, error in
            Task { @MainActor [weak self] in
                guard let self else { return }

                if let result {
                    let hiraganaText = SpeechTextConverter.hiraganaText(
                        from: result.bestTranscription
                    )
                    self.partialText = hiraganaText
                    if result.isFinal {
                        self.finish(with: hiraganaText)
                    }
                }

                if let error {
                    let nsError = error as NSError
                    // kAFAssistantErrorDomain / 216 は正常な打ち切りなので無視する
                    let isBenign = nsError.domain == "kAFAssistantErrorDomain" && nsError.code == 216
                    if !isBenign {
                        self.log.error("recognition failed: \(nsError.domain, privacy: .public) (\(nsError.code))")
                        self.finishCancelled()
                    }
                }
            }
        }

        // フォーマット取得の前に prepare する（シミュレータで sampleRate が 0 になる問題への対策）
        let inputNode = audioEngine.inputNode
        audioEngine.prepare()

        var format = inputNode.outputFormat(forBus: 0)
        if format.sampleRate == 0 {
            log.warning("input sampleRate was 0; falling back to 48kHz")
            format = AVAudioFormat(standardFormatWithSampleRate: 48_000, channels: 1) ?? format
        }

        inputNode.removeTap(onBus: 0)
        inputNode.installTap(onBus: 0, bufferSize: 1024, format: format) { [weak self] buffer, _ in
            self?.recognitionRequest?.append(buffer)
        }

        try audioEngine.start()
    }

    func startRecordingWithAutoStop(maxSeconds: Double = 10) throws {
        try startRecording()
        autoStopTask?.cancel()
        autoStopTask = Task { [weak self] in
            try? await Task.sleep(for: .seconds(maxSeconds))
            guard !Task.isCancelled else { return }
            self?.stopAndFinalize()
        }
    }

    /// 手動／自動停止。現在の partialText を確定として扱う。
    func stopAndFinalize() {
        let text = partialText
        if text.isEmpty {
            finishCancelled()
        } else {
            finish(with: text)
        }
    }

    // MARK: - Single delivery

    private func finish(with text: String) {
        guard !hasFinished else { return }
        hasFinished = true
        teardownAudio()
        // text は認識結果を受け取った時点でひらがな化済みのため、ここでは変換し直さない
        onFinalResult?(text)
    }

    private func finishCancelled() {
        guard !hasFinished else { return }
        hasFinished = true
        teardownAudio()
        onCancelled?()
    }

    // MARK: - Teardown（冪等・コールバックを発火しない）

    private func teardownAudio() {
        autoStopTask?.cancel()
        autoStopTask = nil

        if audioEngine.isRunning {
            audioEngine.stop()
            audioEngine.inputNode.removeTap(onBus: 0)
        }
        recognitionRequest?.endAudio()
        recognitionRequest = nil
        recognitionTask?.cancel()
        recognitionTask = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }

}

// MARK: - SpeechTextConverter

enum SpeechTextConverter {
    static func hiraganaText(from transcription: SFTranscription) -> String {
        let text = transcription.segments
            .map { segment in
                let candidates = [segment.substring] + segment.alternativeSubstrings
                return candidates.first(where: isKanaText).map(katakanaToHiragana)
                    ?? hiraganaText(from: segment.substring)
            }
            .joined()

        guard !text.isEmpty else {
            return hiraganaText(from: transcription.formattedString)
        }
        return applyOnomatopoeiaLongSoundCorrections(text)
    }

    static func hiraganaText(from text: String) -> String {
        // かなだけの文字列は読み推定に通さない。トークナイザのラテン転写を経由すると
        // 長音符が母音へ置き換わり、「どーん」が「どおん」になってしまうため。
        guard containsKanji(text) else {
            return applyOnomatopoeiaLongSoundCorrections(katakanaToHiragana(text))
        }

        let nsText = text as NSString
        let tokenizer = CFStringTokenizerCreate(
            kCFAllocatorDefault,
            text as CFString,
            CFRange(location: 0, length: nsText.length),
            kCFStringTokenizerUnitWord,
            NSLocale(localeIdentifier: "ja_JP") as CFLocale
        )
        guard let tokenizer else {
            return applyOnomatopoeiaLongSoundCorrections(katakanaToHiragana(text))
        }

        var output = ""
        var cursor = 0
        var tokenType = CFStringTokenizerAdvanceToNextToken(tokenizer)

        while tokenType.rawValue != 0 {
            let tokenRange = CFStringTokenizerGetCurrentTokenRange(tokenizer)

            if tokenRange.location > cursor {
                output += nsText.substring(
                    with: NSRange(location: cursor, length: tokenRange.location - cursor)
                )
            }

            let range = NSRange(location: tokenRange.location, length: tokenRange.length)
            let token = nsText.substring(with: range)
            output += hiraganaReading(for: token, tokenizer: tokenizer)

            cursor = tokenRange.location + tokenRange.length
            tokenType = CFStringTokenizerAdvanceToNextToken(tokenizer)
        }

        if cursor < nsText.length {
            output += nsText.substring(from: cursor)
        }

        return applyOnomatopoeiaLongSoundCorrections(katakanaToHiragana(output))
    }

    private static func hiraganaReading(for token: String, tokenizer: CFStringTokenizer) -> String {
        guard let latin = CFStringTokenizerCopyCurrentTokenAttribute(
            tokenizer,
            kCFStringTokenizerAttributeLatinTranscription
        ) as? String else {
            return katakanaToHiragana(token)
        }

        let mutable = NSMutableString(string: latin.lowercased())
        CFStringTransform(mutable as CFMutableString, nil, "Latin-Hiragana" as CFString, false)
        return (mutable as String).filter { !$0.isWhitespace }
    }

    private static func containsKanji(_ text: String) -> Bool {
        text.unicodeScalars.contains { scalar in
            (0x4E00...0x9FFF).contains(scalar.value)
                || (0x3400...0x4DBF).contains(scalar.value)
                || (0xF900...0xFAFF).contains(scalar.value)
        }
    }

    private static func isKanaText(_ text: String) -> Bool {
        var hasKana = false
        for scalar in text.unicodeScalars {
            switch scalar.value {
            case 0x3041...0x3096, 0x30A1...0x30FA, 0x30FC:
                hasKana = true
            case 0x3000, 0x0020:
                continue
            default:
                return false
            }
        }
        return hasKana
    }

    /// カタカナをひらがなへ。長音符（ー）は母音へ展開せずそのまま残す。
    ///
    /// `CFStringTransform` の Katakana-Hiragana は長音符を直前の母音に置き換えるため
    /// （ドーン → どおん）、オノマトペの表記が崩れる。コードポイント演算に揃えている。
    private static func katakanaToHiragana(_ text: String) -> String {
        var result = String.UnicodeScalarView()
        for scalar in text.unicodeScalars {
            if scalar.value >= 0x30A1 && scalar.value <= 0x30F6,
               let converted = UnicodeScalar(scalar.value - 0x60) {
                result.append(converted)
            } else {
                result.append(scalar)
            }
        }
        return String(result)
    }

    private static func applyOnomatopoeiaLongSoundCorrections(_ text: String) -> String {
        var corrected = text
        ["しいん": "しーん"].forEach { source, replacement in
            corrected = corrected.replacingOccurrences(of: source, with: replacement)
        }
        return corrected
    }
}

// MARK: - SpeechError

enum SpeechError: LocalizedError {
    case recognizerUnavailable

    var errorDescription: String? {
        switch self {
        case .recognizerUnavailable:
            return String(localized: "error.recognizer.unavailable")
        }
    }
}
