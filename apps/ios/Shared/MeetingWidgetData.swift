import Foundation

// MARK: - WidgetMeeting

/// A single meeting entry shared between the main app and the widget extension.
struct WidgetMeeting: Codable {
    var title: String
    var startDate: Date
    var endDate: Date
    var calendarName: String?
    var meetingURL: String?
}

// MARK: - MeetingWidgetData

/// Shared DTO written by the main app and read by the widget extension via App Groups.
struct MeetingWidgetData: Codable {

    /// All of today's non-past meetings, sorted by startDate. The widget uses this
    /// to derive what's ongoing and what's next at any given entry.date.
    var meetings: [WidgetMeeting] = []

    /// Timestamp of when the data was last written. Used by the widget to detect stale data.
    var lastUpdated: Date = Date()

    // MARK: Legacy fields — kept so old widget snapshots decode without crashing
    var nextMeetingTitle: String?
    var nextMeetingStart: Date?
    var nextMeetingEnd:   Date?
    var nextMeetingURL:   String?
    var calendarName:     String?
    var isOngoing:        Bool = false

    // MARK: Persistence

    static let appGroupID      = "group.com.sierraespada.wakeywakey"
    static let userDefaultsKey = "meetingWidgetData"

    static func load() -> MeetingWidgetData {
        guard let defaults = UserDefaults(suiteName: appGroupID),
              let data    = defaults.data(forKey: userDefaultsKey),
              let decoded = try? JSONDecoder().decode(MeetingWidgetData.self, from: data)
        else {
            return MeetingWidgetData()
        }
        return decoded
    }

    func save() {
        guard let defaults = UserDefaults(suiteName: Self.appGroupID),
              let data     = try? JSONEncoder().encode(self)
        else { return }
        defaults.set(data, forKey: Self.userDefaultsKey)
    }
}
