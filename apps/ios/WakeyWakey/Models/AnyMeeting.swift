import Foundation
import EventKit

struct AnyMeeting: Identifiable, Hashable {

    let id: String
    let title: String
    let startDate: Date
    let endDate: Date
    let meetingURL: URL?
    let calendarColor: CGColor? // excluded from Hashable via custom impl
    let calendarName: String?
    let location: String?
    let notes: String?
    let isManual: Bool
    let manualId: UUID?

    init(calEvent e: EKEvent) {
        id            = e.eventIdentifier ?? UUID().uuidString
        title         = e.title ?? "Reunión"
        startDate     = e.startDate
        endDate       = e.endDate
        meetingURL    = CalendarService.meetingLink(for: e)
        calendarColor = e.calendar?.cgColor
        calendarName  = e.calendar?.title
        location      = e.location.flatMap { $0.isEmpty ? nil : $0 }
        notes         = e.notes.flatMap { $0.isEmpty ? nil : $0 }
        isManual      = false
        manualId      = nil
    }

    func hash(into hasher: inout Hasher) { hasher.combine(id) }
    static func == (lhs: AnyMeeting, rhs: AnyMeeting) -> Bool { lhs.id == rhs.id }

    init(manual m: ManualEvent) {
        id            = m.id.uuidString
        title         = m.title
        startDate     = m.startDate
        endDate       = m.endDate
        meetingURL    = URL(string: m.meetingURL)
        calendarColor = nil
        calendarName  = "Manual"
        location      = nil
        notes         = nil
        isManual      = true
        manualId      = m.id
    }
}
