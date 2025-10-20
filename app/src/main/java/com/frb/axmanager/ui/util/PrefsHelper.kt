package com.frb.axmanager.ui.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PrefsEnumHelper<Type : Enum<Type>>(
    private val keyPrefix: String,
    private val prefs: String = "settings"
) {
    private val cache = mutableMapOf<String, Boolean>() // cache sementara

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(prefs, Context.MODE_PRIVATE)

    fun saveState(context: Context, type: Type, value: Boolean) {
        val key = "$keyPrefix${type.name}"
        cache[key] = value
        getPrefs(context).edit {
            putBoolean(key, value)
        }
    }

    fun loadState(context: Context, type: Type, default: Boolean = false): Boolean {
        val key = "$keyPrefix${type.name}"

        // kalau sudah pernah dimuat, ambil dari cache
        if (cache.containsKey(key)) return cache[key] ?: default

        // kalau belum, ambil dari SharedPreferences lalu simpan ke cache
        val value = getPrefs(context).getBoolean(key, default)
        cache[key] = value
        return value
    }

    fun clearAll(context: Context) {
        cache.clear()
        getPrefs(context).edit { clear() }
    }

    fun getAll(context: Context): Map<String, *> {
        val all = getPrefs(context).all
        all.forEach { (k, v) ->
            if (v is Boolean) cache[k] = v
        }
        return all
    }
}
