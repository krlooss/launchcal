package com.launchcal

import android.content.SharedPreferences

class DockManager(private val prefs: SharedPreferences) {

    companion object {
        const val SLOT_COUNT = 3
        private const val KEY_PREFIX = "dock_slot_"

        val DEFAULTS = listOf(
            DockItem("phone", null, null),
            DockItem("browser", "com.brave.browser", null),
            DockItem("camera", null, null)
        )
    }

    fun getSlot(index: Int): DockItem {
        val stored = prefs.getString("${KEY_PREFIX}$index", null) ?: return DEFAULTS[index]
        val parts = stored.split("|")
        if (parts.size < 2) return DEFAULTS[index]
        return DockItem(parts[0], parts[1], parts.getOrNull(2))
    }

    fun setSlot(index: Int, packageName: String, activityName: String?, label: String) {
        prefs.edit().putString("${KEY_PREFIX}$index", "$label|$packageName|${activityName.orEmpty()}").apply()
    }

    fun isDefault(index: Int): Boolean = prefs.getString("${KEY_PREFIX}$index", null) == null
}

data class DockItem(val label: String, val packageName: String?, val activityName: String?)
