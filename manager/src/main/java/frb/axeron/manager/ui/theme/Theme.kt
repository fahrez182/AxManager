package frb.axeron.manager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import frb.axeron.manager.ui.viewmodel.SettingsViewModel

fun hexToColor(hex: String): Color {
    val cleanHex = hex.removePrefix("#")
    val cleanDefault = basePrimaryDefault.toHexString().removePrefix("#")
    return when (cleanHex.length) {
        6 -> Color(("FF$cleanHex").toLong(16))
        8 -> Color(cleanHex.toLong(16))
        else -> Color(cleanDefault.toLong(16))
    }
}

@Composable
fun AxManagerTheme(
    // Dynamic color is available on Android 12+ ,
    contentCompose: @Composable (SettingsViewModel) -> Unit
) {
    val settingsViewModel: SettingsViewModel = viewModel<SettingsViewModel>()

    val darkTheme = when (settingsViewModel.getAppThemeId) {
        1 -> true
        2 -> false
        else -> isSystemInDarkTheme()
    }

    val dynamicColor = settingsViewModel.isDynamicColorEnabled
    val customPrimaryColor =
        hexToColor(settingsViewModel.customPrimaryColorHex)

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            when {
                darkTheme -> dynamicDarkColorScheme(context)
                else -> dynamicLightColorScheme(context)
            }
        }

        darkTheme -> getVortexDarkColorScheme(customPrimaryColor)
        else -> getVortexLightColorScheme(customPrimaryColor)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        contentCompose(settingsViewModel)
    }
}