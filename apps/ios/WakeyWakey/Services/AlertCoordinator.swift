import Foundation
import UserNotifications

// Coordina la presentación de AlertView cuando llega una notificación con la app en primer plano.
@MainActor
class AlertCoordinator: ObservableObject {

    static let shared = AlertCoordinator()

    @Published var activeAlert: AlertInfo?

    struct AlertInfo: Identifiable {
        let id              = UUID()
        let notificationId  : String
        let title           : String
        let startTime       : Date
        let meetingURL      : URL?
    }

    func show(from notification: UNNotification) {
        let info      = notification.request.content.userInfo
        let title     = notification.request.content.title
        let urlString = info["meeting_url"] as? String ?? ""
        let startTs   = info["start_time"]  as? TimeInterval ?? Date().timeIntervalSince1970

        activeAlert = AlertInfo(
            notificationId : notification.request.identifier,
            title          : title,
            startTime      : Date(timeIntervalSince1970: startTs),
            meetingURL     : URL(string: urlString)
        )
    }

    func dismiss() {
        activeAlert = nil
    }

    func snooze() {
        guard let alert = activeAlert else { return }
        Task {
            await AlertScheduler.shared.snooze(
                notificationId: alert.notificationId,
                title: alert.title,
                meetingURL: alert.meetingURL
            )
        }
        activeAlert = nil
    }
}
