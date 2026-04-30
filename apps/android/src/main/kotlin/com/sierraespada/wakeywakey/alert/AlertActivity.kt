package com.sierraespada.wakeywakey.alert

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sierraespada.wakeywakey.analytics.AnalyticsProvider
import com.sierraespada.wakeywakey.analytics.Event
import com.sierraespada.wakeywakey.scheduler.AndroidAlarmScheduler
import com.sierraespada.wakeywakey.settings.SettingsRepository
import com.sierraespada.wakeywakey.ui.theme.WakeyWakeyTheme
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

class AlertActivity : ComponentActivity() {

    private var ringtone: android.media.Ringtone? = null
    private val soundHandler  = Handler(Looper.getMainLooper())
    private var soundUri: Uri? = null
    private var soundPlays    = 0
    private var soundRepeat   = false
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen, turn on + keep screen on
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(android.app.KeyguardManager::class.java)
                ?.requestDismissKeyguard(this, null)
        }

        val eventId    = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        val title      = intent.getStringExtra(EXTRA_TITLE) ?: "Meeting"
        val startTime  = intent.getLongExtra(EXTRA_START, System.currentTimeMillis())
        val location   = intent.getStringExtra(EXTRA_LOCATION)
        val meetingUrl = intent.getStringExtra(EXTRA_MEETING_URL)

        // Cancel the notification so the heads-up banner disappears immediately
        if (eventId != -1L) {
            getSystemService(NotificationManager::class.java)?.cancel(eventId.toInt())
        }

        // Read settings (fast — DataStore is cached in memory after first read)
        val settings = runBlocking {
            SettingsRepository.getInstance(applicationContext).settings.firstOrNull()
        }

        // Play alarm sound directly — more reliable than relying on the notification
        // channel sound (which Android won't re-apply if the channel already existed).
        if (settings?.soundEnabled != false) playAlarmSound(repeat = settings?.repeatSound == true)
        if (settings?.vibrationEnabled != false) vibrate()

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
                        stopAlarmSound(); stopVibration()
                        dismissAndFinish(eventId)
                    },
                    onSnooze   = { delayMillis ->
                        AnalyticsProvider.instance.track(
                            Event.ALERT_SNOOZED,
                            mapOf("snooze_minutes" to (delayMillis / 60_000L).toInt()),
                        )
                        scheduleSnooze(eventId, title, startTime, location, meetingUrl, delayMillis)
                        stopAlarmSound(); stopVibration()
                        dismissAndFinish(eventId)
                    },
                    onDismiss  = {
                        AnalyticsProvider.instance.track(Event.ALERT_DISMISSED)
                        stopAlarmSound(); stopVibration()
                        dismissAndFinish(eventId)
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        stopAlarmSound()
        stopVibration()
        super.onDestroy()
    }

    // ─── Sound + vibration ────────────────────────────────────────────────────

    private fun playAlarmSound(repeat: Boolean = false) {
        try {
            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: return
            soundPlays = 0
            // repeat=true → loop indefinitely (SOUND_REPEAT_COUNT = Int.MAX_VALUE)
            // repeat=false → play SOUND_REPEAT_COUNT times then stop
            soundRepeat = repeat
            playSoundOnce()
        } catch (e: Exception) {
            // Never crash because of sound — the visual alert is still shown
        }
    }

    private fun playSoundOnce() {
        val uri = soundUri ?: return
        if (!soundRepeat && soundPlays >= SOUND_REPEAT_COUNT) return
        try {
            ringtone?.stop()
            ringtone = RingtoneManager.getRingtone(this, uri)?.also { rt ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) rt.isLooping = false
                rt.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                rt.play()
                soundPlays++
                soundHandler.postDelayed(::playSoundOnce, SOUND_GAP_MS)
            }
        } catch (_: Exception) {}
    }

    private fun stopAlarmSound() {
        soundHandler.removeCallbacksAndMessages(null)
        try {
            ringtone?.stop()
            ringtone = null
        } catch (_: Exception) {}
    }

    private fun vibrate() {
        try {
            // pattern: [delay, vibrate, pause, vibrate, pause, vibrate]
            val pattern = longArrayOf(0, 400, 150, 400, 150, 600)
            // repeat = -1 → play once (0 would loop forever)
            val effect  = VibrationEffect.createWaveform(pattern, /* repeat= */ -1)
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Vibrator::class.java)
            }
            vibrator?.vibrate(effect)
        } catch (_: Exception) {}
    }

    private fun stopVibration() {
        try { vibrator?.cancel() } catch (_: Exception) {}
        vibrator = null
    }

    // ─── Snooze ───────────────────────────────────────────────────────────────

    private fun scheduleSnooze(
        eventId: Long,
        title: String,
        startTime: Long,
        location: String?,
        meetingUrl: String?,
        delayMillis: Long,
    ) {
        val triggerAt = System.currentTimeMillis() + delayMillis
        val snoozeIntent = Intent("com.sierraespada.wakeywakey.ALARM").apply {
            setPackage(packageName)
            putExtra(AndroidAlarmScheduler.EXTRA_EVENT_ID,    eventId)
            putExtra(AndroidAlarmScheduler.EXTRA_TITLE,       title)
            putExtra(AndroidAlarmScheduler.EXTRA_START,       startTime)
            putExtra(AndroidAlarmScheduler.EXTRA_LOCATION,    location)
            putExtra(AndroidAlarmScheduler.EXTRA_MEETING_URL, meetingUrl)
        }
        val pending = PendingIntent.getBroadcast(
            this, (eventId + SNOOZE_REQUEST_OFFSET).toInt(), snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val am = getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun dismissAndFinish(eventId: Long) {
        if (eventId != -1L) {
            getSystemService(NotificationManager::class.java)?.cancel(eventId.toInt())
        }
        finish()
    }

    companion object {
        const val EXTRA_EVENT_ID    = "event_id"
        const val EXTRA_TITLE       = "event_title"
        const val EXTRA_START       = "event_start"
        const val EXTRA_LOCATION    = "event_location"
        const val EXTRA_MEETING_URL = "meeting_url"
        private const val SNOOZE_REQUEST_OFFSET = 100_000

        /** How many times to play the alert sound. */
        private const val SOUND_REPEAT_COUNT = 3
        /** Milliseconds between each play. Adjust to taste. */
        private const val SOUND_GAP_MS       = 2_000L
    }
}
