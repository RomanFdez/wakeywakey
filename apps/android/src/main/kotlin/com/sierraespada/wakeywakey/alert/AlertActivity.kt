package com.sierraespada.wakeywakey.alert

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sierraespada.wakeywakey.alarm.AlarmReceiver
import com.sierraespada.wakeywakey.analytics.AnalyticsProvider
import com.sierraespada.wakeywakey.analytics.Event
import com.sierraespada.wakeywakey.scheduler.AndroidAlarmScheduler
import com.sierraespada.wakeywakey.ui.theme.WakeyWakeyTheme

/**
 * Full-screen alert activity shown over the lock screen when a meeting starts.
 * Launched via Full-Screen Intent (screen OFF) or direct startActivity (screen ON).
 */
class AlertActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen, turn on display, keep screen on while alert is visible
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Dismiss keyguard so alert shows without requiring a swipe
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val km = getSystemService(android.app.KeyguardManager::class.java)
            km?.requestDismissKeyguard(this, null)
        }

        val eventId    = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        val title      = intent.getStringExtra(EXTRA_TITLE) ?: "Meeting"
        val startTime  = intent.getLongExtra(EXTRA_START, System.currentTimeMillis())
        val location   = intent.getStringExtra(EXTRA_LOCATION)
        val meetingUrl = intent.getStringExtra(EXTRA_MEETING_URL)

        // Cancel the notification immediately so the heads-up banner disappears
        // as soon as this full-screen activity becomes visible.
        if (eventId != -1L) {
            getSystemService(NotificationManager::class.java)?.cancel(eventId.toInt())
        }

        AnalyticsProvider.instance.track(
            Event.ALERT_SHOWN,
            mapOf("event_id" to eventId, "has_link" to (meetingUrl != null)),
        )

        setContent {
            WakeyWakeyTheme {
                AlertScreen(
                    title      = title,
                    startTime  = startTime,
                    location   = location,
                    meetingUrl = meetingUrl,
                    onJoin     = {
                        AnalyticsProvider.instance.track(Event.JOIN_CALL_TAPPED)
                        meetingUrl?.let { url ->
                            startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                        dismissAndFinish(eventId)
                    },
                    onSnooze   = { delayMillis ->
                        val snoozeMinutes = (delayMillis / 60_000L).toInt()
                        AnalyticsProvider.instance.track(
                            Event.ALERT_SNOOZED,
                            mapOf("snooze_minutes" to snoozeMinutes),
                        )
                        scheduleSnooze(
                            eventId    = eventId,
                            title      = title,
                            startTime  = startTime,
                            location   = location,
                            meetingUrl = meetingUrl,
                            delayMillis = delayMillis,
                        )
                        dismissAndFinish(eventId)
                    },
                    onDismiss  = {
                        AnalyticsProvider.instance.track(Event.ALERT_DISMISSED)
                        dismissAndFinish(eventId)
                    },
                )
            }
        }
    }

    // ─── Snooze scheduling ────────────────────────────────────────────────────

    private fun scheduleSnooze(
        eventId: Long,
        title: String,
        startTime: Long,
        location: String?,
        meetingUrl: String?,
        delayMillis: Long,
    ) {
        val triggerAt = System.currentTimeMillis() + delayMillis
        if (triggerAt <= System.currentTimeMillis()) return

        val snoozeIntent = Intent("com.sierraespada.wakeywakey.ALARM").apply {
            setPackage(packageName)
            putExtra(AndroidAlarmScheduler.EXTRA_EVENT_ID,    eventId)
            putExtra(AndroidAlarmScheduler.EXTRA_TITLE,       title)
            putExtra(AndroidAlarmScheduler.EXTRA_START,       startTime)
            putExtra(AndroidAlarmScheduler.EXTRA_LOCATION,    location)
            putExtra(AndroidAlarmScheduler.EXTRA_MEETING_URL, meetingUrl)
        }

        // Use a different requestCode than the original alarm to avoid cancelling it
        val snoozeRequestCode = (eventId + SNOOZE_REQUEST_OFFSET).toInt()

        val pending = PendingIntent.getBroadcast(
            this, snoozeRequestCode, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val am = getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    // ─── Cleanup ──────────────────────────────────────────────────────────────

    private fun dismissAndFinish(eventId: Long) {
        // Cancel the notification that triggered this alert
        if (eventId != -1L) {
            getSystemService(NotificationManager::class.java)
                ?.cancel(eventId.toInt())
        }
        finish()
    }

    companion object {
        const val EXTRA_EVENT_ID    = "event_id"
        const val EXTRA_TITLE       = "event_title"
        const val EXTRA_START       = "event_start"
        const val EXTRA_LOCATION    = "event_location"
        const val EXTRA_MEETING_URL = "meeting_url"

        // Offset to avoid clashing PendingIntent requestCodes with the original alarm
        private const val SNOOZE_REQUEST_OFFSET = 100_000
    }
}
