import SwiftUI

@main
struct WakeyWakeyApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    @StateObject private var settings        = SettingsStore.shared
    @StateObject private var calendarService = CalendarService.shared
    @StateObject private var alertCoordinator = AlertCoordinator.shared

    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(settings)
                .environmentObject(calendarService)
                .environmentObject(alertCoordinator)
                .fullScreenCover(item: $alertCoordinator.activeAlert) { info in
                    AlertView(info: info)
                        .environmentObject(alertCoordinator)
                }
        }
        .onChange(of: scenePhase) { phase in
            if phase == .active {
                // Reprograma alarmas cada vez que la app vuelve al primer plano
                calendarService.loadTodayEvents(enabledIds: settings.enabledCalendarIds)
                Task { @MainActor in
                    await AlertScheduler.shared.rescheduleAll(
                        events: calendarService.todayEvents,
                        minutesBefore: settings.alertMinutesBefore
                    )
                }
            }
        }
    }
}
