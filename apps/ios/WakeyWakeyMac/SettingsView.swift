import SwiftUI
import EventKit

private extension Color {
    static let wkNavy     = Color(red: 0.098, green: 0.098, blue: 0.176)
    static let wkNavyBar  = Color(red: 0.07,  green: 0.07,  blue: 0.13)
    static let wkNavyCard = Color(red: 0.145, green: 0.145, blue: 0.215)
    static let wkYellow   = Color(red: 1.0,   green: 0.878, blue: 0.227)
}

// Toggle estilo JVM: pill amarilla ON / gris OFF con knob blanco.
private struct WKToggleStyle: ToggleStyle {
    func makeBody(configuration: Configuration) -> some View {
        HStack {
            configuration.label
            Spacer()
            RoundedRectangle(cornerRadius: 11)
                .fill(configuration.isOn ? Color.wkYellow : Color.white.opacity(0.18))
                .frame(width: 42, height: 24)
                .overlay(
                    Circle().fill(configuration.isOn ? Color.wkNavy : .white).padding(3)
                        .offset(x: configuration.isOn ? 9 : -9)
                )
                .onTapGesture { configuration.$isOn.wrappedValue.toggle() }
                .animation(.easeOut(duration: 0.15), value: configuration.isOn)
        }
        .contentShape(Rectangle())
    }
}

private enum SettingsTab: String, CaseIterable {
    case calendar = "Calendar"
    case alerts   = "Alerts"
    case menuBar  = "Menu Bar"
    case app      = "App"
    var icon: String {
        switch self {
        case .calendar: return "calendar"
        case .alerts:   return "alarm.fill"
        case .menuBar:  return "menubar.rectangle"
        case .app:      return "gearshape.fill"
        }
    }
}

struct SettingsView: View {
    @StateObject private var settings = SettingsStore.shared
    @StateObject private var calendar = CalendarService.shared
    @StateObject private var mac      = MacSettings.shared
    @StateObject private var soundPlayer = SoundPreviewPlayer()
    @StateObject private var entitlement = MacEntitlementManager.shared

    @State private var tab: SettingsTab = .calendar
    @State private var launchAtLogin = LoginItemManager.isEnabled

    private var isPro: Bool { entitlement.isPro }   // trial o suscripción → todo desbloqueado

