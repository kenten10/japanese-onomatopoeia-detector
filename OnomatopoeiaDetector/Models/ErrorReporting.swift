import Foundation
import Sentry

/// 不具合の報告。
///
/// 送るのは「アプリが落ちた・失敗した」ことを直すために要る情報だけに絞る。
/// 音声、認識したことば、判定履歴は送らない。DSN を設定しなければ何も送らない。
enum ErrorReporting {

    /// 送信先。ビルド設定から Info.plist を通して受け取る。
    private static var dsn: String {
        let value = Bundle.main.object(forInfoDictionaryKey: "SentryDSN") as? String ?? ""
        return value.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    static var isEnabled: Bool { !dsn.isEmpty }

    static func start() {
        let dsn = dsn
        guard !dsn.isEmpty else { return }

        SentrySDK.start { options in
            options.dsn = dsn

            // 既定で付く IP アドレスや利用者情報を送らない
            options.sendDefaultPii = false
            // 画面の写しと構成は、認識したことばが写り込むため採らない
            options.attachScreenshot = false
            options.attachViewHierarchy = false
            // 操作の記録は入力内容をなぞってしまうので採らない
            options.enableAutoBreadcrumbTracking = false
            // 性能計測は行わない
            options.tracesSampleRate = 0

            options.beforeSend = { event in scrub(event) }
        }
    }

    /// 送る直前に、本文が混じりうる場所を落とす。
    private static func scrub(_ event: Event) -> Event? {
        event.user = nil
        event.request = nil
        event.breadcrumbs = nil
        return event
    }
}
