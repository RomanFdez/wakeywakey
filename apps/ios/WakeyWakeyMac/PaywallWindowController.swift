import AppKit
import SwiftUI

@MainActor
final class PaywallWindowController {

    static let shared = PaywallWindowController()

    private var window: NSWindow?

    func show() {
        if let w = window { NSApp.activate(ignoringOtherApps: true); w.makeKeyAndOrderFront(nil); return }
        let hosting = NSHostingController(rootView: PaywallView(onClose: { [weak self] in self?.dismiss() }))
        let w = NSWindow(contentViewController: hosting)
        w.styleMask = [.titled, .closable]
        w.title = "WakeyWakey Pro"
        w.isReleasedWhenClosed = false
        w.appearance = NSAppearance(named: .darkAqua)
        w.center()
        window = w
        NSApp.activate(ignoringOtherApps: true)
        w.makeKeyAndOrderFront(nil)
    }

    func dismiss() { window?.close(); window = nil }
}
