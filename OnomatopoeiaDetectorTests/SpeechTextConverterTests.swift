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

    /// 長音符を母音へ展開してしまうと、オノマトペの表記も評価も崩れる（どーん → どおん）。
    func testKeepsLongSoundMark() {
        for text in ["どーん", "ぐーん", "ざーざー", "がーん", "びゅーん"] {
            XCTAssertEqual(SpeechTextConverter.hiraganaText(from: text), text)
        }
        XCTAssertEqual(SpeechTextConverter.hiraganaText(from: "ドーン"), "どーん")
        XCTAssertEqual(SpeechTextConverter.hiraganaText(from: "ザーザー"), "ざーざー")
    }

    /// 変換済みのテキストを再度渡しても結果が変わらないこと。
    func testConversionIsIdempotent() {
        for text in ["どーん", "ざーざー", "しーん", "ふわふわ", "にっぽんご"] {
            let once = SpeechTextConverter.hiraganaText(from: text)
            XCTAssertEqual(SpeechTextConverter.hiraganaText(from: once), once)
        }
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
