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

        // TODO Slice 5: inicializar Sentry y PostHog aquí con sus API keys
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
        // Muestra el banner incluso con la app en primer plano
        completionHandler([.banner, .sound])
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
            // TODO Slice 3: reprogramar notificación en 60 segundos
            break
        case UNNotificationDefaultActionIdentifier:
            // El usuario tocó la notificación (no una acción)
            openMeetingUrl(from: userInfo)
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
