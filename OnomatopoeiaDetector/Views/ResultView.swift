import SwiftUI

struct ResultView: View {
    let result: EvaluationResult
    let onDismiss: () -> Void

    @EnvironmentObject var vm: AppViewModel
    @State private var starsAnimated = 0
    @State private var savedFeedback = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    // Recognized text
                    recognizedTextCard

                    // Score card
                    scoreCard

                    // Similar onomatopoeia (score >= 3)
                    if result.score >= 3 && !result.similarEntries.isEmpty {
                        similarSection
                    }

                    // Action buttons
                    actionButtons
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 24)
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle(String(localized: "result.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        onDismiss()
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundStyle(.secondary)
                    }
                }
            }
        }
        .onAppear {
            animateStars()
        }
    }

    // MARK: - Recognized Text

    private var recognizedTextCard: some View {
        VStack(spacing: 8) {
            Text(String(localized: "result.recognized"))
                .font(.caption)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)

            Text(result.inputText)
                .font(.system(size: 36, weight: .bold, design: .rounded))
                .foregroundStyle(.primary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
        }
        .padding(20)
        .background(.background, in: RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.06), radius: 8, y: 2)
    }

    // MARK: - Score Card

    private var scoreCard: some View {
        VStack(spacing: 16) {
            Text(String(localized: "result.score.label"))
                .font(.caption)
                .foregroundStyle(.secondary)

            // Stars
            HStack(spacing: 8) {
                ForEach(1...5, id: \.self) { i in
                    Image(systemName: i <= starsAnimated ? "star.fill" : "star")
                        .font(.system(size: 36))
                        .foregroundStyle(i <= result.score ? scoreColor : Color(.systemGray4))
                        .scaleEffect(i <= starsAnimated ? 1.0 : 0.8)
                        .animation(.spring(response: 0.3, dampingFraction: 0.5).delay(Double(i) * 0.08), value: starsAnimated)
                }
            }
            .accessibilityLabel(String(format: String(localized: "score.stars"), result.score))

            // Score number badge
            Text("\(result.score) / 5")
                .font(.title.bold())
                .foregroundStyle(scoreColor)

            // Comment
            Text(result.scoreComment)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(24)
        .frame(maxWidth: .infinity)
        .background(.background, in: RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.06), radius: 8, y: 2)
    }

    private var scoreColor: Color {
        switch result.score {
        case 5: return .yellow
        case 4: return .indigo
        case 3: return .green
        case 2: return .orange
        default: return .gray
        }
    }

    // MARK: - Similar Section

    private var similarSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label(String(localized: "result.similar.title"), systemImage: "sparkles")
                .font(.headline)
                .foregroundStyle(.indigo)

            ForEach(result.similarEntries) { similar in
                SimilarEntryCard(entry: similar.entry)
            }
        }
    }

    // MARK: - Action Buttons

    private var actionButtons: some View {
        VStack(spacing: 12) {
            // Save button
            Button {
                vm.saveCurrentResult()
                withAnimation { savedFeedback = true }
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                    savedFeedback = false
                }
            } label: {
                Label(
                    savedFeedback ? String(localized: "result.saved") : String(localized: "result.save"),
                    systemImage: savedFeedback ? "checkmark.circle.fill" : "bookmark.fill"
                )
                .font(.subheadline.weight(.semibold))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(savedFeedback ? Color.green : Color.indigo, in: RoundedRectangle(cornerRadius: 12))
                .foregroundStyle(.white)
                .animation(.default, value: savedFeedback)
            }

            // Try again
            Button {
                onDismiss()
            } label: {
                Text(String(localized: "result.again"))
                    .font(.subheadline.weight(.semibold))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
                    .foregroundStyle(.primary)
            }
        }
    }

    // MARK: - Animation

    private func animateStars() {
        starsAnimated = 0
        for i in 1...result.score {
            DispatchQueue.main.asyncAfter(deadline: .now() + Double(i) * 0.12) {
                starsAnimated = i
            }
        }
    }
}

// MARK: - SimilarEntryCard

struct SimilarEntryCard: View {
    let entry: OnomatopoeiaEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            // Word + reading + category
            HStack(alignment: .firstTextBaseline) {
                Text(entry.word)
                    .font(.system(size: 22, weight: .bold, design: .rounded))
                    .foregroundStyle(.indigo)

                Text("（\(entry.reading)）")
                    .font(.caption)
                    .foregroundStyle(.secondary)

                Spacer()

                Text(entry.category)
                    .font(.caption2.weight(.medium))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(Color.indigo.opacity(0.12), in: Capsule())
                    .foregroundStyle(.indigo)
            }

            Divider()

            // Meanings
            VStack(alignment: .leading, spacing: 6) {
                meaningRow(flag: "🇯🇵", text: entry.meaning_ja)
                meaningRow(flag: "🇬🇧", text: entry.meaning_en)
            }

            // Examples
            VStack(alignment: .leading, spacing: 4) {
                Label(String(localized: "result.similar.example"), systemImage: "text.bubble")
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(.secondary)

                Text("・\(entry.example_ja)")
                    .font(.caption)
                    .foregroundStyle(.secondary)

                Text("・\(entry.example_en)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(16)
        .background(.background, in: RoundedRectangle(cornerRadius: 14))
        .overlay(
            RoundedRectangle(cornerRadius: 14)
                .strokeBorder(Color.indigo.opacity(0.15), lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.04), radius: 6, y: 2)
    }

    private func meaningRow(flag: String, text: String) -> some View {
        HStack(alignment: .top, spacing: 6) {
            Text(flag)
            Text(text)
                .font(.subheadline)
                .foregroundStyle(.primary)
        }
    }
}
