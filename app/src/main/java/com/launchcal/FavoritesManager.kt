package com.launchcal

import android.content.SharedPreferences

class FavoritesManager(private val prefs: SharedPreferences) {

    private val key = "favorites"

    fun isFavorite(packageName: String): Boolean =
        getFavorites().contains(packageName)

    fun add(packageName: String) {
        val set = getFavorites().toMutableSet()
        set.add(packageName)
        prefs.edit().putStringSet(key, set).apply()
    }

    fun remove(packageName: String) {
        val set = getFavorites().toMutableSet()
        set.remove(packageName)
        prefs.edit().putStringSet(key, set).apply()
    }

    private fun getFavorites(): Set<String> =
        prefs.getStringSet(key, emptySet()) ?: emptySet()
}
