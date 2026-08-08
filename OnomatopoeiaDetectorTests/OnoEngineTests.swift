import XCTest
@testable import OnomatopoeiaDetector

final class OnoEngineTests: XCTestCase {
    func testKnownOnomatopoeiaReceivesHighScore() async {
        let result = await OnoEngine.shared.evaluate(text: "ふわふわ")

        XCTAssertEqual(result.inputText, "ふわふわ")
        XCTAssertGreaterThanOrEqual(result.score, 3)
        XCTAssertLessThanOrEqual(result.score, 5)
        XCTAssertFalse(result.similarEntries.isEmpty)
    }

    func testDictionaryExactMatchReceivesPerfectScore() async {
        let result = await OnoEngine.shared.evaluate(text: "しーん")

        XCTAssertEqual(result.score, 5)
        XCTAssertFalse(result.similarEntries.isEmpty)
    }

    func testPlainPhraseReceivesLowerScoreThanOnomatopoeia() async {
        let onomatopoeia = await OnoEngine.shared.evaluate(text: "どきどき")
        let plainPhrase = await OnoEngine.shared.evaluate(text: "ごはんをたべる")

        XCTAssertGreaterThan(onomatopoeia.score, plainPhrase.score)
    }

    /// 辞書の見出しがカタカナでも、ひらがなの入力と一致すること。
    func testNormalizesKatakanaDictionaryEntriesBeforeMatching() async {
        for text in ["ドキドキ", "どきどき", "キラキラ", "きらきら"] {
            let result = await OnoEngine.shared.evaluate(text: text)
            XCTAssertEqual(result.score, 5, "input=\(text)")
        }

        let similar = await OnoEngine.shared.evaluate(text: "ドキドキ")
        XCTAssertEqual(similar.similarEntries.first?.entry.word, "ドキドキ")
    }

    /// PWA版・Android版と同じ点数になること。3実装で同じ表を持ち、
    /// どれかの評価だけが動いたときに気付けるようにしている。
    func testMatchesScoreTableSharedWithOtherPlatforms() async {
        let expected: [(String, Int)] = [
            ("ふわふわ", 5), ("しーん", 5), ("ドキドキ", 5), ("どきどき", 5),
            ("キラキラ", 5), ("きらきら", 5), ("ぐるぐる", 5),
            ("ざーざー", 3), ("どーん", 3),
            ("ふわふわと", 2), ("わくわくする", 2), ("ごはんをたべる", 2)
        ]

        for (input, score) in expected {
            let result = await OnoEngine.shared.evaluate(text: input)
            XCTAssertEqual(result.score, score, "input=\(input)")
        }
    }

    /// 撥音止めのオノマトペを助詞込みの文として減点しないこと。
    func testDoesNotPenaliseOnomatopoeiaEndingWithSyllabicNasal() async {
        for text in ["どーん", "がーん"] {
            let result = await OnoEngine.shared.evaluate(text: text)
            XCTAssertEqual(result.score, 3, "input=\(text)")
        }
    }

    /// 記号だけの認識結果は正規化すると空になり、あらゆる見出しの部分文字列として
    /// 一致してしまっていた。1文字も辞書のどこかに必ず含まれる。
    func testDoesNotRewardEmptyOrSingleCharacterInput() async {
        for text in ["。", "、。", "ん", "あ", "っ"] {
            let result = await OnoEngine.shared.evaluate(text: text)
            XCTAssertLessThan(result.score, 3, "input=\(text)")
            XCTAssertTrue(result.similarEntries.isEmpty, "input=\(text)")
        }
    }
}
