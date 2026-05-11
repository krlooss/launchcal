package com.launchcal

import android.content.ContentResolver
import android.database.Cursor
import android.provider.CalendarContract
import java.util.Calendar

object CalendarHelper {

    fun getUpcomingEvents(contentResolver: ContentResolver, days: Int = 7): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val now = Calendar.getInstance()
        val end = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY
        )

        val selection = "${CalendarContract.Instances.BEGIN} >= ? AND ${CalendarContract.Instances.BEGIN} <= ?"
        val selectionArgs = arrayOf(now.timeInMillis.toString(), end.timeInMillis.toString())
        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(now.timeInMillis.toString())
            .appendPath(end.timeInMillis.toString())
            .build()

        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, sortOrder)
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
