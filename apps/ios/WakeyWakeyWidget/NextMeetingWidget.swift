import WidgetKit
import SwiftUI
import ActivityKit

// MARK: - Colors (duplicated — widget extension cannot import main app)

private extension Color {
    static let wkYellow = Color(red: 1.0, green: 0.878, blue: 0.227)
    static let wkNavy   = Color(red: 0.102, green: 0.102, blue: 0.180)
}

// MARK: - Shared logo view

private struct AppLogoView: View {
    var size: CGFloat = 28
    var body: some View {
        Image("AppLogo")
            .resizable()
            .scaledToFill()
            .frame(width: size, height: size)
            .clipShape(RoundedRectangle(cornerRadius: size * 0.22, style: .continuous))
    }
}

// MARK: - Timeline entry

struct MeetingEntry: TimelineEntry {
    let date: Date
    let data: MeetingWidgetData
}

// MARK: - Helpers on MeetingWidgetData (widget-side)

private extension MeetingWidgetData {

    /// Meetings that are in progress at the given time.
    func ongoing(at t: Date) -> [WidgetMeeting] {
        meetings.filter { $0.startDate <= t && $0.endDate > t }
    }

    /// Meetings that haven't started yet at the given time, sorted by startDate.
    func upcoming(at t: Date) -> [WidgetMeeting] {
        meetings.filter { $0.startDate > t }.sorted { $0.startDate < $1.startDate }
    }

    /// Returns the group of meetings that start at the same time as the first upcoming one.
    func nextGroup(at t: Date) -> [WidgetMeeting] {
        let up = upcoming(at: t)
        guard let first = up.first else { return [] }
        // "Same time" = within 1 minute
        return up.filter { abs($0.startDate.timeIntervalSince(first.startDate)) < 60 }
    }
}

// MARK: - Timeline provider

struct NextMeetingProvider: TimelineProvider {

    func placeholder(in context: Context) -> MeetingEntry {
        var data = MeetingWidgetData()
        data.meetings = [
            WidgetMeeting(title: "Reunión de equipo",
                          startDate: Date().addingTimeInterval(600),
                          endDate:   Date().addingTimeInterval(3600)),
            WidgetMeeting(title: "1:1 con Marketing",
                          startDate: Date().addingTimeInterval(4200),
                          endDate:   Date().addingTimeInterval(7200)),
        ]
        return MeetingEntry(date: .now, data: data)
    }

    func getSnapshot(in context: Context, completion: @escaping (MeetingEntry) -> Void) {
        completion(MeetingEntry(date: .now, data: MeetingWidgetData.load()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<MeetingEntry>) -> Void) {
        let data = MeetingWidgetData.load()
        let now  = Date()

        // Collect all times where widget state changes: every meeting start + end
        var changeTimes: [Date] = [now]
        for m in data.meetings {
            if m.startDate >= now { changeTimes.append(m.startDate) }
            if m.endDate   >= now { changeTimes.append(m.endDate)   }
        }
        // Also refresh at midnight to catch tomorrow's data
        if let midnight = Calendar.current.date(
            byAdding: .day, value: 1,
            to: Calendar.current.startOfDay(for: now)
        ) { changeTimes.append(midnight) }

        let sortedTimes = changeTimes.sorted().uniqued()
        let entries = sortedTimes.prefix(20).map { MeetingEntry(date: $0, data: data) }

        // Si los datos son de un día anterior, recargar agresivamente cada 10 min
        // hasta que la app o el background task escriba datos frescos.
        let isStale = !Calendar.current.isDateInToday(data.lastUpdated)
        let reloadInterval: TimeInterval = isStale ? 10 * 60 : 15 * 60

        let reloadAfter = sortedTimes.last.map {
            max($0, now.addingTimeInterval(reloadInterval))
        } ?? now.addingTimeInterval(reloadInterval)

        completion(Timeline(entries: Array(entries), policy: .after(reloadAfter)))
    }
}

private extension Array where Element: Hashable {
    func uniqued() -> [Element] {
        var seen = Set<Element>()
        return filter { seen.insert($0).inserted }
    }
}

// MARK: - Widget declaration

struct NextMeetingWidget: Widget {
    let kind = "NextMeetingWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: NextMeetingProvider()) { entry in
            NextMeetingWidgetView(entry: entry)
        }
        .configurationDisplayName("Próxima reunión")
        .description("Muestra tu próxima reunión y las siguientes del día.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}

// MARK: - containerBackground helper

private extension View {
    @ViewBuilder
    func widgetBackground() -> some View {
        if #available(iOSApplicationExtension 17.0, *) {
            containerBackground(for: .widget) { Color.wkNavy }
        } else {
            background(Color.wkNavy)
        }
    }
}

// MARK: - Root view

struct NextMeetingWidgetView: View {
    let entry: MeetingEntry
    @Environment(\.widgetFamily) private var family

