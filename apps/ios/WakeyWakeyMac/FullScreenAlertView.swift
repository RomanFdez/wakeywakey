import SwiftUI

private extension Color {
    static let wkNavy   = Color(red: 0.098, green: 0.098, blue: 0.176)
    static let wkNavyPill = Color(red: 0.16, green: 0.16, blue: 0.24)
    static let wkYellow = Color(red: 1.0,   green: 0.878, blue: 0.227)
}

/// Contenido de la alerta a pantalla completa ("in your face"), con el formato de
/// la app JVM. El contador sigue las reglas del producto (ver `countdown`).
struct FullScreenAlertView: View {
    let title: String
    let startDate: Date
    let location: String?
    let meetingURL: URL?
    var isPreview: Bool = false
    var onJoin: () -> Void
    var onDismiss: () -> Void
    var onSnooze: (Int) -> Void
    var onCustomSnooze: () -> Void

    @State private var now = Date()
    private let ticker = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        ZStack {
            Color.wkNavy.ignoresSafeArea()

            // Cartel de vista previa (clic en una reunión del panel)
            if isPreview {
                VStack {
                    Text("PREVIEW — not a real alert")
                        .font(.system(size: 13, weight: .bold))
                        .kerning(1.2)
                        .foregroundStyle(.red)
                        .padding(.horizontal, 20).padding(.vertical, 10)
                        .background(Color.red.opacity(0.14))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(.red.opacity(0.5), lineWidth: 1))
                        .padding(.top, 44)
                    Spacer()
                }
            }

            VStack(spacing: 22) {
                Text("⏰").font(.system(size: 60))

                Text(title)
                    .font(.system(size: 52, weight: .heavy))
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.center)
                    .lineLimit(3)
                    .minimumScaleFactor(0.5)
                    .padding(.horizontal, 40)

                Text(timeString)
                    .font(.system(size: 20))
                    .foregroundStyle(.white.opacity(0.5))

                if let location, !location.isEmpty {
                    Text("📍 \(location)")
                        .font(.system(size: 15))
                        .foregroundStyle(.white.opacity(0.5))
                        .lineLimit(1)
                }

                Text(countdown.text)
                    .font(.system(size: 28, weight: .heavy))
                    .foregroundStyle(countdown.started ? .red : Color.wkYellow)
                    .monospacedDigit()
                    .padding(.top, 8)

                if meetingURL != nil {
                    JoinButton(action: onJoin).padding(.top, 6)
                }

                HStack(spacing: 10) {
                    Text("Snooze:")
                        .font(.system(size: 13))
                        .foregroundStyle(.white.opacity(0.5))
                    SnoozePill(label: "1 min")   { onSnooze(1) }
                    SnoozePill(label: "5 min")   { onSnooze(5) }
                    if MacEntitlementManager.shared.isPro {
                        SnoozePill(label: "Custom…") { onCustomSnooze() }
                    }
                }

                DismissButton(action: onDismiss)
            }
            .padding(60)
        }
        .onReceive(ticker) { now = $0 }
        .background(KeyCatcher(onEscape: onDismiss))
    }

    private var timeString: String {
        let f = DateFormatter(); f.dateFormat = "HH:mm"
        return f.string(from: startDate)
    }

    // Reglas del contador (ver mensaje del producto).
    private var countdown: (text: String, started: Bool) {
        let t = Int(startDate.timeIntervalSince(now).rounded(.down))
        if t > 0 {
            if t < 60          { return ("Starts in \(t)s", false) }
            if t < 3600        { return ("Starts in \(t / 60) min", false) }
            if t < 86_400      {
                let h = t / 3600, m = (t % 3600) / 60
                return ("Starts in \(h)h \(m)m", false)
            }
            let d = t / 86_400, h = (t % 86_400) / 3600, m = (t % 3600) / 60
            return ("Starts in \(d)d \(h)h \(m)m", false)
        } else {
            let e = -t
            if e < 60   { return ("+\(e)s", true) }
            if e < 3600 { return ("+\(e / 60)m \(e % 60)s", true) }
            let h = e / 3600, m = (e % 3600) / 60
            return ("+\(h)h \(m)m", true)
        }
    }
}

// MARK: - Botones con feedback de hover

private struct JoinButton: View {
    let action: () -> Void
    @State private var hovering = false
    var body: some View {
        Button(action: action) {
            Label("Join now", systemImage: "video.fill")
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(Color.wkNavy)
                .frame(width: 440)
                .padding(.vertical, 16)
                .background(Color.wkYellow)
                .overlay(hovering ? Color.black.opacity(0.12) : Color.clear)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
        .onHover { hovering = $0 }
        .animation(.easeOut(duration: 0.12), value: hovering)
    }
}

private struct DismissButton: View {
    let action: () -> Void
    @State private var hovering = false
    var body: some View {
        Button(action: action) {
            Label("Dismiss", systemImage: "xmark")
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(.white)
                .frame(width: 440)
                .padding(.vertical, 15)
                .background(Capsule().fill(Color.white.opacity(hovering ? 0.1 : 0)))
                .overlay(Capsule().stroke(.white.opacity(hovering ? 0.6 : 0.3), lineWidth: 1.5))
        }
        .buttonStyle(.plain)
        .onHover { hovering = $0 }
        .animation(.easeOut(duration: 0.12), value: hovering)
    }
}

private struct SnoozePill: View {
    let label: String
    let action: () -> Void
    @State private var hovering = false
    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(.white.opacity(0.85))
                .padding(.horizontal, 14).padding(.vertical, 8)
                .background(Color.wkNavyPill.brightness(hovering ? 0.06 : 0))
                .clipShape(RoundedRectangle(cornerRadius: 8))
        }
        .buttonStyle(.plain)
        .onHover { hovering = $0 }
        .animation(.easeOut(duration: 0.12), value: hovering)
    }
}

/// Captura la tecla Esc para cerrar la alerta.
private struct KeyCatcher: NSViewRepresentable {
    let onEscape: () -> Void
    func makeNSView(context: Context) -> NSView {
        let v = KeyView(); v.onEscape = onEscape; return v
    }
    func updateNSView(_ nsView: NSView, context: Context) {}

    final class KeyView: NSView {
        var onEscape: (() -> Void)?
        override var acceptsFirstResponder: Bool { true }
        override func viewDidMoveToWindow() { window?.makeFirstResponder(self) }
        override func keyDown(with event: NSEvent) {
            if event.keyCode == 53 { onEscape?() } else { super.keyDown(with: event) }
        }
    }
}
