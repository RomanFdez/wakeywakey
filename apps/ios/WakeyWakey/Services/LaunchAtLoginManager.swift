#if targetEnvironment(macCatalyst)
import Foundation
import ServiceManagement

/// Arranque al iniciar sesión en Mac, vía `SMAppService` (macOS 13+ / Mac Catalyst 16+).
/// Reemplaza el LaunchAgent de `~/Library/LaunchAgents` que usa la app JVM — ese enfoque
/// no está permitido bajo App Sandbox.
@MainActor
enum LaunchAtLoginManager {

    static var isEnabled: Bool {
        guard #available(macCatalyst 16.0, *) else { return false }
        return SMAppService.mainApp.status == .enabled
    }

    @discardableResult
    static func setEnabled(_ enabled: Bool) -> Bool {
        guard #available(macCatalyst 16.0, *) else { return false }
        do {
            if enabled {
                try SMAppService.mainApp.register()
            } else {
                try SMAppService.mainApp.unregister()
            }
            return true
        } catch {
            return false
        }
    }
}
#endif
