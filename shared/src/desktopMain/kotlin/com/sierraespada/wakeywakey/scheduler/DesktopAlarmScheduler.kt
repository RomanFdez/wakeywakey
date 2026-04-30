package com.sierraespada.wakeywakey.scheduler

import com.sierraespada.wakeywakey.model.CalendarEvent

/**
 * Implementación no-op de AlarmScheduler para Desktop.
 *
 * En Desktop las alarmas no usan AlarmManager del SO — el scheduling real
 * lo hace DesktopScheduler (en apps/windows) mediante un bucle de coroutines.
 * Esta clase existe únicamente para satisfacer el expect/actual o el uso de
 * la interfaz en commonMain si fuera necesario.
 */
class DesktopAlarmScheduler : AlarmScheduler {
    override fun schedule(event: CalendarEvent, minutesBefore: Int) { /* managed by DesktopScheduler */ }
    override fun cancel(eventId: Long)                              { /* managed by DesktopScheduler */ }
}
