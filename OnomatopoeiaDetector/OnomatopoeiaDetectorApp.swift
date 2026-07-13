import SwiftUI

@main
struct OnomatopoeiaDetectorApp: App {

    @State private var viewModel: AppViewModel

    init() {
        // UI 構築前に言語を適用する（既定は英語＝英語学習者向け）
        AppLanguage.applyStoredOrDefault()
        _viewModel = State(initialValue: AppViewModel())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(viewModel)
                .task {
                    await viewModel.requestPermissionsIfNeeded()
                }
        }
    }
}
