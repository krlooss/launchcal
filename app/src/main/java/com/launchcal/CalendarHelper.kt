package com.launchcal

import android.content.ContentResolver
import android.database.Cursor
import android.provider.CalendarContract
import java.util.Calendar

data class CalendarInfo(
    val id: Long,
    val name: String,
    val accountName: String,
    val color: Int
)

object CalendarHelper {

    fun getCalendars(contentResolver: ContentResolver): List<CalendarInfo> {
        val calendars = mutableListOf<CalendarInfo>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR
        )
        val cursor = contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI, projection, null, null, null
        )
        cursor?.use {
            while (it.moveToNext()) {
                calendars.add(CalendarInfo(
                    id = it.getLong(0),
                    name = it.getString(1) ?: "",
                    accountName = it.getString(2) ?: "",
                    color = it.getInt(3)
                ))
            }
        }
        return calendars
    }

    fun getUpcomingEvents(contentResolver: ContentResolver, days: Int = 7, calendarIds: Set<Long>? = null): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val now = Calendar.getInstance()
        val end = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_ID
        )

        val selection: String?
        val selectionArgs: Array<String>?
        if (calendarIds != null && calendarIds.isNotEmpty()) {
            selection = "${CalendarContract.Instances.CALENDAR_ID} IN (${calendarIds.joinToString(",")})"
            selectionArgs = null
        } else {
            selection = null
            selectionArgs = null
        }
        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(now.timeInMillis.toString())
            .appendPath(end.timeInMillis.toString())
            .build()

        val cursor: Cursor? = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val title = it.getString(1) ?: "No title"
                val startTime = it.getLong(2)
                val endTime = it.getLong(3)
                val allDay = it.getInt(4) == 1
                events.add(CalendarEvent(id, title, startTime, endTime, allDay))
            }
        }
        return events
    }
}
