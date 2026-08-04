import Foundation
import ServiceManagement

/// Arranque al iniciar sesión (macOS 13+), vía `SMAppService`.
@MainActor
enum LoginItemManager {
    static var isEnabled: Bool { SMAppService.mainApp.status == .enabled }

    @discardableResult
    static func setEnabled(_ enabled: Bool) -> Bool {
        do {
            if enabled { try SMAppService.mainApp.register() }
            else       { try SMAppService.mainApp.unregister() }
            return true
        } catch {
            return false
        }
    }
}
