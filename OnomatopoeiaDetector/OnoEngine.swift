import Foundation
import NaturalLanguage

// MARK: - OnoEngine

final class OnoEngine {

    // MARK: Singleton
    static let shared = OnoEngine()

    // MARK: Dictionary
    private var dictionary: [OnomatopoeiaEntry] = []

    private init() {
        loadDictionary()
    }

    private func loadDictionary() {
        guard let url = Bundle.main.url(forResource: "onomatopoeia_dict", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let entries = try? JSONDecoder().decode([OnomatopoeiaEntry].self, from: data) else {
            print("[OnoEngine] Failed to load dictionary")
            return
        }
        dictionary = entries
        print("[OnoEngine] Loaded \(dictionary.count) entries")
    }

    // MARK: - Public API

    func evaluate(text: String) -> EvaluationResult {
        let normalized = normalize(text)

        let dictScore    = dictionaryScore(normalized)
        let patternScore = phoneticPatternScore(normalized)
        let symbolScore  = soundSymbolScore(normalized)
        let morphScore   = morphemeScore(normalized)

        // Weighted total: 40 / 30 / 20 / 10
        let raw = dictScore * 0.40
                + patternScore * 0.30
                + symbolScore  * 0.20
                + morphScore   * 0.10

        // Normalize to 1〜5
        let score = max(1, min(5, Int((raw * 4).rounded()) + 1))

        // Similar entries (only collected, displayed by ViewModel when score >= 3)
        let similar = score >= 3 ? findSimilar(to: normalized, top: 3) : []

        return EvaluationResult(
            inputText: text,
            score: score,
            similarEntries: similar,
            date: Date()
        )
    }

    // MARK: - Dictionary Score

    private func dictionaryScore(_ text: String) -> Double {
        // Exact match
        if dictionary.contains(where: { $0.word == text || $0.reading == text }) {
            return 1.0
        }
        // Partial match
        let partials = dictionary.filter {
            text.contains($0.word) || $0.word.contains(text)
        }
        if !partials.isEmpty {
            return 0.7
        }
        // Reading-level partial
        let readingPartials = dictionary.filter {
            let r = hiraganaToRomaji(text)
            return r.contains($0.reading) || $0.reading.contains(r)
        }
        return readingPartials.isEmpty ? 0.0 : 0.4
    }

    // MARK: - Phonetic Pattern Score

    /// Detects ABAB / ABB / AB reduplication patterns typical of onomatopoeia
    private func phoneticPatternScore(_ text: String) -> Double {
        let chars = Array(text)
        let len = chars.count
        guard len >= 2 else { return 0.0 }

        // ABAB pattern (e.g. ふわふわ)
        if len >= 4 && len % 2 == 0 {
            let half = len / 2
            let first = String(chars[0..<half])
            let second = String(chars[half...])
            if first == second { return 1.0 }
        }

        // ABB pattern (e.g. ぐるぐる → not ABB but covers 3-char)
        if len == 3 {
            let first = chars[0]
            let last2 = String(chars[1...])
            if String(chars[1]) == String(chars[2]) { return 0.8 }
            _ = first; _ = last2
        }

        // Contains small kana (っ, ー, ん) – onomatopoeia markers
        let smallKana = Set("っーん")
        let smallCount = chars.filter { smallKana.contains($0) }.count
        if smallCount > 0 { return 0.5 }

        // Consonant repetition heuristic via reading analysis
        if len == 4 {
            let midA = String(chars[1])
            let midB = String(chars[3])
            if midA == midB { return 0.6 }
        }

        return 0.1
    }

    // MARK: - Sound Symbol Score

    /// Scores based on voiced/unvoiced consonant ratio and special mora usage
    private func soundSymbolScore(_ text: String) -> Double {
        var score = 0.0
        let chars = Array(text)

        // Voiced consonants (dakuten) give "heavy/round" feel
        let voicedSet = Set("がぎぐげござじずぜぞだぢづでどばびぶべぼ")
        let voicedCount = Double(chars.filter { voicedSet.contains($0) }.count)
        let voicedRatio = voicedCount / Double(max(chars.count, 1))
        score += min(voicedRatio * 1.5, 0.4)

        // Unvoiced plosives (pa-row) give "light/crisp" feel
        let plosiveSet = Set("ぱぴぷぺぽ")
        if chars.contains(where: { plosiveSet.contains($0) }) { score += 0.3 }

        // Long vowel marker
        if text.contains("ー") { score += 0.2 }

        // Glottal stop (っ)
        if text.contains("っ") { score += 0.2 }

        // Nasal (ん) at end
        if text.hasSuffix("ん") { score += 0.1 }

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
            if tag == .particle      { hasParticle = true }
            if tag == .verb          { hasVerb = true }
            return true
        }

        if hasParticle { return 0.0 }
        if hasVerb     { return 0.3 }
        return 1.0
    }

    // MARK: - Similarity Search

    func findSimilar(to text: String, top k: Int) -> [SimilarEntry] {
        let candidates = dictionary.map { entry -> SimilarEntry in
            let sim = similarity(text, entry.word)
            return SimilarEntry(entry: entry, similarity: sim)
        }
        return candidates
            .sorted { $0.similarity > $1.similarity }
            .prefix(k)
            .map { $0 }
    }

    /// Combined similarity: Levenshtein (60%) + phonetic character overlap (40%)
    private func similarity(_ a: String, _ b: String) -> Double {
        let lev = levenshteinSimilarity(a, b)
        let pho = phoneticOverlap(a, b)
        return lev * 0.6 + pho * 0.4
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
        let setA = Set(a)
        let setB = Set(b)
        let intersection = setA.intersection(setB).count
        let union = setA.union(setB).count
        guard union > 0 else { return 0 }
        return Double(intersection) / Double(union)
    }

    // MARK: - Helpers

    private func normalize(_ text: String) -> String {
        // Katakana → Hiragana
        var s = text
        var result = ""
        for c in s.unicodeScalars {
            let v = c.value
            if v >= 0x30A1 && v <= 0x30F6 {
                result.append(Character(UnicodeScalar(v - 0x60)!))
            } else {
                result.append(Character(c))
            }
        }
        s = result
        // Remove spaces / punctuation
        s = s.filter { !$0.isWhitespace && !$0.isPunctuation }
        return s
    }

    private func hiraganaToRomaji(_ text: String) -> String {
        // Simplified - just return as-is for internal comparison
        return text
    }
}