    var body: some View {
        HStack(spacing: 0) {
            sidebar
            Divider().overlay(Color.black.opacity(0.3))
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    switch tab {
                    case .calendar: calendarTab
                    case .alerts:   alertsTab
                    case .menuBar:  menuBarTab
                    case .app:      appTab
                    }
                }
                .padding(22)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .background(Color.wkNavy)
        }
        .frame(width: 580, height: 600)
        .onAppear { calendar.loadCalendars(); clampFree() }
        .onChange(of: entitlement.isPro) { _ in clampFree() }
        .onChange(of: calendar.availableCalendars.count) { _ in clampFree() }
        // Recarga inmediata de reuniones al tocar selección/filtros (el panel refleja al instante).
        .onChange(of: settings.enabledCalendarIds) { _ in reloadEvents() }
        .onChange(of: settings.videoConferenceOnly) { _ in reloadEvents() }
        .onChange(of: settings.acceptedEventsOnly) { _ in reloadEvents() }
        .onChange(of: settings.showAllDayEvents) { _ in reloadEvents() }
    }

    // MARK: - Sidebar

    private var sidebar: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Settings")
                .font(.system(size: 18, weight: .heavy))
                .foregroundStyle(Color.wkYellow)
                .padding(.horizontal, 14).padding(.top, 16).padding(.bottom, 12)

            ForEach(SettingsTab.allCases, id: \.self) { t in
                Button { tab = t } label: {
                    HStack(spacing: 10) {
                        Image(systemName: t.icon).frame(width: 18)
                        Text(t.rawValue)
                        Spacer()
                    }
                    .font(.system(size: 13, weight: tab == t ? .semibold : .regular))
                    .foregroundStyle(tab == t ? Color.wkNavy : .white.opacity(0.75))
                    .padding(.horizontal, 12).padding(.vertical, 9)
                    .background(tab == t ? Color.wkYellow : Color.clear)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .padding(.horizontal, 8)
            }
            Spacer()
        }
        .frame(width: 160)
        .background(Color.wkNavyBar)
    }

    // MARK: - Calendar tab

    private var calendarTab: some View {
        Group {
            infoCard("Using macOS system calendars",
                     "Google, iCloud, Exchange… all calendars from the macOS Calendar app.")

            sectionLabel("Event filters", "line.3.horizontal.decrease")
            toggleRow("Video meetings only", isOn: $settings.videoConferenceOnly)
            toggleRow("Accepted events only", isOn: $settings.acceptedEventsOnly)
            toggleRow("Include all-day events", isOn: $settings.showAllDayEvents)

            HStack {
                sectionLabel("Calendars", "calendar")
                if !isPro {
                    Text("🔒 Free · max 1")
                        .font(.system(size: 10, weight: .semibold)).foregroundStyle(Color.wkYellow.opacity(0.8))
                }
            }
            if calendar.availableCalendars.isEmpty {
                Text("No calendars found (grant calendar access first).")
                    .font(.system(size: 12)).foregroundStyle(.white.opacity(0.4))
            } else {
                ForEach(calendarsBySource, id: \.source) { group in
                    Text(group.source)
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(.white.opacity(0.4))
                        .padding(.top, 8)
                    ForEach(group.calendars, id: \.calendarIdentifier) { cal in
                        calendarRow(cal)
                    }
                }
            }
        }
    }

    // Calendarios agrupados por fuente (Exchange, iCloud, Other…).
    private var calendarsBySource: [(source: String, calendars: [EKCalendar])] {
        let groups = Dictionary(grouping: calendar.availableCalendars) {
            $0.source?.title ?? "Other"
        }
        return groups.keys.sorted().map { ($0, groups[$0]!.sorted { $0.title < $1.title }) }
    }

    private func calendarRow(_ cal: EKCalendar) -> some View {
        let enabled = (isPro && settings.enabledCalendarIds.isEmpty)
            || settings.enabledCalendarIds.contains(cal.calendarIdentifier)
        return Button {
            toggleCalendar(cal)
        } label: {
            HStack(spacing: 10) {
                Circle().fill(Color(cgColor: cal.cgColor ?? .black)).frame(width: 9, height: 9)
                Text(cal.title).font(.system(size: 13)).foregroundStyle(.white)
                Spacer()
                Image(systemName: enabled ? "checkmark.circle.fill" : "circle")
                    .foregroundStyle(enabled ? Color.wkYellow : .white.opacity(0.3))
            }
            .padding(.vertical, 4).padding(.leading, 8)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func toggleCalendar(_ cal: EKCalendar) {
        // Free: máximo 1 calendario → seleccionar reemplaza.
        if !isPro {
            settings.enabledCalendarIds = settings.enabledCalendarIds.contains(cal.calendarIdentifier)
                ? [] : [cal.calendarIdentifier]
            return
        }
        var ids = settings.enabledCalendarIds
        if ids.isEmpty { ids = Set(calendar.availableCalendars.map { $0.calendarIdentifier }) }
        if ids.contains(cal.calendarIdentifier) { ids.remove(cal.calendarIdentifier) }
        else { ids.insert(cal.calendarIdentifier) }
        settings.enabledCalendarIds = ids
    }

    // MARK: - Alerts tab

    private var alertsTab: some View {
        Group {
            sectionLabel("Alert", "alarm.fill")
            // Presets de minutos antes (como la JVM)
            HStack(spacing: 6) {
                ForEach([1, 2, 5, 10, 15], id: \.self) { m in
                    let sel = settings.alertMinutesBefore == m
                    Button { settings.alertMinutesBefore = m } label: {
                        Text("\(m)m")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(sel ? Color.wkNavy : .white.opacity(0.75))
                            .padding(.horizontal, 12).padding(.vertical, 6)
                            .background(sel ? Color.wkYellow : Color.wkNavyCard)
                            .clipShape(RoundedRectangle(cornerRadius: 7))
                    }
                    .buttonStyle(.plain)
                }
            }
            toggleRow("Alert on active screen only",
                      subtitle: "Off = show on all connected monitors",
                      isOn: $mac.alertActiveScreenOnly)

            sectionLabel("Sound", "speaker.wave.2.fill")
            toggleRow("Sound alert", isOn: $settings.soundEnabled)
            if settings.soundEnabled {
                HStack {
                    Text("Sound").font(.system(size: 13)).foregroundStyle(.white)
                    Spacer()
                    Picker("", selection: $settings.alertSoundName) {
                        ForEach(soundOptions) { s in
                            Text(Self.soundLabel(s.id)).tag(s.id)
                        }
                    }
                    .labelsHidden()
                    .frame(width: 190)
                    .onChange(of: settings.alertSoundName) { id in
                        if let s = AlertSound.all.first(where: { $0.id == id }) { soundPlayer.play(s) }
                    }
                }
                HStack(spacing: 10) {
                    Image(systemName: "speaker.fill").font(.system(size: 11)).foregroundStyle(.white.opacity(0.5))
                    VolumeSlider(value: $mac.volume)
                    Text("\(Int(mac.volume * 100))%")
                        .font(.system(size: 11)).foregroundStyle(.white.opacity(0.6))
                        .frame(width: 34, alignment: .trailing)
                }
                toggleRow("Loop sound", subtitle: "Repeat the sound for 30 seconds",
                          isOn: $settings.repeatSoundUntilDismiss)
            }

            HStack {
                sectionLabel("Working hours", "clock")
                if !isPro {
                    Text("🔒 Pro").font(.system(size: 10, weight: .semibold)).foregroundStyle(Color.wkYellow.opacity(0.8))
                }
            }
            if !isPro {
                Text("Upgrade to silence alerts outside your work schedule.")
                    .font(.system(size: 12)).foregroundStyle(.white.opacity(0.45))
            } else {
            toggleRow("Working hours only",
                      subtitle: "Silence alerts outside your work schedule",
                      isOn: $settings.workingHoursOnly)
            if settings.workingHoursOnly {
                HStack(spacing: 10) {
                    Text("From").font(.system(size: 12)).foregroundStyle(.white.opacity(0.7))
                    hourPicker($settings.workingHoursStart)
                    Text("to").font(.system(size: 12)).foregroundStyle(.white.opacity(0.7))
                    hourPicker($settings.workingHoursEnd)
                }
                daysRow
            }
            }
        }
    }

    /// Versión real del bundle: escribirla a mano se quedaba desfasada en cada release.
    private static var versionLabel: String {
        let info = Bundle.main.infoDictionary
        let v = info?["CFBundleShortVersionString"] as? String ?? "?"
        let b = info?["CFBundleVersion"] as? String ?? "?"
        return "WakeyWakey \(v) (\(b))"
    }

    private static let freeSoundIds = ["notification-1", "notification-2", "notification-3"]

    private var soundOptions: [AlertSound] {
        let base = AlertSound.all.filter { $0.id != "default" }
        return isPro ? base : base.filter { Self.freeSoundIds.contains($0.id) }
    }

    /// Refetch inmediato de reuniones con la selección/filtros actuales. Actualiza los
    /// @Published de CalendarService, así el panel (si está abierto) refleja al instante.
    private func reloadEvents() {
        guard calendar.isAuthorized else { return }
        let ids = settings.enabledCalendarIds
        calendar.loadTodayEvents(enabledIds: ids, settings: settings)
        calendar.loadWeekEvents(enabledIds: ids, settings: settings)
    }

    /// En Free: fuerza máx 1 calendario y sonido a Notification 1-3 (por defecto Notification 1).
    private func clampFree() {
        guard !isPro else { return }
        if settings.enabledCalendarIds.count != 1 {
            let first = settings.enabledCalendarIds.first
                ?? calendar.availableCalendars.first?.calendarIdentifier
            settings.enabledCalendarIds = first.map { [$0] } ?? []
        }
        if !Self.freeSoundIds.contains(settings.alertSoundName) {
            settings.alertSoundName = "notification-1"
        }
    }

    private func hourPicker(_ binding: Binding<Int>) -> some View {
        Picker("", selection: binding) {
            ForEach(0..<24, id: \.self) { h in
                Text(String(format: "%02d:00", h)).tag(h)
            }
        }
        .labelsHidden()
        .frame(width: 90)
    }

    private var daysRow: some View {
        let days: [(Int, String)] = [(2,"M"),(3,"T"),(4,"W"),(5,"T"),(6,"F"),(7,"S"),(1,"S")]
        return HStack(spacing: 8) {
            ForEach(days, id: \.0) { (wd, label) in
                let on = settings.workingDays.contains(wd)
                Button {
                    if on { settings.workingDays.remove(wd) } else { settings.workingDays.insert(wd) }
                } label: {
                    Text(label)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(on ? Color.wkNavy : .white.opacity(0.6))
                        .frame(width: 28, height: 28)
                        .background(on ? Color.wkYellow : Color.wkNavyCard)
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
            }
        }
    }

    // MARK: - Menu Bar tab

    private var previewText: String {
        let now = Date()
        return MenuBarController.composeTitle(
            name: "Quarterly Roadmap Planning Review",
            startDate: now.addingTimeInterval(12 * 60),
            now: now, isToday: true, isTomorrow: false, s: mac
        )
    }

    private var menuBarTab: some View {
        Group {
            sectionLabel("Display", "menubar.rectangle")
            toggleRow("Show next meeting name", isOn: $mac.showMeetingName)
            toggleRow("Show time remaining", isOn: $mac.showTimeRemaining)
            toggleRow("Minutes only  (5m vs 5m 30s)", isOn: $mac.minutesOnly)
            toggleRow("Include tomorrow's meetings", isOn: $mac.includeTomorrow)
            HStack(spacing: 10) {
                Text("Title length").font(.system(size: 13)).foregroundStyle(.white)
                Slider(value: Binding(get: { Double(mac.titleLength) },
                                      set: { mac.titleLength = Int($0) }),
                       in: 5...40, step: 1).tint(Color.wkYellow)
                Text("\(mac.titleLength)").font(.system(size: 12))
                    .foregroundStyle(.white.opacity(0.6)).frame(width: 24)
            }
            toggleRow("Truncate in the middle",
                      subtitle: "e.g. \"Standup…sprint\" instead of \"Standup…\"",
                      isOn: $mac.truncateMiddle)

            sectionLabel("Appearance", "paintpalette.fill")
            toggleRow("Monochrome icon",
                      subtitle: "Icon is always yellow — color applies to text only",
                      isOn: $mac.monochromeIcon)
            HStack(spacing: 10) {
                Text("Text").font(.system(size: 13)).foregroundStyle(.white)
                    .frame(width: 40, alignment: .leading)
                ForEach(MenuBarColor.all, id: \.id) { c in
                    Button { mac.menuTextColor = c.id } label: {
                        Circle().fill(c.color).frame(width: 26, height: 26)
                            .overlay(Circle().stroke(.white, lineWidth: mac.menuTextColor == c.id ? 2.5 : 0))
                    }
                    .buttonStyle(.plain)
                }
            }
            // Preview (usa la misma lógica que la barra real)
            HStack(spacing: 8) {
                Image("MenuBarIcon")
                    .resizable().frame(width: 18, height: 18).clipShape(Circle())
                    .grayscale(mac.monochromeIcon ? 1 : 0)
                Text(previewText)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(MenuBarColor.color(mac.menuTextColor))
            }
            .padding(12).frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.black.opacity(0.3))
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .padding(.top, 6)
        }
    }

    // MARK: - App tab

    private var appTab: some View {
        Group {
            sectionLabel("General", "gearshape.fill")
            Toggle(isOn: Binding(
                get: { launchAtLogin },
                set: { launchAtLogin = $0; LoginItemManager.setEnabled($0) }
            )) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Launch at login").font(.system(size: 13)).foregroundStyle(.white)
                    Text("Start WakeyWakey when you log in")
                        .font(.system(size: 11)).foregroundStyle(.white.opacity(0.5))
                }
            }
            .toggleStyle(WKToggleStyle())

            sectionLabel("Plan", "star.fill")
            planStatus
            if let packages = entitlement.offerings?.current?.availablePackages, !packages.isEmpty {
                ForEach(packages, id: \.identifier) { pkg in
                    Button { Task { await entitlement.purchase(pkg) } } label: {
                        HStack {
                            Text(pkg.storeProduct.localizedTitle).font(.system(size: 13)).foregroundStyle(Color.wkNavy)
                            Spacer()
                            Text(pkg.storeProduct.localizedPriceString).font(.system(size: 13, weight: .bold)).foregroundStyle(Color.wkNavy)
                        }
                        .padding(.horizontal, 14).padding(.vertical, 9)
                        .background(Color.wkYellow).clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    .buttonStyle(.plain)
                }
            } else {
                Text("No subscription options available yet (configure products in App Store Connect).")
                    .font(.system(size: 11)).foregroundStyle(.white.opacity(0.4))
                    .fixedSize(horizontal: false, vertical: true)
            }
            if let err = entitlement.purchaseError {
                Text(err).font(.system(size: 11)).foregroundStyle(.red)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Button("Restore purchases") { Task { await entitlement.restore() } }
                .buttonStyle(.plain).foregroundStyle(Color.wkYellow).font(.system(size: 12, weight: .semibold))
            if let err = entitlement.restoreError {
                Text(err).font(.system(size: 11)).foregroundStyle(.red)
            }
            // 3.1.2(c): enlaces requeridos donde se ofrece la suscripción.
            HStack(spacing: 14) {
                Link("Privacy Policy", destination: PaywallView.privacyPolicyURL)
                Link("Terms of Use (EULA)", destination: PaywallView.termsOfUseURL)
            }
            .font(.system(size: 11, weight: .medium))
            .foregroundStyle(.white.opacity(0.55))

            // Solo visible en el Mac de desarrollo (DevGate). Nadie más lo ve.
            if DevGate.isDevMachine {
                sectionLabel("Developer", "hammer.fill")
                toggleRow("Show debug bar",
                          subtitle: "Panel debug tools (this Mac only)",
                          isOn: $mac.showDebugBar)
            }

            Divider().overlay(Color.white.opacity(0.08)).padding(.vertical, 8)
            Text(Self.versionLabel)
                .font(.system(size: 12)).foregroundStyle(.white.opacity(0.4))
        }
    }

    @ViewBuilder
    private var planStatus: some View {
        if entitlement.trialDaysLeft <= 0 && entitlement.isPro {
            Text("✅ WakeyWakey Pro").font(.system(size: 13, weight: .semibold)).foregroundStyle(Color.wkYellow)
        } else if entitlement.trialDaysLeft > 0 {
            VStack(alignment: .leading, spacing: 2) {
                Text("🔄 Free trial").font(.system(size: 13, weight: .semibold)).foregroundStyle(.white)
                Text("\(entitlement.trialDaysLeft) days remaining").font(.system(size: 11)).foregroundStyle(.white.opacity(0.5))
            }
        } else {
            Text("⛔ Trial expired — upgrade to keep using WakeyWakey")
                .font(.system(size: 13, weight: .semibold)).foregroundStyle(.red)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    // MARK: - Componentes reutilizables

    private func sectionLabel(_ title: String, _ icon: String) -> some View {
        HStack(spacing: 7) {
            Image(systemName: icon).font(.system(size: 12)).foregroundStyle(Color.wkYellow)
            Text(title).font(.system(size: 12, weight: .bold)).foregroundStyle(Color.wkYellow)
        }
        .padding(.top, 8)
    }

    private func toggleRow(_ label: String, subtitle: String? = nil, isOn: Binding<Bool>) -> some View {
        Toggle(isOn: isOn) {
            VStack(alignment: .leading, spacing: 2) {
                Text(label).font(.system(size: 13)).foregroundStyle(.white)
                if let subtitle {
                    Text(subtitle).font(.system(size: 11)).foregroundStyle(.white.opacity(0.5))
                }
            }
        }
        .toggleStyle(WKToggleStyle())
    }

    static func soundLabel(_ id: String) -> String {
        let map: [String: String] = [
            "default": "🔔 System", "clock-alarm": "⏰ Clock Alarm",
            "service-bell": "🛎️ Service Bell", "call-to-attention": "📣 Call to Attention",
            "boxing-ring": "🥊 Boxing Ring", "coin": "🪙 Coin", "level-up": "⏫ Level Up",
            "metal-spring": "🌀 Metal Spring", "notification-1": "🔔 Notification 1",
            "notification-2": "🔔 Notification 2", "notification-3": "🔔 Notification 3",
            "notification-4": "🔔 Notification 4", "notification-5": "🔔 Notification 5",
            "punch": "👊 Punch", "referee-whistle": "📢 Referee Whistle", "whistle": "🎵 Whistle",
        ]
        return map[id] ?? id
    }

    private func infoCard(_ title: String, _ subtitle: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title).font(.system(size: 13, weight: .semibold)).foregroundStyle(.white)
            Text(subtitle).font(.system(size: 11)).foregroundStyle(.white.opacity(0.55))
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.wkNavyCard)
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }
}

// MARK: - Slider de volumen custom (estilo JVM)

private struct VolumeSlider: View {
    @Binding var value: Double   // 0...1

    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let x = max(0, min(w, w * value))
            ZStack(alignment: .leading) {
                Capsule().fill(Color.white.opacity(0.12)).frame(height: 8)
                Capsule().fill(Color(red: 1, green: 0.878, blue: 0.227))
                    .frame(width: x, height: 8)
                RoundedRectangle(cornerRadius: 2)
                    .fill(Color(red: 1, green: 0.878, blue: 0.227))
                    .frame(width: 5, height: 20)
                    .offset(x: x - 2.5)
            }
            .frame(maxHeight: .infinity, alignment: .center)
            .contentShape(Rectangle())
            .gesture(DragGesture(minimumDistance: 0).onChanged { g in
                value = max(0, min(1, g.location.x / w))
            })
        }
        .frame(height: 22)
    }
}
