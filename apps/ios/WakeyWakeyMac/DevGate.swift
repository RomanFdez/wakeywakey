import Foundation

/// Utilidades de desarrollo (barra de debug) visibles SOLO en el Mac de desarrollo,
/// en cualquier build (Debug o Release). Así no hace falta una build de test aparte.
enum DevGate {
    private static let devHardwareUUID = "53786000-0890-5F60-8BAB-9646F9EEEE8B"

    static let isDevMachine: Bool = {
        var bytes = [UInt8](repeating: 0, count: 16)
        var timeout = timespec(tv_sec: 0, tv_nsec: 0)
        guard gethostuuid(&bytes, &timeout) == 0 else { return false }
        let uuid = NSUUID(uuidBytes: bytes) as UUID
        return uuid.uuidString == devHardwareUUID
    }()
}
