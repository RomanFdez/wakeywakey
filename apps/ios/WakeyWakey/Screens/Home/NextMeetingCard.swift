import SwiftUI

struct NextMeetingCard: View {

    let meeting: AnyMeeting

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            CardContent(meeting: meeting, now: context.date)
        }
    }
}

private struct CardContent: View {

    let meeting: AnyMeeting
    let now: Date

    private var secondsLeft: TimeInterval { meeting.startDate.timeIntervalSince(now) }
    private var isOngoing: Bool           { meeting.startDate <= now && meeting.endDate > now }
    private var isImminent: Bool          { secondsLeft < 5 * 60 && secondsLeft > 0 }
    private var isStarting: Bool          { secondsLeft <= 0 }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {

            // ── Header ──────────────────────────────────────────────────
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 6) {
                        Text(statusText)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(labelColor)
                        if isOngoing {
                            Text("En curso")
                                .font(.system(size: 10, weight: .bold))
                                .foregroundStyle(.black)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(Color.green)
                                .clipShape(Capsule())
                        }
                    }

                    Text(meeting.title)
                        .font(.system(size: 22, weight: .heavy, design: .rounded))
                        .foregroundStyle(isImminent ? Color.wkNavy : .white)
                        .lineLimit(2)
                }
                Spacer()

                // Reloj circular — hora fija de inicio
                ZStack {
                    Circle()
                        .stroke(isImminent ? Color.wkNavy.opacity(0.2) : Color.white.opacity(0.15), lineWidth: 3)
                    Text(Self.timeFmt.string(from: meeting.startDate))
                        .font(.system(size: 13, weight: .bold, design: .monospaced))
                        .foregroundStyle(isImminent ? Color.wkNavy : .white)
                }
                .frame(width: 58, height: 58)
            }

            // ── Ubicación ────────────────────────────────────────────────
            if let loc = meeting.location, meeting.meetingURL == nil {
                Label(loc, systemImage: "location.fill")
                    .font(.system(size: 13))
                    .foregroundStyle(isImminent ? Color.wkNavy.opacity(0.6) : .white.opacity(0.5))
                    .lineLimit(1)
            }

            // ── Join button ──────────────────────────────────────────────
            if let url = meeting.meetingURL {
                Button {
                    UIApplication.shared.open(url)
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "video.fill")
                        Text("Unirse ahora")
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
        .background(cardBackground)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .animation(.easeInOut(duration: 0.4), value: isOngoing)
        .animation(.easeInOut(duration: 0.4), value: isUrgent)
        .animation(.easeInOut(duration: 0.4), value: isImminent)
    }

    // MARK: - Helpers

    private var isUrgent: Bool { secondsLeft <= 2 * 60 && secondsLeft > 0 }

    private var cardBackground: Color {
        if isOngoing  { return Color.green.opacity(0.15) }
        if isUrgent   { return Color.wkCoral }
        if isImminent { return Color.wkYellow }
        return Color.white.opacity(0.08)
    }

    private var labelColor: Color {
        if isOngoing  { return Color.green }
        if isImminent { return Color.wkNavy.opacity(0.7) }
        return .white.opacity(0.6)
    }

    private var statusText: String {
        if isOngoing  { return "En curso" }
        if isStarting { return "Empezando ahora" }
        return countdownText
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

    private static let timeFmt: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "HH:mm"
        return f
    }()
}
