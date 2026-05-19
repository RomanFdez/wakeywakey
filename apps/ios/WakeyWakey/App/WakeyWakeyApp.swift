import SwiftUI

@main
struct WakeyWakeyApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    @StateObject private var settings        = SettingsStore.shared
    @StateObject private var calendarService = CalendarService.shared

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(settings)
                .environmentObject(calendarService)
        }
    }
}
