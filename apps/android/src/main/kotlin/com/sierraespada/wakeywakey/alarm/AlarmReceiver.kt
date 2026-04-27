package com.sierraespada.wakeywakey.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.sierraespada.wakeywakey.alert.AlertActivity
import com.sierraespada.wakeywakey.scheduler.AndroidAlarmScheduler

/**
 * Receives the AlarmManager broadcast and launches the full-screen alert.
 *
 * Strategy depends on whether the screen is currently on:
 *
 * • Screen ON  → start AlertActivity directly (BAL exemption granted to
 *                alarm receivers). Post a silent/low-priority notification
 *                just so the user can dismiss it from the shade — no heads-up.
 *
 * • Screen OFF → post a MAX-priority notification with setFullScreenIntent.
 *                Android uses the FSI to launch AlertActivity over the lock screen.
 *                Starting an activity directly is unreliable when screen is off.
 *
 * This avoids the double-flash (heads-up banner + full-screen activity) that
 * happens when both paths fire simultaneously.
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

        val nm = context.getSystemService(NotificationManager::class.java)
        val pm = context.getSystemService(PowerManager::class.java)

        // Ensure the notification channel exists (idempotent)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID, "Meeting Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description          = "Full-screen alerts for upcoming meetings"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )

        if (pm.isInteractive) {
            // ── Screen is ON ─────────────────────────────────────────────────
            // Start the activity directly — the system grants a short BAL window
            // to alarm-triggered BroadcastReceivers (API 29+).
            context.startActivity(alertIntent)

            // Post a silent notification so the user sees it in the shade and
            // can tap to reopen — but don't show a heads-up banner.
            val silentNotification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("⏰ $title")
                .setContentText("Meeting alert — tap to view")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(
                    PendingIntent.getActivity(
                        context, eventId.toInt(), alertIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .build()

            nm.notify(eventId.toInt(), silentNotification)

        } else {
            // ── Screen is OFF / locked ────────────────────────────────────────
            // Use full-screen intent — Android fires it over the lock screen.
            val fullScreenPi = PendingIntent.getActivity(
                context, eventId.toInt(), alertIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

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
    }

    companion object {
        const val CHANNEL_ID = "wakeywakey_alerts"
    }
}
