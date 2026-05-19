import Foundation
import UserNotifications
import EventKit

@MainActor
class AlertScheduler {

    static let shared = AlertScheduler()

    private let center = UNUserNotificationCenter.current()

    // Cancela todas las alarmas WakeyWakey pendientes y reprograma con los eventos de hoy.
    func rescheduleAll(events: [EKEvent], minutesBefore: Int) async {
        let pending = await center.pendingNotificationRequests()
        let stale   = pending.filter { $0.identifier.hasPrefix("ww_event_") }.map { $0.identifier }
        center.removePendingNotificationRequests(withIdentifiers: stale)

        let now = Date()
        for event in events {
            let triggerAt = event.startDate.addingTimeInterval(-Double(minutesBefore) * 60)
            guard triggerAt > now else { continue }
            await schedule(event: event, triggerAt: triggerAt, minutesBefore: minutesBefore)
        }
    }

    func cancel(eventIdentifier: String) {
        center.removePendingNotificationRequests(withIdentifiers: ["ww_event_\(eventIdentifier)"])
    }

    // Reagenda la misma alarma 60 segundos en el futuro (snooze).
    func snooze(notificationId: String, title: String, meetingURL: URL?) async {
        let content = baseContent(title: title, body: "Snoozed · empieza ahora", meetingURL: meetingURL)
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 60, repeats: false)
        let request = UNNotificationRequest(
            identifier: notificationId + "_snooze",
            content: content,
            trigger: trigger
        )
        try? await center.add(request)
    }

    // MARK: - Private

    private func schedule(event: EKEvent, triggerAt: Date, minutesBefore: Int) async {
        let url   = CalendarService.meetingLink(for: event)
        let body  = minutesBefore == 0
            ? "Empezando ahora"
            : "Empieza en \(minutesBefore) min\(url != nil ? " · Toca para unirte" : "")"

        let content = baseContent(title: event.title ?? "Reunión", body: body, meetingURL: url)
        content.userInfo = [
            "event_id"    : event.eventIdentifier ?? "",
            "meeting_url" : url?.absoluteString ?? "",
            "start_time"  : event.startDate.timeIntervalSince1970,
            "title"       : event.title ?? "Reunión",
        ]

        let comps   = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute, .second], from: triggerAt)
        let trigger = UNCalendarNotificationTrigger(dateMatching: comps, repeats: false)
        let request = UNNotificationRequest(
            identifier: "ww_event_\(event.eventIdentifier ?? UUID().uuidString)",
            content: content,
            trigger: trigger
        )
        try? await center.add(request)
    }

    private func baseContent(title: String, body: String, meetingURL: URL?) -> UNMutableNotificationContent {
        let c = UNMutableNotificationContent()
        c.title              = title
        c.body               = body
        c.sound              = .default
        c.categoryIdentifier = "MEETING_ALERT"
        return c
    }
}
