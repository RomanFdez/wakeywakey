import SwiftUI
import EventKit

// MARK: - Paleta de marca (igual que la app JVM/iOS)

private extension Color {
    static let wkNavy       = Color(red: 0.098, green: 0.098, blue: 0.176) // lista (base)
    static let wkNavyHeader = Color(red: 0.145, green: 0.145, blue: 0.215) // zona superior
    static let wkNavyPill   = Color(red: 0.075, green: 0.075, blue: 0.14)  // pill no seleccionada
    static let wkYellow     = Color(red: 1.0,   green: 0.878, blue: 0.227) // #FFE03A
}

private enum PanelTab { case today, all }

/// Panel desplegable del icono de la barra de menú (equivalente al TrayMenuWindow
/// de la app JVM). Reutiliza CalendarService (EventKit). Textos en inglés.
struct MenuPanelView: View {

    @StateObject private var calendar = CalendarService.shared
    @StateObject private var settings = SettingsStore.shared
    @StateObject private var manual   = ManualEventsStore.shared
    @StateObject private var pause    = PauseController.shared
    @StateObject private var mac      = MacSettings.shared
    @StateObject private var entitlement = MacEntitlementManager.shared

    @State private var tab: PanelTab = .today
    @State private var now = Date()
    @State private var lastEventsReload = Date.distantPast
    // 1s: mantiene las cuentas atrás suaves. El refetch de EventKit se limita a cada 30s.
    private let ticker = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    private var meetings: [AnyMeeting] {
        let cal = tab == .today ? calendar.todayEvents : calendar.weekEvents
        let man = (tab == .today ? manual.todayEvents : manual.weekEvents).map { AnyMeeting(manual: $0) }
        // Ocultar reuniones ya terminadas (las en curso sí se muestran).
        return (cal + man)
            .filter { $0.endDate > now }
            .sorted { $0.startDate < $1.startDate }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Zona superior con fondo diferenciado del listado
            VStack(alignment: .leading, spacing: 0) {
                actionBar
                Divider().overlay(Color.black.opacity(0.25))
                header
            }
            .background(Color.wkNavyHeader)

            Divider().overlay(Color.black.opacity(0.3))
            content

            if DevGate.isDevMachine && mac.showDebugBar { debugBar }
        }
        .frame(width: 400)
        .background(Color.wkNavy)
        .task { await ensureAccessAndLoad() }
        .onReceive(ticker) { t in
            now = t
            pause.refreshIfExpired()
            if t.timeIntervalSince(lastEventsReload) >= 30 { lastEventsReload = t; reload() }
        }
    }

    // MARK: - Barra de acciones (tres segmentos iguales, como la JVM)

    private var actionBar: some View {
        HStack(spacing: 0) {
            if pause.isPaused {
                barButton("Resume", systemImage: "play.fill", tint: .green) { pause.resume() }
            } else {
                barButton("Pause 1h", systemImage: "pause.fill") { pause.pauseForOneHour() }
            }
            barDivider
            barButton("Settings", systemImage: "gearshape.fill") { SettingsWindowController.shared.show() }
            barDivider
            barButton("Quit", systemImage: "xmark") { NSApplication.shared.terminate(nil) }
        }
    }

    private var barDivider: some View {
        Rectangle().fill(Color.white.opacity(0.08)).frame(width: 1, height: 20)
    }

    private func barButton(_ title: String, systemImage: String,
                           tint: Color = .white.opacity(0.75),
                           action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: systemImage)
                Text(title)
            }
            .font(.system(size: 12, weight: tint == .green ? .semibold : .regular))
            .foregroundStyle(tint)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    // MARK: - Cabecera (título + pestañas + añadir)

    private var header: some View {
        HStack(spacing: 10) {
            Text("WakeyWakey")
                .font(.system(size: 16, weight: .heavy))
                .foregroundStyle(Color.wkYellow)

            Spacer()

            HStack(spacing: 4) {
                tabPill("Today", isSelected: tab == .today) { tab = .today; reload() }
                tabPill("All",   isSelected: tab == .all)   { tab = .all;   reload() }
            }

            Button { AddEventWindowController.shared.show() } label: {
                Image(systemName: "plus")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(Color.wkNavy)
                    .frame(width: 28, height: 24)
                    .background(Color.wkYellow)
                    .clipShape(RoundedRectangle(cornerRadius: 6))
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }

    private func tabPill(_ title: String, isSelected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(isSelected ? Color.wkNavy : .white.opacity(0.7))
                .padding(.horizontal, 12).padding(.vertical, 5)
                .background(isSelected ? Color.wkYellow : Color.wkNavyPill)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    // MARK: - Contenido según estado

    @ViewBuilder
    private var content: some View {
        if !calendar.isAuthorized {
            permissionPrompt
        } else if meetings.isEmpty {
            emptyState
        } else {
            meetingList
        }
    }

    private var permissionPrompt: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("WakeyWakey needs access to your calendar to alert you before your meetings.")
                .font(.system(size: 13))
                .foregroundStyle(.white.opacity(0.8))
                .fixedSize(horizontal: false, vertical: true)
            Button {
                Task { await ensureAccessAndLoad() }
            } label: {
                Text("Grant calendar access")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(Color.wkNavy)
                    .padding(.horizontal, 14).padding(.vertical, 8)
                    .background(Color.wkYellow)
                    .clipShape(Capsule())
            }
            .buttonStyle(.plain)
        }
        .padding(16)
    }

    private var emptyState: some View {
        VStack(spacing: 8) {
            Image(systemName: "calendar.badge.checkmark")
                .font(.system(size: 26))
                .foregroundStyle(.white.opacity(0.25))
            Text(tab == .today ? "No meetings today" : "No upcoming meetings")
                .font(.system(size: 13))
                .foregroundStyle(.white.opacity(0.4))
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 30)
    }

    // Reuniones agrupadas por día, con etiqueta (Today / Tomorrow / día).
    private var groupedByDay: [(label: String, items: [AnyMeeting])] {
        let cal = Calendar.current
        let groups = Dictionary(grouping: meetings) { cal.startOfDay(for: $0.startDate) }
        return groups.keys.sorted().map { day in
            (dayLabel(day), groups[day]!.sorted { $0.startDate < $1.startDate })
        }
    }

    private func dayLabel(_ date: Date) -> String {
        let cal = Calendar.current
        if cal.isDateInToday(date)    { return "Today" }
        if cal.isDateInTomorrow(date) { return "Tomorrow" }
        let df = DateFormatter()
        df.locale = Locale(identifier: "en")
        df.dateFormat = "EEEE d MMM"
        return df.string(from: date)
    }

    private var meetingList: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 2) {
                ForEach(groupedByDay, id: \.label) { group in
                    Text(group.label)
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(.white.opacity(0.4))
                        .padding(.horizontal, 16)
                        .padding(.top, 12).padding(.bottom, 4)

                    ForEach(group.items) { meeting in
                        MeetingRowMac(meeting: meeting, now: now)
                    }
                }
            }
            .padding(.bottom, 8)
        }
        .scrollIndicators(.never)
        .frame(height: 420)   // altura fija; scroll con la rueda, sin barra visible
    }

    // MARK: - Debug (se elimina en release vía #if DEBUG)

    private var debugBar: some View {
        VStack(spacing: 1) {
            HStack(spacing: 1) {
                debugButton("Simulate alert", "ladybug.fill") {
                    let m = calendar.todayEvents.first { $0.endDate > now }
                    AlertWindowController.shared.show(
                        title:      m?.title ?? "Sample meeting",
                        startDate:  m?.startDate ?? Date().addingTimeInterval(120),
                        location:   m?.location ?? "Microsoft Teams Meeting",
                        meetingURL: m?.meetingURL ?? URL(string: "https://meet.google.com/abc-defg-hij")
                    )
                }
                debugButton("Onboarding", "hand.wave.fill") {
                    OnboardingWindowController.shared.show()
                }
                debugButton("💰 Paywall", nil) {
                    PaywallWindowController.shared.show()
                }
            }
            HStack(spacing: 1) {
                debugButton("⏳ Trial", nil) { MacEntitlementManager.shared.debugSet(.trial) }
                debugButton("🔒 Free",  nil) { MacEntitlementManager.shared.debugSet(.free) }
                debugButton("🔓 Pro",   nil) { MacEntitlementManager.shared.debugSet(.pro) }
            }
            // Contador de día de trial: simula el día N y muestra el estado resultante.
            HStack(spacing: 1) {
                debugButton("–", nil) {
                    MacEntitlementManager.shared.debugSetTrialDay((entitlement.debugTrialDay ?? 1) - 1)
                }
                Text(trialDayLabel)
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(.black)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 7)
                    .background(entitlement.debugTrialDay == nil ? Color.orange
                                : (entitlement.isPro ? Color.green : Color.red.opacity(0.85)))
                debugButton("+", nil) {
                    MacEntitlementManager.shared.debugSetTrialDay((entitlement.debugTrialDay ?? 1) + 1)
                }
            }
        }
    }

    private var trialDayLabel: String {
        guard let day = entitlement.debugTrialDay else { return "Trial day —" }
        return "Day \(day) · \(entitlement.trialDaysLeft)d left · \(entitlement.isPro ? "Pro" : "Free")"
    }

    private func debugButton(_ title: String, _ icon: String?, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Group {
                if let icon { Label(title, systemImage: icon) } else { Text(title) }
            }
            .font(.system(size: 11, weight: .semibold))
            .foregroundStyle(.black)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 7)
            .background(Color.orange)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    // MARK: - Datos

    private func ensureAccessAndLoad() async {
        if !calendar.isAuthorized {
            _ = await calendar.requestCalendarAccess()
        }
        reload()
    }

    private func reload() {
        guard calendar.isAuthorized else { return }
        let ids = settings.enabledCalendarIds
        if tab == .today {
            calendar.loadTodayEvents(enabledIds: ids, settings: settings)
        } else {
            calendar.loadWeekEvents(enabledIds: ids, settings: settings)
        }
    }
}

