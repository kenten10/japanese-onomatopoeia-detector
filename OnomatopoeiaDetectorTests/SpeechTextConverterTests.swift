import XCTest
@testable import OnomatopoeiaDetector

final class SpeechTextConverterTests: XCTestCase {
    func testConvertsKatakanaToHiragana() {
        XCTAssertEqual(SpeechTextConverter.hiraganaText(from: "ドキドキ"), "どきどき")
    }

    func testConvertsRecognizedJapaneseTextToHiraganaReading() {
        let converted = SpeechTextConverter.hiraganaText(from: "日本語を入力")

        XCTAssertFalse(converted.containsKanji)
        XCTAssertTrue(converted.contains("にほんご") || converted.contains("にっぽんご"))
        XCTAssertTrue(converted.contains("にゅうりょく"))
    }

    func testPreservesReceivedHiraganaPronunciation() {
        XCTAssertEqual(SpeechTextConverter.hiraganaText(from: "にほんご"), "にほんご")
        XCTAssertEqual(SpeechTextConverter.hiraganaText(from: "にっぽんご"), "にっぽんご")
    }

    func testCorrectsOnomatopoeiaLongSoundWrittenAsVowelKana() {
        XCTAssertEqual(SpeechTextConverter.hiraganaText(from: "しいん"), "しーん")
        XCTAssertEqual(SpeechTextConverter.hiraganaText(from: "シーン"), "しーん")
    }
}

private extension String {
    var containsKanji: Bool {
        unicodeScalars.contains { scalar in
            (0x4E00...0x9FFF).contains(scalar.value)
                || (0x3400...0x4DBF).contains(scalar.value)
                || (0xF900...0xFAFF).contains(scalar.value)
        }
    }
}
