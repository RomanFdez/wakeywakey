import SwiftUI

struct AlertView: View {

    let info: AlertCoordinator.AlertInfo

    @EnvironmentObject var coordinator: AlertCoordinator
    @State private var now              = Date()
    @State private var showCustomSnooze = false
    @State private var swipeOffset: CGFloat = 0

    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    private var secondsLeft: TimeInterval { info.startTime.timeIntervalSince(now) }
    private var isOverdue: Bool           { secondsLeft < 0 }
    private var canSnoozeToStart: Bool    { secondsLeft > 90 }

    var body: some View {
        ZStack {
            Color.wkYellow.ignoresSafeArea()

            VStack(spacing: 0) {
                Spacer()

                // ── Reloj despertador ─────────────────────────────────────
                Image(systemName: "alarm.fill")
                    .font(.system(size: 52))
                    .foregroundStyle(Color.wkNavy.opacity(0.75))

                // ── Título ───────────────────────────────────────────────
                Text(info.title)
                    .font(.system(size: 32, weight: .heavy, design: .rounded))
                    .foregroundStyle(Color.wkNavy)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
                    .padding(.top, 20)

                // ── Hora ─────────────────────────────────────────────────
                Text("Empieza a las \(formattedTime)")
                    .font(.system(size: 16))
                    .foregroundStyle(Color.wkNavy.opacity(0.55))
                    .padding(.top, 8)

                // ── Sala / URL ───────────────────────────────────────────
                if let url = info.meetingURL {
                    Text("📍 \(friendlyRoomName(url))")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(Color.wkNavy.opacity(0.55))
                        .padding(.top, 4)
                }

                // ── Cuenta atrás ─────────────────────────────────────────
                Group {
                    if isOverdue {
                        Text("Empezando ahora")
                            .font(.system(size: 22, weight: .bold, design: .rounded))
                    } else {
                        Text(countdownText)
                            .font(.system(size: 48, weight: .heavy, design: .rounded))
                            .monospacedDigit()
                            .contentTransition(.numericText())
                    }
                }
                .foregroundStyle(Color.wkNavy)
                .padding(.top, 28)

                Spacer()

                // ── Join ─────────────────────────────────────────────────
                if let url = info.meetingURL {
                    Button {
                        UIApplication.shared.open(url)
                        coordinator.dismiss()
                    } label: {
                        Label("Join now", systemImage: "video.fill")
                            .font(.system(size: 19, weight: .bold))
                            .foregroundStyle(Color.wkYellow)
                            .frame(maxWidth: .infinity)
                            .frame(height: 60)
                            .background(Color.wkNavy)
                            .clipShape(RoundedRectangle(cornerRadius: 18))
                    }
                    .padding(.horizontal, 28)
                }

                // ── Snooze options ───────────────────────────────────────
                VStack(spacing: 10) {
                    Text("Snooze")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(Color.wkNavy.opacity(0.4))
                        .kerning(1.2)

                    HStack(spacing: 8) {
                        snoozeButton("1 min")  { coordinator.snooze() }
                        if canSnoozeToStart {
                            snoozeButton("Al inicio") { snoozeToStart() }
                        }
                        snoozeButton("…") { showCustomSnooze = true }
                    }
                }
                .padding(.horizontal, 28)
                .padding(.top, 20)

                // ── Swipe to dismiss ─────────────────────────────────────
                swipeToDismiss
                    .padding(.horizontal, 28)
                    .padding(.top, 24)
                    .padding(.bottom, 52)
            }
        }
        .onReceive(timer) { now = $0 }
        .sheet(isPresented: $showCustomSnooze) {
            CustomSnoozeSheet { minutes in
                snoozeMinutes(minutes)
            }
            .presentationDetents([.height(340)])
        }
    }

    // MARK: - Swipe to dismiss

    private var swipeToDismiss: some View {
        ZStack(alignment: .leading) {
            // Track
            RoundedRectangle(cornerRadius: 30)
                .fill(Color.wkNavy.opacity(0.12))
                .frame(height: 60)

            // Fill progress
            RoundedRectangle(cornerRadius: 30)
                .fill(Color.wkNavy.opacity(0.18))
                .frame(width: max(60, swipeOffset + 60), height: 60)
                .animation(.interactiveSpring(), value: swipeOffset)

            // Hint text
            Text("desliza para cerrar  ›")
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(Color.wkNavy.opacity(max(0, 0.5 - swipeOffset / 200)))
                .frame(maxWidth: .infinity)

            // Thumb
            ZStack {
                Circle()
                    .fill(Color.wkNavy)
                    .frame(width: 52, height: 52)
                Image(systemName: "chevron.right")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(Color.wkYellow)
            }
            .padding(.leading, 4)
            .offset(x: swipeOffset)
                .gesture(
                    DragGesture()
                        .onChanged { value in
                            swipeOffset = max(0, value.translation.width)
                        }
                        .onEnded { value in
                            let trackWidth = UIScreen.main.bounds.width - 56 - 60
                            if swipeOffset > trackWidth * 0.75 {
                                coordinator.dismiss()
                            } else {
                                withAnimation(.spring(response: 0.3)) { swipeOffset = 0 }
                            }
                        }
                )
        }
    }

