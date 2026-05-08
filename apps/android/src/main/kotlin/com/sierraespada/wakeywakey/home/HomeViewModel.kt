package com.sierraespada.wakeywakey.home

import android.app.Application
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sierraespada.wakeywakey.calendar.AndroidCalendarRepository
import com.sierraespada.wakeywakey.manualert.ManualAlert
import com.sierraespada.wakeywakey.manualert.ManualAlertRepository
import com.sierraespada.wakeywakey.model.CalendarEvent
import com.sierraespada.wakeywakey.scheduler.AndroidAlarmScheduler
import com.sierraespada.wakeywakey.settings.SettingsRepository
import com.sierraespada.wakeywakey.billing.EntitlementManager
import com.sierraespada.wakeywakey.util.applyFreeTierLimits
import com.sierraespada.wakeywakey.util.applySettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean          = true,
    val events: List<CalendarEvent> = emptyList(),
    val nowMillis: Long             = System.currentTimeMillis(),
    val error: String?              = null,
    val showDevBar: Boolean         = true,
) {
    val nextEvent: CalendarEvent?
        get() = events.firstOrNull { it.endTime > nowMillis }

    val laterEvents: List<CalendarEvent>
        get() = if (nextEvent != null) events.drop(1) else events
}

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo         = AndroidCalendarRepository(app)
    private val manualRepo   = ManualAlertRepository.getInstance(app)
    private val settingsRepo = SettingsRepository.getInstance(app)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val calendarObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) = silentReload()
    }

    init {
        loadEvents()
        tickClock()
        startPeriodicRefresh()

        val cr = app.contentResolver
        cr.registerContentObserver(CalendarContract.Events.CONTENT_URI,    true, calendarObserver)
        cr.registerContentObserver(CalendarContract.Instances.CONTENT_URI, true, calendarObserver)

        // Re-render when manual alerts change
        viewModelScope.launch {
            manualRepo.alerts.collect { silentReload() }
        }

        // Re-render when settings change (filters, work hours, etc.) + sync showDevBar
        viewModelScope.launch {
            settingsRepo.settings.collect { s ->
                _uiState.update { it.copy(showDevBar = s.showDevBar) }
                silentReload()
            }
        }

        // Re-render when Pro status changes (e.g. trial expires or purchase completes)
        viewModelScope.launch {
            EntitlementManager.isPro.drop(1).collect { silentReload() }
        }
    }

    override fun onCleared() {
        getApplication<Application>().contentResolver.unregisterContentObserver(calendarObserver)
        super.onCleared()
    }

    fun refresh() = loadEvents()

    private fun loadEvents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            fetchAndUpdate()
        }
    }

    private fun silentReload() {
        viewModelScope.launch { fetchAndUpdate() }
    }

    private suspend fun fetchAndUpdate() {
        try {
            val now      = System.currentTimeMillis()
            val endOfDay = endOfDay(now)
            val settings = settingsRepo.settings.firstOrNull()

            val isPro = EntitlementManager.isPro.value

            val calEvents  = repo.getUpcomingEvents(
                fromTime      = now - 5 * 60_000L,
                toTime        = endOfDay,
                includeAllDay = settings?.showAllDayEvents ?: false,
            )
                .let { if (settings != null) it.applySettings(settings) else it }
                .let { if (!isPro) it.applyFreeTierLimits() else it }  // free: 1 cal, max 3

            val manualEvts = manualRepo.alerts.firstOrNull()
                ?.filter { it.dateTimeMillis > now - 5 * 60_000L }
                ?.map { it.toCalendarEvent() }
                ?: emptyList()

            // Las alertas manuales siempre se muestran (el usuario las creó explícitamente)
            val merged = (calEvents + manualEvts).sortedBy { it.startTime }
            _uiState.update { it.copy(isLoading = false, events = merged) }
        } catch (e: SecurityException) {
            _uiState.update { it.copy(isLoading = false, error = "Calendar permission revoked.") }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = e.message) }
        }
    }

    /** Saves a new manual alert and schedules the alarm. */
    fun addManualAlert(title: String, dateTimeMillis: Long, notes: String) {
        viewModelScope.launch {
            val alert = ManualAlert(
                id            = System.currentTimeMillis(),
                title         = title,
                dateTimeMillis = dateTimeMillis,
                notes         = notes,
            )
            manualRepo.add(alert)
            val minutesBefore = settingsRepo.settings.firstOrNull()?.alertMinutesBefore ?: 1
            AndroidAlarmScheduler(getApplication()).schedule(alert.toCalendarEvent(), minutesBefore)
        }
    }

    /** Removes a manual alert and cancels its alarm. */
    fun removeManualAlert(id: Long) {
        viewModelScope.launch {
            manualRepo.remove(id)
            AndroidAlarmScheduler(getApplication()).cancel(id)
        }
    }

    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (isActive) { delay(30_000L); silentReload() }
        }
    }

    private fun tickClock() {
        viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                _uiState.update { it.copy(nowMillis = System.currentTimeMillis()) }
            }
        }
    }

    private fun endOfDay(now: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = now
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
        }
        return cal.timeInMillis
    }
}
