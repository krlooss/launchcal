package com.launchcal

data class CalendarEvent(
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val allDay: Boolean
)
