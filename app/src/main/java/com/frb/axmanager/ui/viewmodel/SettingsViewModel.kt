package com.frb.axmanager.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val themeOptions = listOf("Follow System", "Dark Theme", "Light Theme")

    var isIgniteWhenRelogEnabled by mutableStateOf(
        prefs.getBoolean("ignite_when_relog", false)
    )
        private set

    var isDynamicColorEnabled by mutableStateOf(
        prefs.getBoolean("enable_dynamic_color", false)
    )
        private set

    var getAppThemeId by mutableIntStateOf(
        prefs.getInt("app_theme_id", 0)
    )
        private set

    var isDeveloperModeEnabled by mutableStateOf(
        prefs.getBoolean("enable_developer_options", false)
    )
        private set

    var isWebDebuggingEnabled by mutableStateOf(
        prefs.getBoolean("enable_web_debugging", false)
    )
        private set

    // fungsi toggle / set manual

    fun setIgniteWhenRelog(enabled: Boolean) {
        isIgniteWhenRelogEnabled = enabled
        prefs.edit { putBoolean("ignite_when_relog", enabled) }
    }

    fun setDynamicColor(enabled: Boolean) {
        isDynamicColorEnabled = enabled
        prefs.edit { putBoolean("enable_dynamic_color", enabled) }
    }

    fun setAppTheme(themeId: Int) {
        getAppThemeId = themeId
        prefs.edit { putInt("app_theme_id", themeId) }
    }

    fun setDeveloperOptions(enabled: Boolean) {
        isDeveloperModeEnabled = enabled
        prefs.edit { putBoolean("enable_developer_options", enabled) }
    }

    fun setWebDebugging(enabled: Boolean) {
        isWebDebuggingEnabled = enabled
        prefs.edit { putBoolean("enable_web_debugging", enabled) }
    }

}