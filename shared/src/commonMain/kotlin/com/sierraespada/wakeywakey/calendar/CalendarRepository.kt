package com.sierraespada.wakeywakey.calendar

import com.sierraespada.wakeywakey.model.CalendarEvent
import com.sierraespada.wakeywakey.model.DeviceCalendar

/**
 * Contrato para leer el calendario del dispositivo.
 * Implementado en cada plataforma (androidMain, desktopMain…).
 */
interface CalendarRepository {

    /**
     * Devuelve eventos que empiezan entre [fromTime] y [toTime] (epoch millis).
     * Los eventos de todo el día se excluyen por defecto.
     */
    suspend fun getUpcomingEvents(
        fromTime: Long,
        toTime: Long,
        includeAllDay: Boolean = false,
    ): List<CalendarEvent>

    /** Lista de calendarios disponibles en el dispositivo. */
    suspend fun getAvailableCalendars(): List<DeviceCalendar>
}
