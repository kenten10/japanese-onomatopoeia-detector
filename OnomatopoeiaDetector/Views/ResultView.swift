import SwiftUI

struct ResultView: View {
    let result: EvaluationResult
    let onDismiss: () -> Void

    @Environment(AppViewModel.self) private var vm
    @State private var starsShown = 0
    @State private var burstIn = false
    @State private var savedFeedback = false
    @State private var saveResetTask: Task<Void, Never>?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 22) {
                    sfxBurst
                    scoreCard
                    if result.score >= 3 && !result.similarEntries.isEmpty {
                        similarSection
                    }
                    actionButtons
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 24)
            }
            .background(Ink.paper.ignoresSafeArea())
            .navigationTitle(String(localized: "result.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { onDismiss() } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 15, weight: .black))
                            .foregroundStyle(Ink.ink)
                    }
                }
            }
        }
        .onAppear {
            withAnimation(.spring(response: 0.45, dampingFraction: 0.55)) { burstIn = true }
            animateStars()
        }
        .onDisappear { saveResetTask?.cancel() }
    }

    // MARK: - SFX burst (signature)

    private var sfxBurst: some View {
        ZStack {
            SpeedLines(color: Ink.ink.opacity(0.18))
            Halftone(color: Ink.vermilion.opacity(0.10))

            Text(result.inputText)
                .font(.sfx(min(52, 220 / max(CGFloat(result.inputText.count), 3) + 20)))
                .foregroundStyle(Ink.ink)
                .minimumScaleFactor(0.4)
                .lineLimit(2)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 12)
                .rotationEffect(.degrees(-4))
                .scaleEffect(burstIn ? 1 : 0.4)
                .opacity(burstIn ? 1 : 0)
        }
        .frame(height: 180)
        .frame(maxWidth: .infinity)
        .mangaPanel(radius: 10)
        .overlay(alignment: .topLeading) {
            Text(String(localized: "result.recognized").uppercased())
                .font(.system(size: 10, weight: .bold, design: .monospaced))
                .tracking(1.5)
                .foregroundStyle(Ink.paper)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(Ink.ink)
                .padding(12)
        }
    }

    // MARK: - Score card

    private var scoreCard: some View {
        VStack(spacing: 14) {
            Text(String(localized: "result.score.label").uppercased())
                .font(.system(size: 11, weight: .bold, design: .monospaced))
                .tracking(2)
                .foregroundStyle(Ink.ink.opacity(Ink.secondaryOpacity))

            HStack(spacing: 6) {
                ForEach(1...5, id: \.self) { i in
                    Image(systemName: i <= starsShown ? "star.fill" : "star")
                        .font(.system(size: 34, weight: .black))
                        .foregroundStyle(i <= result.score ? Ink.score(result.score) : Ink.ink.opacity(0.15))
                        .scaleEffect(i <= starsShown ? 1 : 0.7)
                        .animation(.spring(response: 0.3, dampingFraction: 0.5), value: starsShown)
                }
            }
            .accessibilityLabel(String(format: String(localized: "score.stars"), result.score))

            HStack(alignment: .firstTextBaseline, spacing: 2) {
                Text("\(result.score)")
                    .font(.sfx(48))
                    .foregroundStyle(Ink.score(result.score))
                Text("/ 5")
                    .font(.mangaHeading(22))
                    .foregroundStyle(Ink.ink.opacity(0.5))
            }

            Text(result.scoreComment)
                .font(.mangaHeading(16))
                .foregroundStyle(Ink.ink)
                .multilineTextAlignment(.center)
        }
        .padding(22)
        .frame(maxWidth: .infinity)
        .mangaPanel(radius: 10)
    }

    // MARK: - Similar

    private var similarSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label(String(localized: "result.similar.title"), systemImage: "sparkles")
                .font(.mangaHeading(17))
                .foregroundStyle(Ink.ink)

            ForEach(result.similarEntries) { similar in
                SimilarEntryCard(entry: similar.entry)
            }
        }
    }

    // MARK: - Actions

    private var actionButtons: some View {
        VStack(spacing: 12) {
            Button {
                if vm.saveCurrentResult() {
                    withAnimation { savedFeedback = true }
                    saveResetTask?.cancel()
                    saveResetTask = Task {
                        try? await Task.sleep(for: .seconds(1.5))
                        if !Task.isCancelled { savedFeedback = false }
                    }
                }
            } label: {
                Label(
                    savedFeedback ? String(localized: "result.saved") : String(localized: "result.save"),
                    systemImage: savedFeedback ? "checkmark" : "bookmark.fill"
                )
                .font(.mangaHeading(16))
                .foregroundStyle(Ink.paper)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 15)
                .background(savedFeedback ? Ink.score(5) : Ink.ink)
                .animation(.default, value: savedFeedback)
            }
            .buttonStyle(MangaButtonStyle())

            Button { onDismiss() } label: {
                Text(String(localized: "result.again"))
                    .font(.mangaHeading(16))
                    .foregroundStyle(Ink.ink)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 15)
                    .background(Ink.panel)
            }
            .buttonStyle(MangaButtonStyle())
        }
    }

    // MARK: - Animation

    private func animateStars() {
        starsShown = 0
        for i in 1...result.score {
            let delay = Double(i) * 0.12
            Task { @MainActor in
                try? await Task.sleep(for: .seconds(delay))
                starsShown = i
            }
        }
    }
}

// MARK: - Manga button style (押し込みでオフセット影が縮む)

struct MangaButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        let pressed = configuration.isPressed
        return configuration.label
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(Ink.ink, lineWidth: 2.5))
            .background(
                RoundedRectangle(cornerRadius: 8)
                    .fill(Ink.ink)
                    .offset(x: pressed ? 1 : 4, y: pressed ? 1 : 4)
            )
            .offset(x: pressed ? 3 : 0, y: pressed ? 3 : 0)
            .animation(.easeOut(duration: 0.08), value: pressed)
    }
}

// MARK: - SimilarEntryCard

struct SimilarEntryCard: View {
    let entry: OnomatopoeiaEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .firstTextBaseline) {
                Text(entry.word)
                    .font(.sfx(24))
                    .foregroundStyle(Ink.ink)

                Text("（\(entry.reading)）")
                    .font(.caption)
                    .foregroundStyle(Ink.ink.opacity(Ink.secondaryOpacity))

                Spacer()

                Text(entry.category)
                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .foregroundStyle(Ink.paper)
                    .background(Ink.vermilionText, in: Capsule())
            }

            Rectangle().fill(Ink.ink.opacity(0.15)).frame(height: 1)

            VStack(alignment: .leading, spacing: 6) {
                meaningRow(flag: "🇯🇵", text: entry.meaning_ja)
                meaningRow(flag: "🇬🇧", text: entry.meaning_en)
            }

            VStack(alignment: .leading, spacing: 4) {
                Label(String(localized: "result.similar.example"), systemImage: "text.bubble")
                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                    .foregroundStyle(Ink.ink.opacity(Ink.secondaryOpacity))

                Text("・\(entry.example_ja)")
                    .font(.caption)
                    .foregroundStyle(Ink.ink.opacity(0.7))
                Text("・\(entry.example_en)")
                    .font(.caption)
                    .foregroundStyle(Ink.ink.opacity(0.7))
            }
        }
        .padding(16)
        .mangaPanel(radius: 8, offset: 3)
    }

    private func meaningRow(flag: String, text: String) -> some View {
        HStack(alignment: .top, spacing: 6) {
            Text(flag)
            Text(text)
                .font(.subheadline)
                .foregroundStyle(Ink.ink)
        }
    }
}
