import SwiftUI

struct AlertView: View {

    let info: AlertCoordinator.AlertInfo

    @EnvironmentObject var coordinator: AlertCoordinator
    @State private var now = Date()
    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    private var secondsLeft: TimeInterval { info.startTime.timeIntervalSince(now) }
    private var isOverdue: Bool           { secondsLeft < 0 }

    var body: some View {
        ZStack {
            Color.wkYellow.ignoresSafeArea()

            VStack(spacing: 0) {
                Spacer()

                // ── Countdown ────────────────────────────────────────────
                if isOverdue {
                    Text("Empezando ahora")
                        .font(.system(size: 34, weight: .heavy, design: .rounded))
                        .foregroundStyle(Color.wkNavy)
                } else {
                    Text(countdownText)
                        .font(.system(size: 80, weight: .heavy, design: .rounded))
                        .foregroundStyle(Color.wkNavy)
                        .monospacedDigit()
                        .contentTransition(.numericText())

                    Text("para tu reunión")
                        .font(.system(size: 18))
                        .foregroundStyle(Color.wkNavy.opacity(0.55))
                        .padding(.top, 4)
                }

                // ── Título ───────────────────────────────────────────────
                Text(info.title)
                    .font(.system(size: 26, weight: .bold, design: .rounded))
                    .foregroundStyle(Color.wkNavy)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 36)
                    .padding(.top, 28)

                // ── Hora ─────────────────────────────────────────────────
                Text("Empieza a las \(formattedTime)")
                    .font(.system(size: 15))
                    .foregroundStyle(Color.wkNavy.opacity(0.55))
                    .padding(.top, 8)

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

                // ── Snooze + Cerrar ──────────────────────────────────────
                HStack(spacing: 12) {
                    Button("Snooze 1 min") { coordinator.snooze() }
                        .frame(maxWidth: .infinity)
                    Button("Cerrar")        { coordinator.dismiss() }
                        .frame(maxWidth: .infinity)
                }
                .font(.system(size: 16, weight: .medium))
                .foregroundStyle(Color.wkNavy.opacity(0.65))
                .frame(height: 52)
                .padding(.horizontal, 28)
                .padding(.top, 12)
                .padding(.bottom, 52)
            }
        }
        .onReceive(timer) { now = $0 }
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
}
