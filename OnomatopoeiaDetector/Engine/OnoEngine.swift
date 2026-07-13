import Foundation
import NaturalLanguage
import os

// MARK: - OnoEngine

/// オノマトペらしさを評価するエンジン。辞書は初期化時に一度だけ読み込む不変データのため
/// actor として隔離し、評価処理をメインスレッド外で安全に実行できるようにしている。
actor OnoEngine {

    static let shared = OnoEngine()

    private let dictionary: [OnomatopoeiaEntry]
    private static let log = Logger(subsystem: "OnomatopoeiaDetector", category: "OnoEngine")

    private init() {
        dictionary = Self.loadDictionary()
    }

    private static func loadDictionary() -> [OnomatopoeiaEntry] {
        guard let url = Bundle.main.url(forResource: "onomatopoeia_dict", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let entries = try? JSONDecoder().decode([OnomatopoeiaEntry].self, from: data) else {
            log.error("Failed to load onomatopoeia_dict.json")
            return []
        }
        log.info("Loaded \(entries.count) dictionary entries")
        return entries
    }

    // MARK: - Public API

    func evaluate(text: String) -> EvaluationResult {
        let normalized = normalize(text)

        if isDictionaryExactMatch(normalized) {
            return EvaluationResult(
                inputText: text,
                score: 5,
                similarEntries: findSimilar(to: normalized, top: 3),
                date: Date()
            )
        }

        let dictScore    = dictionaryScore(normalized)
        let patternScore = phoneticPatternScore(normalized)
        let symbolScore  = soundSymbolScore(normalized)
        let morphScore   = morphemeScore(normalized)

        // 重み付き合計: 40 / 30 / 20 / 10
        let raw = dictScore * 0.40
                + patternScore * 0.30
                + symbolScore  * 0.20
                + morphScore   * 0.10

        // 1〜5 に正規化
        let score = max(1, min(5, Int((raw * 4).rounded()) + 1))

        let similar = score >= 3 ? findSimilar(to: normalized, top: 3) : []

        return EvaluationResult(
            inputText: text,
            score: score,
            similarEntries: similar,
            date: Date()
        )
    }

    // MARK: - Dictionary Score

    private func isDictionaryExactMatch(_ text: String) -> Bool {
        dictionary.contains { $0.word == text || $0.reading == text }
    }

    private func dictionaryScore(_ text: String) -> Double {
        // 完全一致
        if isDictionaryExactMatch(text) {
            return 1.0
        }
        // 部分一致
        let partials = dictionary.filter {
            text.contains($0.word) || $0.word.contains(text)
        }
        if !partials.isEmpty {
            return 0.7
        }
        // 読みレベルの部分一致
        let readingPartials = dictionary.filter {
            text.contains($0.reading) || $0.reading.contains(text)
        }
        return readingPartials.isEmpty ? 0.0 : 0.4
    }

    // MARK: - Phonetic Pattern Score

    /// オノマトペに典型的な ABAB / 畳語などの反復パターンを検出する
    private func phoneticPatternScore(_ text: String) -> Double {
        let chars = Array(text)
        let len = chars.count
        guard len >= 2 else { return 0.0 }

        // ABAB パターン（例: ふわふわ）
        if len >= 4 && len % 2 == 0 {
            let half = len / 2
            if String(chars[0..<half]) == String(chars[half...]) { return 1.0 }
        }

        // 3文字で後半2文字が畳語（例: ぐるる）
        if len == 3 && chars[1] == chars[2] {
            return 0.8
        }

        // 促音・長音・撥音（オノマトペの目印）
        let smallKana = Set("っーん")
        if chars.contains(where: { smallKana.contains($0) }) { return 0.5 }

        // 4文字で 2・4文字目が一致（子音反復のヒューリスティック）
        if len == 4 && chars[1] == chars[3] { return 0.6 }

        return 0.1
    }

    // MARK: - Sound Symbol Score

    /// 濁音・半濁音・特殊モーラの比率で音象徴性を採点する
    private func soundSymbolScore(_ text: String) -> Double {
        var score = 0.0
        let chars = Array(text)

        // 濁音（重い・丸い印象）
        let voicedSet = Set("がぎぐげござじずぜぞだぢづでどばびぶべぼ")
        let voicedCount = Double(chars.filter { voicedSet.contains($0) }.count)
        let voicedRatio = voicedCount / Double(max(chars.count, 1))
        score += min(voicedRatio * 1.5, 0.4)

        // 半濁音（軽い・鋭い印象）
        let plosiveSet = Set("ぱぴぷぺぽ")
        if chars.contains(where: { plosiveSet.contains($0) }) { score += 0.3 }

        if text.contains("ー") { score += 0.2 } // 長音
        if text.contains("っ") { score += 0.2 } // 促音
        if text.hasSuffix("ん") { score += 0.1 } // 撥音止め

        return min(score, 1.0)
    }

    // MARK: - Morpheme Score

    private func morphemeScore(_ text: String) -> Double {
        let tagger = NLTagger(tagSchemes: [.lexicalClass])
        tagger.string = text
        var hasParticle = false
        var hasVerb = false

        tagger.enumerateTags(in: text.startIndex..<text.endIndex,
                             unit: .word,
                             scheme: .lexicalClass) { tag, _ in
            if tag == .particle { hasParticle = true }
            if tag == .verb     { hasVerb = true }
            return true
        }

        if hasParticle { return 0.0 }
        if hasVerb     { return 0.3 }
        return 1.0
    }

    // MARK: - Similarity Search

    func findSimilar(to text: String, top k: Int) -> [SimilarEntry] {
        dictionary
            .map { SimilarEntry(entry: $0, similarity: similarity(text, $0.word)) }
            .sorted { $0.similarity > $1.similarity }
            .prefix(k)
            .map { $0 }
    }

    /// 複合類似度: レーベンシュタイン(60%) + 文字集合の重なり(40%)
    private func similarity(_ a: String, _ b: String) -> Double {
        levenshteinSimilarity(a, b) * 0.6 + phoneticOverlap(a, b) * 0.4
    }

    private func levenshteinSimilarity(_ a: String, _ b: String) -> Double {
        let dist = levenshtein(Array(a), Array(b))
        let maxLen = max(a.count, b.count)
        guard maxLen > 0 else { return 1.0 }
        return 1.0 - Double(dist) / Double(maxLen)
    }

    private func levenshtein<T: Equatable>(_ a: [T], _ b: [T]) -> Int {
        let m = a.count, n = b.count
        var dp = Array(repeating: Array(repeating: 0, count: n + 1), count: m + 1)
        for i in 0...m { dp[i][0] = i }
        for j in 0...n { dp[0][j] = j }
        for i in 1...m {
            for j in 1...n {
                dp[i][j] = a[i-1] == b[j-1]
                    ? dp[i-1][j-1]
                    : 1 + min(dp[i-1][j], dp[i][j-1], dp[i-1][j-1])
            }
        }
        return dp[m][n]
    }

    private func phoneticOverlap(_ a: String, _ b: String) -> Double {
        let setA = Set(a), setB = Set(b)
        let union = setA.union(setB).count
        guard union > 0 else { return 0 }
        return Double(setA.intersection(setB).count) / Double(union)
    }

    // MARK: - Helpers

    /// カタカナ→ひらがな正規化＋空白・記号除去
    private func normalize(_ text: String) -> String {
        var result = ""
        for c in text.unicodeScalars {
            let v = c.value
            if v >= 0x30A1 && v <= 0x30F6 {
                result.append(Character(UnicodeScalar(v - 0x60)!))
            } else {
                result.append(Character(c))
            }
        }
        return result.filter { !$0.isWhitespace && !$0.isPunctuation }
    }
}
