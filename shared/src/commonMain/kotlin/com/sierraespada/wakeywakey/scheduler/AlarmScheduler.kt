package com.sierraespada.wakeywakey.scheduler

import com.sierraespada.wakeywakey.model.CalendarEvent

/**
 * Programa y cancela alarmas para eventos de calendario.
 * En Android usa AlarmManager; en Desktop usará timers del SO.
 */
interface AlarmScheduler {
    /** Programa una alarma para [minutesBefore] minutos antes de [event]. */
    fun schedule(event: CalendarEvent, minutesBefore: Int)

    /** Cancela la alarma asociada a [eventId]. */
    fun cancel(eventId: Long)

    /** Reprograma la lista completa de eventos (llamado tras boot o sync). */
    fun rescheduleAll(events: List<CalendarEvent>, minutesBefore: Int) {
        events.forEach { schedule(it, minutesBefore) }
    }
}
