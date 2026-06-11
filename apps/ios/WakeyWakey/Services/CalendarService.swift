import EventKit
import Foundation

@MainActor
class CalendarService: ObservableObject {

    static let shared = CalendarService()

    private let store = EKEventStore()

    @Published var calendarAuthStatus: EKAuthorizationStatus = EKEventStore.authorizationStatus(for: .event)
    @Published var availableCalendars: [EKCalendar] = []
    // AnyMeeting (value type) en lugar de EKEvent para que el EKEventStore
    // pueda liberar su caché interna tras cada fetch.
    @Published var todayEvents: [AnyMeeting] = []
    @Published var weekEvents:  [AnyMeeting] = []

    var isAuthorized: Bool {
        if #available(iOS 17, *) {
            return calendarAuthStatus == .fullAccess
        } else {
            return calendarAuthStatus == .authorized
        }
    }

    // MARK: - Permissions

    func requestCalendarAccess() async -> Bool {
        do {
            let granted: Bool
            if #available(iOS 17, *) {
                granted = try await store.requestFullAccessToEvents()
            } else {
                granted = try await store.requestAccess(to: .event)
            }
            calendarAuthStatus = EKEventStore.authorizationStatus(for: .event)
            if granted { loadCalendars() }
            return granted
        } catch {
            return false
        }
    }

    func loadCalendars() {
        availableCalendars = store.calendars(for: .event)
            .sorted { $0.title < $1.title }
    }

    // MARK: - Events loading

    func loadTodayEvents(enabledIds: Set<String>, settings: SettingsStore? = nil) {
        guard isAuthorized else { return }
        let cal   = Calendar.current
        let start = cal.startOfDay(for: Date())
        let end   = cal.date(byAdding: .day, value: 1, to: start)!
        todayEvents = fetchAnyMeetings(from: start, to: end, enabledIds: enabledIds, settings: settings)
    }

    func loadWeekEvents(enabledIds: Set<String>, settings: SettingsStore? = nil) {
        guard isAuthorized else { return }
        let cal   = Calendar.current
        let start = cal.startOfDay(for: Date())
        let end   = cal.date(byAdding: .day, value: 7, to: start)!
        weekEvents = fetchAnyMeetings(from: start, to: end, enabledIds: enabledIds, settings: settings)
    }

    // MARK: - Private fetch

    private func fetchAnyMeetings(from start: Date, to end: Date,
                                   enabledIds: Set<String>, settings: SettingsStore?) -> [AnyMeeting] {
        let cal = Calendar.current

        let calendars: [EKCalendar]? = enabledIds.isEmpty
            ? nil
            : store.calendars(for: .event).filter { enabledIds.contains($0.calendarIdentifier) }

        let predicate = store.predicateForEvents(withStart: start, end: end, calendars: calendars)
        var events = store.events(matching: predicate)

        if let s = settings {
            if !s.showAllDayEvents   { events = events.filter { !$0.isAllDay } }
            if s.videoConferenceOnly { events = events.filter { CalendarService.meetingLink(for: $0) != nil } }
            if s.acceptedEventsOnly {
                events = events.filter { event in
                    guard let attendees = event.attendees,
                          let me = attendees.first(where: { $0.isCurrentUser })
                    else { return true }
                    return me.participantStatus != .declined
                }
            }
            if s.workingHoursOnly {
                events = events.filter { event in
                    let hour = cal.component(.hour, from: event.startDate)
                    return hour >= s.workingHoursStart && hour < s.workingHoursEnd
                }
            }
        } else {
            events = events.filter { !$0.isAllDay }
        }

        // Convertir a AnyMeeting antes de salir del scope — los EKEvent quedan
        // sin referencias externas y el EKEventStore puede liberar su caché.
        return events
            .sorted { $0.startDate < $1.startDate }
            .map { AnyMeeting(calEvent: $0) }
    }

    // MARK: - Meeting link detection

    nonisolated static func meetingLink(for event: EKEvent) -> URL? {
        if let url = event.url, isMeetingURL(url) { return url }
        let text = [event.notes, event.location].compactMap { $0 }.joined(separator: " ")
        return extractURL(from: text)
    }

    private nonisolated static func isMeetingURL(_ url: URL) -> Bool {
        let host = url.host ?? ""
        return calendarMeetingHosts.contains { host.contains($0) }
    }

    private nonisolated static func extractURL(from text: String) -> URL? {
        guard let detector = calendarURLDetector else { return nil }
        let matches = detector.matches(in: text, range: NSRange(text.startIndex..., in: text))
        for match in matches {
            if let url = match.url, isMeetingURL(url) { return url }
        }
        return nil
    }
}

private let calendarURLDetector: NSDataDetector? =
    try? NSDataDetector(types: NSTextCheckingResult.CheckingType.link.rawValue)

private let calendarMeetingHosts = [
    "meet.google.com", "zoom.us", "teams.microsoft.com", "teams.live.com",
    "webex.com", "whereby.com", "jitsi", "gotomeeting.com", "bluejeans.com",
    "chime.aws", "around.co", "gather.town", "discord.com", "slack.com/huddle",
]
