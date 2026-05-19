import UIKit
import UserNotifications

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        let center = UNUserNotificationCenter.current()
        center.delegate = self
        registerNotificationCategories(center: center)

        // TODO Slice 5: inicializar Sentry y PostHog
        // SentrySDK.start { options in options.dsn = "..." }
        // PostHogSDK.shared.setup(PostHogConfig(apiKey: "...", host: "..."))

        return true
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
        // En foreground mostramos AlertView en lugar del banner del sistema
        Task { @MainActor in
            AlertCoordinator.shared.show(from: notification)
        }
        completionHandler([.sound]) // sonido sí, banner no (ya mostramos AlertView)
    }

    // MARK: - Response handling (app en background / notif tocada)

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
            // Abre AlertView para que el usuario vea el countdown y decida unirse
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
            title: "Join now",
            options: [.foreground]
        )
        let snoozeAction = UNNotificationAction(
            identifier: "SNOOZE_1MIN",
            title: "Snooze 1 min",
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
