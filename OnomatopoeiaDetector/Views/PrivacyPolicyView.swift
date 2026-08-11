import SwiftUI

struct PrivacyPolicyView: View {
    var body: some View {
        List {
            Section {
                Text(String(localized: "privacy.summary"))
            }

            Section(String(localized: "privacy.audio.title")) {
                Text(String(localized: "privacy.audio.body"))
            }

            Section(String(localized: "privacy.storage.title")) {
                Text(String(localized: "privacy.storage.body"))
            }

            Section(String(localized: "privacy.diagnostics.title")) {
                Text(String(localized: "privacy.diagnostics.body"))
            }

            Section(String(localized: "privacy.feedback.title")) {
                Text(String(localized: "privacy.feedback.body"))
            }

            Section(String(localized: "privacy.control.title")) {
                Text(String(localized: "privacy.control.body"))
            }
        }
        .navigationTitle(String(localized: "settings.privacy"))
        .navigationBarTitleDisplayMode(.inline)
    }
}
