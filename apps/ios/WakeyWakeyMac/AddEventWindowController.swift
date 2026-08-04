import AppKit
import SwiftUI

/// Muestra el formulario "Add event" en una ventana flotante.
@MainActor
final class AddEventWindowController {

    static let shared = AddEventWindowController()

    private var window: NSWindow?

    func show() {
        dismiss()

        let root = AddEventView(
            onSave: { [weak self] event in
                ManualEventsStore.shared.add(event)
                self?.dismiss()
            },
            onCancel: { [weak self] in self?.dismiss() }
        )

        let hosting = NSHostingController(rootView: root)
        let w = NSWindow(contentViewController: hosting)
        w.styleMask = [.titled, .closable]
        w.title = "Add event"
        w.isReleasedWhenClosed = false
        w.center()

        window = w
        NSApp.activate(ignoringOtherApps: true)
        w.makeKeyAndOrderFront(nil)
    }

    func dismiss() {
        window?.orderOut(nil)
        window = nil
    }
}
