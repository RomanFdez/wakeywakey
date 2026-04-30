package com.sierraespada.wakeywakey.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.*
import com.sierraespada.wakeywakey.calendar.AndroidCalendarRepository
import com.sierraespada.wakeywakey.scheduler.AndroidAlarmScheduler
import com.sierraespada.wakeywakey.settings.SettingsRepository
import com.sierraespada.wakeywakey.util.applySettings
import com.sierraespada.wakeywakey.widget.MeetingWidgetReceiver
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

class CalendarSyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext

        // Skip if calendar permission not granted
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED) return Result.success()

        return try {
            val settingsRepo = SettingsRepository.getInstance(context)
            val settings     = settingsRepo.settings.firstOrNull() ?: return Result.success()
            val repo         = AndroidCalendarRepository(context)
            val scheduler    = AndroidAlarmScheduler(context)
            val now          = System.currentTimeMillis()
            val tomorrow     = now + 24 * 60 * 60_000L

            // Cancelar alarmas huérfanas
            val prevIds = settingsRepo.getScheduledAlarmIds()

            val allEvents = repo.getUpcomingEvents(
                fromTime      = now,
                toTime        = tomorrow,
                includeAllDay = settings.showAllDayEvents,
            )
            val filteredEvents = allEvents.applySettings(settings)

            val newIds = filteredEvents.map { it.id }.toSet()
            (prevIds - newIds).forEach { orphanId -> scheduler.cancel(orphanId) }

            scheduler.rescheduleAll(filteredEvents, minutesBefore = settings.alertMinutesBefore)
            settingsRepo.setScheduledAlarmIds(newIds)

            // Refrescar el widget con los datos actualizados
            MeetingWidgetReceiver.update(context)

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "calendar_sync_periodic"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<CalendarSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints(requiresBatteryNotLow = false))
                .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,  // don't reset if already scheduled
                request,
            )
        }
    }
}
