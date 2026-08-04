#if targetEnvironment(macCatalyst)
import Foundation
import Combine

/// Scheduler residente para Mac Catalyst.
///
/// En iOS las alertas se disparan con notificaciones locales (`AlertScheduler`) porque
/// la app puede estar suspendida. En Mac la app está viva (residente), así que un timer
/// interno revisa `CalendarService.todayEvents` y dispara la alerta en el minuto exacto.
///
/// De momento `fire` muestra `AlertView` dentro de la ventana (vía `AlertCoordinator`).
/// El "takeover" a pantalla completa por encima de otras apps (nivel screen-saver +
/// activar la app) se añade en el incremento con ejecución en vivo, vía un bundle AppKit.
@MainActor
final class DesktopAlertScheduler: ObservableObject {

    static let shared = DesktopAlertScheduler()

    /// Cada cuánto revisa si toca disparar. 15 s da precisión de <¼ min sin gastar CPU.
    private let checkInterval: TimeInterval = 15

    /// Margen tras el inicio de la reunión durante el cual todavía tiene sentido
    /// disparar (evita alertas "zombis" de reuniones que empezaron hace rato).
    private let lateGrace: TimeInterval = 2 * 60

    private var timer: Timer?
    private var cancellables = Set<AnyCancellable>()
    /// Ids de reuniones ya disparadas (o descartadas por tardías) en esta sesión.
    private var handledEventIds = Set<String>()

    private init() {}

    func start() {
        stop()

        // Si la lista de hoy cambia (nuevo día, recarga), olvida los ids que ya no existen
        // para no bloquear futuras alertas con el mismo identificador reutilizado.
        CalendarService.shared.$todayEvents
            .sink { [weak self] _ in self?.pruneHandled() }
            .store(in: &cancellables)

        let t = Timer(timeInterval: checkInterval, repeats: true) { [weak self] _ in
            Task { @MainActor in self?.tick() }
        }
        RunLoop.main.add(t, forMode: .common)
        timer = t
        tick()
    }

    func stop() {
        timer?.invalidate()
        timer = nil
        cancellables.removeAll()
    }

    // MARK: - Núcleo

    private func tick() {
        guard EntitlementManager.shared.isPro else { return }

        let minutesBefore = SettingsStore.shared.alertMinutesBefore
        let now = Date()

        for meeting in CalendarService.shared.todayEvents {
            guard !handledEventIds.contains(meeting.id) else { continue }

            let triggerAt = meeting.startDate.addingTimeInterval(-Double(minutesBefore) * 60)
            guard triggerAt <= now else { continue }   // aún no toca

            // Demasiado tarde: márcala como manejada pero no la dispares.
            if now.timeIntervalSince(meeting.startDate) > lateGrace {
                handledEventIds.insert(meeting.id)
                continue
            }

            handledEventIds.insert(meeting.id)
            fire(for: meeting)
        }
    }

    private func fire(for meeting: AnyMeeting) {
        AlertCoordinator.shared.activeAlert = AlertCoordinator.AlertInfo(
            notificationId: "mac_\(meeting.id)",
            title:          meeting.title,
            startTime:      meeting.startDate,
            meetingURL:     meeting.meetingURL
        )
        // TODO(Fase 3 · run en vivo): traer la app al frente y elevar la ventana a nivel
        // screen-saver mediante un bundle AppKit para tapar todas las demás apps.
    }

    private func pruneHandled() {
        let current = Set(CalendarService.shared.todayEvents.map { $0.id })
        handledEventIds.formIntersection(current)
    }
}
#endif
