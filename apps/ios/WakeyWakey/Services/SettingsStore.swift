import Foundation

class SettingsStore: ObservableObject {

    static let shared = SettingsStore()

    private let defaults = UserDefaults.standard

    // MARK: - Onboarding
    @Published var onboardingCompleted: Bool {
        didSet { defaults.set(onboardingCompleted, forKey: Keys.onboardingCompleted) }
    }

    // MARK: - General
    @Published var alertMinutesBefore: Int {
        didSet { defaults.set(alertMinutesBefore, forKey: Keys.minutesBefore) }
    }
    @Published var soundEnabled: Bool {
        didSet { defaults.set(soundEnabled, forKey: Keys.soundEnabled) }
    }
    @Published var repeatSoundUntilDismiss: Bool {
        didSet { defaults.set(repeatSoundUntilDismiss, forKey: Keys.repeatSound) }
    }
    @Published var vibrationEnabled: Bool {
        didSet { defaults.set(vibrationEnabled, forKey: Keys.vibration) }
    }

    // MARK: - Eventos
    @Published var videoConferenceOnly: Bool {
        didSet { defaults.set(videoConferenceOnly, forKey: Keys.videoOnly) }
    }
    @Published var acceptedEventsOnly: Bool {
        didSet { defaults.set(acceptedEventsOnly, forKey: Keys.acceptedOnly) }
    }
    @Published var showAllDayEvents: Bool {
        didSet { defaults.set(showAllDayEvents, forKey: Keys.allDay) }
    }
    @Published var workingHoursOnly: Bool {
        didSet { defaults.set(workingHoursOnly, forKey: Keys.workingHours) }
    }
    @Published var workingHoursStart: Int {
        didSet { defaults.set(workingHoursStart, forKey: Keys.workingHoursStart) }
    }
    @Published var workingHoursEnd: Int {
        didSet { defaults.set(workingHoursEnd, forKey: Keys.workingHoursEnd) }
    }
    // Días laborables: Set de weekday ISO (2=Lun … 6=Vie, 7=Sáb, 1=Dom)
    @Published var workingDays: Set<Int> {
        didSet { defaults.set(Array(workingDays), forKey: Keys.workingDays) }
    }

    // MARK: - Calendarios
    @Published var enabledCalendarIds: Set<String> {
        didSet { defaults.set(Array(enabledCalendarIds), forKey: Keys.calendarIds) }
    }

    init() {
        onboardingCompleted     = defaults.bool(forKey: Keys.onboardingCompleted)
        enabledCalendarIds      = Set(defaults.stringArray(forKey: Keys.calendarIds) ?? [])

        let saved = defaults.integer(forKey: Keys.minutesBefore)
        alertMinutesBefore      = saved == 0 ? 1 : saved

        soundEnabled            = defaults.value(forKey: Keys.soundEnabled)    as? Bool ?? true
        repeatSoundUntilDismiss = defaults.value(forKey: Keys.repeatSound)     as? Bool ?? false
        vibrationEnabled        = defaults.value(forKey: Keys.vibration)       as? Bool ?? true
        videoConferenceOnly     = defaults.value(forKey: Keys.videoOnly)       as? Bool ?? false
        acceptedEventsOnly      = defaults.value(forKey: Keys.acceptedOnly)    as? Bool ?? true
        showAllDayEvents        = defaults.value(forKey: Keys.allDay)          as? Bool ?? false
        workingHoursOnly        = defaults.value(forKey: Keys.workingHours)    as? Bool ?? false
        workingHoursStart       = defaults.value(forKey: Keys.workingHoursStart) as? Int ?? 9
        workingHoursEnd         = defaults.value(forKey: Keys.workingHoursEnd)   as? Int ?? 18
        workingDays             = Set(defaults.array(forKey: Keys.workingDays) as? [Int] ?? [2,3,4,5,6])
    }

    func completeOnboarding() { onboardingCompleted = true }

    private enum Keys {
        static let onboardingCompleted = "onboarding_completed"
        static let calendarIds         = "calendar_ids"
        static let minutesBefore       = "minutes_before"
        static let soundEnabled        = "sound_enabled"
        static let repeatSound         = "repeat_sound"
        static let vibration           = "vibration_enabled"
        static let videoOnly           = "video_conference_only"
        static let acceptedOnly        = "accepted_events_only"
        static let allDay              = "show_all_day_events"
        static let workingHours        = "working_hours_only"
        static let workingHoursStart   = "working_hours_start"
        static let workingHoursEnd     = "working_hours_end"
        static let workingDays         = "working_days"
    }
}
