package com.sierraespada.wakeywakey.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sierraespada.wakeywakey.calendar.AndroidCalendarRepository
import com.sierraespada.wakeywakey.model.DeviceCalendar
import com.sierraespada.wakeywakey.model.UserSettings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: UserSettings           = UserSettings(),
    val availableCalendars: List<DeviceCalendar> = emptyList(),
    val isLoading: Boolean               = true,
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo     = SettingsRepository.getInstance(app)
    private val calRepo  = AndroidCalendarRepository(app)

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.settings.collect { settings ->
                _state.update { it.copy(settings = settings, isLoading = false) }
            }
        }
        viewModelScope.launch {
            val calendars = runCatching { calRepo.getAvailableCalendars() }.getOrDefault(emptyList())
            _state.update { it.copy(availableCalendars = calendars) }
        }
    }

    fun setMinutesBefore(v: Int)          = viewModelScope.launch { repo.setMinutesBefore(v) }
    fun setSoundEnabled(v: Boolean)       = viewModelScope.launch { repo.setSoundEnabled(v) }
    fun setRepeatSound(v: Boolean)        = viewModelScope.launch { repo.setRepeatSound(v) }
    fun setVibrationEnabled(v: Boolean)   = viewModelScope.launch { repo.setVibrationEnabled(v) }
    fun setWorkHoursEnabled(v: Boolean)   = viewModelScope.launch { repo.setWorkHoursEnabled(v) }
    fun setWorkHoursStart(h: Int)         = viewModelScope.launch { repo.setWorkHoursStart(h) }
    fun setWorkHoursEnd(h: Int)           = viewModelScope.launch { repo.setWorkHoursEnd(h) }
    fun setWorkDays(days: Set<Int>)         = viewModelScope.launch { repo.setWorkDays(days) }
    fun setFilterVideoOnly(v: Boolean)    = viewModelScope.launch { repo.setFilterVideoOnly(v) }
    fun setFilterAcceptedOnly(v: Boolean) = viewModelScope.launch { repo.setFilterAcceptedOnly(v) }
    fun setShowAllDayEvents(v: Boolean)   = viewModelScope.launch { repo.setShowAllDayEvents(v) }

    fun toggleCalendar(id: Long) {
        val current = _state.value.settings.enabledCalendarIds
        val updated = if (id in current) current - id else current + id
        viewModelScope.launch { repo.setEnabledCalendarIds(updated) }
    }
}
