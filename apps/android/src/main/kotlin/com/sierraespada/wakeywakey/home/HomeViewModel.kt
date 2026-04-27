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
import com.sierraespada.wakeywakey.model.CalendarEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean          = true,
    val events: List<CalendarEvent> = emptyList(),
    val nowMillis: Long             = System.currentTimeMillis(),
    val error: String?              = null,
) {
    val nextEvent: CalendarEvent?
        get() = events.firstOrNull { it.startTime > nowMillis - 5 * 60_000L }

    val laterEvents: List<CalendarEvent>
        get() = if (nextEvent != null) events.drop(1) else events
}

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AndroidCalendarRepository(app)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * ContentObserver: fires immediately when a local calendar change happens
     * (e.g. event created/edited on this device).
     */
    private val calendarObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) = silentReload()
    }

    init {
        loadEvents()
        tickClock()
        startPeriodicRefresh()

        // Watch both URIs — Events for local edits, Instances for sync'd changes
        val cr = app.contentResolver
        cr.registerContentObserver(CalendarContract.Events.CONTENT_URI,    true, calendarObserver)
        cr.registerContentObserver(CalendarContract.Instances.CONTENT_URI, true, calendarObserver)
    }

    override fun onCleared() {
        getApplication<Application>().contentResolver.unregisterContentObserver(calendarObserver)
        super.onCleared()
    }

    /** Called by pull-to-refresh — shows the spinner. */
    fun refresh() = loadEvents()

    /** Visible load: sets isLoading = true so the PTR spinner shows. */
    private fun loadEvents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            fetchAndUpdate()
        }
    }

    /**
     * Silent background reload — updates events without showing the spinner.
     * Used by the ContentObserver and the periodic timer so the UI doesn't
     * flash a loading state every 30 seconds.
     */
    private fun silentReload() {
        viewModelScope.launch { fetchAndUpdate() }
    }

    private suspend fun fetchAndUpdate() {
        try {
            val now      = System.currentTimeMillis()
            val endOfDay = endOfDay(now)
            val events   = repo.getUpcomingEvents(fromTime = now - 5 * 60_000L, toTime = endOfDay)
            _uiState.update { it.copy(isLoading = false, events = events) }
        } catch (e: SecurityException) {
            _uiState.update { it.copy(isLoading = false, error = "Calendar permission revoked.") }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = e.message) }
        }
    }

    /**
     * Silent reload every 30 seconds.
     * Catches cases where the ContentObserver misses a notification
     * (e.g. Google Calendar sync happening in the background).
     */
    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                silentReload()
            }
        }
    }

    /** Updates the clock every second for the live countdown. */
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