    private var ongoing: [WidgetMeeting]  { entry.data.ongoing(at: entry.date)  }
    private var upcoming: [WidgetMeeting] { entry.data.upcoming(at: entry.date) }
    private var nextGroup: [WidgetMeeting] { entry.data.nextGroup(at: entry.date) }

    var body: some View {
        ZStack {
            if #available(iOSApplicationExtension 17.0, *) { Color.clear } else { Color.wkNavy }

            Group {
                if ongoing.isEmpty && upcoming.isEmpty {
                    NoMeetingView()
                } else {
                    switch family {
                    case .systemSmall:  SmallView(ongoing: ongoing, nextGroup: nextGroup)
                    case .systemLarge:  LargeView(ongoing: ongoing, upcoming: upcoming)
                    default:            MediumView(ongoing: ongoing, nextGroup: nextGroup, upcoming: upcoming)
                    }
                }
            }
        }
        .widgetBackground()
    }
}

// MARK: - Small (2×2)

private struct SmallView: View {
    let ongoing: [WidgetMeeting]
    let nextGroup: [WidgetMeeting]

    private var isOngoing: Bool { !ongoing.isEmpty }
    private var current: WidgetMeeting? { ongoing.first }
    private var next: WidgetMeeting?    { nextGroup.first }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {

            // ── Brand ────────────────────────────────────────────
            AppLogoView(size: 26)

            Spacer()

            if isOngoing, let m = current {
                // EN CURSO
                Label("EN CURSO", systemImage: "circle.fill")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(Color.green)
                    .labelStyle(CompactLabelStyle())
                    .padding(.bottom, 6)

                Text(m.title)
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(.white)
                    .lineLimit(3)
                    .fixedSize(horizontal: false, vertical: true)

            } else if let m = next {
                // Próxima
                Text(m.startDate, style: .time)
                    .font(.system(size: 26, weight: .heavy, design: .rounded))
                    .foregroundStyle(Color.wkYellow)
                    .monospacedDigit()
                    .padding(.bottom, 2)

                Text(m.title)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(.white)
                    .lineLimit(3)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
    }
}

// MARK: - Medium (4×2)

private struct MediumView: View {
    let ongoing: [WidgetMeeting]
    let nextGroup: [WidgetMeeting]
    let upcoming: [WidgetMeeting]

    private var isOngoing: Bool { !ongoing.isEmpty }
    private var current: WidgetMeeting? { ongoing.first }
    private var afterCurrent: WidgetMeeting? { upcoming.first }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {

            // ── Brand ────────────────────────────────────────────
            AppLogoView(size: 26)
                .padding(.bottom, 10)

            if isOngoing, let m = current {
                // ── EN CURSO ─────────────────────────────────────
                HStack(spacing: 6) {
                    Circle()
                        .fill(Color.green)
                        .frame(width: 7, height: 7)
                    Text("EN CURSO")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(Color.green)
                }
                .padding(.bottom, 4)

                Text(m.title)
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(.white)
                    .lineLimit(1)

                if let next = afterCurrent {
                    Spacer(minLength: 10)
                    // Siguiente
                    HStack(spacing: 6) {
                        Text(next.startDate, style: .time)
                            .font(.system(size: 12, weight: .semibold, design: .monospaced))
                            .foregroundStyle(Color.wkYellow)
                        Text(next.title)
                            .font(.system(size: 12))
                            .foregroundStyle(.white.opacity(0.55))
                            .lineLimit(1)
                    }
                }

            } else {
                // ── Próxima(s) ────────────────────────────────────
                let count = nextGroup.count
                if count > 1, let first = nextGroup.first {
                    // Simultáneas
                    Text(first.startDate, style: .time)
                        .font(.system(size: 26, weight: .heavy, design: .rounded))
                        .foregroundStyle(Color.wkYellow)
                        .monospacedDigit()
                        .padding(.bottom, 2)
                    Text("\(count) reuniones")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(.white)
                } else if let m = nextGroup.first {
                    // Una sola reunión
                    Text(m.startDate, style: .time)
                        .font(.system(size: 26, weight: .heavy, design: .rounded))
                        .foregroundStyle(Color.wkYellow)
                        .monospacedDigit()
                        .padding(.bottom, 2)
                    Text(m.title)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(.white)
                        .lineLimit(2)

                    // Segunda reunión si existe y es diferente hora
                    if let second = upcoming.dropFirst().first,
                       abs(second.startDate.timeIntervalSince(m.startDate)) >= 60 {
                        Spacer(minLength: 8)
                        HStack(spacing: 6) {
                            Text(second.startDate, style: .time)
                                .font(.system(size: 12, weight: .semibold, design: .monospaced))
                                .foregroundStyle(Color.wkYellow.opacity(0.6))
                            Text(second.title)
                                .font(.system(size: 12))
                                .foregroundStyle(.white.opacity(0.4))
                                .lineLimit(1)
                        }
                    }
                }
            }

            Spacer(minLength: 0)
        }
        .padding(16)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
    }
}

// MARK: - Large (4×4)

private enum LargeListItem: Identifiable {
    case dayHeader(String)
    case meeting(WidgetMeeting)

