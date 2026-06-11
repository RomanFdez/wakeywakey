import UIKit
import UserNotifications
import UserNotificationsUI

class NotificationViewController: UIViewController, UNNotificationContentExtension {

    private let logoView  = UIImageView()
    private let appLabel  = UILabel()
    private let titleLabel = UILabel()
    private let bodyLabel  = UILabel()
    private let joinButton = UIButton(type: .system)

    private let navy = UIColor(red: 0.102, green: 0.102, blue: 0.18, alpha: 1)

    private var meetingURL: URL?

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
    }

    func didReceive(_ notification: UNNotification) {
        let content = notification.request.content
        titleLabel.text = content.title
        bodyLabel.text  = content.body

        let urlString = content.userInfo["meeting_url"] as? String ?? ""
        if let url = URL(string: urlString), !urlString.isEmpty {
            meetingURL = url
            joinButton.isHidden = false
        } else {
            meetingURL = nil
            joinButton.isHidden = true
        }
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

    // MARK: - Actions

    @objc private func joinTapped() {
        guard let url = meetingURL else { return }
        extensionContext?.open(url, completionHandler: nil)
    }

    // MARK: - UI

    private func setupUI() {
        view.backgroundColor = UIColor(red: 1.0, green: 0.878, blue: 0.227, alpha: 1.0) // wkYellow

        // ── Logo + app name header ───────────────────────────────────────
        if let icon = UIImage(named: "AppIcon") {
            logoView.image = icon
        }
        logoView.contentMode        = .scaleAspectFill
        logoView.clipsToBounds      = true
        logoView.layer.cornerRadius = 8
        logoView.layer.cornerCurve  = .continuous
        logoView.translatesAutoresizingMaskIntoConstraints = false

        appLabel.text      = "WakeyWakey"
        appLabel.font      = UIFont.systemFont(ofSize: 13, weight: .semibold)
        appLabel.textColor = navy.withAlphaComponent(0.7)

        let headerStack = UIStackView(arrangedSubviews: [logoView, appLabel])
        headerStack.axis      = .horizontal
        headerStack.spacing   = 6
        headerStack.alignment = .center

        // ── Content ──────────────────────────────────────────────────────
        titleLabel.font          = UIFont.systemFont(ofSize: 20, weight: .heavy)
        titleLabel.textColor     = navy
        titleLabel.numberOfLines = 2

        bodyLabel.font          = UIFont.systemFont(ofSize: 15, weight: .regular)
        bodyLabel.textColor     = navy.withAlphaComponent(0.8)
        bodyLabel.numberOfLines = 2

        joinButton.setTitle("Unirse ahora", for: .normal)
        joinButton.backgroundColor = navy
        joinButton.setTitleColor(.white, for: .normal)
        joinButton.titleLabel?.font = UIFont.systemFont(ofSize: 16, weight: .bold)
        joinButton.layer.cornerRadius = 12
        joinButton.addTarget(self, action: #selector(joinTapped), for: .touchUpInside)

        let stack = UIStackView(arrangedSubviews: [headerStack, titleLabel, bodyLabel, joinButton])
        stack.axis      = .vertical
        stack.spacing   = 12
        stack.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(stack)

        NSLayoutConstraint.activate([
            logoView.widthAnchor.constraint(equalToConstant: 28),
            logoView.heightAnchor.constraint(equalToConstant: 28),

            stack.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            stack.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            stack.topAnchor.constraint(equalTo: view.topAnchor, constant: 16),
            stack.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -16),
            joinButton.heightAnchor.constraint(equalToConstant: 48),
        ])
    }
}
