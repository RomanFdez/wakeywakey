import Foundation

/// Scheduler residente de macOS: cada 15 s comprueba las reuniones de hoy
/// (calendario + alertas ad-hoc) y dispara la alerta full-screen en el minuto
/// configurado antes del inicio. Respeta la pausa.
@MainActor
final class DesktopMacScheduler {

    static let shared = DesktopMacScheduler()

    private let checkInterval: TimeInterval = 15
    private let lateGrace: TimeInterval = 2 * 60   // no disparar reuniones empezadas hace >2 min

    private var timer: Timer?
    private var handledIds = Set<String>()

    private init() {}

    func start() {
        stop()
        let t = Timer(timeInterval: checkInterval, repeats: true) { [weak self] _ in
            Task { @MainActor in self?.tick() }
        }
        RunLoop.main.add(t, forMode: .common)
        timer = t
        tick()
    }

    func stop() { timer?.invalidate(); timer = nil }

    private func passesWorkingHours(_ m: AnyMeeting, _ s: SettingsStore) -> Bool {
        guard s.workingHoursOnly else { return true }
        let cal = Calendar.current
        let wd = cal.component(.weekday, from: m.startDate)   // 1=Dom … 7=Sáb
        guard s.workingDays.contains(wd) else { return false }
        let hour = cal.component(.hour, from: m.startDate)
        return hour >= s.workingHoursStart && hour < s.workingHoursEnd
    }

    private func tick() {
        PauseController.shared.refreshIfExpired()
        guard !PauseController.shared.isPaused else { return }
        guard MacEntitlementManager.shared.isPro else { return }   // trial/Pro (DEBUG siempre Pro)

        let settings = SettingsStore.shared
        let cal = CalendarService.shared
        if cal.isAuthorized {
            cal.loadTodayEvents(enabledIds: settings.enabledCalendarIds, settings: settings)
        }

        let events = cal.todayEvents
            + ManualEventsStore.shared.todayEvents.map { AnyMeeting(manual: $0) }

        let minutesBefore = settings.alertMinutesBefore
        let now = Date()

        for meeting in events {
            guard !handledIds.contains(meeting.id) else { continue }
            let triggerAt = meeting.startDate.addingTimeInterval(-Double(minutesBefore) * 60)
            guard triggerAt <= now else { continue }
            if now.timeIntervalSince(meeting.startDate) > lateGrace {
                handledIds.insert(meeting.id)
                continue
            }
            // Working hours: silenciar fuera del horario/días laborables.
            if !passesWorkingHours(meeting, settings) {
                handledIds.insert(meeting.id)
                continue
            }
            handledIds.insert(meeting.id)
            AlertWindowController.shared.show(
                title:      meeting.title,
                startDate:  meeting.startDate,
                location:   meeting.location,
                meetingURL: meeting.meetingURL,
                isPreview:  false
            )
        }
    }
}
