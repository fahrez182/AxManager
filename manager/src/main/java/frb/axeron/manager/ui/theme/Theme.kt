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
import frb.axeron.manager.ui.component.blend
import frb.axeron.manager.ui.viewmodel.SettingsViewModel

fun hexToColor(hex: String?): Color? {
    if (hex == null) return null
    return try {
        val cleanHex = hex.removePrefix("#")
        when (cleanHex.length) {
            6 -> Color(("FF$cleanHex").toLong(16))
            8 -> Color(cleanHex.toLong(16))
            else -> null
        }
    } catch (e: Exception) {
        null
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
    val customPrimaryColor = hexToColor(settingsViewModel.customPrimaryColorHex)

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val dynamicDarkColorScheme = dynamicDarkColorScheme(context)
            val dynamicLightColorScheme = dynamicLightColorScheme(context)

            when {
                darkTheme -> vortexDarkColorScheme().copy(
                    primary = dynamicDarkColorScheme.primary.blend(AMOLED_BLACK, 0.2f),
                    onPrimary = dynamicDarkColorScheme.onPrimary.blend(AMOLED_BLACK, 0.2f),
                    primaryContainer = dynamicDarkColorScheme.primaryContainer.blend(
                        AMOLED_BLACK,
                        0.2f
                    ),
                    onPrimaryContainer = dynamicDarkColorScheme.onPrimaryContainer.blend(
                        Color.White,
                        0.2f
                    ),

                    secondary = dynamicDarkColorScheme.secondary.blend(AMOLED_BLACK, 0.25f),
                    onSecondary = dynamicDarkColorScheme.onSecondary.blend(AMOLED_BLACK, 0.5f),
                    secondaryContainer = dynamicDarkColorScheme.secondaryContainer.blend(
                        AMOLED_BLACK,
                        0.2f
                    ),
                    onSecondaryContainer = dynamicDarkColorScheme.onSecondaryContainer.blend(
                        Color.White,
                        0.2f
                    ),
                )

                else -> vortexLightColorScheme().copy(
                    primary = dynamicLightColorScheme.primary.blend(Color.White, 0.25f),
                    onPrimary = dynamicLightColorScheme.onPrimary.blend(Color.White, 0.8f),
                    primaryContainer = dynamicLightColorScheme.primaryContainer.blend(
                        AMOLED_BLACK,
                        0.1f
                    ),
                    onPrimaryContainer = dynamicLightColorScheme.onPrimaryContainer.blend(
                        AMOLED_BLACK,
                        0.2f
                    ),
                    secondary = dynamicLightColorScheme.secondary.blend(Color.White, 0.25f),
                    onSecondary = dynamicLightColorScheme.onSecondary.blend(Color.White, 0.8f),
                    secondaryContainer = dynamicLightColorScheme.secondaryContainer.blend(
                        AMOLED_BLACK,
                        0.1f
                    ),
                    onSecondaryContainer = dynamicLightColorScheme.onSecondaryContainer.blend(
                        AMOLED_BLACK,
                        0.2f
                    )

                )
            }
        }

        darkTheme -> vortexDarkColorScheme(customPrimaryColor)
        else -> vortexLightColorScheme(customPrimaryColor)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        contentCompose(settingsViewModel)
    }
}