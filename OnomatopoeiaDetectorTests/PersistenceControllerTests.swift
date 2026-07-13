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

    func testAddAndFetchHistoryReturnsNewestFirst() {
        persistence.addHistory(inputText: "ふわふわ", score: 5)
        persistence.addHistory(inputText: "さらさら", score: 4)

        let history = persistence.fetchHistory()

        XCTAssertEqual(history.count, 2)
        XCTAssertEqual(history.first?.inputText, "さらさら")
        XCTAssertEqual(history.first?.score, 4)
    }

    func testDeleteHistoryItemRemovesOnlySelectedItem() {
        persistence.addHistory(inputText: "ふわふわ", score: 5)
        persistence.addHistory(inputText: "さらさら", score: 4)
        let target = persistence.fetchHistory().first { $0.inputText == "ふわふわ" }

        XCTAssertNotNil(target)
        persistence.delete(item: target!)

        let history = persistence.fetchHistory()
        XCTAssertEqual(history.map(\.inputText), ["さらさら"])
    }

    func testHistoryIsPrunedToOneHundredItems() {
        for index in 0..<105 {
            persistence.addHistory(inputText: "word-\(index)", score: 3)
        }

        XCTAssertEqual(persistence.fetchHistory().count, 100)
    }
}
