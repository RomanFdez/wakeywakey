import Foundation
import Sentry

/// Reporte de errores con Sentry.
///
/// El DSN llega desde `Secrets.xcconfig` (fuera del repo) vía Info.plist. Si no hay
/// DSN configurado —desarrollo, o un clon del repo sin secretos— queda desactivado
/// y la app funciona igual. Mismo patrón que la app JVM.
///
/// PRIVACIDAD: solo diagnóstico. Nada de PII y, sobre todo, ningún dato de
/// calendario (títulos, asistentes, ubicaciones): son datos sensibles del usuario
/// y la app declara únicamente "datos de diagnóstico" en App Store Connect.
enum CrashReporting {

    static func start() {
        guard let dsn = configuredDSN else { return }
        SentrySDK.start { options in
            options.dsn = dsn
            options.environment = isDebugBuild ? "development" : "production"
            options.releaseName = releaseName
            // Nunca datos personales (IP, etc.).
            options.sendDefaultPii = false
            // Los breadcrumbs automáticos de UI pueden capturar títulos de ventana
            // (nombre de la reunión) → desactivados.
            options.enableAutoBreadcrumbTracking = false
            options.tracesSampleRate = isDebugBuild ? 1.0 : 0.2
        }
    }

    /// Errores manejados que no llegan a crash pero conviene conocer.
    static func capture(_ error: Error, context: [String: String] = [:]) {
        guard configuredDSN != nil else { return }
        SentrySDK.capture(error: error) { scope in
            context.forEach { scope.setExtra(value: $0.value, key: $0.key) }
        }
    }

    // MARK: - Configuración

    /// DSN del Info.plist. Se guarda sin "https://" porque en los .xcconfig "//"
    /// abre un comentario; aquí se reconstruye.
    private static var configuredDSN: String? {
        let raw = (Bundle.main.object(forInfoDictionaryKey: "WWSentryDSN") as? String ?? "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        guard !raw.isEmpty else { return nil }
        return raw.contains("://") ? raw : "https://\(raw)"
    }

    private static var releaseName: String {
        let info = Bundle.main.infoDictionary
        let version = info?["CFBundleShortVersionString"] as? String ?? "0"
        let build   = info?["CFBundleVersion"] as? String ?? "0"
        return "wakeywakey-mac@\(version)+\(build)"
    }

    private static var isDebugBuild: Bool {
        #if DEBUG
        return true
        #else
        return false
        #endif
    }
}
