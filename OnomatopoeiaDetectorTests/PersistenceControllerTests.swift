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
}
