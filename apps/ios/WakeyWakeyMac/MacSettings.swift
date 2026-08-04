import Foundation

/// Ajustes específicos de macOS que no están en el `SettingsStore` compartido con iOS.
@MainActor
final class MacSettings: ObservableObject {
    static let shared = MacSettings()

    private let d = UserDefaults.standard

    // Alerts
    @Published var alertActiveScreenOnly: Bool { didSet { save(\.alertActiveScreenOnly, "mac_active_screen_only") } }
    @Published var volume: Double              { didSet { d.set(volume, forKey: "mac_volume") } }

    // Menu Bar · Display
    @Published var showMeetingName: Bool   { didSet { save(\.showMeetingName, "mb_show_name") } }
    @Published var showTimeRemaining: Bool { didSet { save(\.showTimeRemaining, "mb_show_time") } }
    @Published var minutesOnly: Bool       { didSet { save(\.minutesOnly, "mb_minutes_only") } }
    @Published var includeTomorrow: Bool   { didSet { save(\.includeTomorrow, "mb_include_tomorrow") } }
    @Published var titleLength: Int        { didSet { d.set(titleLength, forKey: "mb_title_length") } }
    @Published var truncateMiddle: Bool    { didSet { save(\.truncateMiddle, "mb_truncate_middle") } }

    // Menu Bar · Appearance
    @Published var monochromeIcon: Bool    { didSet { save(\.monochromeIcon, "mb_mono_icon") } }
    @Published var menuTextColor: String   { didSet { d.set(menuTextColor, forKey: "mb_text_color") } }

    // Solo efectivo en el Mac de desarrollo (ver DevGate). Permite ocultar la
    // barra de debug para hacer capturas limpias.
    @Published var showDebugBar: Bool      { didSet { save(\.showDebugBar, "mb_show_debug_bar") } }

    private func save(_ kp: KeyPath<MacSettings, Bool>, _ key: String) {
        d.set(self[keyPath: kp], forKey: key)
    }

    private init() {
        alertActiveScreenOnly = d.value(forKey: "mac_active_screen_only") as? Bool ?? true
        volume            = d.value(forKey: "mac_volume") as? Double ?? 0.5
        showMeetingName   = d.value(forKey: "mb_show_name") as? Bool ?? true
        showTimeRemaining = d.value(forKey: "mb_show_time") as? Bool ?? true
        minutesOnly       = d.value(forKey: "mb_minutes_only") as? Bool ?? true
        includeTomorrow   = d.value(forKey: "mb_include_tomorrow") as? Bool ?? true
        titleLength       = d.value(forKey: "mb_title_length") as? Int ?? 16
        truncateMiddle    = d.value(forKey: "mb_truncate_middle") as? Bool ?? false
        monochromeIcon    = d.value(forKey: "mb_mono_icon") as? Bool ?? false
        menuTextColor     = d.string(forKey: "mb_text_color") ?? "auto"
        showDebugBar      = d.value(forKey: "mb_show_debug_bar") as? Bool ?? true
    }
}
