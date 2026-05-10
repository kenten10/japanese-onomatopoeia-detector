import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var vm: AppViewModel
    @State private var showClearConfirm = false

    private let appVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"

    var body: some View {
        NavigationStack {
            Form {
                // Language
                Section {
                    Picker(String(localized: "settings.language"), selection: $vm.appLanguage) {
                        ForEach(AppLanguage.allCases, id: \.self) { lang in
                            Text(lang.displayName).tag(lang)
                        }
                    }
                    .pickerStyle(.menu)
                    .onChange(of: vm.appLanguage) { _, newValue in
                        vm.setLanguage(newValue)
                    }
                } header: {
                    Text(String(localized: "settings.language"))
                }

                // History
                Section {
                    Button(role: .destructive) {
                        showClearConfirm = true
                    } label: {
                        Label(String(localized: "settings.history.clear"), systemImage: "trash")
                    }
                } header: {
                    Text(String(localized: "history.title"))
                }

                // About
                Section {
                    HStack {
                        Text(String(localized: "settings.version"))
                        Spacer()
                        Text(appVersion)
                            .foregroundStyle(.secondary)
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        Text(String(localized: "settings.about"))
                            .font(.subheadline.weight(.semibold))
                        Text(String(localized: "settings.about.desc"))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 4)
                } header: {
                    Text(String(localized: "settings.about"))
                }
            }
            .navigationTitle(String(localized: "settings.title"))
            .confirmationDialog(
                String(localized: "history.clear.confirm"),
                isPresented: $showClearConfirm,
                titleVisibility: .visible
            ) {
                Button(String(localized: "history.clear.yes"), role: .destructive) {
                    vm.clearAllHistory()
                }
                Button(String(localized: "history.clear.cancel"), role: .cancel) {}
            }
        }
    }
}
