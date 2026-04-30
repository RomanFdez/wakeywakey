package com.sierraespada.wakeywakey.util

import com.sierraespada.wakeywakey.model.CalendarEvent
import com.sierraespada.wakeywakey.model.UserSettings
import java.util.Calendar

// ── Free tier ─────────────────────────────────────────────────────────────────

/** Máximo de eventos diarios visibles en el tier gratuito (trial expirado). */
const val FREE_TIER_MAX_DAILY_EVENTS = 3

/**
 * Restricciones del tier gratuito:
 *  - Solo el primer calendario (el de menor calendarId, normalmente el principal)
 *  - Máximo [FREE_TIER_MAX_DAILY_EVENTS] eventos en total
 *
 * Se aplica en HomeViewModel y SchedulerService DESPUÉS de [applySettings],
 * solo cuando !isPro y la lista no incluye alertas manuales.
 */
fun List<CalendarEvent>.applyFreeTierLimits(): List<CalendarEvent> {
    if (isEmpty()) return this
    val firstCalendarId = minOf { it.calendarId }
    return filter { it.calendarId == firstCalendarId }
        .take(FREE_TIER_MAX_DAILY_EVENTS)
}

// ── Filtros de settings ───────────────────────────────────────────────────────

/**
 * Aplica los filtros de UserSettings a una lista de eventos.
 * Usado por SchedulerService, CalendarSyncWorker y HomeViewModel para
 * garantizar comportamiento consistente en todos los puntos de entrada.
 */
fun List<CalendarEvent>.applySettings(settings: UserSettings): List<CalendarEvent> {
    // Si la app está en pausa, no programar ninguna alarma
    if (settings.isPaused) return emptyList()

    return filter { event ->
        // 1. Filtrar por calendarios activos (vacío = todos)
        settings.isCalendarEnabled(event.calendarId) &&

        // 2. Solo eventos con videollamada
        (!settings.filterVideoOnly || event.meetingLink != null) &&

        // 3. Solo eventos aceptados (excluye DECLINED=2; NONE=0 y ACCEPTED=1 se muestran)
        (!settings.filterAcceptedOnly || event.selfAttendeeStatus != 2) &&

        // 4. Horas laborales
        (!settings.workHoursEnabled || event.isInWorkHours(settings))
    }
}

private fun CalendarEvent.isInWorkHours(settings: UserSettings): Boolean {
    val cal = Calendar.getInstance().apply { timeInMillis = startTime }
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)   // 1=Dom … 7=Sáb (constantes Calendar)
    val hourOfDay = cal.get(Calendar.HOUR_OF_DAY)
    return dayOfWeek in settings.workDays &&
           hourOfDay >= settings.workHoursStart &&
           hourOfDay < settings.workHoursEnd
}
