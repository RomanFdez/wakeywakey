import SwiftUI
import EventKit

struct NextMeetingCard: View {

    let event: EKEvent
    let now: Date

    private var secondsLeft: TimeInterval { event.startDate.timeIntervalSince(now) }
    private var isImminent: Bool          { secondsLeft < 5 * 60 && secondsLeft > -60 }
    private var isStarting: Bool          { secondsLeft <= 0 }
    private var meetingURL: URL?          { CalendarService.meetingLink(for: event) }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {

            // ── Header ──────────────────────────────────────────────────
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(isStarting ? "Empezando ahora" : countdownText)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(isImminent ? Color.wkNavy.opacity(0.7) : .white.opacity(0.6))

                    Text(event.title ?? "Reunión")
                        .font(.system(size: 22, weight: .heavy, design: .rounded))
                        .foregroundStyle(isImminent ? Color.wkNavy : .white)
                        .lineLimit(2)
                }
                Spacer()

                // Reloj circular
                ZStack {
                    Circle()
                        .stroke(isImminent ? Color.wkNavy.opacity(0.2) : Color.white.opacity(0.15), lineWidth: 3)
                    Text(timeString)
                        .font(.system(size: 13, weight: .bold, design: .monospaced))
                        .foregroundStyle(isImminent ? Color.wkNavy : .white)
                }
                .frame(width: 58, height: 58)
            }

            // ── Location/link ────────────────────────────────────────────
            if let loc = event.location, !loc.isEmpty, meetingURL == nil {
                Label(loc, systemImage: "location.fill")
                    .font(.system(size: 13))
                    .foregroundStyle(isImminent ? Color.wkNavy.opacity(0.6) : .white.opacity(0.5))
                    .lineLimit(1)
            }

            // ── Join button ──────────────────────────────────────────────
            if let url = meetingURL {
                Button {
                    UIApplication.shared.open(url)
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "video.fill")
                        Text("Join now")
                            .fontWeight(.bold)
                    }
                    .font(.system(size: 16))
                    .foregroundStyle(isImminent ? Color.wkYellow : Color.wkNavy)
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                    .background(isImminent ? Color.wkNavy : Color.wkYellow)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
        }
        .padding(20)
        .background(isImminent ? Color.wkYellow : Color.white.opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .animation(.easeInOut(duration: 0.4), value: isImminent)
    }

    // MARK: - Formatting

    private var timeString: String {
        let fmt = DateFormatter()
        fmt.dateFormat = "HH:mm"
        return fmt.string(from: event.startDate)
    }

    private var countdownText: String {
        let s = max(0, Int(secondsLeft))
        let h = s / 3600
        let m = (s % 3600) / 60
        let sec = s % 60

        if h > 0  { return "en \(h)h \(m)m" }
        if m >= 5 { return "en \(m) min" }
        if m > 0  { return "en \(m)m \(sec)s" }
        return "en \(sec)s"
    }
}
