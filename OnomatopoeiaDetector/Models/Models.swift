import Foundation

// MARK: - Dictionary Entry

struct OnomatopoeiaEntry: Codable, Identifiable, Equatable, Sendable {
    var id: String { word }
    let word: String
    let reading: String
    let meaning_ja: String
    let meaning_en: String
    let example_ja: String
    let example_en: String
    let category: String
}

// MARK: - Evaluation Result

struct EvaluationResult: Identifiable, Equatable, Sendable {
    let id = UUID()
    let inputText: String
    let score: Int            // 1〜5
    let similarEntries: [SimilarEntry]
    let date: Date

    var scoreComment: String {
        switch score {
        case 5: return String(localized: "result.comment.5")
        case 4: return String(localized: "result.comment.4")
        case 3: return String(localized: "result.comment.3")
        case 2: return String(localized: "result.comment.2")
        default: return String(localized: "result.comment.1")
        }
    }
}

struct SimilarEntry: Identifiable, Equatable, Sendable {
    let id = UUID()
    let entry: OnomatopoeiaEntry
    let similarity: Double
}

// MARK: - History Item (Core Data mapped)

struct HistoryItem: Identifiable {
    let id: UUID
    let inputText: String
    let score: Int
    let date: Date
}

// MARK: - Recording State

enum RecordingState :Equatable {
    case idle
    case recording
    case recognizing
    case evaluating
    case result(EvaluationResult)
    case error(String)
}

// MARK: - App Language

enum AppLanguage: String, CaseIterable {
    case system = "system"
    case japanese = "ja"
    case english = "en"

    /// 英語学習者向けアプリのため、既定の表示言語は英語。
    static let defaultLanguage: AppLanguage = .english

    private static let storageKey = "appLanguage"

    var displayName: String {
        switch self {
        case .system:   return String(localized: "settings.language.auto")
        case .japanese: return String(localized: "settings.language.ja")
        case .english:  return String(localized: "settings.language.en")
        }
    }

    /// 保存済みの選択。未設定なら既定（英語）。
    static var stored: AppLanguage {
        guard let raw = UserDefaults.standard.string(forKey: storageKey),
              let lang = AppLanguage(rawValue: raw) else { return .defaultLanguage }
        return lang
    }

    /// 選択を保存し、`AppleLanguages` に反映する（実際の切り替えは次回起動時に反映される）。
    func persist() {
        UserDefaults.standard.set(rawValue, forKey: Self.storageKey)
        applyToBundle()
    }

    /// バンドルのローカライズ解決に使われる `AppleLanguages` を上書きする。
    func applyToBundle() {
        switch self {
        case .system:
            // 端末の言語設定に従う
            UserDefaults.standard.removeObject(forKey: "AppleLanguages")
        case .japanese, .english:
            UserDefaults.standard.set([rawValue], forKey: "AppleLanguages")
        }
    }

    /// 起動時に呼ぶ。保存済み設定（なければ既定＝英語）を UI 表示前に適用する。
    static func applyStoredOrDefault() {
        stored.applyToBundle()
    }
}
