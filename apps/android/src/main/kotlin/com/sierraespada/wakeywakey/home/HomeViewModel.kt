package com.sierraespada.wakeywakey.home

import android.app.Application
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
    /** Próxima reunión que aún no ha empezado (o está en curso ≤ 5 min). */
    val nextEvent: CalendarEvent?
        get() = events.firstOrNull { it.startTime > nowMillis - 5 * 60_000L }

    /** Resto de reuniones del día (sin la próxima). */
    val laterEvents: List<CalendarEvent>
        get() = if (nextEvent != null) events.drop(1) else events
}

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AndroidCalendarRepository(app)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadEvents()
        tickClock()
    }

    fun refresh() = loadEvents()

    private fun loadEvents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
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
    }

    /** Actualiza nowMillis cada segundo para el countdown en vivo. */
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
