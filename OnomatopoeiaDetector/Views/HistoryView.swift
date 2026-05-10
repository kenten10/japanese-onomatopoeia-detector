import SwiftUI

struct HistoryView: View {
    @EnvironmentObject var vm: AppViewModel
    @State private var showClearConfirm = false

    var body: some View {
        NavigationStack {
            Group {
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
                        .font(.caption)
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

    // MARK: - Empty State

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "clock.badge.questionmark")
                .font(.system(size: 60))
                .foregroundStyle(.secondary.opacity(0.5))
            Text(String(localized: "history.empty"))
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - History List

    private var historyList: some View {
        List {
            ForEach(vm.history) { item in
                HistoryRow(item: item)
                    .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16))
            }
            .onDelete { indexSet in
                indexSet.forEach { vm.deleteHistoryItem(vm.history[$0]) }
            }
        }
        .listStyle(.insetGrouped)
    }
}

// MARK: - HistoryRow

struct HistoryRow: View {
    let item: HistoryItem

    private var starColor: Color {
        switch item.score {
        case 5: return .yellow
        case 4: return .indigo
        case 3: return .green
        case 2: return .orange
        default: return .gray
        }
    }

    var body: some View {
        HStack(spacing: 12) {
            // Score badge
            ZStack {
                Circle()
                    .fill(starColor.opacity(0.15))
                    .frame(width: 44, height: 44)

                Text("\(item.score)")
                    .font(.system(size: 18, weight: .bold, design: .rounded))
                    .foregroundStyle(starColor)
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(item.inputText)
                    .font(.headline)
                    .lineLimit(1)

                HStack(spacing: 4) {
                    ForEach(1...5, id: \.self) { i in
                        Image(systemName: i <= item.score ? "star.fill" : "star")
                            .font(.system(size: 10))
                            .foregroundStyle(i <= item.score ? starColor : Color(.systemGray4))
                    }

                    Text(item.date.formatted(date: .abbreviated, time: .shortened))
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .padding(.leading, 4)
                }
            }

            Spacer()
        }
        .padding(.vertical, 4)
    }
}
