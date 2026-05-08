package com.sierraespada.wakeywakey.windows.scheduler

import com.sierraespada.wakeywakey.calendar.CalendarRepository
import com.sierraespada.wakeywakey.model.CalendarEvent
import com.sierraespada.wakeywakey.windows.billing.DesktopEntitlementManager
import com.sierraespada.wakeywakey.windows.settings.DesktopSettingsRepository
import kotlinx.coroutines.*
import java.util.Calendar

/**
 * Motor de alertas para Desktop.
 *
 * Funciona como un bucle de coroutines:
 *  1. Cada 15 min sincroniza los eventos del día desde [calendarRepo].
 *  2. Por cada evento sin alarma activa calcula el delay hasta [alertMinutesBefore] min antes.
 *  3. Lanza un Job hijo que espera el delay y llama a [onAlertFired].
 *  4. Si el evento desaparece o cambia, cancela el Job correspondiente.
 *
 * No usa AlarmManager ni ninguna API de SO — todo vive dentro del proceso.
 */
class DesktopScheduler(
    calendarRepo: CalendarRepository,
    private val onAlertFired: (CalendarEvent) -> Unit,
) {
    @Volatile private var calendarRepo: CalendarRepository = calendarRepo
    private val scope          = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val scheduledJobs  = mutableMapOf<Long, Job>()   // eventId → Job

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun start() {
        scope.launch {
            while (isActive) {
                sync()
                delay(SYNC_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        scope.cancel()
    }

    /** Fuerza una sincronización inmediata (p. ej. al volver de Settings). */
    fun syncNow() {
        scope.launch { sync() }
    }

    /** Reemplaza el repositorio activo (cuando el usuario conecta/desconecta cuenta). */
    fun updateRepo(repo: CalendarRepository) {
        calendarRepo = repo
    }

    // ── Sync ──────────────────────────────────────────────────────────────────

    private suspend fun sync() {
        val settings  = DesktopSettingsRepository.settings.value
        if (settings.isPaused) return

        val now       = System.currentTimeMillis()
        val endOfDay  = todayEndMillis()
        val events    = runCatching {
            calendarRepo.getUpcomingEvents(
                fromTime     = now,
                toTime       = endOfDay,
                includeAllDay = settings.showAllDayEvents,
            )
        }.getOrElse { emptyList() }

        // Filtra igual que en Android (video-only, accepted-only, calendars)
        val filtered = events.filter { event ->
            val calOk   = settings.enabledCalendarIds.isEmpty() ||
                          event.calendarId in settings.enabledCalendarIds
            val videoOk = !settings.filterVideoOnly || event.meetingLink != null
            val accOk   = !settings.filterAcceptedOnly || event.selfAttendeeStatus != 2
            calOk && videoOk && accOk
        }.let { list ->
            // Aplica restricciones del tier gratuito si el trial expiró
            if (!DesktopEntitlementManager.isPro.value && !DesktopEntitlementManager.isTrialActive) {
                val firstCalId = list.minOfOrNull { it.calendarId }
                list.filter { it.calendarId == firstCalId }
                    .take(DesktopEntitlementManager.FREE_TIER_MAX_DAILY_ALERTS)
            } else list
        }

        // Cancela jobs de eventos que ya no están en la lista
        val currentIds = filtered.map { it.id }.toSet()
        scheduledJobs.keys.filterNot { it in currentIds }.forEach { id ->
            scheduledJobs.remove(id)?.cancel()
        }

        // Programa nuevos eventos
        filtered.forEach { event ->
            if (event.id !in scheduledJobs) {
                val alertTime = event.startTime - settings.alertMinutesBefore * 60_000L
                val delayMs   = alertTime - System.currentTimeMillis()
                if (delayMs > 0) {
                    scheduledJobs[event.id] = scope.launch {
                        delay(delayMs)
                        onAlertFired(event)
                        scheduledJobs.remove(event.id)
                    }
                }
            }
        }
    }

    // ── Snooze ────────────────────────────────────────────────────────────────

    /** Reprograma la alerta de [event] para que se dispare en [delayMs] ms. */
    fun snooze(event: CalendarEvent, delayMs: Long) {
        scheduledJobs.remove(event.id)?.cancel()
        scheduledJobs[event.id] = scope.launch {
            delay(delayMs)
            onAlertFired(event)
            scheduledJobs.remove(event.id)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun todayEndMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
        return cal.timeInMillis
    }

    companion object {
        private const val SYNC_INTERVAL_MS = 15 * 60 * 1_000L
    }
}
