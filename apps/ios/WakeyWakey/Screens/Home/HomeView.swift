import SwiftUI
import UserNotifications

// MARK: - Scope

private enum ViewScope: String, CaseIterable {
    case today = "Hoy"
    case week  = "Esta semana"
}

// MARK: - Day section model

private struct DaySection: Identifiable {
    let id: Date          // startOfDay
    let header: String
    let meetings: [AnyMeeting]
}

// MARK: - HomeView

struct HomeView: View {

    @EnvironmentObject var settings:      SettingsStore
    @EnvironmentObject var calendarService: CalendarService
    @EnvironmentObject var entitlements:  EntitlementManager
    @Environment(\.showPaywall) private var showPaywall
    @StateObject private var manualStore = ManualEventsStore.shared

    @State private var now = Date()
    @State private var scope: ViewScope         = .today
    @State private var showSettings             = false
    @State private var showAddManual            = false
    @State private var selectedMeeting: AnyMeeting?
    @State private var cachedMeetings: [AnyMeeting]      = []
    @State private var cachedWeekMeetings: [AnyMeeting]  = []
    @State private var cachedWeekSections: [DaySection]  = []
    @State private var nextMeetingWasOngoing              = false
    #if DEBUG
    @State private var soundTestCounter   = 0
    @State private var soundTestScheduled = false
    @State private var showDebugTrialBar  = true
    #endif
    // 30 s es suficiente para actualizar "isNow" y "isPast" en la lista.
    // La precisión de segundo solo la necesita NextMeetingCard, que tiene su propio timer.
    private let timer = Timer.publish(every: 30, on: .main, in: .common).autoconnect()

    // Solo filtra por tiempo — no reconstruye AnyMeeting cada tick
    private var allEvents: [AnyMeeting] {
        cachedMeetings.filter { $0.endDate > now }
    }

    private var nextMeeting: AnyMeeting? {
        let upcoming = allEvents.filter { $0.startDate > now }
        if let imminent = upcoming.first(where: { $0.startDate.timeIntervalSince(now) <= 5 * 60 }) {
            return imminent
        }
        return allEvents.first { $0.endDate > now }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.wkNavy.ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {

                        // ── Próxima reunión ──────────────────────────────
                        if let next = nextMeeting {
                            NextMeetingCard(meeting: next)
                                .padding(.horizontal, 20)
                                .padding(.top, 8)
                                .padding(.bottom, 20)
                        } else {
                            EmptyStateCard()
                                .padding(.horizontal, 20)
                                .padding(.top, 8)
                                .padding(.bottom, 20)
                        }

                        // ── Debug trial bar ──────────────────────────────
                        #if DEBUG
                        if showDebugTrialBar {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 6) {
                                    Text("Trial:")
                                        .font(.system(size: 10, weight: .bold))
                                        .foregroundStyle(.white.opacity(0.4))
                                    ForEach([14, 7, 1, 0], id: \.self) { days in
                                        Button("D\(EntitlementManager.trialDays - days)") {
                                            entitlements.debugSetTrialDays(days)
                                        }
                                        .font(.system(size: 10, weight: .semibold))
                                        .foregroundStyle(entitlements.trialDaysLeft == days ? Color.wkNavy : .white)
                                        .padding(.horizontal, 8).padding(.vertical, 4)
                                        .background(entitlements.trialDaysLeft == days ? Color.wkYellow : Color.white.opacity(0.1))
                                        .clipShape(RoundedRectangle(cornerRadius: 6))
                                    }
                                    Button("×") { showDebugTrialBar = false }
                                        .font(.system(size: 10))
                                        .foregroundStyle(.white.opacity(0.3))
                                }
                                .padding(.horizontal, 20)
                            }
                            .padding(.bottom, 10)
                        }
                        #endif

                        // ── Selector Hoy / Esta semana ───────────────────
                        scopePicker
                            .padding(.horizontal, 20)
                            .padding(.bottom, 20)

                        // ── Lista ────────────────────────────────────────
                        if scope == .today {
                            todaySection
                        } else {
                            weekSectionView
                        }

