package com.sierraespada.wakeywakey.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.sierraespada.wakeywakey.scheduler.AndroidAlarmScheduler

/**
 * Handles inline notification action buttons (Snooze, Dismiss) shown in the
 * heads-up notification when SYSTEM_ALERT_WINDOW is not granted.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(AndroidAlarmScheduler.EXTRA_EVENT_ID, -1L)
        val nm      = context.getSystemService(NotificationManager::class.java)

        when (intent.action) {
            ACTION_SNOOZE -> {
                nm.cancel(eventId.toInt())
                scheduleSnooze(
                    context    = context,
                    eventId    = eventId,
                    title      = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_TITLE) ?: "Meeting",
                    start      = intent.getLongExtra(AndroidAlarmScheduler.EXTRA_START, 0L),
                    location   = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_LOCATION),
                    meetingUrl = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_MEETING_URL),
                )
            }
            ACTION_DISMISS -> nm.cancel(eventId.toInt())
        }
    }

    private fun scheduleSnooze(
        context: Context,
        eventId: Long,
        title: String,
        start: Long,
        location: String?,
        meetingUrl: String?,
    ) {
        val triggerAt = System.currentTimeMillis() + SNOOZE_DELAY_MS

        val alarmIntent = Intent("com.sierraespada.wakeywakey.ALARM").apply {
            setPackage(context.packageName)
            putExtra(AndroidAlarmScheduler.EXTRA_EVENT_ID,    eventId)
            putExtra(AndroidAlarmScheduler.EXTRA_TITLE,       title)
            putExtra(AndroidAlarmScheduler.EXTRA_START,       start)
            putExtra(AndroidAlarmScheduler.EXTRA_LOCATION,    location)
            putExtra(AndroidAlarmScheduler.EXTRA_MEETING_URL, meetingUrl)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            (eventId + SNOOZE_ALARM_REQUEST_OFFSET).toInt(),
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val am = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    companion object {
        const val ACTION_SNOOZE  = "com.sierraespada.wakeywakey.NOTIF_SNOOZE"
        const val ACTION_DISMISS = "com.sierraespada.wakeywakey.NOTIF_DISMISS"

        private const val SNOOZE_DELAY_MS             = 5 * 60 * 1_000L
        private const val SNOOZE_ALARM_REQUEST_OFFSET = 500_000
    }
}
