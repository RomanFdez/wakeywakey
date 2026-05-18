@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.sierraespada.wakeywakey.scheduler

import com.sierraespada.wakeywakey.model.CalendarEvent
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.posix.CLOCK_REALTIME
import platform.posix.clock_gettime
import platform.posix.timespec

// NSDate reference date = 2001-01-01 UTC; Unix epoch = 1970-01-01 UTC → diff = 978307200 s
private const val REFERENCE_OFFSET_S = 978307200.0

/**
 * Programa notificaciones locales vía UNUserNotificationCenter.
 * La categoría "MEETING_ALERT" y sus acciones (Join / Snooze) se registran
 * en AppDelegate.swift, igual que la UI del NotificationContent extension.
 */
class IosAlarmScheduler : AlarmScheduler {

    override fun schedule(event: CalendarEvent, minutesBefore: Int) {
        val triggerAt = event.startTime - minutesBefore * 60_000L
        if (triggerAt <= nowMillis()) return

        val content = UNMutableNotificationContent().apply {
            setTitle(event.title)
            setBody(bodyText(event, minutesBefore))
            setSound(UNNotificationSound.defaultSound())
            setCategoryIdentifier("MEETING_ALERT")
            setUserInfo(
                mapOf(
                    "event_id"    to event.id.toString(),
                    "meeting_url" to (event.meetingLink ?: ""),
                    "start_time"  to event.startTime.toString(),
                )
            )
        }

        // NSDate designated init: timeIntervalSinceReferenceDate
        val fireDate   = NSDate(timeIntervalSinceReferenceDate = triggerAt / 1000.0 - REFERENCE_OFFSET_S)
        val units      = NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or
                         NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond
        val components = NSCalendar.currentCalendar.components(units, fromDate = fireDate)
        val trigger    = UNCalendarNotificationTrigger
            .triggerWithDateMatchingComponents(components, repeats = false)

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = notificationId(event.id),
            content    = content,
            trigger    = trigger,
        )
        UNUserNotificationCenter.currentNotificationCenter()
            .addNotificationRequest(request) { _ -> }
    }

    override fun cancel(eventId: Long) {
        UNUserNotificationCenter.currentNotificationCenter()
            .removePendingNotificationRequestsWithIdentifiers(listOf(notificationId(eventId)))
    }

    private fun notificationId(eventId: Long) = "ww_event_$eventId"

    private fun bodyText(event: CalendarEvent, minutesBefore: Int): String {
        val label = when (minutesBefore) {
            0    -> "Starting now"
            1    -> "Starts in 1 minute"
            else -> "Starts in $minutesBefore minutes"
        }
        return if (event.meetingLink != null) "$label · Tap to join" else label
    }

    private fun nowMillis(): Long = memScoped {
        val ts = alloc<timespec>()
        clock_gettime(CLOCK_REALTIME.toUInt(), ts.ptr)
        ts.tv_sec * 1000L + ts.tv_nsec / 1_000_000L
    }
}
