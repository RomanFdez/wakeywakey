import SwiftUI

private extension Color {
    static let wkNavy   = Color(red: 0.098, green: 0.098, blue: 0.176)
    static let wkYellow = Color(red: 1.0,   green: 0.878, blue: 0.227)
}

/// Onboarding de primera vez (estilo JVM, adaptado a calendario del sistema/EventKit).
struct OnboardingView: View {
    var onFinish: () -> Void

    @StateObject private var calendar = CalendarService.shared
    @State private var state: Step = .welcome
    @State private var working = false

    private enum Step { case welcome, done, denied }

    var body: some View {
        VStack(spacing: 18) {
            switch state {
            case .welcome:  welcome
            case .done:     done
            case .denied:   denied
            }
        }
        .frame(width: 400)
        .padding(34)
        .background(Color.wkNavy)
    }

    private var welcome: some View {
        VStack(spacing: 16) {
            Text("📅").font(.system(size: 52))
            Text("Connect your calendar")
                .font(.system(size: 22, weight: .heavy)).foregroundStyle(.white)
            Text("WakeyWakey reads your upcoming meetings so it can alert you before they start.")
                .font(.system(size: 13)).foregroundStyle(.white.opacity(0.7))
                .multilineTextAlignment(.center).fixedSize(horizontal: false, vertical: true)
            primaryButton(working ? "Requesting…" : "Grant calendar access") {
                working = true
                Task {
                    let ok = await calendar.requestCalendarAccess()
                    working = false
                    state = ok ? .done : .denied
                }
            }
            Text("Only calendar data is read — no changes are made.")
                .font(.system(size: 11)).foregroundStyle(.white.opacity(0.45))
                .multilineTextAlignment(.center)
        }
    }

    private var done: some View {
        VStack(spacing: 16) {
            Text("✅").font(.system(size: 52))
            Text("Calendar connected!")
                .font(.system(size: 20, weight: .heavy)).foregroundStyle(.white)
            Text("WakeyWakey now lives in your menu bar and will alert you before meetings.")
                .font(.system(size: 13)).foregroundStyle(.white.opacity(0.7))
                .multilineTextAlignment(.center).fixedSize(horizontal: false, vertical: true)
            primaryButton("Let's go!") { finish() }
        }
    }

    private var denied: some View {
        VStack(spacing: 14) {
            Text("⚠️").font(.system(size: 48))
            Text("Calendar access denied")
                .font(.system(size: 18, weight: .heavy)).foregroundStyle(.white)
            Text("Enable it in System Settings › Privacy & Security › Calendars, then reopen WakeyWakey.")
                .font(.system(size: 12)).foregroundStyle(.white.opacity(0.7))
                .multilineTextAlignment(.center).fixedSize(horizontal: false, vertical: true)
            primaryButton("Open System Settings") {
                if let u = URL(string: "x-apple.systempreferences:com.apple.preference.security?Privacy_Calendars") {
                    NSWorkspace.shared.open(u)
                }
            }
            Button("Skip for now") { finish() }
                .buttonStyle(.plain).foregroundStyle(.white.opacity(0.5)).font(.system(size: 12))
        }
    }

    private func primaryButton(_ title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 15, weight: .bold)).foregroundStyle(Color.wkNavy)
                .frame(maxWidth: .infinity).padding(.vertical, 12)
                .background(Color.wkYellow).clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    private func finish() {
        SettingsStore.shared.completeOnboarding()
        onFinish()
    }
}
