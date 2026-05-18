import UIKit
import UserNotifications
import UserNotificationsUI

// Slice 3 completará esta UI con SwiftUI + reloj + botón Join amarillo.
// Por ahora muestra título y cuerpo de la notificación.
class NotificationViewController: UIViewController, UNNotificationContentExtension {

    private let titleLabel = UILabel()
    private let bodyLabel  = UILabel()
    private let joinButton = UIButton(type: .system)

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
    }

    func didReceive(_ notification: UNNotification) {
        let content = notification.request.content
        titleLabel.text = content.title
        bodyLabel.text  = content.body

        let urlString = content.userInfo["meeting_url"] as? String ?? ""
        joinButton.isHidden = urlString.isEmpty
    }

    func didReceive(
        _ response: UNNotificationResponse,
        completionHandler completion: @escaping (UNNotificationContentExtensionResponseOption) -> Void
    ) {
        if response.actionIdentifier == "JOIN_NOW" {
            completion(.dismissAndForwardAction)
        } else {
            completion(.dismiss)
        }
    }

    // MARK: - UI

    private func setupUI() {
        view.backgroundColor = UIColor(red: 1.0, green: 0.878, blue: 0.227, alpha: 1.0) // #FFE03A

        titleLabel.font = UIFont.systemFont(ofSize: 20, weight: .heavy)
        titleLabel.textColor = UIColor(red: 0.102, green: 0.102, blue: 0.18, alpha: 1) // #1A1A2E
        titleLabel.numberOfLines = 2

        bodyLabel.font = UIFont.systemFont(ofSize: 15, weight: .regular)
        bodyLabel.textColor = UIColor(red: 0.102, green: 0.102, blue: 0.18, alpha: 0.8)
        bodyLabel.numberOfLines = 2

        joinButton.setTitle("Join now", for: .normal)
        joinButton.backgroundColor = UIColor(red: 0.102, green: 0.102, blue: 0.18, alpha: 1)
        joinButton.setTitleColor(.white, for: .normal)
        joinButton.titleLabel?.font = UIFont.systemFont(ofSize: 16, weight: .bold)
        joinButton.layer.cornerRadius = 12

        let stack = UIStackView(arrangedSubviews: [titleLabel, bodyLabel, joinButton])
        stack.axis = .vertical
        stack.spacing = 12
        stack.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(stack)

        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            stack.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            stack.topAnchor.constraint(equalTo: view.topAnchor, constant: 16),
            stack.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -16),
            joinButton.heightAnchor.constraint(equalToConstant: 48),
        ])
    }
}
