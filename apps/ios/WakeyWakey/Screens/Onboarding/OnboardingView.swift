import SwiftUI

struct OnboardingView: View {

    @EnvironmentObject var settings: SettingsStore
    @EnvironmentObject var calendarService: CalendarService
    @State private var step = 0

    var body: some View {
        ZStack {
            Color.wkNavy.ignoresSafeArea()

            switch step {
            case 0: WelcomeStep       { step = 1 }
            case 1: PermissionsStep   { step = 2 }
            case 2: CalendarPickerStep { step = 3 }
            case 3: TrialStep         { settings.completeOnboarding() }
            default: EmptyView()
            }
        }
        .animation(.easeInOut(duration: 0.3), value: step)
    }
}
