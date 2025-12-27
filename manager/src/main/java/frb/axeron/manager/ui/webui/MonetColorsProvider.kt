package frb.axeron.manager.ui.webui

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import frb.axeron.api.core.Engine
import frb.axeron.manager.ui.theme.getVortexDarkColorScheme
import frb.axeron.manager.ui.theme.getVortexLightColorScheme
import java.util.concurrent.atomic.AtomicReference

/**
 * @author rifsxd
 * @date 2025/6/2.
 */
object MonetColorsProvider {
    private val colorsCss: AtomicReference<String?> = AtomicReference(null)

    fun getColorsCss(): String {
        return getColorsCss(Engine.application)
    }

    fun getColorsCss(context: Context): String {

        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

        val darkTheme = when (prefs.getInt("app_theme_id", 0)) {
            1 -> true
            2 -> false
            else -> isDark
        }

        val dynamicColor: Boolean = prefs.getBoolean("enable_dynamic_color", false)

        var colorScheme = if (darkTheme) {
            getVortexDarkColorScheme()
        } else {
            getVortexLightColorScheme()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dynamicColor) {
            colorScheme = if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        val monetColors = mapOf(
            "primary" to colorScheme.primary.toArgb().toHex(),
            "onPrimary" to colorScheme.onPrimary.toArgb().toHex(),
            "primaryContainer" to colorScheme.primaryContainer.toArgb().toHex(),
            "onPrimaryContainer" to colorScheme.onPrimaryContainer.toArgb().toHex(),
            "inversePrimary" to colorScheme.inversePrimary.toArgb().toHex(),
            "secondary" to colorScheme.secondary.toArgb().toHex(),
            "onSecondary" to colorScheme.onSecondary.toArgb().toHex(),
            "secondaryContainer" to colorScheme.secondaryContainer.toArgb().toHex(),
            "onSecondaryContainer" to colorScheme.onSecondaryContainer.toArgb().toHex(),
            "tertiary" to colorScheme.tertiary.toArgb().toHex(),
            "onTertiary" to colorScheme.onTertiary.toArgb().toHex(),
            "tertiaryContainer" to colorScheme.tertiaryContainer.toArgb().toHex(),
            "onTertiaryContainer" to colorScheme.onTertiaryContainer.toArgb().toHex(),
            "background" to colorScheme.background.toArgb().toHex(),
            "onBackground" to colorScheme.onBackground.toArgb().toHex(),
            "surface" to colorScheme.surface.toArgb().toHex(),
            "tonalSurface" to colorScheme.surfaceColorAtElevation(1.dp).toArgb().toHex(),
            "onSurface" to colorScheme.onSurface.toArgb().toHex(),
            "surfaceVariant" to colorScheme.surfaceVariant.toArgb().toHex(),
            "onSurfaceVariant" to colorScheme.onSurfaceVariant.toArgb().toHex(),
            "surfaceTint" to colorScheme.surfaceTint.toArgb().toHex(),
            "inverseSurface" to colorScheme.inverseSurface.toArgb().toHex(),
            "inverseOnSurface" to colorScheme.inverseOnSurface.toArgb().toHex(),
            "error" to colorScheme.error.toArgb().toHex(),
            "onError" to colorScheme.onError.toArgb().toHex(),
            "errorContainer" to colorScheme.errorContainer.toArgb().toHex(),
            "onErrorContainer" to colorScheme.onErrorContainer.toArgb().toHex(),
            "outline" to colorScheme.outline.toArgb().toHex(),
            "outlineVariant" to colorScheme.outlineVariant.toArgb().toHex(),
            "scrim" to colorScheme.scrim.toArgb().toHex(),
            "surfaceBright" to colorScheme.surfaceBright.toArgb().toHex(),
            "surfaceDim" to colorScheme.surfaceDim.toArgb().toHex(),
            "surfaceContainer" to colorScheme.surfaceContainer.toArgb().toHex(),
            "surfaceContainerHigh" to colorScheme.surfaceContainerHigh.toArgb().toHex(),
            "surfaceContainerHighest" to colorScheme.surfaceContainerHighest.toArgb().toHex(),
            "surfaceContainerLow" to colorScheme.surfaceContainerLow.toArgb().toHex(),
            "surfaceContainerLowest" to colorScheme.surfaceContainerLowest.toArgb().toHex(),
            "filledTonalButtonContentColor" to colorScheme.onPrimaryContainer.toArgb().toHex(),
            "filledTonalButtonContainerColor" to colorScheme.secondaryContainer.toArgb().toHex(),
            "filledTonalButtonDisabledContentColor" to colorScheme.onSurfaceVariant.toArgb().toHex(),
            "filledTonalButtonDisabledContainerColor" to colorScheme.surfaceVariant.toArgb().toHex(),
            "filledCardContentColor" to colorScheme.onPrimaryContainer.toArgb().toHex(),
            "filledCardContainerColor" to colorScheme.primaryContainer.toArgb().toHex(),
            "filledCardDisabledContentColor" to colorScheme.onSurfaceVariant.toArgb().toHex(),
            "filledCardDisabledContainerColor" to colorScheme.surfaceVariant.toArgb().toHex()
        )
        return monetColors.toCssVars()
    }

    private fun Map<String, String>.toCssVars(): String {
        return buildString {
            append(":root {\n")
            for ((k, v) in this@toCssVars) {
                append("  --$k: $v;\n")
            }
            append("}\n")
        }
    }

    private fun Int.toHex(): String {
        return String.format("#%06X", 0xFFFFFF and this)
    }
}