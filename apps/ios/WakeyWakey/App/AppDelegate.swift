import UIKit
import UserNotifications
import ActivityKit
import BackgroundTasks

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    private static let bgTaskID = "com.sierraespada.wakeywakey.refresh"

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        let center = UNUserNotificationCenter.current()
        center.delegate = self
        registerNotificationCategories(center: center)
        registerBackgroundTask()

        return true
    }

    // MARK: - Background refresh (BGTaskScheduler)

    private func registerBackgroundTask() {
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: Self.bgTaskID,
            using: nil
        ) { task in
            self.handleBackgroundRefresh(task: task as! BGAppRefreshTask)
        }
        scheduleBackgroundRefresh()
    }

    func scheduleBackgroundRefresh() {
        let request = BGAppRefreshTaskRequest(identifier: Self.bgTaskID)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60) // no antes de 15 min
        try? BGTaskScheduler.shared.submit(request)
    }

    private func handleBackgroundRefresh(task: BGAppRefreshTask) {
        // Re-schedule antes de hacer trabajo para no perder la siguiente ventana
        scheduleBackgroundRefresh()

        let workTask = Task { @MainActor in
            let settings   = SettingsStore.shared
            let calService = CalendarService.shared

            // Cargar hoy (para notificaciones) Y la semana (para el widget)
            calService.loadTodayEvents(enabledIds: settings.enabledCalendarIds, settings: settings)
            calService.loadWeekEvents(enabledIds: settings.enabledCalendarIds, settings: settings)

            // Escribir reuniones de la semana al widget para que al día siguiente
            // ya tenga datos sin necesitar que el usuario abra la app
            WidgetDataWriter.write(from: calService.weekEvents)

            await AlertScheduler.shared.rescheduleAll(
                events:        calService.todayEvents,
                minutesBefore: settings.alertMinutesBefore,
                soundName:     settings.alertSoundName
            )

            task.setTaskCompleted(success: true)
        }

        task.expirationHandler = {
            workTask.cancel()
            task.setTaskCompleted(success: false)
        }
    }

    // MARK: - Foreground delivery

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        guard notification.request.content.categoryIdentifier == "MEETING_ALERT" else {
            completionHandler([.banner, .sound])
            return
        }
        Task { @MainActor in
            AlertCoordinator.shared.show(from: notification)
            if #available(iOS 16.2, *) {
                LiveActivityManager.shared.markOngoing()
            }
        }
        completionHandler([.sound])
    }

    // MARK: - Response handling

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo

        switch response.actionIdentifier {

        case "JOIN_NOW":
            openMeetingUrl(from: userInfo)

        case "SNOOZE_1MIN":
            let title  = response.notification.request.content.title
            let urlStr = userInfo["meeting_url"] as? String ?? ""
            let url    = URL(string: urlStr)
            Task { @MainActor in
                await AlertScheduler.shared.snooze(
                    notificationId: response.notification.request.identifier,
                    title: title,
                    meetingURL: url
                )
            }

        case UNNotificationDefaultActionIdentifier:
            Task { @MainActor in
                AlertCoordinator.shared.show(from: response.notification)
            }

        default:
            break
        }

        completionHandler()
    }

    // MARK: - Helpers

    private func registerNotificationCategories(center: UNUserNotificationCenter) {
        let joinAction = UNNotificationAction(
            identifier: "JOIN_NOW",
            title: "Unirse ahora",
            options: [.foreground]
        )
        let snoozeAction = UNNotificationAction(
            identifier: "SNOOZE_1MIN",
            title: "Posponer 1 min",
            options: []
        )
        let category = UNNotificationCategory(
            identifier: "MEETING_ALERT",
            actions: [joinAction, snoozeAction],
            intentIdentifiers: [],
            options: [.customDismissAction]
        )
        center.setNotificationCategories([category])
    }

    private func openMeetingUrl(from userInfo: [AnyHashable: Any]) {
        guard
            let urlString = userInfo["meeting_url"] as? String,
            !urlString.isEmpty,
            let url = URL(string: urlString)
        else { return }
        UIApplication.shared.open(url)
    }
}
