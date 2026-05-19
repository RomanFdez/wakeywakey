import SwiftUI
import EventKit

struct MeetingRow: View {

    let meeting: AnyMeeting
    let now: Date

    private var isPast: Bool { meeting.endDate < now }
    private var isNow: Bool  { meeting.startDate <= now && meeting.endDate > now }

    var body: some View {
        HStack(spacing: 14) {

            // ── Time column ──────────────────────────────────────────────
            VStack(alignment: .trailing, spacing: 2) {
                Text(timeString(meeting.startDate))
                    .font(.system(size: 13, weight: .semibold, design: .monospaced))
                Text(timeString(meeting.endDate))
                    .font(.system(size: 11, design: .monospaced))
                    .foregroundStyle(.white.opacity(0.3))
            }
            .foregroundStyle(isPast ? .white.opacity(0.3) : .white.opacity(0.8))
            .frame(width: 44, alignment: .trailing)

            // ── Status dot ───────────────────────────────────────────────
            Circle()
                .fill(dotColor)
                .frame(width: 8, height: 8)

            // ── Title ────────────────────────────────────────────────────
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Text(meeting.title)
                        .font(.system(size: 15, weight: isNow ? .semibold : .regular))
                        .foregroundStyle(isPast ? .white.opacity(0.3) : .white.opacity(isNow ? 1 : 0.85))
                        .lineLimit(1)

                    if isNow {
                        Text("Ongoing")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(.black)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.green)
                            .clipShape(Capsule())
                    }

                    if meeting.isManual {
                        Image(systemName: "pencil")
                            .font(.system(size: 9))
                            .foregroundStyle(.white.opacity(0.35))
                    }
                }

                HStack(spacing: 6) {
                    if let name = meeting.calendarName { Text(name) }
                    Text("·")
                    Text(durationText)
                }
                .font(.system(size: 12))
                .foregroundStyle(.white.opacity(0.3))

                if let loc = meeting.location, meeting.meetingURL == nil {
                    Label(loc, systemImage: "location.fill")
                        .font(.system(size: 11))
                        .foregroundStyle(.white.opacity(0.25))
                        .lineLimit(1)
                }
            }

            Spacer()

            // ── Join icon ────────────────────────────────────────────────
            if let url = meeting.meetingURL, !isPast {
                Button {
                    UIApplication.shared.open(url)
                } label: {
                    Image(systemName: "video.fill")
                        .font(.system(size: 14))
                        .foregroundStyle(Color.wkYellow)
                        .padding(8)
                        .background(Color.wkYellow.opacity(0.15))
                        .clipShape(Circle())
                }
            }

            #if DEBUG
            Button {
                AlertCoordinator.shared.activeAlert = AlertCoordinator.AlertInfo(
                    notificationId: "debug_\(meeting.id)",
                    title: meeting.title,
                    startTime: meeting.startDate,
                    meetingURL: meeting.meetingURL
                )
            } label: {
                Image(systemName: "bell.fill")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.wkCoral)
                    .padding(8)
                    .background(Color.wkCoral.opacity(0.15))
                    .clipShape(Circle())
            }
            #endif
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(rowBackground)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private var dotColor: Color {
        if isNow  { return .green }
        if isPast { return .white.opacity(0.2) }
        return .white.opacity(0.5)
    }

    private var rowBackground: Color {
        if isNow  { return Color.green.opacity(0.10) }
        return Color.white.opacity(0.05)
    }

    private var durationText: String {
        let mins = Int(meeting.endDate.timeIntervalSince(meeting.startDate) / 60)
        if mins < 60 { return "\(mins)m" }
        let h = mins / 60; let m = mins % 60
        return m == 0 ? "\(h)h" : "\(h)h \(m)m"
    }

    private func timeString(_ date: Date) -> String {
        let fmt = DateFormatter()
        fmt.dateFormat = "HH:mm"
        return fmt.string(from: date)
    }
}
