import SwiftUI
import EventKit

struct MeetingRow: View {

    let event: EKEvent
    let now: Date

    private var isPast: Bool       { event.endDate < now }
    private var isNow: Bool        { event.startDate <= now && event.endDate > now }
    private var hasLink: Bool      { CalendarService.meetingLink(for: event) != nil }

    var body: some View {
        HStack(spacing: 14) {

            // ── Time column ──────────────────────────────────────────────
            VStack(alignment: .trailing, spacing: 2) {
                Text(timeString(event.startDate))
                    .font(.system(size: 13, weight: .semibold, design: .monospaced))
                Text(timeString(event.endDate))
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
            VStack(alignment: .leading, spacing: 2) {
                Text(event.title ?? "Reunión")
                    .font(.system(size: 15, weight: isNow ? .semibold : .regular))
                    .foregroundStyle(isPast ? .white.opacity(0.3) : .white.opacity(isNow ? 1 : 0.85))
                    .lineLimit(1)

                if let cal = event.calendar {
                    Text(cal.title)
                        .font(.system(size: 12))
                        .foregroundStyle(.white.opacity(0.3))
                }
            }

            Spacer()

            // ── Join icon ────────────────────────────────────────────────
            if hasLink && !isPast {
                Button {
                    if let url = CalendarService.meetingLink(for: event) {
                        UIApplication.shared.open(url)
                    }
                } label: {
                    Image(systemName: "video.fill")
                        .font(.system(size: 14))
                        .foregroundStyle(Color.wkYellow)
                        .padding(8)
                        .background(Color.wkYellow.opacity(0.15))
                        .clipShape(Circle())
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(rowBackground)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private var dotColor: Color {
        if isNow  { return .wkYellow }
        if isPast { return .white.opacity(0.2) }
        return .white.opacity(0.5)
    }

    private var rowBackground: Color {
        if isNow  { return Color.wkYellow.opacity(0.08) }
        return Color.white.opacity(0.05)
    }

    private func timeString(_ date: Date) -> String {
        let fmt = DateFormatter()
        fmt.dateFormat = "HH:mm"
        return fmt.string(from: date)
    }
}
