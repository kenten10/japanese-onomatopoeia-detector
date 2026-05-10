import Foundation

// MARK: - Dictionary Entry

struct OnomatopoeiaEntry: Codable, Identifiable, Equatable {
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

struct EvaluationResult: Identifiable {
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

    var scoreColor: String {
        switch score {
        case 5: return "ScoreGold"
        case 4: return "ScoreBlue"
        case 3: return "ScoreGreen"
        case 2: return "ScoreOrange"
        default: return "ScoreGray"
        }
    }
}

extension EvaluationResult: Equatable {
    static func == (lhs: EvaluationResult, rhs: EvaluationResult) -> Bool {
        lhs.inputText == rhs.inputText &&
        lhs.score == rhs.score &&
        lhs.date == rhs.date
    }
}

struct SimilarEntry: Identifiable {
    let id = UUID()
    let entry: OnomatopoeiaEntry
    let similarity: Double
}

extension SimilarEntry: Equatable {
    static func == (lhs: SimilarEntry, rhs: SimilarEntry) -> Bool {
        lhs.entry == rhs.entry &&
        lhs.similarity == rhs.similarity
    }
}

// MARK: - History Item (Core Data mapped)

struct HistoryItem: Identifiable {
    let id: UUID
    let inputText: String
    let score: Int
    let date: Date
}

// MARK: - Recording State

enum RecordingState: Equatable {
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

    var displayName: String {
        switch self {
        case .system:   return String(localized: "settings.language.auto")
        case .japanese: return String(localized: "settings.language.ja")
        case .english:  return String(localized: "settings.language.en")
        }
    }
}
