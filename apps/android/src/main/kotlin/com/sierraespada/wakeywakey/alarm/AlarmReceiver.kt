package com.sierraespada.wakeywakey.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.sierraespada.wakeywakey.alert.AlertActivity
import com.sierraespada.wakeywakey.scheduler.AndroidAlarmScheduler

/**
 * Recibe la alarma de AlarmManager y lanza la alerta full-screen.
 * El sistema lo dispara incluso con la pantalla apagada gracias a
 * RTC_WAKEUP + setExactAndAllowWhileIdle.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(AndroidAlarmScheduler.EXTRA_EVENT_ID, -1L)
        if (eventId == -1L) return

        val alertIntent = Intent(context, AlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_HISTORY
            putExtra(AlertActivity.EXTRA_EVENT_ID,    eventId)
            putExtra(AlertActivity.EXTRA_TITLE,       intent.getStringExtra(AndroidAlarmScheduler.EXTRA_TITLE))
            putExtra(AlertActivity.EXTRA_START,       intent.getLongExtra(AndroidAlarmScheduler.EXTRA_START, 0L))
            putExtra(AlertActivity.EXTRA_LOCATION,    intent.getStringExtra(AndroidAlarmScheduler.EXTRA_LOCATION))
            putExtra(AlertActivity.EXTRA_MEETING_URL, intent.getStringExtra(AndroidAlarmScheduler.EXTRA_MEETING_URL))
        }

        val fullScreenPi = PendingIntent.getActivity(
            context, eventId.toInt(), alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nm = context.getSystemService(NotificationManager::class.java)

        // Canal de alta prioridad — necesario en Android 8+
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID, "Meeting Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Full-screen alerts for upcoming meetings"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )

        val title = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_TITLE) ?: "Meeting"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ $title")
            .setContentText("Your meeting is starting now")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenPi, true)   // ← lanza AlertActivity
            .build()

        nm.notify(eventId.toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "wakeywakey_alerts"
    }
}
