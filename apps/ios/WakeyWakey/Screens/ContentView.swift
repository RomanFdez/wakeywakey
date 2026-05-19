import SwiftUI

struct ContentView: View {

    @EnvironmentObject var settings: SettingsStore

    var body: some View {
        if settings.onboardingCompleted {
            HomeView()
        } else {
            OnboardingView()
        }
    }
}
