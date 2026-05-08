package com.sierraespada.wakeywakey.windows.home

import com.sierraespada.wakeywakey.calendar.CalendarRepository
import com.sierraespada.wakeywakey.model.CalendarEvent
import com.sierraespada.wakeywakey.model.DeviceCalendar
import com.sierraespada.wakeywakey.windows.calendar.CustomEventsRepository
import com.sierraespada.wakeywakey.windows.settings.DesktopSettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.Calendar

data class HomeUiState(
    val events           : List<CalendarEvent> = emptyList(),
    val isLoading        : Boolean             = false,
    val error            : String?             = null,
    val nowMillis        : Long                = System.currentTimeMillis(),
    /** Calendarios cargados directamente vía getAvailableCalendars() — no depende de eventos. */
    val allCalendars     : List<DeviceCalendar> = emptyList(),
) {
    val nextEvent: CalendarEvent?
        get() = events.firstOrNull { it.endTime > nowMillis }

    val laterEvents: List<CalendarEvent>
        get() = if (nextEvent == null) events
                else events.drop(1).filter { it.endTime > nowMillis }

    /** Calendarios para Settings: usa allCalendars si está disponible, si no deriva de eventos. */
    val availableCalendars: List<Pair<Long, String>>
        get() = if (allCalendars.isNotEmpty())
                    allCalendars.map { it.id to it.name }.sortedBy { it.second }
                else
                    events
                        .groupBy { it.calendarId }
                        .map { (id, evts) -> id to evts.first().calendarName }
                        .sortedBy { it.second }
}

/**
 * ViewModel del HomeScreen para Desktop.
 *
 * Nota: no usa Jetpack ViewModel (que es Android-only).
 * El ciclo de vida lo gestiona AppState: llama a [start] al arrancar y
 * a [dispose] al cerrar la app.
 */
class HomeViewModel(
    calendarRepo: CalendarRepository,
) {
    @Volatile private var calendarRepo: CalendarRepository = calendarRepo
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun start() {
        refresh()
        // Tick de reloj cada segundo para actualizar countdown
        scope.launch {
            while (isActive) {
                delay(1_000L)
                _uiState.update { it.copy(nowMillis = System.currentTimeMillis()) }
            }
        }
        // Auto-refresh de eventos cada 30 segundos
        scope.launch {
            while (isActive) {
                delay(30_000L)
                refresh()
            }
        }
        // Reacciona a cambios de settings
        scope.launch {
            DesktopSettingsRepository.settings.collect { refresh() }
        }
        // Reacciona a eventos personalizados añadidos/eliminados
        scope.launch {
            CustomEventsRepository.events.collect { refresh() }
        }
    }

    fun dispose() { scope.cancel() }

    /** Reemplaza el repositorio activo y refresca inmediatamente. */
    fun updateRepo(repo: CalendarRepository) {
        calendarRepo = repo
        _uiState.update { it.copy(allCalendars = emptyList()) }
        refresh()
    }

    /**
     * Igual que [updateRepo] pero además limpia los [enabledCalendarIds] guardados.
     * Solo debe llamarse al cambiar de modo (OAuth↔MAC), nunca en el arranque normal.
     */
    fun switchRepo(repo: CalendarRepository) {
        calendarRepo = repo
        val s = DesktopSettingsRepository.settings.value
        if (s.enabledCalendarIds.isNotEmpty()) {
            DesktopSettingsRepository.save(s.copy(enabledCalendarIds = emptySet()))
        }
        _uiState.update { it.copy(allCalendars = emptyList()) }
        refresh()
    }

    // ── Acciones ──────────────────────────────────────────────────────────────

    fun refresh() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val now       = System.currentTimeMillis()
            val endOfWeek = now + 7L * 24 * 60 * 60 * 1000  // 7 días vista para el popup "All"
            val s         = DesktopSettingsRepository.settings.value

            // Carga calendarios disponibles independientemente de los eventos
            val calendars = runCatching { calendarRepo.getAvailableCalendars() }.getOrElse { emptyList() }
            if (calendars.isNotEmpty()) {
                _uiState.update { it.copy(allCalendars = calendars) }
            }

            val events = runCatching {
                calendarRepo.getUpcomingEvents(
                    fromTime      = now - 60 * 60_000L, // incluye eventos ya en curso
                    toTime        = endOfWeek,
                    includeAllDay = s.showAllDayEvents,
                )
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
                return@launch
            }.getOrElse { emptyList() }

            // Aplica filtros de settings (misma lógica que Android EventFilterUtils)
            val filtered = events.filter { event ->
                val calOk   = s.enabledCalendarIds.isEmpty() ||
                              event.calendarId in s.enabledCalendarIds
                val videoOk = !s.filterVideoOnly || event.meetingLink != null
                val accOk   = !s.filterAcceptedOnly || event.selfAttendeeStatus != 2
                calOk && videoOk && accOk
            }.sortedBy { it.startTime }

            // Mezcla eventos del calendario con eventos personalizados del usuario
            val customEvents = CustomEventsRepository.upcomingEvents(
                fromMillis = now - 60 * 60_000L,
                toMillis   = endOfWeek,
            )
            val merged = (filtered + customEvents).distinctBy { it.id }.sortedBy { it.startTime }

            _uiState.update {
                it.copy(
                    events    = merged,
                    isLoading = false,
                    nowMillis = System.currentTimeMillis(),
                )
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    // Kept for reference; fetch window is now endOfWeek computed inline in refresh()
    @Suppress("unused")
    private fun todayEndMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
        return cal.timeInMillis
    }
}
