package com.launchcal

import android.content.Intent
import android.provider.CalendarContract
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar
import java.util.Date

class CalendarAdapter(events: List<CalendarEvent>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed class ListItem {
        data class DayHeader(val label: String, val dayStart: Long) : ListItem()
        data class Event(val event: CalendarEvent) : ListItem()
    }

    private var items: List<ListItem> = buildItems(events)

    private fun buildItems(events: List<CalendarEvent>): List<ListItem> {
        val result = mutableListOf<ListItem>()
        var lastDay = ""
        for (event in events) {
            val dayLabel = formatDayLabel(event.startTime)
            if (dayLabel != lastDay) {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = event.startTime
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                result.add(ListItem.DayHeader(dayLabel, cal.timeInMillis))
                lastDay = dayLabel
            }
            result.add(ListItem.Event(event))
        }
        return result
    }

    private fun formatDayLabel(timeMillis: Long): String {
        val eventCal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val today = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

        return when {
            isSameDay(eventCal, today) -> "Today"
            isSameDay(eventCal, tomorrow) -> "Tomorrow"
            else -> DateFormat.format("EEEE, d MMMM", Date(timeMillis)).toString()
                .replaceFirstChar { it.uppercase() }
        }
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    override fun getItemViewType(position: Int) = when (items[position]) {
        is ListItem.DayHeader -> 0
        is ListItem.Event -> 1
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> DayHeaderViewHolder(inflater.inflate(R.layout.item_calendar_day_header, parent, false))
            else -> EventViewHolder(inflater.inflate(R.layout.item_calendar_event, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.DayHeader -> {
                val h = holder as DayHeaderViewHolder
                h.header.text = item.label
                h.addButton.setOnClickListener {
                    val intent = Intent(Intent.ACTION_INSERT).apply {
                        data = CalendarContract.Events.CONTENT_URI
                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, item.dayStart + 9 * 3600000)
                        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, item.dayStart + 10 * 3600000)
                    }
                    it.context.startActivity(intent)
                }
            }
            is ListItem.Event -> {
                val h = holder as EventViewHolder
                h.title.text = item.event.title
                h.time.text = if (item.event.allDay) {
                    "All day"
                } else {
                    val start = DateFormat.format("HH:mm", Date(item.event.startTime))
                    val end = DateFormat.format("HH:mm", Date(item.event.endTime))
                    "$start – $end"
                }
            }
        }
    }

    fun updateEvents(newEvents: List<CalendarEvent>) {
        items = buildItems(newEvents)
        notifyDataSetChanged()
    }

    private class DayHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val header: TextView = view.findViewById(R.id.dayHeader)
        val addButton: ImageView = view.findViewById(R.id.addEventButton)
    }

    private class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val time: TextView = view.findViewById(R.id.eventTime)
        val title: TextView = view.findViewById(R.id.eventTitle)
    }
}
