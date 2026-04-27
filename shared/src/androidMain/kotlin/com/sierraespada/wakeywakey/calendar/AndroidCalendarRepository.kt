package com.sierraespada.wakeywakey.calendar

import android.content.Context
import android.net.Uri
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
        val events        = mutableListOf<CalendarEvent>()

        // Build the Instances URI with time range embedded in the path.
        // ContentUris.appendId is the canonical way — avoids toString() edge cases.
        val uri: Uri = android.content.ContentUris.appendId(
            android.content.ContentUris.appendId(
                CalendarContract.Instances.CONTENT_URI.buildUpon(),
                fromTime,
            ),
            toTime,
        ).build()

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.ALL_DAY,
        )

        // Only filter ALL_DAY here — time range is already in the URI.
        // Adding BEGIN >= / <= again causes problems on several OEMs.
        val selection = if (includeAllDay) null
                        else "${CalendarContract.Instances.ALL_DAY} = 0"

        context.contentResolver
            .query(uri, projection, selection, null, "${CalendarContract.Instances.BEGIN} ASC")
            ?.use { cursor ->
                val colId       = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
                val colTitle    = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val colStart    = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val colEnd      = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
                val colLocation = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)
                val colDesc     = cursor.getColumnIndexOrThrow(CalendarContract.Instances.DESCRIPTION)
                val colCalId    = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
                val colAllDay   = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)

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
            val result     = mutableListOf<DeviceCalendar>()
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

    private suspend fun getCalendarNames(): Map<Long, String> =
        getAvailableCalendars().associate { it.id to it.name }
}
