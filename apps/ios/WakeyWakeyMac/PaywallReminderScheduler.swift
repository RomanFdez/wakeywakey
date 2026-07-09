import AppKit
import CoreGraphics

/// Recordatorio de conversión: si el usuario está en Free, muestra el paywall en
/// franjas concretas del día (por defecto 10:00 y 13:00), **solo si está usando el
/// ordenador** (poca inactividad de teclado/ratón). Máximo una vez por franja y día.
@MainActor
final class PaywallReminderScheduler {
    static let shared = PaywallReminderScheduler()

    /// Horas del día en las que recordar (se dispara en el primer minuto activo de esa hora).
    private let reminderHours = [10, 13]
    /// El usuario se considera "activo" si lleva menos de esto sin input.
    private let idleThreshold: TimeInterval = 120
    /// Franjas ya mostradas ("yyyy-MM-dd-HH"), para no repetir dentro de la misma hora/día.
    private var shownSlots: Set<String> = []

    private var timer: Timer?

    func start() {
        guard timer == nil else { return }
        let t = Timer(timeInterval: 60, repeats: true) { [weak self] _ in
            Task { @MainActor in self?.tick() }
        }
        RunLoop.main.add(t, forMode: .common)
        timer = t
    }

    private func tick() {
        // Solo para usuarios Free.
        guard !MacEntitlementManager.shared.isPro else { return }

        let now = Date()
        let hour = Calendar.current.component(.hour, from: now)
        guard reminderHours.contains(hour) else { return }

        let key = Self.slotKey(now)
        guard !shownSlots.contains(key) else { return }

        // Solo si el ordenador se está usando.
        guard userIsActive() else { return }

        shownSlots.insert(key)
        PaywallWindowController.shared.show()
    }

    /// Segundos desde el último input de teclado/ratón (kCGAnyInputEventType = ~0).
    private func userIsActive() -> Bool {
        guard let anyInput = CGEventType(rawValue: ~0) else { return true }
        let idle = CGEventSource.secondsSinceLastEventType(.combinedSessionState, eventType: anyInput)
        return idle < idleThreshold
    }

    private static func slotKey(_ date: Date) -> String {
        let f = DateFormatter(); f.dateFormat = "yyyy-MM-dd-HH"
        return f.string(from: date)
    }
}
