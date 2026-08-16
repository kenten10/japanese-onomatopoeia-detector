import SwiftUI

struct HistoryView: View {
    @Environment(AppViewModel.self) private var vm
    @State private var showClearConfirm = false

    var body: some View {
        NavigationStack {
            ZStack {
                Ink.paper.ignoresSafeArea()

                if vm.history.isEmpty {
                    emptyState
                } else {
                    historyList
                }
            }
            .navigationTitle(String(localized: "history.title"))
            .toolbar {
                if !vm.history.isEmpty {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button(String(localized: "history.clear.all"), role: .destructive) {
                            showClearConfirm = true
                        }
                        .font(.system(size: 13, weight: .bold))
                        .tint(Ink.vermilionText)
                    }
                }
            }
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

    // MARK: - Empty state

    private var emptyState: some View {
        VStack(spacing: 12) {
            // マンガで「閑散・空っぽ」を表す描き文字
            Text("がらーん")
                .font(.sfx(52))
                .foregroundStyle(Ink.ink.opacity(0.16))
                .rotationEffect(.degrees(-5))
                .accessibilityHidden(true)
            Text(String(localized: "history.empty"))
                .font(.mangaHeading(16))
                .foregroundStyle(Ink.ink.opacity(Ink.secondaryOpacity))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - List

    private var historyList: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(vm.history) { item in
                    HistoryRow(item: item)
                        .contextMenu {
                            Button(String(localized: "history.delete"), role: .destructive) {
                                vm.deleteHistoryItem(item)
                            }
                        }
                }
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 12)
        }
    }
}

// MARK: - HistoryRow

struct HistoryRow: View {
    let item: HistoryItem

    var body: some View {
        HStack(spacing: 14) {
            // スコアバッジ
            ZStack {
                Circle()
                    .fill(Ink.score(item.score))
                    .frame(width: 46, height: 46)
                    .overlay(Circle().stroke(Ink.ink, lineWidth: 2))
                Text("\(item.score)")
                    .font(.sfx(20))
                    .foregroundStyle(Ink.paper)
            }

            VStack(alignment: .leading, spacing: 5) {
                Text(item.inputText)
                    .font(.mangaHeading(18))
                    .foregroundStyle(Ink.ink)
                    .lineLimit(1)

                HStack(spacing: 3) {
                    ForEach(1...5, id: \.self) { i in
                        Image(systemName: i <= item.score ? "star.fill" : "star")
                            .font(.system(size: 9, weight: .black))
                            .foregroundStyle(i <= item.score ? Ink.score(item.score) : Ink.ink.opacity(0.15))
                    }
                    Text(item.date.formatted(date: .abbreviated, time: .shortened))
                        .font(.system(size: 11, weight: .medium, design: .monospaced))
                        .foregroundStyle(Ink.ink.opacity(Ink.secondaryOpacity))
                        .padding(.leading, 6)
                }
            }

            Spacer()
        }
        .padding(14)
        .mangaPanel(radius: 8, offset: 3)
    }
}
