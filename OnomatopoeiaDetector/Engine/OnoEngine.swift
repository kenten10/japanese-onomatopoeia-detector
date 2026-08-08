import Foundation
import NaturalLanguage
import os

// MARK: - OnoEngine

/// オノマトペらしさを評価するエンジン。辞書は初期化時に一度だけ読み込む不変データのため
/// actor として隔離し、評価処理をメインスレッド外で安全に実行できるようにしている。
actor OnoEngine {

    static let shared = OnoEngine()

    /// 照合用に見出し語を正規化して保持したエントリ。
    ///
    /// 辞書には `ドキドキ` のようなカタカナ表記の見出しがあり、入力側だけを正規化すると
    /// 一致しない。辞書側も同じ正規化を通してから比較する。
    ///
    /// `reading` は `fuwafuwa` のようなローマ字表記で、入力は常にひらがなへ揃えられる。
    /// 照合には使えないためここでは持たない（読みは結果表示にだけ使う）。
    private struct NormalizedEntry {
        let entry: OnomatopoeiaEntry
        let word: String
    }

    /// 辞書と部分一致で照合する最小の文字数。
    private static let minMatchLength = 2

    private let normalizedDictionary: [NormalizedEntry]
    private static let log = Logger(subsystem: "OnomatopoeiaDetector", category: "OnoEngine")

    private init() {
        normalizedDictionary = Self.loadDictionary().map {
            NormalizedEntry(entry: $0, word: Self.normalize($0.word))
        }
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
        let normalized = Self.normalize(text)

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
        normalizedDictionary.contains { $0.word == text }
    }

    private func dictionaryScore(_ text: String) -> Double {
        // 完全一致
        if isDictionaryExactMatch(text) {
            return 1.0
        }
        // 1文字以下は語として照合しない。空文字はあらゆる見出しの部分文字列になり、
        // 「ん」「ー」のような1文字も辞書のどこかに必ず含まれてしまうため。
        guard text.count >= Self.minMatchLength else {
            return 0.0
        }
        // 部分一致
        let partials = normalizedDictionary.filter {
            text.contains($0.word) || $0.word.contains(text)
        }
        return partials.isEmpty ? 0.0 : 0.7
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

    /// 助詞を含めば 0、動詞を含めば 0.3、どちらも無い独立語なら 1.0。
    ///
    /// `NLTagger` は日本語の単語分割はできるが、品詞（`.lexicalClass`）は
    /// かな・漢字を問わず `.otherWord` しか返さない。そのため分割だけを借り、
    /// 語の判定は自前の一覧で行う。PWA 版・Android 版が使う kuromoji ほどの
    /// 精度は無いが、「オノマトペ＋助詞／する」という頻出形は拾える。
    private func morphemeScore(_ text: String) -> Double {
        let tokens = words(in: text)
        if tokens.contains(where: { Self.particles.contains($0) }) { return 0.0 }
        if tokens.contains(where: { Self.verbForms.contains($0) }) { return 0.3 }
        return 1.0
    }

    private func words(in text: String) -> [String] {
        let tagger = NLTagger(tagSchemes: [.lexicalClass])
        tagger.string = text
        var tokens: [String] = []
        tagger.enumerateTags(in: text.startIndex..<text.endIndex,
                             unit: .word,
                             scheme: .lexicalClass) { _, range in
            tokens.append(String(text[range]))
            return true
        }
        return tokens
    }

    /// 単独の語として現れたら助詞とみなすもの。
    private static let particles: Set<String> = [
        "を", "が", "は", "に", "へ", "と", "で", "の", "も", "や",
        "から", "まで", "より", "など", "ね", "よ", "か", "ば", "し"
    ]

    /// オノマトペに続きやすい動詞と、その主な活用形。
    private static let verbForms: Set<String> = [
        "する", "した", "して", "します", "しない", "される", "させる",
        "なる", "なった", "なって", "ある", "あった", "いる", "いた",
        "くる", "きた", "いく", "いった", "みる", "みた"
    ]

    // MARK: - Similarity Search

    func findSimilar(to text: String, top k: Int) -> [SimilarEntry] {
        Array(
            normalizedDictionary
                .map { SimilarEntry(entry: $0.entry, similarity: similarity(text, $0.word)) }
                .sorted { $0.similarity > $1.similarity }
                .prefix(k)
        )
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
    static func normalize(_ text: String) -> String {
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