                        Spacer(minLength: 40)
                    }
                }
                .refreshable {
                    calendarService.loadTodayEvents(enabledIds: settings.enabledCalendarIds, settings: settings)
                    calendarService.loadWeekEvents(enabledIds: settings.enabledCalendarIds, settings: settings)
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .principal) {
                    VStack(spacing: 1) {
                        Text("WakeyWakey")
                            .font(.system(size: 17, weight: .semibold))
                            .foregroundStyle(.white)
                        Text(todayShort)
                            .font(.system(size: 11))
                            .foregroundStyle(.white.opacity(0.5))
                    }
                }
                ToolbarItem(placement: .topBarLeading) {
                    HStack(spacing: 14) {
                        Button { showAddManual = true } label: {
                            Image(systemName: "plus")
                                .foregroundStyle(Color.wkYellow)
                        }
                        #if DEBUG
                        Button { scheduleSoundTest() } label: {
                            ZStack(alignment: .topTrailing) {
                                Image(systemName: soundTestScheduled ? "speaker.wave.3.fill" : "speaker.wave.2.fill")
                                    .foregroundStyle(soundTestScheduled ? Color.green : Color.orange)
                                if soundTestCounter > 0 {
                                    Text("\(soundTestCounter)")
                                        .font(.system(size: 9, weight: .bold))
                                        .foregroundStyle(.black)
                                        .padding(2)
                                        .background(Color.orange)
                                        .clipShape(Circle())
                                        .offset(x: 6, y: -6)
                                }
                            }
                        }
                        #endif
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    HStack(spacing: 14) {
                        // ⭐ Pro badge / trial indicator
                        Button { showPaywall() } label: {
                            if entitlements.isPro && entitlements.trialDaysLeft == 0 {
                                EmptyView()
                            } else if entitlements.trialDaysLeft > 0 {
                                Text("Trial · \(entitlements.trialDaysLeft)d")
                                    .font(.system(size: 11, weight: .semibold))
                                    .foregroundStyle(Color.wkYellow)
                                    .padding(.horizontal, 8).padding(.vertical, 3)
                                    .background(Color.wkYellow.opacity(0.15))
                                    .clipShape(Capsule())
                            } else {
                                Text("⭐ Pro")
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundStyle(.white)
                                    .padding(.horizontal, 8).padding(.vertical, 3)
                                    .background(Color(red: 1, green: 0.42, blue: 0.42).opacity(0.8))
                                    .clipShape(Capsule())
                            }
                        }
                        Button { showSettings = true } label: {
                            Image(systemName: "gearshape.fill")
                                .foregroundStyle(Color.wkYellow)
                        }
                    }
                }
            }
        }
        .sheet(isPresented: $showSettings) {
            SettingsView()
                .environmentObject(settings)
                .environmentObject(calendarService)
                .environmentObject(entitlements)
        }
        .sheet(isPresented: $showAddManual) {
            AddManualEventSheet(store: manualStore)
        }
        .sheet(item: $selectedMeeting) { meeting in
            EventDetailSheet(
                meeting: meeting,
                onDelete: meeting.isManual ? {
                    if let id = meeting.manualId { manualStore.delete(id: id) }
                } : nil
            )
        }
        .onAppear {
            calendarService.loadCalendars()
            calendarService.loadTodayEvents(enabledIds: settings.enabledCalendarIds, settings: settings)
            calendarService.loadWeekEvents(enabledIds: settings.enabledCalendarIds, settings: settings)
            rebuildCache()
        }
        .onReceive(timer) { t in
            now = t
            let isOngoing = nextMeeting.map { $0.startDate <= t } ?? false

            // Transition: meeting just started → refresh widget + Live Activity
            if isOngoing != nextMeetingWasOngoing {
                nextMeetingWasOngoing = isOngoing
                WidgetDataWriter.write(from: cachedWeekMeetings)
                if #available(iOS 16.2, *) {
                    LiveActivityManager.shared.sync(nextMeeting: nextMeeting)
                }
            }

            // Auto-dismiss Live Activity 2 min after meeting starts
            if #available(iOS 16.2, *), isOngoing,
               let start = nextMeeting?.startDate,
               t.timeIntervalSince(start) > 2 * 60 {
                LiveActivityManager.shared.end()
            }
        }
        .onChange(of: calendarService.todayEvents.count) { _ in rebuildCache() }
        .onChange(of: calendarService.weekEvents.count)  { _ in rebuildCache() }
        .onChange(of: manualStore.events.count)          { _ in rebuildCache() }
    }

    // MARK: - Subviews

    private var scopePicker: some View {
        HStack(spacing: 0) {
            ForEach(ViewScope.allCases, id: \.self) { s in
                Button {
                    withAnimation(.easeInOut(duration: 0.18)) { scope = s }
                } label: {
                    Text(s.rawValue)
                        .font(.system(size: 13, weight: scope == s ? .semibold : .regular))
                        .foregroundStyle(scope == s ? Color.wkNavy : .white.opacity(0.55))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 7)
                        .background(
                            scope == s
                                ? Color.wkYellow
                                : Color.clear
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(3)
        .background(Color.white.opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    @ViewBuilder
    private var todaySection: some View {
        if allEvents.isEmpty {
            Text("Sin más reuniones hoy")
                .font(.system(size: 14))
                .foregroundStyle(.white.opacity(0.3))
                .frame(maxWidth: .infinity)
                .padding(.top, 8)
        } else {
            Text(todayLabel)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(.white.opacity(0.4))
                .kerning(1.2)
                .padding(.horizontal, 20)
                .padding(.bottom, 12)

            VStack(spacing: 8) {
                ForEach(allEvents) { meeting in
                    Button { selectedMeeting = meeting } label: {
                        MeetingRow(meeting: meeting, now: now)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 20)
        }
    }

    @ViewBuilder
    private var weekSectionView: some View {
        if cachedWeekSections.isEmpty {
            Text("Sin reuniones esta semana")
                .font(.system(size: 14))
                .foregroundStyle(.white.opacity(0.3))
                .frame(maxWidth: .infinity)
                .padding(.top, 8)
        } else {
            ForEach(cachedWeekSections) { section in
                VStack(alignment: .leading, spacing: 0) {
                    Text(section.header)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(.white.opacity(0.4))
                        .kerning(1.2)
                        .padding(.horizontal, 20)
                        .padding(.bottom, 12)

                    VStack(spacing: 8) {
                        ForEach(section.meetings) { meeting in
                            Button { selectedMeeting = meeting } label: {
                                MeetingRow(meeting: meeting, now: now)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 20)
                }
                .padding(.bottom, 24)
            }
        }
    }

    // MARK: - Helpers

    private func dayHeader(for date: Date) -> String {
        let cal     = Calendar.current
        let dateStr = HomeView.sectionFmt.string(from: date).uppercased()
        if cal.isDateInToday(date)    { return "HOY · \(dateStr)" }
        if cal.isDateInTomorrow(date) { return "MAÑANA · \(dateStr)" }
        return dateStr
    }

    private func rebuildCache() {
        let now = Date()

        // Today
        let fromCal = calendarService.todayEvents
        let fromMan = manualStore.todayEvents.map { AnyMeeting(manual: $0) }
        cachedMeetings = (fromCal + fromMan).sorted { $0.startDate < $1.startDate }

        // Week (7 días)
        let weekCal = calendarService.weekEvents
        let weekMan = manualStore.weekEvents.map { AnyMeeting(manual: $0) }
        let allWeek = (weekCal + weekMan).sorted { $0.startDate < $1.startDate }
        cachedWeekMeetings = allWeek

        // Week sections — computed once here, not on every render tick
        let cal = Calendar.current
        let filtered = allWeek.filter { m in
            cal.isDateInToday(m.startDate) ? m.endDate > now : true
        }
        var dict: [Date: [AnyMeeting]] = [:]
        for m in filtered {
            let day = cal.startOfDay(for: m.startDate)
            dict[day, default: []].append(m)
        }
        cachedWeekSections = dict.keys.sorted().map { day in
            DaySection(
                id: day,
                header: dayHeader(for: day),
                meetings: dict[day]!.sorted { $0.startDate < $1.startDate }
            )
        }

        // Escribe reuniones de la semana (no solo hoy) para que el widget
        // siga mostrando reuniones mañana aunque la app no se abra.
        WidgetDataWriter.write(from: cachedWeekMeetings)
        if #available(iOS 16.2, *) {
            LiveActivityManager.shared.sync(nextMeeting: nextMeeting)
        }
    }

    private var todayLabel: String { HomeView.sectionFmt.string(from: Date()).uppercased() }
    private var todayShort: String { HomeView.shortFmt.string(from: Date()).capitalized }

    // MARK: - Static formatters (never reallocated)

    private static let sectionFmt: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "es")
        f.dateFormat = "EEEE d MMMM"
        return f
    }()

    private static let shortFmt: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "es")
        f.dateFormat = "EEEE, d MMM"
        return f
    }()

    #if DEBUG
    private func scheduleSoundTest() {
        soundTestCounter += 1
        let n       = soundTestCounter
        let title   = "SOUND \(n)"
        let start   = Date().addingTimeInterval(2 * 60)
        let end     = Date().addingTimeInterval(3 * 60)
        manualStore.add(ManualEvent(title: title, startDate: start, endDate: end))

        let soundName = settings.alertSoundName
        let cafExists = Bundle.main.url(forResource: soundName, withExtension: "caf") != nil
        print("🔔 [SoundTest \(n)] sound=\(soundName).caf exists=\(cafExists)")

        let content = UNMutableNotificationContent()
        content.title              = title
        content.body               = "Test sonido – \(soundName).caf\(cafExists ? " ✓" : " ✗ NO ENCONTRADO")"
        content.categoryIdentifier = "MEETING_ALERT"
        content.interruptionLevel  = .timeSensitive
        content.sound = UNNotificationSound(
            named: UNNotificationSoundName(rawValue: "\(soundName).caf")
        )

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 60, repeats: false)
        let request = UNNotificationRequest(
            identifier: "debug_sound_\(n)",
            content: content,
            trigger: trigger
        )
        Task {
            do {
                try await UNUserNotificationCenter.current().add(request)
                soundTestScheduled = true
                try? await Task.sleep(nanoseconds: 5_000_000_000)
                soundTestScheduled = false
            } catch {
                print("🔔 [SoundTest] Error al programar: \(error)")
            }
        }
    }
    #endif
}

// MARK: - Empty state

private struct EmptyStateCard: View {
    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "calendar.badge.checkmark")
                .font(.system(size: 40))
                .foregroundStyle(.white.opacity(0.3))
            Text("Sin reuniones hoy")
                .font(.system(size: 17, weight: .medium))
                .foregroundStyle(.white.opacity(0.4))
        }
        .frame(maxWidth: .infinity)
        .padding(32)
        .background(Color.white.opacity(0.05))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}