    var id: String {
        switch self {
        case .dayHeader(let s):  return "h_\(s)"
        case .meeting(let m):    return "m_\(m.startDate.timeIntervalSince1970)_\(m.title)"
        }
    }
}

private struct LargeView: View {
    let ongoing: [WidgetMeeting]
    let upcoming: [WidgetMeeting]

    private var isOngoing: Bool { !ongoing.isEmpty }
    private var current: WidgetMeeting? { ongoing.first }

    private var listItems: [LargeListItem] {
        var result: [LargeListItem] = []
        var lastDay: Date? = nil
        var count = 0
        let max = isOngoing ? 3 : 4
        for m in upcoming {
            guard count < max else { break }
            let day = Calendar.current.startOfDay(for: m.startDate)
            if let prev = lastDay, day != prev {
                result.append(.dayHeader(widgetDayLabel(for: m.startDate)))
            }
            lastDay = day
            result.append(.meeting(m))
            count += 1
        }
        return result
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {

            // ── Brand ────────────────────────────────────────────
            AppLogoView(size: 26)
                .padding(.bottom, 14)

            // ── En curso ─────────────────────────────────────────
            if isOngoing, let m = current {
                HStack(spacing: 8) {
                    Circle()
                        .fill(Color.green)
                        .frame(width: 8, height: 8)
                    Text("EN CURSO")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(Color.green)
                }
                .padding(.bottom, 4)

                Text(m.title)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(.white)
                    .lineLimit(2)
                    .padding(.bottom, 16)

                if !listItems.isEmpty {
                    Text("SIGUIENTES")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(.white.opacity(0.3))
                        .kerning(1.0)
                        .padding(.bottom, 8)
                }
            } else if !listItems.isEmpty {
                Text("PRÓXIMAS REUNIONES")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(.white.opacity(0.3))
                    .kerning(1.0)
                    .padding(.bottom, 8)
            }

            // ── Lista con separadores de día ──────────────────────
            VStack(alignment: .leading, spacing: 12) {
                ForEach(listItems) { item in
                    switch item {
                    case .dayHeader(let label):
                        HStack(spacing: 6) {
                            Rectangle()
                                .fill(Color.white.opacity(0.12))
                                .frame(height: 0.5)
                            Text(label)
                                .font(.system(size: 9, weight: .semibold))
                                .foregroundStyle(.white.opacity(0.35))
                                .kerning(0.8)
                                .fixedSize()
                            Rectangle()
                                .fill(Color.white.opacity(0.12))
                                .frame(height: 0.5)
                        }
                        .padding(.vertical, 2)
                    case .meeting(let m):
                        HStack(spacing: 12) {
                            Text(m.startDate, style: .time)
                                .font(.system(size: 13, weight: .semibold, design: .monospaced))
                                .foregroundStyle(Color.wkYellow)
                                .frame(width: 48, alignment: .leading)
                                .monospacedDigit()
                            Text(m.title)
                                .font(.system(size: 14))
                                .foregroundStyle(.white.opacity(0.85))
                                .lineLimit(1)
                        }
                    }
                }
            }

            Spacer(minLength: 0)
        }
        .padding(16)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
    }
}

private func widgetDayLabel(for date: Date) -> String {
    let cal = Calendar.current
    if cal.isDateInToday(date)    { return "HOY" }
    if cal.isDateInTomorrow(date) { return "MAÑANA" }
    let fmt = DateFormatter()
    fmt.locale = Locale(identifier: "es")
    fmt.dateFormat = "EEE d MMM"
    return fmt.string(from: date).uppercased()
}

// MARK: - No meeting

private struct NoMeetingView: View {
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: "calendar.badge.checkmark")
                .font(.system(size: 28))
                .foregroundStyle(.white.opacity(0.25))
            Text("Sin reuniones")
                .font(.system(size: 13))
                .foregroundStyle(.white.opacity(0.35))
        }
    }
}

