import SwiftUI

struct HomeView: View {

    @EnvironmentObject var settings: SettingsStore
    @EnvironmentObject var calendarService: CalendarService

    @State private var now = Date()
    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        NavigationStack {
            ZStack {
                Color.wkNavy.ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {

                        // ── Próxima reunión ──────────────────────────────
                        if let next = calendarService.nextEvent {
                            NextMeetingCard(event: next, now: now)
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
                        if !calendarService.todayEvents.isEmpty {
                            sectionHeader
                                .padding(.horizontal, 20)
                                .padding(.bottom, 12)

                            VStack(spacing: 8) {
                                ForEach(calendarService.todayEvents, id: \.eventIdentifier) { event in
                                    MeetingRow(event: event, now: now)
                                }
                            }
                            .padding(.horizontal, 20)
                        }

                        Spacer(minLength: 40)
                    }
                }
                .refreshable {
                    calendarService.loadTodayEvents(enabledIds: settings.enabledCalendarIds)
                }
            }
            .navigationTitle("WakeyWakey")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { } label: {
                        Image(systemName: "gearshape.fill")
                            .foregroundStyle(Color.wkYellow)
                    }
                }
                #if DEBUG
                ToolbarItem(placement: .topBarLeading) {
                    Button("🔔 Test") {
                        AlertCoordinator.shared.activeAlert = AlertCoordinator.AlertInfo(
                            notificationId: "test",
                            title: "Standup diario",
                            startTime: Date().addingTimeInterval(90),
                            meetingURL: URL(string: "https://meet.google.com/abc-defg-hij")
                        )
                    }
                    .foregroundStyle(Color.wkYellow)
                }
                #endif
            }
        }
        .onAppear {
            calendarService.loadCalendars()
            calendarService.loadTodayEvents(enabledIds: settings.enabledCalendarIds)
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
