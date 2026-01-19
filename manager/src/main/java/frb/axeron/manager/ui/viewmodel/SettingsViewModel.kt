package frb.axeron.manager.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import frb.axeron.api.core.AxeronSettings
import frb.axeron.manager.ui.theme.basePrimaryDefault
import frb.axeron.manager.ui.theme.toHexString
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
//    private val prefs = AxeronSettings.getPreferences()

    val themeOptions = listOf("Follow System", "Dark Theme", "Light Theme")

    var isIgniteWhenRelogEnabled by mutableStateOf(
        AxeronSettings.getEnableIgniteRelog()
    )
        private set

    var isActivateOnBootEnabled by mutableStateOf(
        AxeronSettings.getStartOnBoot()
    )
        private set

    var isTcpModeEnabled by mutableStateOf(
        AxeronSettings.getTcpMode()
    )
        private set

    var tcpPortInt: Int? by mutableStateOf(
        AxeronSettings.getTcpPort()
    )
        private set

    var isDynamicColorEnabled by mutableStateOf(
        AxeronSettings.getEnableDynamicColor()
    )
        private set

    var getAppThemeId by mutableIntStateOf(
        AxeronSettings.getAppThemeId()
    )
        private set

    var isDeveloperModeEnabled by mutableStateOf(
        AxeronSettings.getEnableDeveloperOptions()
    )
        private set

    var isWebDebuggingEnabled by mutableStateOf(
        AxeronSettings.getEnableWebDebugging()
    )
        private set

    // fungsi toggle / set manual

    fun setIgniteWhenRelog(enabled: Boolean) {
        viewModelScope.launch {
            isIgniteWhenRelogEnabled = enabled
            AxeronSettings.setEnableIgniteRelog(enabled)
        }
    }

    fun setActivateOnBoot(enabled: Boolean) {
        viewModelScope.launch {
            isActivateOnBootEnabled = enabled
            AxeronSettings.setStartOnBoot(enabled)
        }
    }

    fun setTcpMode(enabled: Boolean) {
        viewModelScope.launch {
            isTcpModeEnabled = enabled
            AxeronSettings.setTcpMode(enabled)
        }
    }

    fun setTcpPort(port: Int?) {
        viewModelScope.launch {
            tcpPortInt = port
            AxeronSettings.setTcpPort(port)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            isDynamicColorEnabled = enabled
            AxeronSettings.setEnableDynamicColor(enabled)
        }
    }

    fun setAppTheme(themeId: Int) {
        viewModelScope.launch {
            getAppThemeId = themeId
            AxeronSettings.setAppThemeId(themeId)
        }
    }

    fun setDeveloperOptions(enabled: Boolean) {
        viewModelScope.launch {
            isDeveloperModeEnabled = enabled
            AxeronSettings.setEnableDeveloperOptions(enabled)
        }
    }

    fun setWebDebugging(enabled: Boolean) {
        viewModelScope.launch {
            isWebDebuggingEnabled = enabled
            AxeronSettings.setEnableWebDebugging(enabled)
        }
    }

    var customPrimaryColorHex by mutableStateOf(
        AxeronSettings.getCustomPrimaryColor() ?: basePrimaryDefault.toHexString()
    )
        private set

    fun setCustomPrimaryColor(hex: String) {
        viewModelScope.launch {
            customPrimaryColorHex = hex
            AxeronSettings.setPrimaryColor(hex)
        }
    }

    fun removeCustomPrimaryColor() {
        viewModelScope.launch {
            customPrimaryColorHex = basePrimaryDefault.toHexString()
            AxeronSettings.removePrimaryColor()
        }
    }

}