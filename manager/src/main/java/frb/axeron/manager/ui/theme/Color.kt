package frb.axeron.manager.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

val GREEN = Color(0xFF4CAF50)             // Green
val RED = Color(0xFFF44336)               // Red
val YELLOW = Color(0xFFFFEB3B)            // Yellow
val ORANGE = Color(0xFFFF9800)            // Orange

fun Color.blend(other: Color, ratio: Float): Color {
    val inv = 1f - ratio
    return Color(
        red = red * inv + other.red * ratio,
        green = green * inv + other.green * ratio,
        blue = blue * inv + other.blue * ratio,
        alpha = alpha
    )
}


fun Color.saturate(factor: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)

    hsl[1] = (hsl[1] * factor).coerceIn(0f, 10f)

    return Color(ColorUtils.HSLToColor(hsl))
}

fun Color.adjust(
    hueDelta: Float = 0f,
    satMul: Float = 1f,
    lightMul: Float = 1f
): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    hsl[0] = (hsl[0] + hueDelta) % 360f
    hsl[1] = (hsl[1] * satMul).coerceIn(0f, 1f)
    hsl[2] = (hsl[2] * lightMul).coerceIn(0f, 1f)
    return Color(ColorUtils.HSLToColor(hsl))
}


fun Color.toHexString(includeAlpha: Boolean = true): String {
    val argb = toArgb()
    return if (includeAlpha) {
        String.format("#%08X", argb)
    } else {
        String.format("#%06X", argb and 0xFFFFFF)
    }
}


val AMOLED_BLACK: Color = Color(0xFF000000)

val basePrimaryDefault = Color(0xFFFFB487)

//private val baseSecondaryDefault = Color(0xFFDE5900)
//private val baseTertiaryDefault = Color(0xFFAD6804)
private val baseError = Color(0xFFFF452C)

// Surface logic → tipikal dark mode M3


fun getVortexDarkColorScheme(
    customColor: Color = basePrimaryDefault,
): ColorScheme {
    val secondary = customColor.adjust(hueDelta = 20f, satMul = 0.70f)
    val tertiary  = customColor.adjust(hueDelta = -60f, satMul = 0.70f)
    val black = customColor.saturate(5f).blend(Color.Black, 0.7f)
    val white = Color.White.blend(black, 0.10f)
    val surfaceBase = Color(0xFF101010).blend(black, 0.08f)
    val surfaceBright = surfaceBase.blend(Color.White, 0.10f)
    val surfaceDim = surfaceBase.blend(Color.Black, 0.10f)

    return vortexDarkColorScheme(white, black, customColor, secondary, tertiary, surfaceBase, surfaceBright, surfaceDim)
}


private fun vortexDarkColorScheme(
    white: Color,
    black: Color,
    primary: Color,
    secondary: Color,
    tertiary: Color,
    surfaceBase: Color,
    surfaceBright: Color,
    surfaceDim: Color
) = darkColorScheme(

    // PRIMARY
    primary = primary,
    onPrimary = black,
    primaryContainer = primary.blend(black, 0.7f),
    onPrimaryContainer = primary.blend(white, 0.85f),
    inversePrimary = primary.blend(white, 0.40f),

    // SECONDARY
    secondary = secondary.blend(white, 0.8f).blend(surfaceBase, 0.2f),
    onSecondary = black,
    secondaryContainer = secondary.blend(white, 0.35f).blend(surfaceBase, 0.7f),
    onSecondaryContainer = white,

    // TERTIARY
    tertiary = tertiary.blend(white, 0.8f).blend(surfaceBase, 0.2f),
    onTertiary = black,
    tertiaryContainer = tertiary.blend(white, 0.35f).blend(surfaceBase, 0.7f),
    onTertiaryContainer = white,

    // BACKGROUND / SURFACE
    background = surfaceBase,
    onBackground = white,
    surface = surfaceBase,
    onSurface = white,
    surfaceVariant = surfaceBase.blend(white, 0.25f),
    onSurfaceVariant = white.copy(alpha = 0.8f),
    surfaceTint = white,
    inverseSurface = white,
    inverseOnSurface = black,

    // ERROR
    error = baseError,
    onError = white,
    errorContainer = baseError.blend(surfaceBase, 0.4f),
    onErrorContainer = white,

    // OUTLINE
    outline = Color(0xFF909090),
    outlineVariant = Color(0xFF707070),

    // SCRIM
    scrim = Color(0xFF000000),

    // SURFACE MULTI-LAYER (Material 3 elevations)
    surfaceBright = surfaceBright,
    surfaceDim = surfaceDim,
    surfaceContainerLowest = surfaceBase.blend(white, 0.02f),
    surfaceContainerLow = surfaceBase.blend(white, 0.04f),
    surfaceContainer = surfaceBase.blend(white, 0.06f),
    surfaceContainerHigh = surfaceBase.blend(white, 0.09f),
    surfaceContainerHighest = surfaceBase.blend(white, 0.12f),

    // FIXED TONES (M3 requirement untuk stabil light/dark)
    primaryFixed = primary,
    primaryFixedDim = primary.blend(black, 0.25f),
    onPrimaryFixed = white,
    onPrimaryFixedVariant = white.copy(alpha = 0.7f),

    secondaryFixed = secondary,
    secondaryFixedDim = secondary.blend(black, 0.25f),
    onSecondaryFixed = black,
    onSecondaryFixedVariant = black.copy(alpha = 0.7f),

    tertiaryFixed = tertiary,
    tertiaryFixedDim = tertiary.blend(black, 0.25f),
    onTertiaryFixed = black,
    onTertiaryFixedVariant = black.copy(alpha = 0.7f)
)


