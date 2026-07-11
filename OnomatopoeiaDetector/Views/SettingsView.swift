import SwiftUI

struct SettingsView: View {
    @Environment(AppViewModel.self) private var vm
    @State private var showClearConfirm = false

    private let appVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"

    var body: some View {
        @Bindable var vm = vm
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
                    .tint(Ink.vermilion)
                    .onChange(of: vm.appLanguage) { _, newValue in
                        vm.setLanguage(newValue)
                    }
                } header: {
                    sectionHeader("settings.language")
                }

                // History
                Section {
                    Button(role: .destructive) {
                        showClearConfirm = true
                    } label: {
                        Label(String(localized: "settings.history.clear"), systemImage: "trash")
                    }
                    .tint(Ink.vermilion)
                } header: {
                    sectionHeader("history.title")
                }

                // About
                Section {
                    HStack {
                        Text(String(localized: "settings.version"))
                            .foregroundStyle(Ink.ink)
                        Spacer()
                        Text(appVersion)
                            .font(.system(.body, design: .monospaced))
                            .foregroundStyle(Ink.ink.opacity(0.5))
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        Text(String(localized: "settings.about"))
                            .font(.mangaHeading(15))
                            .foregroundStyle(Ink.ink)
                        Text(String(localized: "settings.about.desc"))
                            .font(.caption)
                            .foregroundStyle(Ink.ink.opacity(0.6))
                    }
                    .padding(.vertical, 4)
                } header: {
                    sectionHeader("settings.about")
                }
            }
            .scrollContentBackground(.hidden)
            .background(Ink.paper.ignoresSafeArea())
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

    private func sectionHeader(_ key: String.LocalizationValue) -> some View {
        Text(String(localized: key).uppercased())
            .font(.system(size: 11, weight: .bold, design: .monospaced))
            .tracking(1.5)
            .foregroundStyle(Ink.ink.opacity(0.5))
    }
}
