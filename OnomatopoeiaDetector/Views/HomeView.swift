import SwiftUI

struct HomeView: View {
    @EnvironmentObject var vm: AppViewModel
    @State private var showResult = false
    @State private var pulseScale: CGFloat = 1.0

    var body: some View {
        NavigationStack {
            ZStack {
                // Background gradient
                LinearGradient(
                    colors: [Color(.systemBackground), Color.indigo.opacity(0.08)],
                    startPoint: .top, endPoint: .bottom
                )
                .ignoresSafeArea()

                VStack(spacing: 0) {
                    // App header
                    appHeader

                    Spacer()

                    // State-dependent center content
                    centerContent

                    Spacer()

                    // Mic button
                    micButton
                        .padding(.bottom, 48)
                }
            }
            .navigationBarHidden(true)
            .sheet(isPresented: $showResult) {
                if case .result(let r) = vm.recordingState {
                    ResultView(result: r, onDismiss: {
                        showResult = false
                        vm.resetToIdle()
                    })
                }
            }
            .onChange(of: vm.recordingState) { _, newState in
                if case .result = newState {
                    showResult = true
                }
            }
        }
    }

    // MARK: - App Header

    private var appHeader: some View {
        VStack(spacing: 4) {
            Text(String(localized: "app.title"))
                .font(.system(size: 28, weight: .bold, design: .rounded))
                .foregroundStyle(
                    LinearGradient(colors: [.indigo, .purple], startPoint: .leading, endPoint: .trailing)
                )
            Text(String(localized: "app.subtitle"))
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(.top, 20)
        .padding(.bottom, 8)
    }

    // MARK: - Center Content

    @ViewBuilder
    private var centerContent: some View {
        switch vm.recordingState {
        case .idle:
            idleContent
        case .recording:
            recordingContent
        case .recognizing, .evaluating:
            processingContent
        case .result, .error:
            idleContent
        }
    }

    private var idleContent: some View {
        VStack(spacing: 16) {
            Image(systemName: "waveform.circle")
                .font(.system(size: 80))
                .foregroundStyle(.indigo.opacity(0.3))

            Text(String(localized: "home.no.result"))
                .font(.title2.bold())
                .foregroundStyle(.primary)

            Text(String(localized: "home.no.result.sub"))
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding()
    }

    private var recordingContent: some View {
        VStack(spacing: 20) {
            // Waveform animation
            WaveformView()
                .frame(height: 80)
                .padding(.horizontal, 32)

            Text(vm.speech.partialText.isEmpty
                 ? String(localized: "recording.listening")
                 : vm.speech.partialText)
                .font(.title3.bold())
                .foregroundStyle(.primary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
                .animation(.default, value: vm.speech.partialText)

            Text(String(localized: "recording.tap.stop"))
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }

    private var processingContent: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.5)
                .tint(.indigo)

            Text(String(localized: "recording.recognizing"))
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
    }

    // MARK: - Mic Button

    private var micButton: some View {
        let isRecording = {
            if case .recording = vm.recordingState { return true }
            return false
        }()

        return Button {
            if isRecording {
                vm.stopRecording()
            } else {
                vm.startRecording()
            }
        } label: {
            ZStack {
                // Pulse ring (recording only)
                if isRecording {
                    Circle()
                        .stroke(Color.red.opacity(0.3), lineWidth: 3)
                        .frame(width: 100, height: 100)
                        .scaleEffect(pulseScale)
                        .animation(
                            .easeInOut(duration: 0.8).repeatForever(autoreverses: true),
                            value: pulseScale
                        )
                }

                Circle()
                    .fill(isRecording
                          ? LinearGradient(colors: [.red, .pink], startPoint: .topLeading, endPoint: .bottomTrailing)
                          : LinearGradient(colors: [.indigo, .purple], startPoint: .topLeading, endPoint: .bottomTrailing))
                    .frame(width: 80, height: 80)
                    .shadow(color: (isRecording ? Color.red : Color.indigo).opacity(0.4), radius: 12, y: 6)

                Image(systemName: isRecording ? "stop.fill" : "mic.fill")
                    .font(.system(size: 32, weight: .semibold))
                    .foregroundStyle(.white)
            }
        }
        .accessibilityLabel(isRecording
                            ? String(localized: "recording.tap.stop")
                            : String(localized: "home.record.button"))
        .onAppear { pulseScale = 1.15 }
        .disabled({
            switch vm.recordingState {
            case .recognizing, .evaluating: return true
            default: return false
            }
        }())
    }
}

// MARK: - WaveformView

struct WaveformView: View {
    @State private var phases: [CGFloat] = (0..<12).map { _ in CGFloat.random(in: 0.2...1.0) }
    private let timer = Timer.publish(every: 0.12, on: .main, in: .common).autoconnect()

    var body: some View {
        HStack(spacing: 4) {
            ForEach(0..<12, id: \.self) { i in
                RoundedRectangle(cornerRadius: 3)
                    .fill(
                        LinearGradient(colors: [.indigo, .purple], startPoint: .bottom, endPoint: .top)
                    )
                    .frame(width: 6)
                    .scaleEffect(y: phases[i], anchor: .center)
                    .animation(.easeInOut(duration: 0.2), value: phases[i])
            }
        }
        .onReceive(timer) { _ in
            for i in phases.indices {
                phases[i] = CGFloat.random(in: 0.15...1.0)
            }
        }
    }
}
