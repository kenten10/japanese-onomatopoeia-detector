import SwiftUI

struct ContentView: View {
    @Environment(AppViewModel.self) private var vm

    var body: some View {
        TabView {
            HomeView()
                .tabItem {
                    Label(String(localized: "home.tab"), systemImage: "waveform")
                }

            HistoryView()
                .tabItem {
                    Label(String(localized: "history.title"), systemImage: "book.closed.fill")
                }

            SettingsView()
                .tabItem {
                    Label(String(localized: "settings.title"), systemImage: "gearshape.fill")
                }
        }
        .tint(Ink.vermilion)
    }
}
