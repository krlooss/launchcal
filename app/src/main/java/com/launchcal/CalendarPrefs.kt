package com.launchcal

import android.content.SharedPreferences

class CalendarPrefs(private val prefs: SharedPreferences) {

    private val key = "enabled_calendars"

    fun getEnabledCalendarIds(): Set<Long>? {
        val stored = prefs.getStringSet(key, null) ?: return null
        return stored.mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun setEnabledCalendarIds(ids: Set<Long>) {
        prefs.edit().putStringSet(key, ids.map { it.toString() }.toSet()).apply()
    }
}
