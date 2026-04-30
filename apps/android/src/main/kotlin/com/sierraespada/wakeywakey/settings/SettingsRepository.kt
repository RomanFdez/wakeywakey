package com.sierraespada.wakeywakey.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.sierraespada.wakeywakey.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("user_settings")

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.dataStore

    val onboardingCompleted: Flow<Boolean> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[Keys.ONBOARDING_COMPLETED] ?: false }

    suspend fun completeOnboarding() =
        store.edit { it[Keys.ONBOARDING_COMPLETED] = true }

    suspend fun resetOnboarding() =
        store.edit { it[Keys.ONBOARDING_COMPLETED] = false }

    val settings: Flow<UserSettings> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            UserSettings(
                alertMinutesBefore = prefs[Keys.MINUTES_BEFORE]      ?: 1,
                soundEnabled       = prefs[Keys.SOUND_ENABLED]        ?: true,
                repeatSound        = prefs[Keys.REPEAT_SOUND]         ?: false,
                vibrationEnabled   = prefs[Keys.VIBRATION_ENABLED]    ?: true,
                filterVideoOnly    = prefs[Keys.FILTER_VIDEO_ONLY]    ?: false,
                filterAcceptedOnly = prefs[Keys.FILTER_ACCEPTED_ONLY] ?: false,
                showAllDayEvents   = prefs[Keys.SHOW_ALL_DAY]         ?: false,
                enabledCalendarIds = prefs[Keys.CALENDAR_IDS]
                    ?.split(",")?.mapNotNull { it.toLongOrNull() }?.toSet()
                    ?: emptySet(),
                workHoursEnabled   = prefs[Keys.WORK_HOURS_ENABLED]   ?: false,
                workHoursStart     = prefs[Keys.WORK_HOURS_START]     ?: 8,
                workHoursEnd       = prefs[Keys.WORK_HOURS_END]       ?: 20,
                workDays           = prefs[Keys.WORK_DAYS]
                    ?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet()
                    ?: setOf(2, 3, 4, 5, 6),
                pausedUntil        = prefs[Keys.PAUSED_UNTIL],
            )
        }

    suspend fun setMinutesBefore(value: Int) =
        store.edit { it[Keys.MINUTES_BEFORE] = value }

    suspend fun setSoundEnabled(value: Boolean) =
        store.edit { it[Keys.SOUND_ENABLED] = value }

    suspend fun setRepeatSound(value: Boolean) =
        store.edit { it[Keys.REPEAT_SOUND] = value }

    suspend fun setVibrationEnabled(value: Boolean) =
        store.edit { it[Keys.VIBRATION_ENABLED] = value }

    suspend fun setEnabledCalendarIds(ids: Set<Long>) =
        store.edit { it[Keys.CALENDAR_IDS] = ids.joinToString(",") }

    suspend fun setWorkHoursEnabled(value: Boolean) =
        store.edit { it[Keys.WORK_HOURS_ENABLED] = value }

    suspend fun setWorkHoursStart(hour: Int) =
        store.edit { it[Keys.WORK_HOURS_START] = hour }

    suspend fun setWorkHoursEnd(hour: Int) =
        store.edit { it[Keys.WORK_HOURS_END] = hour }

    suspend fun setWorkDays(days: Set<Int>) =
        store.edit { it[Keys.WORK_DAYS] = days.joinToString(",") }

    suspend fun setFilterVideoOnly(value: Boolean) =
        store.edit { it[Keys.FILTER_VIDEO_ONLY] = value }

    suspend fun setFilterAcceptedOnly(value: Boolean) =
        store.edit { it[Keys.FILTER_ACCEPTED_ONLY] = value }

    suspend fun setShowAllDayEvents(value: Boolean) =
        store.edit { it[Keys.SHOW_ALL_DAY] = value }

    // ── Pause ──────────────────────────────────────────────────────────────────

    suspend fun pauseUntil(epochMillis: Long) =
        store.edit { it[Keys.PAUSED_UNTIL] = epochMillis }

    suspend fun clearPause() =
        store.edit { it.remove(Keys.PAUSED_UNTIL) }

    // ── Scheduled alarm IDs (para cancelar alarmas huérfanas) ─────────────────

    suspend fun getScheduledAlarmIds(): Set<Long> =
        store.data.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { prefs ->
                prefs[Keys.SCHEDULED_IDS]
                    ?.split(",")?.mapNotNull { it.toLongOrNull() }?.toSet()
                    ?: emptySet()
            }.firstOrNull() ?: emptySet()

    suspend fun setScheduledAlarmIds(ids: Set<Long>) =
        store.edit {
            it[Keys.SCHEDULED_IDS] = if (ids.isEmpty()) "" else ids.joinToString(",")
        }

    // ── App trial (14 días desde primera instalación) ──────────────────────

    /** Devuelve la fecha de instalación guardada, o null si es la primera vez. */
    suspend fun getInstallDate(): Long? =
        store.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { it[Keys.INSTALL_DATE] }
            .firstOrNull()

    /** Devuelve la fecha de primera instalación (ms). La guarda si es la primera vez. */
    suspend fun getOrSetInstallDate(): Long {
        val existing = store.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { it[Keys.INSTALL_DATE] }
            .firstOrNull()
        if (existing != null) return existing
        val now = System.currentTimeMillis()
        store.edit { it[Keys.INSTALL_DATE] = now }
        return now
    }

    // ── Singleton ──────────────────────────────────────────────────────────

    companion object {
        @Volatile private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
    }

    // ── DataStore keys ─────────────────────────────────────────────────────

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val MINUTES_BEFORE       = intPreferencesKey("minutes_before")
        val SOUND_ENABLED     = booleanPreferencesKey("sound_enabled")
        val REPEAT_SOUND      = booleanPreferencesKey("repeat_sound")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val CALENDAR_IDS      = stringPreferencesKey("calendar_ids")
        val FILTER_VIDEO_ONLY    = booleanPreferencesKey("filter_video_only")
        val FILTER_ACCEPTED_ONLY = booleanPreferencesKey("filter_accepted_only")
        val SHOW_ALL_DAY         = booleanPreferencesKey("show_all_day")
        val WORK_HOURS_ENABLED   = booleanPreferencesKey("work_hours_enabled")
        val WORK_HOURS_START     = intPreferencesKey("work_hours_start")
        val WORK_HOURS_END       = intPreferencesKey("work_hours_end")
        val WORK_DAYS            = stringPreferencesKey("work_days")
        val SCHEDULED_IDS        = stringPreferencesKey("scheduled_alarm_ids")
        val PAUSED_UNTIL         = longPreferencesKey("paused_until")
        val INSTALL_DATE         = longPreferencesKey("install_date")
    }
}
