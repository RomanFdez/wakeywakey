package com.sierraespada.wakeywakey.scheduler

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sierraespada.wakeywakey.R
import com.sierraespada.wakeywakey.calendar.AndroidCalendarRepository
import com.sierraespada.wakeywakey.settings.SettingsRepository
import com.sierraespada.wakeywakey.billing.EntitlementManager
import com.sierraespada.wakeywakey.util.applyFreeTierLimits
import com.sierraespada.wakeywakey.util.applySettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull

/**
 * Foreground Service que escanea el calendario y programa alarmas.
 * Se lanza al arrancar la app y desde BootReceiver.
 * El escaneo periódico en background lo gestiona CalendarSyncWorker (WorkManager).
 */
class SchedulerService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        scheduleUpcomingAlarms()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun scheduleUpcomingAlarms() {
        // Si el usuario todavía no ha concedido el permiso de calendario (Slice 2
        // implementa el onboarding que lo solicita), salimos silenciosamente.
        val hasCalendarPermission = ContextCompat.checkSelfPermission(
            applicationContext, Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCalendarPermission) {
            stopSelf()
            return
        }

        scope.launch {
            try {
                val settingsRepo = SettingsRepository.getInstance(applicationContext)
                val settings     = settingsRepo.settings.firstOrNull()
                val repo         = AndroidCalendarRepository(applicationContext)
                val scheduler    = AndroidAlarmScheduler(applicationContext)
                val now          = System.currentTimeMillis()
                val tomorrow     = now + 24 * 60 * 60 * 1000L

                // Cancelar alarmas de eventos que ya no existen o quedaron fuera del filtro
                val prevIds = settingsRepo.getScheduledAlarmIds()

                val isPro = EntitlementManager.isPro.value

                val allEvents = repo.getUpcomingEvents(
                    fromTime     = now,
                    toTime       = tomorrow,
                    includeAllDay = settings?.showAllDayEvents ?: false,
                )
                val filteredEvents = (if (settings != null) allEvents.applySettings(settings) else allEvents)
                    .let { if (!isPro) it.applyFreeTierLimits() else it }  // free: 1 cal, max 3 alerts

                val newIds = filteredEvents.map { it.id }.toSet()
                (prevIds - newIds).forEach { orphanId -> scheduler.cancel(orphanId) }

                scheduler.rescheduleAll(filteredEvents, minutesBefore = settings?.alertMinutesBefore ?: 1)
                settingsRepo.setScheduledAlarmIds(newIds)
            } catch (e: SecurityException) {
                // El usuario revocó el permiso mientras el servicio corría
                android.util.Log.w("SchedulerService", "Calendar permission revoked: ${e.message}")
            } finally {
                stopSelf()
            }
        }
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID, getString(R.string.notif_channel_service_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply { description = getString(R.string.notif_channel_service_desc) }
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notif_service_text))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID       = "wakeywakey_service"
        private const val NOTIFICATION_ID  = 1001
    }
}
