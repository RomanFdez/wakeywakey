package com.sierraespada.wakeywakey.alert

import android.app.KeyguardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sierraespada.wakeywakey.analytics.AnalyticsProvider
import com.sierraespada.wakeywakey.analytics.Event
import com.sierraespada.wakeywakey.ui.theme.WakeyWakeyTheme

/**
 * Alerta full-screen que se muestra sobre la pantalla de bloqueo.
 * Se lanza vía Full-Screen Intent desde AlarmReceiver.
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
        // FLAG_KEEP_SCREEN_ON must be set on all API levels to prevent the
        // screen from dimming while the alert is displayed
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Intenta desbloquear el keyguard para mostrar la alerta sin swipe
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val km = getSystemService(KeyguardManager::class.java)
            km?.requestDismissKeyguard(this, null)
        }

        val eventId    = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        val title      = intent.getStringExtra(EXTRA_TITLE) ?: "Meeting"
        val startTime  = intent.getLongExtra(EXTRA_START, System.currentTimeMillis())
        val location   = intent.getStringExtra(EXTRA_LOCATION)
        val meetingUrl = intent.getStringExtra(EXTRA_MEETING_URL)

        // Analytics
        AnalyticsProvider.instance.track(
            Event.ALERT_SHOWN,
            mapOf("event_id" to eventId, "has_link" to (meetingUrl != null))
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
                        finish()
                    },
                    onSnooze   = {
                        AnalyticsProvider.instance.track(Event.ALERT_SNOOZED)
                        // TODO Slice 4: reprogramar alarma +5 min
                        finish()
                    },
                    onDismiss  = {
                        AnalyticsProvider.instance.track(Event.ALERT_DISMISSED)
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_EVENT_ID    = "event_id"
        const val EXTRA_TITLE       = "event_title"
        const val EXTRA_START       = "event_start"
        const val EXTRA_LOCATION    = "event_location"
        const val EXTRA_MEETING_URL = "meeting_url"
    }
}