fun getVortexLightColorScheme(
    customColor: Color = basePrimaryDefault,
): ColorScheme {
    val black = customColor.saturate(5f).blend(Color.Black, 0.7f)
    val primary = customColor.blend(black, 0.5f)
    val secondary = customColor.adjust(hueDelta = 20f, satMul = 0.70f)
    val tertiary  = customColor.adjust(hueDelta = -60f, satMul = 0.70f)
    val white = primary.blend(Color.White, 0.95f)
    val surfaceBaseLight = Color(0xFFFFFFFF).blend(white, 0.02f)
    val surfaceBrightLight = surfaceBaseLight.blend(Color.Black, 0.10f)
    val surfaceDimLight = surfaceBaseLight.blend(Color.White, 0.10f)

    return vortexLightColorScheme(white, black, primary, secondary, tertiary, surfaceBaseLight, surfaceBrightLight, surfaceDimLight)
}

private fun vortexLightColorScheme(
    white: Color,
    black: Color,
    primary: Color,
    secondary: Color,
    tertiary: Color,
    surfaceBase: Color,
    surfaceBright: Color,
    surfaceDim: Color
) = lightColorScheme(
    primary = primary,
    onPrimary = white,
    primaryContainer = primary.blend(white, 0.75f),
    onPrimaryContainer = black,

    inversePrimary = primary.blend(surfaceBase, 0.40f),

    secondary = secondary.blend(black, 0.85f).blend(surfaceBase, 0.4f),
    onSecondary = black,
    secondaryContainer = secondary.blend(black, 0.45f).blend(surfaceBase, 0.7f),
    onSecondaryContainer = black,

    tertiary = tertiary.blend(black, 0.85f).blend(surfaceBase, 0.4f),
    onTertiary = black,
    tertiaryContainer = tertiary.blend(black, 0.45f).blend(surfaceBase, 0.7f),
    onTertiaryContainer = black,

    background = surfaceBase,
    onBackground = black,

    surface = surfaceBase,
    onSurface = black,

    surfaceVariant = surfaceBase.blend(black, 0.4f),
    onSurfaceVariant = black.copy(alpha = 0.75f),

    surfaceTint = primary,

    inverseSurface = black,
    inverseOnSurface = white,

    error = baseError,
    onError = white,
    errorContainer = baseError.blend(surfaceBase, 0.25f),
    onErrorContainer = white,

    outline = Color(0xFF7F7F7F),
    outlineVariant = Color(0xFFBDBDBD),

    // SCRIM
    scrim = Color(0xFF000000),

    // SURFACE MULTI-LAYER (Material 3 elevations)
    surfaceBright = surfaceBright,
    surfaceDim = surfaceDim,
    surfaceContainerLowest = surfaceBase.blend(black, 0.10f),
    surfaceContainerLow = surfaceBase.blend(black, 0.08f),
    surfaceContainer = surfaceBase.blend(black, 0.06f),
    surfaceContainerHigh = surfaceBase.blend(black, 0.04f),
    surfaceContainerHighest = surfaceBase.blend(black, 0.02f),

    primaryFixed = primary,
    primaryFixedDim = primary.blend(black, 0.20f),
    onPrimaryFixed = white,
    onPrimaryFixedVariant = white.copy(alpha = 0.8f),

    secondaryFixed = secondary,
    secondaryFixedDim = secondary.blend(black, 0.20f),
    onSecondaryFixed = black,
    onSecondaryFixedVariant = black.copy(alpha = 0.7f),

    tertiaryFixed = tertiary,
    tertiaryFixedDim = tertiary.blend(black, 0.20f),
    onTertiaryFixed = black,
    onTertiaryFixedVariant = black.copy(alpha = 0.7f)
)
