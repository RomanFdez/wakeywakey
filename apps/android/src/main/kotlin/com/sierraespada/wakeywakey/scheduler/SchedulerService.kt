package com.sierraespada.wakeywakey.scheduler

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sierraespada.wakeywakey.alarm.AlarmReceiver
import com.sierraespada.wakeywakey.calendar.AndroidCalendarRepository
import kotlinx.coroutines.*

/**
 * Foreground Service que mantiene el scheduler activo.
 * Escanea el calendario y programa alarmas cada vez que se inicia.
 *
 * TODO Slice 4: añadir WorkManager para escaneo periódico cada 15 min
 * cuando la app está en background.
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
        scope.launch {
            val repo      = AndroidCalendarRepository(applicationContext)
            val scheduler = AndroidAlarmScheduler(applicationContext)
            val now       = System.currentTimeMillis()
            val tomorrow  = now + 24 * 60 * 60 * 1000L

            val events = repo.getUpcomingEvents(fromTime = now, toTime = tomorrow)
            scheduler.rescheduleAll(events, minutesBefore = 1) // TODO: leer de UserSettings
            stopSelf()
        }
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID, "WakeyWakey Running",
                NotificationManager.IMPORTANCE_MIN
            ).apply { description = "Keeps meeting alerts active" }
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("WakeyWakey")
            .setContentText("Meeting alerts active")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID       = "wakeywakey_service"
        private const val NOTIFICATION_ID  = 1001
    }
}
