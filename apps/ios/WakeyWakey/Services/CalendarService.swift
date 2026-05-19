import EventKit
import Foundation

@MainActor
class CalendarService: ObservableObject {

    static let shared = CalendarService()

    private let store = EKEventStore()

    @Published var calendarAuthStatus: EKAuthorizationStatus = EKEventStore.authorizationStatus(for: .event)
    @Published var availableCalendars: [EKCalendar] = []

    var isAuthorized: Bool {
        if #available(iOS 17, *) {
            return calendarAuthStatus == .fullAccess
        } else {
            return calendarAuthStatus == .authorized
        }
    }

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
}
