package com.sierraespada.wakeywakey.calendar

import com.sierraespada.wakeywakey.model.CalendarEvent
import com.sierraespada.wakeywakey.model.DeviceCalendar
import platform.EventKit.EKAuthorizationStatusDenied
import platform.EventKit.EKAuthorizationStatusNotDetermined
import platform.EventKit.EKAuthorizationStatusRestricted
import platform.EventKit.EKCalendar
import platform.EventKit.EKEntityType
import platform.EventKit.EKEvent
import platform.EventKit.EKEventStore
import platform.Foundation.NSDate

// NSDate reference date = 2001-01-01 UTC; Unix epoch = 1970-01-01 UTC → diff = 978307200 s
private const val REFERENCE_OFFSET_S = 978307200.0

/**
 * Lee el calendario nativo de iOS vía EventKit.
 * El permiso se solicita desde Swift en el onboarding.
 * Esta clase solo lee; asume que el acceso ya fue concedido.
 */
class IosCalendarRepository : CalendarRepository {

    private val store = EKEventStore()

    override suspend fun getUpcomingEvents(
        fromTime: Long,
        toTime: Long,
        includeAllDay: Boolean,
    ): List<CalendarEvent> {
        if (!isAuthorized()) return emptyList()

        // NSDate designated init: timeIntervalSinceReferenceDate (2001-01-01 base)
        val fromDate = NSDate(timeIntervalSinceReferenceDate = fromTime / 1000.0 - REFERENCE_OFFSET_S)
        val toDate   = NSDate(timeIntervalSinceReferenceDate = toTime / 1000.0 - REFERENCE_OFFSET_S)

        val calendars = store.calendarsForEntityType(EKEntityType.EKEntityTypeEvent)
        val predicate = store.predicateForEventsWithStartDate(fromDate, toDate, calendars)

        return store.eventsMatchingPredicate(predicate)
            .filterIsInstance<EKEvent>()
            .filter { includeAllDay || !it.allDay }
            .sortedBy { it.startDate?.timeIntervalSinceReferenceDate }
            .map { it.toCalendarEvent() }
    }

    override suspend fun getAvailableCalendars(): List<DeviceCalendar> {
        if (!isAuthorized()) return emptyList()

        return store.calendarsForEntityType(EKEntityType.EKEntityTypeEvent)
            .filterIsInstance<EKCalendar>()
            .map { cal ->
                DeviceCalendar(
                    id          = cal.calendarIdentifier.hashCode().toLong(),
                    name        = cal.title,
                    accountName = cal.source?.title ?: "",
                    color       = 0,
                    isVisible   = true,
                )
            }
    }

    private fun isAuthorized(): Boolean {
        val status = EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeEvent)
        return status != EKAuthorizationStatusDenied &&
               status != EKAuthorizationStatusRestricted &&
               status != EKAuthorizationStatusNotDetermined
    }
}

private fun EKEvent.toCalendarEvent(): CalendarEvent {
    val desc = notes
    val loc  = location
    // Convert from NSDate (reference 2001-01-01) to epoch millis
    val startMs = startDate?.let { ((it.timeIntervalSinceReferenceDate + REFERENCE_OFFSET_S) * 1000).toLong() } ?: 0L
    val endMs   = endDate?.let   { ((it.timeIntervalSinceReferenceDate + REFERENCE_OFFSET_S) * 1000).toLong() } ?: 0L
    return CalendarEvent(
        id                 = eventIdentifier.hashCode().toLong(),
        title              = title ?: "No title",
        startTime          = startMs,
        endTime            = endMs,
        location           = loc,
        description        = desc,
        calendarId         = calendar?.calendarIdentifier?.hashCode()?.toLong() ?: 0L,
        calendarName       = calendar?.title ?: "",
        meetingLink        = MeetingLinkDetector.extractFromEvent(desc, loc),
        isAllDay           = allDay,
        selfAttendeeStatus = 0,
    )
}
