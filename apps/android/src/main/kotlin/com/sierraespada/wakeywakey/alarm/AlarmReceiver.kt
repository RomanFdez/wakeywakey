package com.sierraespada.wakeywakey.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.sierraespada.wakeywakey.R
import com.sierraespada.wakeywakey.alert.AlertActivity
import com.sierraespada.wakeywakey.calendar.MeetingLinkDetector
import com.sierraespada.wakeywakey.scheduler.AndroidAlarmScheduler

/**
 * Receives the AlarmManager broadcast and shows the full-screen alert.
 *
 * Two paths depending on whether the user granted SYSTEM_ALERT_WINDOW:
 *
 *   A) Overlay granted  → startActivity() directly. SYSTEM_ALERT_WINDOW exempts the
 *      app from Android 10+ Background Activity Launch (BAL) restrictions, so
 *      AlertActivity opens full-screen over whatever is on screen.
 *
 *   B) Overlay NOT granted → post a MAX-priority heads-up notification with
 *      setFullScreenIntent (works when locked/screen-off) plus inline action
 *      buttons (Join, Snooze 5m, Dismiss) so the user can act without opening
 *      the app when the phone is unlocked.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(AndroidAlarmScheduler.EXTRA_EVENT_ID, -1L)
        if (eventId == -1L) return

        // ── Releer el evento fresco desde el Calendar Provider ────────────────
        // Los extras del Intent reflejan el estado del evento CUANDO SE PROGRAMÓ
        // la alarma. Si el evento fue editado después (título, link, etc.) los
        // extras estarían desactualizados. Siempre consultamos la fuente de verdad.
        val fresh = readEventFromCalendar(context, eventId)

        val title      = fresh?.first  ?: intent.getStringExtra(AndroidAlarmScheduler.EXTRA_TITLE)
                         ?: context.getString(R.string.notif_fallback_meeting_title)
        val start      = fresh?.second ?: intent.getLongExtra(AndroidAlarmScheduler.EXTRA_START, 0L)
        val location   = fresh?.third  ?: intent.getStringExtra(AndroidAlarmScheduler.EXTRA_LOCATION)
        val meetingUrl = fresh?.fourth ?: intent.getStringExtra(AndroidAlarmScheduler.EXTRA_MEETING_URL)

        val alertIntent = Intent(context, AlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_HISTORY
            putExtra(AlertActivity.EXTRA_EVENT_ID,    eventId)
            putExtra(AlertActivity.EXTRA_TITLE,       title)
            putExtra(AlertActivity.EXTRA_START,       start)
            putExtra(AlertActivity.EXTRA_LOCATION,    location)
            putExtra(AlertActivity.EXTRA_MEETING_URL, meetingUrl)
        }

        // Always post the notification — guarantees delivery when screen is locked/off
        // and provides action buttons as fallback.
        // AlertActivity.onCreate() cancels this notification immediately on launch,
        // so there is no visible double-alert.
        postHeadsUpNotification(context, eventId, title, start, location, meetingUrl, alertIntent)

        // Additionally, if overlay permission is granted, launch the Activity directly.
        // SYSTEM_ALERT_WINDOW exempts from BAL restrictions (Android 10+), so the
        // full-screen AlertActivity appears even when the phone is unlocked.
        if (Settings.canDrawOverlays(context)) {
            context.startActivity(alertIntent)
        }
    }

    // ─── Option B helpers ─────────────────────────────────────────────────────

    private fun postHeadsUpNotification(
        context: Context,
        eventId: Long,
        title: String,
        start: Long,
        location: String?,
        meetingUrl: String?,
        alertIntent: Intent,
    ) {
        val nm = context.getSystemService(NotificationManager::class.java)

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID, context.getString(R.string.notif_channel_alerts_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description          = context.getString(R.string.notif_channel_alerts_desc)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
        )

        // Tapping the notification body opens AlertActivity
        val fullScreenPi = PendingIntent.getActivity(
            context, eventId.toInt(), alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val bodyText = buildString {
            append(context.getString(R.string.notif_body_starting_now))
            if (!location.isNullOrBlank()) append("\n📍 $location")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ $title")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)           // stays visible until user acts
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPi, /* highPriority = */ true)
            .setContentIntent(fullScreenPi)

        // ── Join action (only when meeting URL is available) ─────────────────
        if (meetingUrl != null) {
            val joinPi = PendingIntent.getActivity(
                context,
                (eventId + JOIN_REQUEST_OFFSET).toInt(),
                Intent(Intent.ACTION_VIEW, Uri.parse(meetingUrl))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(android.R.drawable.ic_menu_call, context.getString(R.string.notif_action_join), joinPi)
        }

        // ── Snooze 5 min action ──────────────────────────────────────────────
        val snoozeIntent = Intent(NotificationActionReceiver.ACTION_SNOOZE).apply {
            setClass(context, NotificationActionReceiver::class.java)
            putExtra(AndroidAlarmScheduler.EXTRA_EVENT_ID,    eventId)
            putExtra(AndroidAlarmScheduler.EXTRA_TITLE,       title)
            putExtra(AndroidAlarmScheduler.EXTRA_START,       start)
            putExtra(AndroidAlarmScheduler.EXTRA_LOCATION,    location)
            putExtra(AndroidAlarmScheduler.EXTRA_MEETING_URL, meetingUrl)
        }
        builder.addAction(
            android.R.drawable.ic_popup_sync,
            context.getString(R.string.notif_action_snooze),
            PendingIntent.getBroadcast(
                context,
                (eventId + SNOOZE_REQUEST_OFFSET).toInt(),
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        )

        // ── Dismiss action ───────────────────────────────────────────────────
        val dismissIntent = Intent(NotificationActionReceiver.ACTION_DISMISS).apply {
            setClass(context, NotificationActionReceiver::class.java)
            putExtra(AndroidAlarmScheduler.EXTRA_EVENT_ID, eventId)
        }
        builder.addAction(
            android.R.drawable.ic_delete,
            context.getString(R.string.notif_action_dismiss),
            PendingIntent.getBroadcast(
                context,
                (eventId + DISMISS_REQUEST_OFFSET).toInt(),
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        )

        nm.notify(eventId.toInt(), builder.build())
    }

    companion object {
        const val CHANNEL_ID = "wakeywakey_alerts"
        private const val JOIN_REQUEST_OFFSET    = 200_000
        private const val SNOOZE_REQUEST_OFFSET  = 300_000
        private const val DISMISS_REQUEST_OFFSET = 400_000

        /**
         * Lee los datos frescos de un evento desde el Calendar Provider.
         *
         * Ejecuta una query sincrónica (single-row, rápida) porque estamos en
         * BroadcastReceiver.onReceive() y no podemos usar coroutines directamente.
         *
         * @return Quadruple(title, startTime, location, meetingUrl) o null si el
         *         evento fue borrado o no tenemos permiso READ_CALENDAR.
         */
        fun readEventFromCalendar(
            context: Context,
            eventId: Long,
        ): Quadruple<String, Long, String?, String?>? {
            val uri        = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            val projection = arrayOf(
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.EVENT_LOCATION,
                CalendarContract.Events.DESCRIPTION,
            )
            return try {
                context.contentResolver.query(uri, projection, null, null, null)
                    ?.use { cursor ->
                        if (!cursor.moveToFirst()) return null
                        val title  = cursor.getString(0)?.takeIf { it.isNotBlank() }
                        val start  = cursor.getLong(1)
                        val loc    = cursor.getString(2)?.takeIf { it.isNotBlank() }
                        val desc   = cursor.getString(3)
                        val link   = MeetingLinkDetector.extractFromEvent(desc, loc)
                        Quadruple(title ?: "", start, loc, link)
                    }
            } catch (_: SecurityException) {
                null // READ_CALENDAR permission revoked at runtime
            }
        }
    }
}

/** Tuple de 4 elementos — evita depender de Arrow o añadir una librería externa. */
data class Quadruple<A, B, C, D>(
    val first:  A,
    val second: B,
    val third:  C,
    val fourth: D,
)
