import SwiftUI

struct HomeView: View {

    @EnvironmentObject var settings: SettingsStore
    @EnvironmentObject var calendarService: CalendarService
    @StateObject private var manualStore = ManualEventsStore.shared

    @State private var now = Date()
    @State private var showSettings   = false
    @State private var showAddManual  = false
    @State private var selectedMeeting: AnyMeeting?
    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    // Mezcla eventos de calendario y manuales, ordenados por hora
    private var allEvents: [AnyMeeting] {
        let fromCal = calendarService.todayEvents.map { AnyMeeting(calEvent: $0) }
        let fromMan = manualStore.todayEvents.map    { AnyMeeting(manual: $0)   }
        return (fromCal + fromMan)
            .filter { $0.endDate > now }
            .sorted { $0.startDate < $1.startDate }
    }

    private var nextMeeting: AnyMeeting? {
        // Prioridad: evento que empieza en ≤5 min > evento en curso > próximo evento
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
                            NextMeetingCard(meeting: next, now: now)
                                .padding(.horizontal, 20)
                                .padding(.top, 8)
                                .padding(.bottom, 24)
                        } else {
                            EmptyStateCard()
                                .padding(.horizontal, 20)
                                .padding(.top, 8)
                                .padding(.bottom, 24)
                        }

                        // ── Lista del día ────────────────────────────────
                        if !allEvents.isEmpty {
                            sectionHeader
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

                        Spacer(minLength: 40)
                    }
                }
                .refreshable {
                    calendarService.loadTodayEvents(enabledIds: settings.enabledCalendarIds, settings: settings)
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
                    Button { showAddManual = true } label: {
                        Image(systemName: "plus")
                            .foregroundStyle(Color.wkYellow)
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showSettings = true } label: {
                        Image(systemName: "gearshape.fill")
                            .foregroundStyle(Color.wkYellow)
                    }
                }
            }
        }
        .sheet(isPresented: $showSettings) {
            SettingsView()
                .environmentObject(settings)
                .environmentObject(calendarService)
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
        }
        .onReceive(timer) { now = $0 }
    }

    private var sectionHeader: some View {
        Text(todayLabel)
            .font(.system(size: 12, weight: .semibold))
            .foregroundStyle(.white.opacity(0.4))
            .kerning(1.2)
    }

    private var todayLabel: String {
        let fmt = DateFormatter()
        fmt.dateFormat = "EEEE d MMMM"
        fmt.locale = Locale(identifier: "es")
        return fmt.string(from: Date()).uppercased()
    }

    private var todayShort: String {
        let fmt = DateFormatter()
        fmt.dateFormat = "EEEE, d MMM"
        fmt.locale = Locale(identifier: "es")
        return fmt.string(from: Date()).capitalized
    }
}

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
