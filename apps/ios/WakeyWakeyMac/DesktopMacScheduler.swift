import Foundation

/// Scheduler residente de macOS: cada 15 s comprueba las reuniones de hoy
/// (calendario + alertas ad-hoc) y dispara la alerta full-screen en el minuto
/// configurado antes del inicio. Respeta la pausa.
@MainActor
final class DesktopMacScheduler {

    static let shared = DesktopMacScheduler()

    // Se comprueba cada segundo para que la alerta salte a la hora exacta: con un
    // intervalo mayor, saltaba en el siguiente tick (hasta 15 s tarde). El fetch de
    // EventKit es lo caro, así que se recarga solo cada `reloadInterval`.
    private let checkInterval: TimeInterval = 1
    private let reloadInterval: TimeInterval = 30
    private let lateGrace: TimeInterval = 2 * 60   // no disparar reuniones empezadas hace >2 min

    private var timer: Timer?
    private var handledIds = Set<String>()
    private var lastLoad = Date.distantPast
    /// Evita que App Nap ralentice el timer: sin esto macOS agrupa y retrasa los
    /// disparos de una app de fondo (que es lo que siempre es una app de barra de
    /// menús) y la alerta llega tarde. Permite dormir el equipo: con el Mac dormido
    /// no hay alerta que mostrar y bloquear el sueño gastaría batería sin motivo.
    private var activityToken: NSObjectProtocol?

    private init() {}

    func start() {
        stop()
        if activityToken == nil {
            activityToken = ProcessInfo.processInfo.beginActivity(
                options: .userInitiatedAllowingIdleSystemSleep,
                reason: "Avisar de las reuniones a su hora")
        }
        let t = Timer(timeInterval: checkInterval, repeats: true) { [weak self] _ in
            Task { @MainActor in self?.tick() }
        }
        t.tolerance = 0            // sin margen: el sistema no puede agrupar el disparo
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
        let now = Date()
        if cal.isAuthorized, now.timeIntervalSince(lastLoad) >= reloadInterval {
            lastLoad = now
            cal.loadTodayEvents(enabledIds: settings.enabledCalendarIds, settings: settings)
        }

        let events = cal.todayEvents
            + ManualEventsStore.shared.todayEvents.map { AnyMeeting(manual: $0) }

        let minutesBefore = settings.alertMinutesBefore

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
