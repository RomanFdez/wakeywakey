package com.sierraespada.wakeywakey.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.sierraespada.wakeywakey.model.CalendarEvent

class AndroidAlarmScheduler(private val context: Context) : AlarmScheduler {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(event: CalendarEvent, minutesBefore: Int) {
        val triggerAt = event.startTime - minutesBefore * 60_000L
        if (triggerAt <= System.currentTimeMillis()) return // ya pasó

        val pending = buildPendingIntent(event) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            // Sin permiso de alarma exacta — usamos inexacta como fallback
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            return
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
    }

    override fun cancel(eventId: Long) {
        val intent = alarmIntent(eventId)
        val pending = PendingIntent.getBroadcast(
            context, eventId.toInt(), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pending)
        pending.cancel()
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun buildPendingIntent(event: CalendarEvent): PendingIntent? {
        val intent = alarmIntent(event.id).apply {
            putExtra(EXTRA_EVENT_ID,    event.id)
            putExtra(EXTRA_TITLE,       event.title)
            putExtra(EXTRA_START,       event.startTime)
            putExtra(EXTRA_LOCATION,    event.location)
            putExtra(EXTRA_MEETING_URL, event.meetingLink)
        }
        return PendingIntent.getBroadcast(
            context, event.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun alarmIntent(eventId: Long) =
        Intent("com.sierraespada.wakeywakey.ALARM").apply {
            setPackage(context.packageName)
            putExtra(EXTRA_EVENT_ID, eventId)
        }

    companion object {
        const val EXTRA_EVENT_ID    = "event_id"
        const val EXTRA_TITLE       = "event_title"
        const val EXTRA_START       = "event_start"
        const val EXTRA_LOCATION    = "event_location"
        const val EXTRA_MEETING_URL = "meeting_url"
    }
}
