import WidgetKit
import Foundation

@MainActor
struct WidgetDataWriter {

    /// Writes the week's meetings to App Groups so the widget shows them.
    /// Only writes if the user has an active Pro subscription / trial.
    static func write(from meetings: [AnyMeeting]) {
        guard EntitlementManager.shared.isPro else {
            clear()
            return
        }

        let now = Date()
        let relevant = meetings
            .filter { $0.endDate > now }
            .prefix(8)
            .map { m in
                WidgetMeeting(
                    title:        m.title,
                    startDate:    m.startDate,
                    endDate:      m.endDate,
                    calendarName: m.calendarName,
                    meetingURL:   m.meetingURL?.absoluteString
                )
            }

        var data         = MeetingWidgetData()
        data.meetings    = Array(relevant)
        data.lastUpdated = now
        data.save()
        WidgetCenter.shared.reloadTimelines(ofKind: "NextMeetingWidget")
    }

    /// Limpia el widget (sin reuniones). Llamar al expirar el trial/suscripción.
    static func clear() {
        var data         = MeetingWidgetData()
        data.meetings    = []
        data.lastUpdated = Date()
        data.save()
        WidgetCenter.shared.reloadTimelines(ofKind: "NextMeetingWidget")
    }
}
