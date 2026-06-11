import SwiftUI

@main
struct WakeyWakeyApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    @StateObject private var settings         = SettingsStore.shared
    @StateObject private var calendarService  = CalendarService.shared
    @StateObject private var alertCoordinator = AlertCoordinator.shared
    @StateObject private var entitlements     = EntitlementManager.shared

    @Environment(\.scenePhase) private var scenePhase

    init() {
        EntitlementManager.shared.configure()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(settings)
                .environmentObject(calendarService)
                .environmentObject(alertCoordinator)
                .environmentObject(entitlements)
                .fullScreenCover(item: $alertCoordinator.activeAlert) { info in
                    AlertView(info: info)
                        .environmentObject(alertCoordinator)
                }
        }
        .onChange(of: scenePhase) { phase in
            guard phase == .active else { return }

            // Cargar hoy (notificaciones) + semana (widget) cada vez que
            // la app vuelve al primer plano.
            calendarService.loadTodayEvents(enabledIds: settings.enabledCalendarIds, settings: settings)
            calendarService.loadWeekEvents(enabledIds: settings.enabledCalendarIds, settings: settings)

            // Escribir datos de la semana al widget directamente, sin esperar
            // a que HomeView detecte un cambio en el count de eventos.
            // Así el widget siempre tiene datos frescos al abrir la app.
            WidgetDataWriter.write(from: calendarService.weekEvents)

            // Reprogramar alarmas con los eventos de hoy
            Task { @MainActor in
                await AlertScheduler.shared.rescheduleAll(
                    events:        calendarService.todayEvents,
                    minutesBefore: settings.alertMinutesBefore,
                    soundName:     settings.alertSoundName
                )
            }

            entitlements.refresh()
        }
    }
}
