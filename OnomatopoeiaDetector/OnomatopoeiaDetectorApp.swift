import SwiftUI

@main
struct OnomatopoeiaDetectorApp: App {

    @StateObject private var viewModel = AppViewModel()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(viewModel)
                .task {
                    await viewModel.requestPermissionsIfNeeded()
                }
        }
    }
}
