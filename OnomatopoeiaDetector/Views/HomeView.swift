import SwiftUI

struct HomeView: View {
    @Environment(AppViewModel.self) private var vm
    @State private var showResult = false
    @State private var pulse = false

    private var isRecording: Bool {
        if case .recording = vm.recordingState { return true }
        return false
    }

    private var isProcessing: Bool {
        switch vm.recordingState {
        case .recognizing, .evaluating: return true
        default: return false
        }
    }

    private var errorMessage: String? {
        if case .error(let message) = vm.recordingState { return message }
        return nil
    }

    private var showError: Binding<Bool> {
        Binding(
            get: { errorMessage != nil },
            set: { presenting in if !presenting { vm.resetToIdle() } }
        )
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Ink.paper.ignoresSafeArea()

                // 上部にだけ薄く網点を敷き、紙の質感を出す
                Halftone(color: Ink.ink.opacity(0.06))
                    .frame(height: 220)
                    .frame(maxHeight: .infinity, alignment: .top)
                    .ignoresSafeArea()

                VStack(spacing: 0) {
                    appHeader
                    Spacer()
                    centerContent
                    Spacer()
                    micButton
                        .padding(.bottom, 44)
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
                if case .result = newState { showResult = true }
            }
            .alert(String(localized: "error.title"), isPresented: showError) {
                Button(String(localized: "error.ok"), role: .cancel) { vm.resetToIdle() }
            } message: {
                Text(errorMessage ?? "")
            }
        }
    }

    // MARK: - Header

    private var appHeader: some View {
        VStack(spacing: 6) {
            Text(String(localized: "app.title"))
                .font(.mangaHeading(30))
                .foregroundStyle(Ink.ink)
                .overlay(alignment: .bottom) {
                    // 見出しの下に朱色の力線
                    Ink.vermilion
                        .frame(height: 4)
                        .offset(y: 6)
                }

            Text(String(localized: "app.subtitle").uppercased())
                .font(.system(size: 11, weight: .bold, design: .monospaced))
                .tracking(2)
                .foregroundStyle(Ink.ink.opacity(0.5))
        }
        .padding(.top, 28)
    }

    // MARK: - Center

    @ViewBuilder
    private var centerContent: some View {
        switch vm.recordingState {
        case .recording:
            recordingContent
        case .recognizing, .evaluating:
            processingContent
        default:
            idleContent
        }
    }

    private var idleContent: some View {
        VStack(spacing: 20) {
            // マンガで「静寂」を表す描き文字。無音状態そのものを主役に。
            Text("シーン…")
                .font(.sfx(64))
                .foregroundStyle(Ink.ink.opacity(0.16))
                .rotationEffect(.degrees(-6))

            VStack(spacing: 8) {
                Text(String(localized: "home.no.result"))
                    .font(.mangaHeading(22))
                    .foregroundStyle(Ink.ink)

                Text(String(localized: "home.no.result.sub"))
                    .font(.subheadline)
                    .foregroundStyle(Ink.ink.opacity(0.55))
                    .multilineTextAlignment(.center)
            }
        }
        .padding()
    }

    private var recordingContent: some View {
        VStack(spacing: 24) {
            WaveformView()
                .frame(height: 72)
                .padding(.horizontal, 40)

            Text(vm.speech.partialText.isEmpty
                 ? String(localized: "recording.listening")
                 : vm.speech.partialText)
                .font(.sfx(vm.speech.partialText.isEmpty ? 24 : 40))
                .foregroundStyle(vm.speech.partialText.isEmpty ? Ink.ink.opacity(0.4) : Ink.ink)
                .multilineTextAlignment(.center)
                .minimumScaleFactor(0.5)
                .lineLimit(2)
                .padding(.horizontal, 28)
                .animation(.spring(response: 0.35, dampingFraction: 0.7), value: vm.speech.partialText)

            Text(String(localized: "recording.tap.stop"))
                .font(.system(size: 12, weight: .bold, design: .monospaced))
                .tracking(1)
                .foregroundStyle(Ink.vermilion)
        }
    }

    private var processingContent: some View {
        VStack(spacing: 18) {
            ProgressView()
                .scaleEffect(1.4)
                .tint(Ink.vermilion)
            Text(String(localized: "recording.recognizing"))
                .font(.system(size: 13, weight: .bold, design: .monospaced))
                .tracking(1)
                .foregroundStyle(Ink.ink.opacity(0.6))
        }
    }

    // MARK: - Mic button

    private var micButton: some View {
        Button {
            if isRecording { vm.stopRecording() } else { vm.startRecording() }
        } label: {
            ZStack {
                // 録音中は集中線を放射
                if isRecording {
                    SpeedLines(color: Ink.vermilion.opacity(0.55), count: 40)
                        .frame(width: 168, height: 168)
                        .scaleEffect(pulse ? 1.06 : 0.9)
                        .opacity(pulse ? 0.9 : 0.4)
                        .animation(.easeInOut(duration: 0.7).repeatForever(autoreverses: true), value: pulse)
                }

                // ハードなオフセット影
                Circle()
                    .fill(Ink.ink)
                    .frame(width: 86, height: 86)
                    .offset(x: 4, y: 4)

                Circle()
                    .fill(isRecording ? Ink.ink : Ink.vermilion)
                    .frame(width: 86, height: 86)
                    .overlay(Circle().stroke(Ink.ink, lineWidth: 3))

                Image(systemName: isRecording ? "stop.fill" : "mic.fill")
                    .font(.system(size: 32, weight: .black))
                    .foregroundStyle(Ink.paper)
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(isRecording
                            ? String(localized: "recording.tap.stop")
                            : String(localized: "home.record.button"))
        .disabled(isProcessing)
        .opacity(isProcessing ? 0.4 : 1)
        .onAppear { pulse = true }
    }
}

// MARK: - WaveformView

struct WaveformView: View {
    @State private var phases: [CGFloat] = (0..<13).map { _ in CGFloat.random(in: 0.2...1.0) }
    private let timer = Timer.publish(every: 0.12, on: .main, in: .common).autoconnect()

    var body: some View {
        HStack(spacing: 5) {
            ForEach(0..<13, id: \.self) { i in
                RoundedRectangle(cornerRadius: 1.5)
                    .fill(i % 3 == 0 ? Ink.vermilion : Ink.ink)
                    .frame(width: 6)
                    .scaleEffect(y: phases[i], anchor: .center)
                    .animation(.easeInOut(duration: 0.18), value: phases[i])
            }
        }
        .onReceive(timer) { _ in
            for i in phases.indices { phases[i] = CGFloat.random(in: 0.15...1.0) }
        }
    }
}
