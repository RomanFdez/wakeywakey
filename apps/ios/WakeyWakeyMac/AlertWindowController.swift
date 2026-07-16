import AppKit
import SwiftUI
import AVFoundation

/// Muestra la alerta "in your face" como ventana(s) sin bordes que cubren la pantalla,
/// por encima de todo (nivel escudo) y robando el foco. Según ajuste: solo la pantalla
/// activa, o todas las conectadas.
@MainActor
final class AlertWindowController {

    static let shared = AlertWindowController()

    private var windows: [NSWindow] = []
    private var audioPlayer: AVAudioPlayer?
    private var soundStopWork: DispatchWorkItem?

    /// Reuniones simultáneas: si ya hay una alerta en pantalla, las siguientes
    /// esperan en cola y aparecen al descartar (Dismiss/Join/Snooze) la actual.
    private struct PendingAlert {
        let title: String; let startDate: Date
        let location: String?; let meetingURL: URL?; let isPreview: Bool
    }
    private var queue: [PendingAlert] = []

    func show(title: String, startDate: Date, location: String?, meetingURL: URL?, isPreview: Bool = false) {
        let alert = PendingAlert(title: title, startDate: startDate, location: location,
                                 meetingURL: meetingURL, isPreview: isPreview)
        if windows.isEmpty {
            display(alert)
        } else {
            queue.append(alert)
        }
    }

    func dismiss() {
        teardown()
        if !queue.isEmpty { display(queue.removeFirst()) }
    }

    private func display(_ a: PendingAlert) {
        let screens: [NSScreen] = MacSettings.shared.alertActiveScreenOnly
            ? [activeScreen()]
            : NSScreen.screens

        for screen in screens {
            windows.append(makeWindow(on: screen, title: a.title, startDate: a.startDate,
                                      location: a.location, meetingURL: a.meetingURL, isPreview: a.isPreview))
        }

        NSApp.activate(ignoringOtherApps: true)
        windows.forEach { $0.makeKeyAndOrderFront(nil) }
        playSound(loop: !a.isPreview && SettingsStore.shared.repeatSoundUntilDismiss)
    }

    private func teardown() {
        soundStopWork?.cancel()
        soundStopWork = nil
        audioPlayer?.stop()
        audioPlayer = nil
        windows.forEach { $0.orderOut(nil) }
        windows.removeAll()
    }

    // MARK: - Ventanas

    private func activeScreen() -> NSScreen {
        let mouse = NSEvent.mouseLocation
        return NSScreen.screens.first { NSMouseInRect(mouse, $0.frame, false) }
            ?? NSScreen.main ?? NSScreen.screens[0]
    }

    private func makeWindow(on screen: NSScreen, title: String, startDate: Date,
                            location: String?, meetingURL: URL?, isPreview: Bool) -> NSWindow {
        let w = NSWindow(contentRect: screen.frame, styleMask: .borderless, backing: .buffered, defer: false)
        w.level = NSWindow.Level(rawValue: Int(CGShieldingWindowLevel()))
        w.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary, .stationary]
        w.isOpaque = true
        w.backgroundColor = .black
        w.hasShadow = false
        w.isReleasedWhenClosed = false

        let root = FullScreenAlertView(
            title: title, startDate: startDate, location: location,
            meetingURL: meetingURL, isPreview: isPreview,
            onJoin: { [weak self] in
                if let url = meetingURL { MeetingLauncher.open(url) }
                self?.dismiss()
            },
            onDismiss: { [weak self] in self?.dismiss() },
            onSnooze: { [weak self] minutes in
                self?.snooze(minutes: minutes, title: title, startDate: startDate,
                             location: location, meetingURL: meetingURL)
            },
            onCustomSnooze: { [weak self] in
                guard let self, let minutes = self.askCustomMinutes() else { return }
                self.snooze(minutes: minutes, title: title, startDate: startDate,
                            location: location, meetingURL: meetingURL)
            }
        )
        w.contentView = NSHostingView(rootView: root)
        w.setFrame(screen.frame, display: true)
        return w
    }

    // MARK: - Snooze

    private func snooze(minutes: Int, title: String, startDate: Date,
                        location: String?, meetingURL: URL?) {
        dismiss()
        DispatchQueue.main.asyncAfter(deadline: .now() + Double(minutes) * 60) { [weak self] in
            self?.show(title: title, startDate: startDate, location: location, meetingURL: meetingURL)
        }
    }

    private func askCustomMinutes() -> Int? {
        let alert = NSAlert()
        alert.messageText = "Snooze for how many minutes?"
        alert.addButton(withTitle: "Snooze")
        alert.addButton(withTitle: "Cancel")
        let field = NSTextField(frame: NSRect(x: 0, y: 0, width: 200, height: 24))
        field.stringValue = "10"
        alert.accessoryView = field
        return alert.runModal() == .alertFirstButtonReturn ? Int(field.stringValue) : nil
    }

    // MARK: - Sonido

    private func playSound(loop: Bool) {
        guard SettingsStore.shared.soundEnabled else { return }
        let id = SettingsStore.shared.alertSoundName
        guard id != "default",
              let url = Bundle.main.url(forResource: id, withExtension: "mp3")
        else { NSSound.beep(); return }
        audioPlayer = try? AVAudioPlayer(contentsOf: url)
        audioPlayer?.volume = Float(MacSettings.shared.volume)
        audioPlayer?.numberOfLoops = loop ? -1 : 0
        audioPlayer?.play()
        // Loop = repetir durante 30 s (no indefinidamente).
        if loop {
            let work = DispatchWorkItem { [weak self] in self?.audioPlayer?.stop() }
            soundStopWork = work
            DispatchQueue.main.asyncAfter(deadline: .now() + 30, execute: work)
        }
    }
}
