import AppKit
import SwiftUI

/// Muestra la ventana de Settings (una sola instancia).
@MainActor
final class SettingsWindowController {

    static let shared = SettingsWindowController()

    private var window: NSWindow?

    func show() {
        if let w = window {
            NSApp.activate(ignoringOtherApps: true)
            w.makeKeyAndOrderFront(nil)
            return
        }

        let hosting = NSHostingController(rootView: SettingsView())
        let w = NSWindow(contentViewController: hosting)
        w.styleMask = [.titled, .closable]
        w.title = "WakeyWakey — Settings"
        w.isReleasedWhenClosed = false
        w.appearance = NSAppearance(named: .darkAqua)   // controles nativos visibles sobre navy
        w.center()
        window = w

        NSApp.activate(ignoringOtherApps: true)
        w.makeKeyAndOrderFront(nil)
    }
}