    // MARK: - Helpers

    private func snoozeButton(_ label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(Color.wkNavy.opacity(0.7))
                .frame(maxWidth: .infinity)
                .frame(height: 44)
                .background(Color.wkNavy.opacity(0.1))
                .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    private func snoozeToStart() {
        let delay = max(10, secondsLeft)
        Task { @MainActor in
            await AlertScheduler.shared.snoozeSeconds(
                notificationId: info.notificationId,
                title: info.title,
                meetingURL: info.meetingURL,
                seconds: delay
            )
        }
        coordinator.dismiss()
    }

    private func snoozeMinutes(_ minutes: Int) {
        Task { @MainActor in
            await AlertScheduler.shared.snoozeSeconds(
                notificationId: info.notificationId,
                title: info.title,
                meetingURL: info.meetingURL,
                seconds: Double(minutes * 60)
            )
        }
        coordinator.dismiss()
    }

    private var countdownText: String {
        let s = max(0, Int(secondsLeft))
        return String(format: "%d:%02d", s / 60, s % 60)
    }

    private var formattedTime: String {
        let f = DateFormatter()
        f.dateFormat = "HH:mm"
        return f.string(from: info.startTime)
    }

    private func friendlyRoomName(_ url: URL) -> String {
        let host = url.host ?? ""
        switch true {
        case host.contains("teams.microsoft") || host.contains("teams.live"):
            return "Reunión de Teams"
        case host.contains("meet.google"):
            return "Google Meet"
        case host.contains("zoom.us"):
            return "Reunión de Zoom"
        case host.contains("webex"):
            return "Webex"
        case host.contains("whereby"):
            return "Whereby"
        case host.contains("discord"):
            return "Discord"
        case host.contains("slack"):
            return "Slack Huddle"
        case host.contains("around"):
            return "Around"
        case host.contains("gather"):
            return "Gather"
        default:
            return host.isEmpty ? url.absoluteString : host
        }
    }
}

// MARK: - Custom snooze sheet

struct CustomSnoozeSheet: View {

    let onSnooze: (Int) -> Void
    @Environment(\.dismiss) private var dismiss

    private let presets = [2, 5, 10, 15, 30]
    @State private var selected: Int? = 5
    @State private var customText = ""

    private var effectiveMinutes: Int {
        if let c = Int(customText), c > 0 { return c }
        return selected ?? 0
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.wkNavy.ignoresSafeArea()

                VStack(spacing: 20) {
                    // Chips
                    HStack(spacing: 8) {
                        ForEach(presets, id: \.self) { min in
                            Button {
                                selected = min
                                customText = ""
                            } label: {
                                Text("\(min)m")
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundStyle(selected == min && customText.isEmpty ? Color.wkNavy : .white)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 44)
                                    .background(selected == min && customText.isEmpty ? Color.wkYellow : Color.white.opacity(0.1))
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                        }
                    }
                    .padding(.horizontal, 20)

                    // Campo libre
                    HStack {
                        Image(systemName: "pencil")
                            .foregroundStyle(.white.opacity(0.4))
                        TextField("Minutos personalizados", text: $customText)
                            .keyboardType(.numberPad)
                            .foregroundStyle(.white)
                            .onChange(of: customText) { _ in selected = nil }
                    }
                    .padding(.horizontal, 16)
                    .frame(height: 48)
                    .background(Color.white.opacity(0.07))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .padding(.horizontal, 20)

                    // Confirm
                    Button {
                        let m = effectiveMinutes
                        if m > 0 {
                            onSnooze(m)
                            dismiss()
                        }
                    } label: {
                        Text(effectiveMinutes > 0 ? "Posponer \(effectiveMinutes) min" : "Posponer")
                            .font(.system(size: 17, weight: .bold))
                            .foregroundStyle(Color.wkNavy)
                            .frame(maxWidth: .infinity)
                            .frame(height: 54)
                            .background(effectiveMinutes > 0 ? Color.wkYellow : Color.wkYellow.opacity(0.4))
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .disabled(effectiveMinutes == 0)
                    .padding(.horizontal, 20)
                }
                .padding(.top, 24)
            }
            .navigationTitle("Posponer")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancelar") { dismiss() }
                        .foregroundStyle(.white.opacity(0.6))
                }
            }
        }
    }
}
