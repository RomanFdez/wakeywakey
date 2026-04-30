package com.sierraespada.wakeywakey.manualert

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sierraespada.wakeywakey.model.CalendarEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ── Model ──────────────────────────────────────────────────────────────────────

@Serializable
data class ManualAlert(
    val id: Long,
    val title: String,
    val dateTimeMillis: Long,
    val notes: String = "",
) {
    /** Convert to CalendarEvent so the home screen can render it uniformly. */
    fun toCalendarEvent() = CalendarEvent(
        id           = id,
        title        = title,
        startTime    = dateTimeMillis,
        endTime      = dateTimeMillis + 30 * 60_000L,
        location     = null,
        description  = notes.ifBlank { null },
        calendarId   = MANUAL_CALENDAR_ID,
        calendarName = "Manual alert",
        meetingLink  = null,
        isAllDay     = false,
    )

    companion object {
        const val MANUAL_CALENDAR_ID = -1L
    }
}

// ── Repository ─────────────────────────────────────────────────────────────────

private val Context.manualAlertStore by preferencesDataStore("manual_alerts")
private val KEY = stringPreferencesKey("alerts_json")
private val json = Json { ignoreUnknownKeys = true }

class ManualAlertRepository(context: Context) {

    private val store = context.applicationContext.manualAlertStore

    val alerts: Flow<List<ManualAlert>> = store.data.map { prefs ->
        prefs[KEY]?.let { runCatching { json.decodeFromString<List<ManualAlert>>(it) }.getOrNull() }
            ?: emptyList()
    }

    suspend fun add(alert: ManualAlert) = store.edit { prefs ->
        val current = prefs[KEY]?.let { runCatching { json.decodeFromString<List<ManualAlert>>(it) }.getOrNull() }
            ?: emptyList()
        prefs[KEY] = json.encodeToString(current + alert)
    }

    suspend fun remove(id: Long) = store.edit { prefs ->
        val current = prefs[KEY]?.let { runCatching { json.decodeFromString<List<ManualAlert>>(it) }.getOrNull() }
            ?: emptyList()
        prefs[KEY] = json.encodeToString(current.filter { it.id != id })
    }

    companion object {
        @Volatile private var instance: ManualAlertRepository? = null
        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: ManualAlertRepository(context.applicationContext).also { instance = it }
        }
    }
}
