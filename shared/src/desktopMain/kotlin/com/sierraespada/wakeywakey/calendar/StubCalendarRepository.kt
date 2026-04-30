package com.sierraespada.wakeywakey.calendar

import com.sierraespada.wakeywakey.model.CalendarEvent
import com.sierraespada.wakeywakey.model.DeviceCalendar

/**
 * Implementación stub del repositorio de calendario para Desktop.
 *
 * Devuelve listas vacías hasta que se implemente la integración real con
 * Google Calendar API / Microsoft Graph API (Slice 5.2).
 *
 * Sustituir por GoogleCalendarRepository o MicrosoftCalendarRepository
 * según la cuenta que el usuario conecte en el onboarding.
 */
class StubCalendarRepository : CalendarRepository {

    override suspend fun getUpcomingEvents(
        fromTime: Long,
        toTime: Long,
        includeAllDay: Boolean,
    ): List<CalendarEvent> = emptyList()

    override suspend fun getAvailableCalendars(): List<DeviceCalendar> = emptyList()
}
