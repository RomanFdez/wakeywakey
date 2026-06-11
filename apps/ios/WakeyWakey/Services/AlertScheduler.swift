import Foundation
import UserNotifications
import EventKit
import AVFoundation

@MainActor
class AlertScheduler {

    static let shared = AlertScheduler()

    private let center = UNUserNotificationCenter.current()

    func rescheduleAll(events: [AnyMeeting], minutesBefore: Int, soundName: String = "clock-alarm") async {
        guard EntitlementManager.shared.isPro else {
            let pending = await center.pendingNotificationRequests()
            let stale   = pending.filter { $0.identifier.hasPrefix("ww_event_") }.map { $0.identifier }
            center.removePendingNotificationRequests(withIdentifiers: stale)
            return
        }

        let pending = await center.pendingNotificationRequests()
        let stale   = pending.filter { $0.identifier.hasPrefix("ww_event_") }.map { $0.identifier }
        center.removePendingNotificationRequests(withIdentifiers: stale)

        let now = Date()
        for meeting in events {
            let triggerAt = meeting.startDate.addingTimeInterval(-Double(minutesBefore) * 60)
            guard triggerAt > now else { continue }
            await schedule(meeting: meeting, triggerAt: triggerAt, minutesBefore: minutesBefore, soundName: soundName)
        }
    }

    func cancel(eventIdentifier: String) {
        center.removePendingNotificationRequests(withIdentifiers: ["ww_event_\(eventIdentifier)"])
    }

    func snooze(notificationId: String, title: String, meetingURL: URL?) async {
        await snoozeSeconds(notificationId: notificationId, title: title, meetingURL: meetingURL, seconds: 60)
    }

    func snoozeSeconds(notificationId: String, title: String, meetingURL: URL?, seconds: Double) async {
        let content = baseContent(title: title, body: "Pospuesto · empieza pronto", meetingURL: meetingURL)
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: max(5, seconds), repeats: false)
        let request = UNNotificationRequest(
            identifier: notificationId + "_snooze_\(Int(seconds))",
            content: content,
            trigger: trigger
        )
        try? await center.add(request)
    }

    // MARK: - Private

    private func schedule(meeting: AnyMeeting, triggerAt: Date, minutesBefore: Int, soundName: String = "clock-alarm") async {
        let url  = meeting.meetingURL
        let body: String
        if minutesBefore <= 1 {
            body = url != nil ? "Empieza ahora · Toca para unirte" : "Empieza ahora"
        } else {
            body = "Empieza en \(minutesBefore) min\(url != nil ? " · Toca para unirte" : "")"
        }

        let content = baseContent(title: meeting.title, body: body, meetingURL: url, soundName: soundName)
        content.userInfo = [
            "event_id"    : meeting.id,
            "meeting_url" : url?.absoluteString ?? "",
            "start_time"  : meeting.startDate.timeIntervalSince1970,
            "title"       : meeting.title,
        ]

        let comps   = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute, .second], from: triggerAt)
        let trigger = UNCalendarNotificationTrigger(dateMatching: comps, repeats: false)
        let request = UNNotificationRequest(
            identifier: "ww_event_\(meeting.id)",
            content: content,
            trigger: trigger
        )
        try? await center.add(request)
    }

    private func baseContent(title: String, body: String, meetingURL: URL?, soundName: String = "clock-alarm") -> UNMutableNotificationContent {
        let c = UNMutableNotificationContent()
        c.title              = title
        c.body               = body
        c.categoryIdentifier = "MEETING_ALERT"
        c.interruptionLevel  = .timeSensitive
        if soundName == "default" {
            c.sound = .default
        } else {
            // .caf is the only format iOS guarantees for UNNotificationSound;
            // MP3 is not officially supported and plays silently on many devices.
            c.sound = UNNotificationSound(named: UNNotificationSoundName(rawValue: "\(soundName).caf"))
        }
        return c
    }
}
