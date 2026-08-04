import Foundation
// Live Activities (ActivityKit) no existen en Mac Catalyst.
#if canImport(ActivityKit) && !targetEnvironment(macCatalyst)
import ActivityKit

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
#endif
