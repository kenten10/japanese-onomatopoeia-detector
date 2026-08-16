import XCTest
@testable import OnomatopoeiaDetector

@MainActor
final class PersistenceControllerTests: XCTestCase {
    private var persistence: PersistenceController!

    override func setUp() {
        super.setUp()
        persistence = PersistenceController(inMemory: true)
    }

    override func tearDown() {
        persistence = nil
        super.tearDown()
    }

    func testAddAndFetchHistoryReturnsNewestFirst() throws {
        try persistence.addHistory(inputText: "ふわふわ", score: 5)
        try persistence.addHistory(inputText: "さらさら", score: 4)

        let history = try persistence.fetchHistory()

        XCTAssertEqual(history.count, 2)
        XCTAssertEqual(history.first?.inputText, "さらさら")
        XCTAssertEqual(history.first?.score, 4)
    }

    func testDeleteHistoryItemRemovesOnlySelectedItem() throws {
        try persistence.addHistory(inputText: "ふわふわ", score: 5)
        try persistence.addHistory(inputText: "さらさら", score: 4)
        let target = try persistence.fetchHistory().first { $0.inputText == "ふわふわ" }

        XCTAssertNotNil(target)
        try persistence.delete(item: target!)

        let history = try persistence.fetchHistory()
        XCTAssertEqual(history.map(\.inputText), ["さらさら"])
    }

    func testHistoryIsPrunedToOneHundredItems() throws {
        for index in 0..<105 {
            try persistence.addHistory(inputText: "word-\(index)", score: 3)
        }

        XCTAssertEqual(try persistence.fetchHistory().count, 100)
    }

    /// 間引きで消えるのは古い方であること。`Date()` は続けて呼ぶと同じ値を返すことがあり、
    /// 日付が同値だと並び順が決まらないため、新しい方が消えることがあった。
    func testPruningKeepsTheNewestItems() throws {
        for index in 0..<105 {
            try persistence.addHistory(inputText: "word-\(index)", score: 3)
        }

        let history = try persistence.fetchHistory()
        let texts = history.map(\.inputText)

        XCTAssertEqual(history.count, 100)
        XCTAssertEqual(texts.first, "word-104")
        XCTAssertFalse(texts.contains("word-0"))
        XCTAssertFalse(texts.contains("word-4"))
        XCTAssertTrue(texts.contains("word-5"))
    }

    /// 取得結果が新しい順に並んでいること。
    func testHistoryIsReturnedInDescendingDateOrder() throws {
        for text in ["ふわふわ", "さらさら", "どきどき"] {
            try persistence.addHistory(inputText: text, score: 3)
        }

        let history = try persistence.fetchHistory()

        XCTAssertEqual(history.map(\.inputText), ["どきどき", "さらさら", "ふわふわ"])
        XCTAssertEqual(history.map(\.date), history.map(\.date).sorted(by: >))
    }
}
