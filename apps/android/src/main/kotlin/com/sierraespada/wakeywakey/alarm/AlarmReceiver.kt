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
 * Receives the AlarmManager broadcast and shows the full-screen alert.
 *
 * Strategy: always post a MAX-priority notification with setFullScreenIntent.
 * Android handles both cases:
 *   • Screen OFF / locked → system fires the fullScreenIntent immediately,
 *     AlertActivity launches over the lock screen.
 *   • Screen ON           → system shows a brief heads-up AND fires the
 *     fullScreenIntent. AlertActivity cancels the notification in onCreate()
 *     so the heads-up banner disappears as soon as the activity is visible.
 *
 * We no longer call startActivity() directly because Android 10+ Background
 * Activity Launch (BAL) restrictions silently block it for most alarm types,
 * resulting in only the heads-up showing instead of the full-screen activity.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(AndroidAlarmScheduler.EXTRA_EVENT_ID, -1L)
        if (eventId == -1L) return

        val title = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_TITLE) ?: "Meeting"

        val alertIntent = Intent(context, AlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_HISTORY
            putExtra(AlertActivity.EXTRA_EVENT_ID,    eventId)
            putExtra(AlertActivity.EXTRA_TITLE,       title)
            putExtra(AlertActivity.EXTRA_START,       intent.getLongExtra(AndroidAlarmScheduler.EXTRA_START, 0L))
            putExtra(AlertActivity.EXTRA_LOCATION,    intent.getStringExtra(AndroidAlarmScheduler.EXTRA_LOCATION))
            putExtra(AlertActivity.EXTRA_MEETING_URL, intent.getStringExtra(AndroidAlarmScheduler.EXTRA_MEETING_URL))
        }

        val fullScreenPi = PendingIntent.getActivity(
            context, eventId.toInt(), alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val nm = context.getSystemService(NotificationManager::class.java)

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID, "Meeting Alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description          = "Full-screen alerts for upcoming meetings"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ $title")
            .setContentText("Your meeting is starting now")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenPi, /* highPriority = */ true)
            .build()

        nm.notify(eventId.toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "wakeywakey_alerts"
    }
}