// MARK: - Fila de reunión (estilo JVM)

private struct MeetingRowMac: View {
    let meeting: AnyMeeting
    let now: Date

    private var barColor: Color {
        if let cg = meeting.calendarColor { return Color(cgColor: cg) }
        return .orange
    }

    private var isOngoing: Bool { meeting.startDate <= now && meeting.endDate > now }

    private var countdownText: String {
        if isOngoing { return "In progress" }
        let secs = Int(meeting.startDate.timeIntervalSince(now))
        if secs < 0 { return "Ended" }
        // Redondeo hacia arriba: mientras no haya empezado nunca muestra "In 0m".
        if secs < 3600 { return "In \((secs + 59) / 60)m" }
        let mins = secs / 60, h = mins / 60, m = mins % 60
        return m == 0 ? "In \(h)h" : "In \(h)h \(m)m"
    }

    private var isImminent: Bool {
        !isOngoing && meeting.startDate > now && meeting.startDate.timeIntervalSince(now) < 15 * 60
    }

    private var timeRange: String {
        let f = DateFormatter(); f.dateFormat = "HH:mm"
        return "\(f.string(from: meeting.startDate))–\(f.string(from: meeting.endDate))"
    }

    var body: some View {
        HStack(spacing: 10) {
            RoundedRectangle(cornerRadius: 2)
                .fill(barColor)
                .frame(width: 3, height: 38)

            VStack(alignment: .leading, spacing: 3) {
                Text(meeting.title)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(.white)
                    .lineLimit(1)
                HStack(spacing: 5) {
                    if isOngoing {
                        Circle().fill(.green).frame(width: 6, height: 6)
                    }
                    Text(countdownText)
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(isOngoing ? .green : (isImminent ? .red : Color.wkYellow))
                    Text("· \(timeRange)")
                        .font(.system(size: 11))
                        .foregroundStyle(.white.opacity(0.5))
                }
            }

            Spacer(minLength: 4)

            HStack(spacing: 6) {
                if let url = meeting.meetingURL {
                    // Solo la cámara inicia la reunión.
                    Button { NSWorkspace.shared.open(url) } label: {
                        Image(systemName: "video.fill")
                            .font(.system(size: 13))
                            .foregroundStyle(Color.wkYellow)
                            .padding(6)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .help("Join meeting")
                }
                // Papelera solo en alertas ad-hoc (creadas con "+").
                if meeting.isManual, let mid = meeting.manualId {
                    Button { ManualEventsStore.shared.delete(id: mid) } label: {
                        Image(systemName: "trash")
                            .font(.system(size: 12))
                            .foregroundStyle(.white.opacity(0.4))
                            .padding(6)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .help("Delete alert")
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .contentShape(Rectangle())
        // Clic en la fila = vista previa de la alerta de esa reunión (no inicia la reunión).
        .onTapGesture {
            AlertWindowController.shared.show(
                title:      meeting.title,
                startDate:  meeting.startDate,
                location:   meeting.location,
                meetingURL: meeting.meetingURL,
                isPreview:  true
            )
        }
    }
}
