package com.sierraespada.wakeywakey.windows.settings

import com.sierraespada.wakeywakey.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.prefs.Preferences

/**
 * Persiste UserSettings usando java.util.prefs.Preferences (registro en Windows,
 * ~/Library/Preferences en macOS, ~/.java/prefs en Linux).
 *
 * Expone [settings] como StateFlow para que HomeViewModel y el Scheduler
 * reaccionen a cambios en tiempo real.
 */
object DesktopSettingsRepository {

    private val prefs = Preferences.userRoot().node("com/sierraespada/wakeywakey")

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    // ── Lectura / escritura ───────────────────────────────────────────────────

    private fun load(): UserSettings = UserSettings(
        alertMinutesBefore   = prefs.getInt    ("alertMinutesBefore", 1),
        soundEnabled         = prefs.getBoolean("soundEnabled", true),
        repeatSound          = prefs.getBoolean("repeatSound", false),
        vibrationEnabled     = prefs.getBoolean("vibrationEnabled", false),
        filterVideoOnly      = prefs.getBoolean("filterVideoOnly", false),
        filterAcceptedOnly   = prefs.getBoolean("filterAcceptedOnly", false),
        showAllDayEvents     = prefs.getBoolean("showAllDayEvents", false),
        enabledCalendarIds   = prefs.get("enabledCalendarIds", "")
            .split(",").mapNotNull { it.toLongOrNull() }.toSet(),
        workHoursEnabled     = prefs.getBoolean("workHoursEnabled", false),
        workHoursStart       = prefs.getInt    ("workHoursStart", 8),
        workHoursEnd         = prefs.getInt    ("workHoursEnd", 20),
        workDays             = prefs.get("workDays", "2,3,4,5,6")
            .split(",").mapNotNull { it.toIntOrNull() }.toSet(),
        trayShowMeetingName  = prefs.getBoolean("trayShowMeetingName", true),
        trayShowTimeRemaining= prefs.getBoolean("trayShowTimeRemaining", true),
        trayIncludeTomorrow  = prefs.getBoolean("trayIncludeTomorrow", false),
        trayMonochromeIcon   = prefs.getBoolean("trayMonochromeIcon", false),
        alertAllScreens      = prefs.getBoolean("alertAllScreens", false),
        alertSoundId         = prefs.get       ("alertSoundId", "bell"),
        alertVolume          = prefs.getFloat  ("alertVolume", 0.8f),
        countdownMinutesOnly = prefs.getBoolean("countdownMinutesOnly", false),
        trayTitleMaxLength   = prefs.getInt    ("trayTitleMaxLength", 20),
        trayTitleTruncateMiddle = prefs.getBoolean("trayTitleTruncateMiddle", true),
        trayAccentColor      = prefs.get       ("trayAccentColor", "system"),
        pausedUntil          = prefs.getLong   ("pausedUntil", 0L).takeIf { it > 0 },
        showDevBar           = prefs.getBoolean("showDevBar", true),
    )

    fun save(s: UserSettings) {
        prefs.putInt    ("alertMinutesBefore", s.alertMinutesBefore)
        prefs.putBoolean("soundEnabled",       s.soundEnabled)
        prefs.putBoolean("repeatSound",        s.repeatSound)
        prefs.putBoolean("filterVideoOnly",    s.filterVideoOnly)
        prefs.putBoolean("filterAcceptedOnly", s.filterAcceptedOnly)
        prefs.putBoolean("showAllDayEvents",   s.showAllDayEvents)
        prefs.put       ("enabledCalendarIds", s.enabledCalendarIds.joinToString(","))
        prefs.putBoolean("workHoursEnabled",    s.workHoursEnabled)
        prefs.putInt    ("workHoursStart",      s.workHoursStart)
        prefs.putInt    ("workHoursEnd",        s.workHoursEnd)
        prefs.put       ("workDays",            s.workDays.joinToString(","))
        prefs.putBoolean("trayShowMeetingName", s.trayShowMeetingName)
        prefs.putBoolean("trayShowTimeRemaining", s.trayShowTimeRemaining)
        prefs.putBoolean("trayIncludeTomorrow", s.trayIncludeTomorrow)
        prefs.putBoolean("trayMonochromeIcon",  s.trayMonochromeIcon)
        prefs.putBoolean("alertAllScreens",       s.alertAllScreens)
        prefs.put       ("alertSoundId",           s.alertSoundId)
        prefs.putFloat  ("alertVolume",            s.alertVolume)
        prefs.putBoolean("countdownMinutesOnly",   s.countdownMinutesOnly)
        prefs.putInt    ("trayTitleMaxLength",      s.trayTitleMaxLength)
        prefs.putBoolean("trayTitleTruncateMiddle", s.trayTitleTruncateMiddle)
        prefs.put       ("trayAccentColor",        s.trayAccentColor)
        prefs.putLong   ("pausedUntil",         s.pausedUntil ?: 0L)
        prefs.putBoolean("showDevBar",           s.showDevBar)
        prefs.flush()
        _settings.value = s
    }

    fun setPausedUntil(millis: Long?) {
        save(_settings.value.copy(pausedUntil = millis))
    }

    fun setAlertMinutes(minutes: Int) {
        save(_settings.value.copy(alertMinutesBefore = minutes))
    }

    // ── Wizard de primer arranque ─────────────────────────────────────────────

    /** true si el usuario ya completó (o saltó) el wizard de configuración inicial. */
    val wizardCompleted: Boolean
        get() = prefs.getBoolean("wizardCompleted", false)

    fun setWizardCompleted() {
        prefs.putBoolean("wizardCompleted", true)
        prefs.flush()
    }
}
