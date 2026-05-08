package com.sierraespada.wakeywakey.windows.calendar

import com.sierraespada.wakeywakey.calendar.CalendarRepository
import com.sierraespada.wakeywakey.model.CalendarEvent
import com.sierraespada.wakeywakey.model.DeviceCalendar
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Repositorio que combina múltiples [CalendarRepository] en paralelo.
 *
 * Fusiona eventos de Google + Microsoft (u otros) en una sola lista ordenada.
 * Deduplicación básica: si dos repos devuelven el mismo evento (mismo título +
 * mismo startTime), se conserva solo el primero (el que tenga calendarColor).
 */
class CombinedCalendarRepository(
    private val repos: List<CalendarRepository>,
) : CalendarRepository {

    override suspend fun getUpcomingEvents(
        fromTime:      Long,
        toTime:        Long,
        includeAllDay: Boolean,
    ): List<CalendarEvent> = coroutineScope {
        repos
            .map { repo ->
                async {
                    runCatching {
                        repo.getUpcomingEvents(fromTime, toTime, includeAllDay)
                    }.getOrElse { emptyList() }
                }
            }
            .map { it.await() }
            .flatten()
            .sortedBy { it.startTime }
            .distinctBy { "${it.title.trim().lowercase()}|${it.startTime}" }
    }

    override suspend fun getAvailableCalendars(): List<DeviceCalendar> = coroutineScope {
        repos
            .map { repo -> async { runCatching { repo.getAvailableCalendars() }.getOrElse { emptyList() } } }
            .map { it.await() }
            .flatten()
            .distinctBy { it.id }
    }
}
