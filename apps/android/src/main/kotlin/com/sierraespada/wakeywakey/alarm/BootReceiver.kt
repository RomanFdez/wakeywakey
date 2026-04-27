package com.sierraespada.wakeywakey.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sierraespada.wakeywakey.calendar.AndroidCalendarRepository
import com.sierraespada.wakeywakey.scheduler.AndroidAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Se dispara tras reinicio del dispositivo o actualización de la app.
 * Reprograma todas las alarmas para las próximas 24 horas porque
 * AlarmManager las pierde al apagar el teléfono.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val pendingResult = goAsync()   // BroadcastReceiver puede hacer trabajo asíncrono

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo      = AndroidCalendarRepository(context)
                val scheduler = AndroidAlarmScheduler(context)
                val now       = System.currentTimeMillis()
                val tomorrow  = now + 24 * 60 * 60 * 1000L

                val events = repo.getUpcomingEvents(fromTime = now, toTime = tomorrow)
                scheduler.rescheduleAll(events, minutesBefore = 1) // TODO: leer de UserSettings
            } finally {
                pendingResult.finish()
            }
        }
    }
}
