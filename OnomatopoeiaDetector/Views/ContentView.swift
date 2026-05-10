import SwiftUI

struct ContentView: View {
    @EnvironmentObject var vm: AppViewModel

    var body: some View {
        TabView {
            HomeView()
                .tabItem {
                    Label("home", systemImage: "mic.circle.fill")
                }

            HistoryView()
                .tabItem {
                    Label(String(localized: "history.title"), systemImage: "clock.fill")
                }

            SettingsView()
                .tabItem {
                    Label(String(localized: "settings.title"), systemImage: "gearshape.fill")
                }
        }
        .tint(.indigo)
    }
}