// MARK: - Compact label style (for "EN CURSO" dot + text)

private struct CompactLabelStyle: LabelStyle {
    func makeBody(configuration: Configuration) -> some View {
        HStack(spacing: 4) {
            configuration.icon
                .font(.system(size: 6))
            configuration.title
        }
    }
}

// MARK: - Live Activity (sin cambios)

@available(iOSApplicationExtension 16.2, *)
struct MeetingLiveActivity: Widget {
    let kind = "MeetingLiveActivity"

    var body: some WidgetConfiguration {
        ActivityConfiguration(for: MeetingActivityAttributes.self) { context in
            LockScreenLiveActivityView(state: context.state)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Image(systemName: "alarm.fill")
                        .foregroundStyle(Color.wkYellow)
                }
                DynamicIslandExpandedRegion(.center) {
                    VStack(spacing: 2) {
                        Text(context.state.title)
                            .font(.system(size: 15, weight: .bold))
                            .foregroundStyle(.white)
                            .lineLimit(1)
                        if context.state.isOngoing {
                            (Text("+") + Text(context.state.startTime, style: .timer))
                                .font(.system(size: 12, design: .monospaced))
                                .foregroundStyle(Color.green)
                        } else {
                            Text(context.state.startTime, style: .relative)
                                .font(.system(size: 12))
                                .foregroundStyle(.white.opacity(0.6))
                        }
                    }
                }
                DynamicIslandExpandedRegion(.trailing) {
                    if let urlStr = context.state.meetingURL, let url = URL(string: urlStr) {
                        Link(destination: url) {
                            Image(systemName: "video.fill")
                                .foregroundStyle(Color.wkYellow)
                        }
                    }
                }
            } compactLeading: {
                AppLogoView(size: 16)
            } compactTrailing: {
                if context.state.isOngoing {
                    (Text("+") + Text(context.state.startTime, style: .timer))
                        .font(.system(size: 10, design: .monospaced))
                        .foregroundStyle(Color.green)
                        .monospacedDigit()
                        .frame(maxWidth: 48)
                } else {
                    Text(context.state.startTime, style: .relative)
                        .font(.system(size: 10, design: .monospaced))
                        .foregroundStyle(.white.opacity(0.85))
                        .monospacedDigit()
                        .frame(maxWidth: 44)
                }
            } minimal: {
                AppLogoView(size: 12)
            }
        }
    }
}

@available(iOSApplicationExtension 16.2, *)
private struct LockScreenLiveActivityView: View {
    let state: MeetingActivityAttributes.ContentState

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: "alarm.fill")
                .font(.system(size: 24))
                .foregroundStyle(state.isOngoing ? Color.green : Color.wkYellow)

            VStack(alignment: .leading, spacing: 2) {
                Text(state.title)
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(.white)
                    .lineLimit(1)
                if state.isOngoing {
                    (Text("En curso  +") + Text(state.startTime, style: .timer))
                        .font(.system(size: 12, design: .monospaced))
                        .foregroundStyle(Color.green)
                        .monospacedDigit()
                } else {
                    (Text("Empieza en ") + Text(state.startTime, style: .relative))
                        .font(.system(size: 12))
                        .foregroundStyle(.white.opacity(0.6))
                }
            }

            Spacer()

            if let urlStr = state.meetingURL, let url = URL(string: urlStr) {
                Link(destination: url) {
                    Text("Unirse")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(Color.wkNavy)
                        .padding(.horizontal, 12).padding(.vertical, 6)
                        .background(Color.wkYellow)
                        .clipShape(Capsule())
                }
            }
        }
        .padding(16)
        .background(Color.wkNavy)
    }
}
