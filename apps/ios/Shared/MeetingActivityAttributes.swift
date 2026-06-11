import ActivityKit
import Foundation

@available(iOS 16.2, *)
struct MeetingActivityAttributes: ActivityAttributes {

    struct ContentState: Codable, Hashable {
        var title: String
        var startTime: Date
        var meetingURL: String?
        var isOngoing: Bool
    }

    var calendarName: String?
}
