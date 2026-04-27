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
 * Receives the AlarmManager broadcast and launches the full-screen alert.
 *
 * Two-pronged approach for reliable full-screen display on all API levels:
 *
 * 1. Start AlertActivity directly — works when screen is ON (Android 10+
 *    grants a brief activity-start window to alarm-triggered receivers).
 * 2. Post a high-priority notification with setFullScreenIntent — fires
 *    AlertActivity when screen is OFF / device is locked.
 *
 * Both paths are needed because:
 * - When screen is ON  → Android demotes fullScreenIntent to a heads-up
 *   notification unless we start the activity ourselves.
 * - When screen is OFF → direct startActivity may be suppressed; the
 *   notification's fullScreenIntent takes over.
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

        // ── 1. Direct activity launch (screen ON path) ────────────────────────
        // Alarm receivers triggered by setExactAndAllowWhileIdle are granted a
        // short BAL (Background Activity Launch) exemption on API 29+.
        context.startActivity(alertIntent)

        // ── 2. Full-screen notification (screen OFF / lock screen path) ───────
        val fullScreenPi = PendingIntent.getActivity(
            context, eventId.toInt(), alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nm = context.getSystemService(NotificationManager::class.java)

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID, "Meeting Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description         = "Full-screen alerts for upcoming meetings"
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
            .setFullScreenIntent(fullScreenPi, true)
            .build()

        nm.notify(eventId.toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "wakeywakey_alerts"
    }
}
