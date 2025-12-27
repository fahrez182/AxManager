package frb.axeron.manager.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import frb.axeron.api.core.AxeronSettings
import frb.axeron.manager.ui.theme.basePrimaryDefault
import frb.axeron.manager.ui.theme.toHexString
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = AxeronSettings.getPreferences()

    val themeOptions = listOf("Follow System", "Dark Theme", "Light Theme")

    var isIgniteWhenRelogEnabled by mutableStateOf(
        prefs.getBoolean("ignite_when_relog", false)
    )
        private set

    var isActivateOnBootEnabled by mutableStateOf(
        AxeronSettings.getStartOnBoot()
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
        viewModelScope.launch {
            isIgniteWhenRelogEnabled = enabled
            prefs.edit { putBoolean("ignite_when_relog", enabled) }
        }
    }

    fun setActivateOnBoot(enabled: Boolean) {
        viewModelScope.launch {
            isActivateOnBootEnabled = enabled
            AxeronSettings.setStartOnBoot(enabled)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            isDynamicColorEnabled = enabled
            prefs.edit { putBoolean("enable_dynamic_color", enabled) }
        }
    }

    fun setAppTheme(themeId: Int) {
        viewModelScope.launch {
            getAppThemeId = themeId
            prefs.edit { putInt("app_theme_id", themeId) }
        }
    }

    fun setDeveloperOptions(enabled: Boolean) {
        viewModelScope.launch {
            isDeveloperModeEnabled = enabled
            prefs.edit { putBoolean("enable_developer_options", enabled) }
        }
    }

    fun setWebDebugging(enabled: Boolean) {
        viewModelScope.launch {
            isWebDebuggingEnabled = enabled
            prefs.edit { putBoolean("enable_web_debugging", enabled) }
        }
    }

    var customPrimaryColorHex by mutableStateOf(
        prefs.getString("custom_primary_color", null) ?: basePrimaryDefault.toHexString()
    )
        private set

    fun setCustomPrimaryColor(hex: String) {
        viewModelScope.launch {
            customPrimaryColorHex = hex
            prefs.edit {
                putString("custom_primary_color", hex)
            }
        }
    }

    fun removeCustomPrimaryColor() {
        customPrimaryColorHex = basePrimaryDefault.toHexString()
        viewModelScope.launch {
            prefs.edit { remove("custom_primary_color") }
        }
    }

}