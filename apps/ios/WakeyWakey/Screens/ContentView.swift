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

// Placeholder hasta Slice 2 (HomeScreen)
struct HomeView: View {
    var body: some View {
        ZStack {
            Color.wkNavy.ignoresSafeArea()
            VStack(spacing: 16) {
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 64))
                    .foregroundStyle(Color.wkYellow)
                Text("¡Todo listo!")
                    .font(.system(size: 28, weight: .heavy, design: .rounded))
                    .foregroundStyle(Color.wkYellow)
                Text("Home · Slice 2 próximamente")
                    .font(.system(size: 14))
                    .foregroundStyle(.white.opacity(0.5))
            }
        }
    }
}
