package com.frb.axmanager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frb.axmanager.ui.component.blend
import com.frb.axmanager.ui.viewmodel.SettingsViewModel

private val DarkColorScheme = darkColorScheme(
    //Primary
    primary = PRIMARY.blend(AMOLED_BLACK, 0.2f),
    onPrimary = PRIMARY.blend(AMOLED_BLACK, 0.8f),
    primaryContainer = PRIMARY_DARK.blend(AMOLED_BLACK, 0.2f),
    onPrimaryContainer = Color.White.blend(PRIMARY, 0.2f),
    //Secondary
    secondary = SECONDARY,
    onSecondary = SECONDARY.blend(Color.White, 0.5f),
    secondaryContainer = SECONDARY_DARK.blend(AMOLED_BLACK, 0.55f),
    onSecondaryContainer = Color.White.blend(PRIMARY, 0.2f),
    //Tertiary
    tertiary = TERTIARY,
    onTertiary = Color.White.blend(AMOLED_BLACK, 0.2f),
    tertiaryContainer = TERTIARY.blend(AMOLED_BLACK, 0.4f),
    onTertiaryContainer = Color.White.blend(TERTIARY, 0.2f),
    //Surface
    surface = DARK_BLEND.blend(AMOLED_BLACK, 0.7f),
    onSurface = Color.White.blend(AMOLED_BLACK, 0.1f),
    surfaceVariant = DARK_BLEND.blend(AMOLED_BLACK, 0.4f),
    onSurfaceVariant = Color.White.blend(AMOLED_BLACK, 0.3f),
    surfaceTint = Color.White.blend(AMOLED_BLACK, 0.4f),
    surfaceContainer = DARK_BLEND.blend(AMOLED_BLACK, 0.4f),
    surfaceContainerLowest = DARK_BLEND.blend(AMOLED_BLACK, 0.6f),
    surfaceContainerLow = DARK_BLEND.blend(AMOLED_BLACK, 0.5f),
    surfaceContainerHigh = DARK_BLEND.blend(AMOLED_BLACK, 0.45f),
    surfaceContainerHighest = DARK_BLEND.blend(AMOLED_BLACK, 0.4f),
    errorContainer = RED.blend(AMOLED_BLACK, 0.5f),
    outline = DARK_BLEND.blend(Color.White, 0.1f),
    background = DARK_BLEND.blend(AMOLED_BLACK, 0.7f),
)

private val LightColorScheme = lightColorScheme(
    //Primary
    primary = PRIMARY.blend(AMOLED_BLACK, 0.2f),
    onPrimary = PRIMARY.blend(Color.White, 0.8f),
    primaryContainer = PRIMARY_DARK.blend(Color.White, 0.65f),
    onPrimaryContainer = DARK_BLEND,
    //Secondary
    secondary = SECONDARY.blend(AMOLED_BLACK, 0.2f),
    onSecondary = SECONDARY.blend(Color.White, 0.8f),
    secondaryContainer = SECONDARY_DARK.blend(Color.White, 0.55f),
    onSecondaryContainer = DARK_BLEND,
    //Surface
    surface = DARK_BLEND.blend(Color.White, 0.82f),
    surfaceVariant = DARK_BLEND.blend(Color.White, 0.7f),
    onSurfaceVariant = AMOLED_BLACK.blend(Color.White, 0.2f),
    surfaceContainer = DARK_BLEND.blend(Color.White, 0.95f),
    surfaceContainerLowest = DARK_BLEND.blend(Color.White, 0.98f),
    surfaceContainerLow = DARK_BLEND.blend(Color.White, 0.95f),
    surfaceContainerHigh = DARK_BLEND.blend(Color.White, 0.9f),
    surfaceContainerHighest = DARK_BLEND.blend(Color.White, 0.85f),
    errorContainer = RED.blend(Color.White, 0.5f),
    outline = DARK_BLEND.blend(Color.White, 0.1f),
    background = DARK_BLEND.blend(Color.White, 0.82f),
)

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

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val dynamicDarkColorScheme = dynamicDarkColorScheme(context)
            val dynamicLightColorScheme = dynamicLightColorScheme(context)

            when {
                darkTheme -> DarkColorScheme.copy(
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

                else -> LightColorScheme.copy(
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

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        contentCompose(settingsViewModel)
    }
}