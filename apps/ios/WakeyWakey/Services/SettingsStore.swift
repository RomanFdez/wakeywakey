import Foundation

class SettingsStore: ObservableObject {

    static let shared = SettingsStore()

    private let defaults = UserDefaults.standard

    @Published var onboardingCompleted: Bool {
        didSet { defaults.set(onboardingCompleted, forKey: Keys.onboardingCompleted) }
    }

    @Published var enabledCalendarIds: Set<String> {
        didSet { defaults.set(Array(enabledCalendarIds), forKey: Keys.calendarIds) }
    }

    @Published var alertMinutesBefore: Int {
        didSet { defaults.set(alertMinutesBefore, forKey: Keys.minutesBefore) }
    }

    init() {
        onboardingCompleted = defaults.bool(forKey: Keys.onboardingCompleted)
        enabledCalendarIds  = Set(defaults.stringArray(forKey: Keys.calendarIds) ?? [])
        let saved = defaults.integer(forKey: Keys.minutesBefore)
        alertMinutesBefore  = saved == 0 ? 1 : saved
    }

    func completeOnboarding() {
        onboardingCompleted = true
    }

    private enum Keys {
        static let onboardingCompleted = "onboarding_completed"
        static let calendarIds         = "calendar_ids"
        static let minutesBefore       = "minutes_before"
    }
}
