import XCTest
@testable import OnomatopoeiaDetector

final class AppLanguageTests: XCTestCase {
    override func tearDown() {
        UserDefaults.standard.removeObject(forKey: "appLanguage")
        UserDefaults.standard.removeObject(forKey: "AppleLanguages")
        super.tearDown()
    }

    func testDefaultLanguageIsEnglishWhenNothingIsStored() {
        UserDefaults.standard.removeObject(forKey: "appLanguage")

        XCTAssertEqual(AppLanguage.stored, .english)
    }

    func testPersistStoresLanguageAndAppliesAppleLanguages() {
        AppLanguage.japanese.persist()

        XCTAssertEqual(AppLanguage.stored, .japanese)
        XCTAssertEqual(UserDefaults.standard.stringArray(forKey: "AppleLanguages"), ["ja"])
    }

    func testSystemLanguageClearsAppleLanguagesOverride() {
        AppLanguage.japanese.persist()
        AppLanguage.system.persist()

        XCTAssertEqual(AppLanguage.stored, .system)
        let bundleID = Bundle.main.bundleIdentifier ?? "com.kensukeyoshida.OnomatopoeiaDetector"
        XCTAssertNil(UserDefaults.standard.persistentDomain(forName: bundleID)?["AppleLanguages"])
    }
}
