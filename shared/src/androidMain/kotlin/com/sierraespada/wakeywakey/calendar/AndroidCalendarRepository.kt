package com.sierraespada.wakeywakey.calendar

import android.content.Context
import android.provider.CalendarContract
import com.sierraespada.wakeywakey.model.CalendarEvent
import com.sierraespada.wakeywakey.model.DeviceCalendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidCalendarRepository(private val context: Context) : CalendarRepository {

    override suspend fun getUpcomingEvents(
        fromTime: Long,
        toTime: Long,
        includeAllDay: Boolean,
    ): List<CalendarEvent> = withContext(Dispatchers.IO) {

        val calendarNames = getCalendarNames()
        val events = mutableListOf<CalendarEvent>()

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.ALL_DAY,
        )

        val selection = buildString {
            append("${CalendarContract.Events.DTSTART} >= ?")
            append(" AND ${CalendarContract.Events.DTSTART} <= ?")
            append(" AND ${CalendarContract.Events.DELETED} = 0")
            if (!includeAllDay) append(" AND ${CalendarContract.Events.ALL_DAY} = 0")
        }
        val args = arrayOf(fromTime.toString(), toTime.toString())
        val sort = "${CalendarContract.Events.DTSTART} ASC"

        context.contentResolver
            .query(CalendarContract.Events.CONTENT_URI, projection, selection, args, sort)
            ?.use { cursor ->
                val colId          = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
                val colTitle       = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                val colStart       = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                val colEnd         = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
                val colLocation    = cursor.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION)
                val colDesc        = cursor.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)
                val colCalId       = cursor.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID)
                val colAllDay      = cursor.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)

                while (cursor.moveToNext()) {
                    val calId = cursor.getLong(colCalId)
                    val desc  = cursor.getString(colDesc)
                    val loc   = cursor.getString(colLocation)

                    events += CalendarEvent(
                        id           = cursor.getLong(colId),
                        title        = cursor.getString(colTitle) ?: "No title",
                        startTime    = cursor.getLong(colStart),
                        endTime      = cursor.getLong(colEnd),
                        location     = loc,
                        description  = desc,
                        calendarId   = calId,
                        calendarName = calendarNames[calId] ?: "",
                        meetingLink  = MeetingLinkDetector.extractFromEvent(desc, loc),
                        isAllDay     = cursor.getInt(colAllDay) == 1,
                    )
                }
            }

        events
    }

    override suspend fun getAvailableCalendars(): List<DeviceCalendar> =
        withContext(Dispatchers.IO) {
            val result = mutableListOf<DeviceCalendar>()
            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.CALENDAR_COLOR,
                CalendarContract.Calendars.VISIBLE,
            )

            context.contentResolver
                .query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null)
                ?.use { cursor ->
                    while (cursor.moveToNext()) {
                        result += DeviceCalendar(
                            id          = cursor.getLong(0),
                            name        = cursor.getString(1) ?: "Calendar",
                            accountName = cursor.getString(2) ?: "",
                            color       = cursor.getInt(3),
                            isVisible   = cursor.getInt(4) == 1,
                        )
                    }
                }
            result
        }

    /** Mapa calendarId → nombre para enriquecer eventos. */
    private suspend fun getCalendarNames(): Map<Long, String> =
        getAvailableCalendars().associate { it.id to it.name }
}
