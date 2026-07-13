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
}
