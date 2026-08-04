import AppKit
import SwiftUI

@MainActor
final class OnboardingWindowController {

    static let shared = OnboardingWindowController()

    private var window: NSWindow?

    func show() {
        if let w = window { NSApp.activate(ignoringOtherApps: true); w.makeKeyAndOrderFront(nil); return }

        let hosting = NSHostingController(rootView: OnboardingView(onFinish: { [weak self] in self?.dismiss() }))
        let w = NSWindow(contentViewController: hosting)
        w.styleMask = [.titled, .closable]
        w.title = "Welcome to WakeyWakey"
        w.isReleasedWhenClosed = false
        w.appearance = NSAppearance(named: .darkAqua)
        w.center()
        window = w

        NSApp.activate(ignoringOtherApps: true)
        w.makeKeyAndOrderFront(nil)
    }

    func dismiss() {
        window?.close()
        window = nil
    }
}
