import SwiftUI

struct ContentView: View {

    @EnvironmentObject var settings:     SettingsStore
    @EnvironmentObject var entitlements: EntitlementManager

    @State private var showPaywall = false

    var body: some View {
        Group {
            if settings.onboardingCompleted {
                HomeView()
                    .sheet(isPresented: $showPaywall) {
                        PaywallView()
                            .environmentObject(entitlements)
                    }
            } else {
                OnboardingView()
            }
        }
        // Mostrar paywall al abrir si el trial ya expiró
        .onAppear {
            if !entitlements.isPro { showPaywall = true }
        }
        // Mostrar paywall cuando el trial expira mientras la app está abierta
        .onChange(of: entitlements.isPro) { isPro in
            if !isPro { showPaywall = true }
        }
        // Exponer el trigger al entorno para que HomeView y SettingsView puedan abrirlo
        .environment(\.showPaywall, { showPaywall = true })
    }
}

// MARK: - EnvironmentKey para abrir el paywall desde cualquier vista

private struct ShowPaywallKey: EnvironmentKey {
    static let defaultValue: () -> Void = {}
}

extension EnvironmentValues {
    var showPaywall: () -> Void {
        get { self[ShowPaywallKey.self] }
        set { self[ShowPaywallKey.self] = newValue }
    }
}
