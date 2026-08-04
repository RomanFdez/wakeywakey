import AppKit

/// Abre el enlace de una reunión prefiriendo la app nativa si está instalada.
///
/// Teams: los enlaces `https://teams.microsoft.com/l/meetup-join/…` abren el navegador,
/// que a su vez pregunta "¿continuar aquí o abrir la app?". Si el cliente de Teams está
/// instalado (maneja el esquema `msteams://`), reescribimos el enlace a ese esquema y
/// Teams se abre directamente. Si no está instalado, se abre el `https://` en el
/// navegador como siempre — cubre ambos casos sin configuración.
enum MeetingLauncher {

    static func open(_ url: URL) {
        if let native = nativeAppURL(for: url), isSchemeHandled(native) {
            NSWorkspace.shared.open(native)
        } else {
            NSWorkspace.shared.open(url)
        }
    }

    // MARK: - Reescritura por proveedor

    private static func nativeAppURL(for url: URL) -> URL? {
        let host = url.host?.lowercased() ?? ""

        // Microsoft Teams (tenants de empresa y personales)
        if host.contains("teams.microsoft.com") || host.contains("teams.live.com") {
            var comps = URLComponents(url: url, resolvingAgainstBaseURL: false)
            comps?.scheme = "msteams"
            return comps?.url
        }

        // Zoom: https://zoom.us/j/<id>?pwd=x → zoommtg://zoom.us/join?confno=<id>&pwd=x
        if host.contains("zoom.us"), url.path.hasPrefix("/j/") {
            let confno = url.lastPathComponent
            guard !confno.isEmpty, confno.allSatisfy(\.isNumber) else { return nil }
            var comps = URLComponents()
            comps.scheme = "zoommtg"
            comps.host = url.host
            comps.path = "/join"
            var items = [URLQueryItem(name: "confno", value: confno)]
            if let pwd = URLComponents(url: url, resolvingAgainstBaseURL: false)?
                .queryItems?.first(where: { $0.name == "pwd" })?.value {
                items.append(URLQueryItem(name: "pwd", value: pwd))
            }
            comps.queryItems = items
            return comps.url
        }

        return nil
    }

    /// ¿Hay alguna app instalada que maneje este esquema? (no lanza nada)
    private static func isSchemeHandled(_ url: URL) -> Bool {
        NSWorkspace.shared.urlForApplication(toOpen: url) != nil
    }
}
