import Foundation

/// Estado de "pausa": mientras está pausado no se dispara ninguna alerta.
/// Persiste hasta `pausedUntil` (1 hora por defecto).
@MainActor
final class PauseController: ObservableObject {

    static let shared = PauseController()

    @Published var pausedUntil: Date?

    private let key = "paused_until"

    private init() {
        if let t = UserDefaults.standard.object(forKey: key) as? Date, t > Date() {
            pausedUntil = t
        }
    }

    var isPaused: Bool { (pausedUntil ?? .distantPast) > Date() }

    func pauseForOneHour() {
        let until = Date().addingTimeInterval(3600)
        pausedUntil = until
        UserDefaults.standard.set(until, forKey: key)
    }

    func resume() {
        pausedUntil = nil
        UserDefaults.standard.removeObject(forKey: key)
    }

    /// Llamar periódicamente: si la pausa expiró, limpia el estado (para refrescar UI).
    func refreshIfExpired() {
        if let u = pausedUntil, u <= Date() { resume() }
    }
}
