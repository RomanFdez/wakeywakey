import SwiftUI
import AppKit
import Combine
import CoreImage

/// App de barra de menú nativa de macOS (como la JVM: icono arriba, sin Dock).
/// Usa un `NSStatusItem` de AppKit (en vez de `MenuBarExtra`) para poder mostrar
/// el logo circular a color + el texto de la próxima reunión — algo que
/// `MenuBarExtra` no permite de forma fiable.
@main
struct WakeyWakeyMacApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) private var delegate

    var body: some Scene {
        // App tipo agente (LSUIElement): sin ventana principal. La UI vive en el
        // NSStatusItem + NSPopover que gestiona el AppDelegate.
        Settings { EmptyView() }
    }
}

@MainActor
final class AppDelegate: NSObject, NSApplicationDelegate {

    private var statusItem: NSStatusItem!
    private let popover = NSPopover()
    private var cancellables = Set<AnyCancellable>()

    func applicationDidFinishLaunching(_ notification: Notification) {
        CrashReporting.start()
        MacEntitlementManager.shared.configure()
        MenuBarController.shared.start()
        DesktopMacScheduler.shared.start()
        PaywallReminderScheduler.shared.start()

        // ── Status item ──────────────────────────────────────────────
        statusItem = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)
        if let button = statusItem.button {
            button.imagePosition = .imageLeading
            button.action = #selector(togglePopover)
            button.target = self
        }

        // ── Popover con el panel SwiftUI ─────────────────────────────
        popover.behavior = .transient
        popover.contentSize = NSSize(width: 400, height: 520)
        popover.contentViewController = NSHostingController(rootView: MenuPanelView())

        // Título coloreado según ajustes; icono mono/color según ajustes.
        MenuBarController.shared.$menuBarTitle
            .receive(on: RunLoop.main)
            .sink { [weak self] title in self?.updateTitle(title) }
            .store(in: &cancellables)

        MacSettings.shared.objectWillChange
            .receive(on: RunLoop.main)
            .sink { [weak self] _ in DispatchQueue.main.async { self?.applyAppearance() } }
            .store(in: &cancellables)

        applyAppearance()

        if !SettingsStore.shared.onboardingCompleted {
            OnboardingWindowController.shared.show()
        }

        // Paywall automático al expirar el trial (como la JVM).
        MacEntitlementManager.shared.$isPro
            .receive(on: RunLoop.main)
            .sink { pro in
                if !pro && MacEntitlementManager.shared.trialDaysLeft <= 0 {
                    PaywallWindowController.shared.show()
                }
            }
            .store(in: &cancellables)
    }

    private func updateTitle(_ title: String) {
        guard let button = statusItem.button else { return }
        guard !title.isEmpty else { button.attributedTitle = NSAttributedString(string: ""); return }
        let color = MenuBarColor.ns(MacSettings.shared.menuTextColor)
        button.attributedTitle = NSAttributedString(
            string: "  \(title)",
            attributes: [.foregroundColor: color, .font: NSFont.systemFont(ofSize: 13)]
        )
    }

    private func applyAppearance() {
        let icon = MacSettings.shared.monochromeIcon
            ? Self.monochromeIcon(diameter: 18)
            : Self.circularIcon(named: "MenuBarIcon", diameter: 18)
        statusItem.button?.image = icon
        updateTitle(MenuBarController.shared.menuBarTitle)
    }

    /// Icono monocromo: el logo real en escala de grises (conserva la forma del bee).
    private static func monochromeIcon(diameter: CGFloat) -> NSImage? {
        guard let colored = circularIcon(named: "MenuBarIcon", diameter: diameter),
              let tiff = colored.tiffRepresentation,
              let bitmap = NSBitmapImageRep(data: tiff)
        else { return circularIcon(named: "MenuBarIcon", diameter: diameter) }

        let ci = CIImage(bitmapImageRep: bitmap)
        let filter = CIFilter(name: "CIPhotoEffectMono")
        filter?.setValue(ci, forKey: kCIInputImageKey)
        guard let output = filter?.outputImage else { return colored }

        let rep = NSCIImageRep(ciImage: output)
        let img = NSImage(size: NSSize(width: diameter, height: diameter))
        img.addRepresentation(rep)
        img.isTemplate = false
        return img
    }

    @objc private func togglePopover() {
        guard let button = statusItem.button else { return }
        if popover.isShown {
            popover.performClose(nil)
        } else {
            popover.show(relativeTo: button.bounds, of: button, preferredEdge: .minY)
            popover.contentViewController?.view.window?.makeKey()
        }
    }

    /// Recorta el logo (cuadrado redondeado) a un círculo, manteniendo el color.
    private static func circularIcon(named: String, diameter: CGFloat) -> NSImage? {
        guard let src = NSImage(named: named) else { return nil }
        let size = NSSize(width: diameter, height: diameter)
        let out = NSImage(size: size)
        out.lockFocus()
        let rect = NSRect(origin: .zero, size: size)
        NSBezierPath(ovalIn: rect).addClip()
        src.draw(in: rect, from: .zero, operation: .sourceOver, fraction: 1)
        out.unlockFocus()
        out.isTemplate = false   // conservar el amarillo (no monocromo)
        return out
    }
}
