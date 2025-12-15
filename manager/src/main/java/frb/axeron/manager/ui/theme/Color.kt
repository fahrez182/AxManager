package frb.axeron.manager.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

//
//import androidx.compose.ui.graphics.Color
//import frb.axeron.manager.ui.component.blend
//
//val PRIMARY = Color(0xFF95CDF8)
//val PRIMARY_DARK = Color(0xFF004B6F)
//
//
//val SECONDARY = Color(0xFF9ECDFF)
//val SECONDARY_DARK = Color(0xFF5484A6)
//
//val TERTIARY = Color(0xFF91FFFC)
//
//val TERTIARY_DARK = Color(0xFF00726F)
//
//val AMOLED_BLACK = Color(0xFF000000)
//
//val DARK_BLEND = Color(0xFF363636).blend(PRIMARY_DARK, 0.1f)
//
//
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

//fun Color.contrastingColor(): Color {
//    // luminance check
//    val luminance = (0.299 * red + 0.587 * green + 0.114 * blue)
//    return if (luminance > 0.5) Color.Black else Color.White
//}

fun Color.toHexString(includeAlpha: Boolean = true): String {
    val argb = toArgb()
    return if (includeAlpha) {
        String.format("#%08X", argb)
    } else {
        String.format("#%06X", argb and 0xFFFFFF)
    }
}


val AMOLED_BLACK: Color = Color(0xFF000000)

val basePrimaryDefault = Color(0xFFF56A1C)
//private val baseSecondaryDefault = Color(0xFFDE5900)
//private val baseTertiaryDefault = Color(0xFFAD6804)
private val baseError = Color(0xFFFF452C)

// Surface logic → tipikal dark mode M3
private val surfaceBase = Color(0xFF151515)
private val surfaceBright = surfaceBase.blend(Color.White, 0.10f)
private val surfaceDim = surfaceBase.blend(Color.Black, 0.10f)

private val surfaceBaseLight = Color(0xFFFFFFFF)
private val surfaceBrightLight = surfaceBaseLight.blend(Color.Black, 0.05f)
private val surfaceDimLight = surfaceBaseLight.blend(Color.Black, 0.10f)


fun vortexDarkColorScheme(customPrimary: Color = basePrimaryDefault) = darkColorScheme(

    // PRIMARY
    primary = customPrimary,
    onPrimary = customPrimary.blend(Color.Black, 0.80f),
    primaryContainer = customPrimary.blend(surfaceBase, 0.35f),
    onPrimaryContainer = customPrimary.blend(Color.White, 0.85f),
    inversePrimary = customPrimary.blend(Color.White, 0.40f),

    // SECONDARY
    secondary = customPrimary.blend(Color.White, 0.9f).blend(surfaceBase, 0.4f),
    onSecondary = Color.Black,
    secondaryContainer = customPrimary.blend(Color.White, 0.55f).blend(surfaceBase, 0.7f),
    onSecondaryContainer = Color.White,

    // TERTIARY
    tertiary = customPrimary.blend(Color.White, 0.9f).blend(surfaceBase, 0.4f),
    onTertiary = Color.Black,
    tertiaryContainer = customPrimary.blend(Color.White, 0.55f).blend(surfaceBase, 0.7f),
    onTertiaryContainer = Color.White,

    // BACKGROUND / SURFACE
    background = surfaceBase,
    onBackground = Color.White,
    surface = surfaceBase,
    onSurface = Color.White,
    surfaceVariant = surfaceBase.blend(Color.White, 0.15f),
    onSurfaceVariant = Color.White.copy(alpha = 0.7f),
    surfaceTint = customPrimary,
    inverseSurface = Color.White,
    inverseOnSurface = Color.Black,

    // ERROR
    error = baseError,
    onError = Color.White,
    errorContainer = baseError.blend(surfaceBase, 0.4f),
    onErrorContainer = Color.White,

    // OUTLINE
    outline = Color(0xFF4F4F4F),
    outlineVariant = Color(0xFF2A2A2A),

    // SCRIM
    scrim = Color(0xFF000000),

    // SURFACE MULTI-LAYER (Material 3 elevations)
    surfaceBright = surfaceBright,
    surfaceDim = surfaceDim,
    surfaceContainer = surfaceBase.blend(Color.White, 0.05f),
    surfaceContainerLow = surfaceBase.blend(Color.White, 0.06f),
    surfaceContainerLowest = surfaceBase.blend(Color.White, 0.04f),
    surfaceContainerHigh = surfaceBase.blend(Color.White, 0.08f),
    surfaceContainerHighest = surfaceBase.blend(Color.White, 0.12f),

    // FIXED TONES (M3 requirement untuk stabil light/dark)
    primaryFixed = customPrimary,
    primaryFixedDim = customPrimary.blend(Color.Black, 0.25f),
    onPrimaryFixed = Color.White,
    onPrimaryFixedVariant = Color.White.copy(alpha = 0.7f),

    secondaryFixed = customPrimary,
    secondaryFixedDim = customPrimary.blend(Color.Black, 0.25f),
    onSecondaryFixed = Color.Black,
    onSecondaryFixedVariant = Color.Black.copy(alpha = 0.7f),

    tertiaryFixed = customPrimary,
    tertiaryFixedDim = customPrimary.blend(Color.Black, 0.25f),
    onTertiaryFixed = Color.Black,
    onTertiaryFixedVariant = Color.Black.copy(alpha = 0.7f)
)


fun vortexLightColorScheme(customPrimary: Color = basePrimaryDefault) = lightColorScheme(
    primary = customPrimary,
    onPrimary = customPrimary.blend(Color.White, 0.80f),
    primaryContainer = customPrimary.blend(surfaceBaseLight, 0.45f),
    onPrimaryContainer = customPrimary.blend(Color.Black, 0.85f),

    inversePrimary = customPrimary.blend(surfaceBaseLight, 0.40f),

    secondary = customPrimary.blend(Color.Black, 0.85f).blend(surfaceBaseLight, 0.4f),
    onSecondary = Color.Black,
    secondaryContainer = customPrimary.blend(Color.Black, 0.45f).blend(surfaceBaseLight, 0.7f),
    onSecondaryContainer = Color.Black,

    tertiary = customPrimary.blend(Color.Black, 0.85f).blend(surfaceBaseLight, 0.4f),
    onTertiary = Color.Black,
    tertiaryContainer = customPrimary.blend(Color.Black, 0.45f).blend(surfaceBaseLight, 0.7f),
    onTertiaryContainer = Color.Black,

    background = surfaceBaseLight,
    onBackground = Color.Black,

    surface = surfaceBaseLight,
    onSurface = Color.Black,

    surfaceVariant = surfaceBaseLight.blend(Color.Black, 0.12f),
    onSurfaceVariant = Color.Black.copy(alpha = 0.75f),

    surfaceTint = customPrimary,

    inverseSurface = Color.Black,
    inverseOnSurface = Color.White,

    error = baseError,
    onError = Color.White,
    errorContainer = baseError.blend(surfaceBaseLight, 0.25f),
    onErrorContainer = Color.White,

    outline = Color(0xFF7F7F7F),
    outlineVariant = Color(0xFFBDBDBD),

    // SCRIM
    scrim = Color(0xFF000000),

    // SURFACE MULTI-LAYER (Material 3 elevations)
    surfaceBright = surfaceBrightLight,
    surfaceDim = surfaceDimLight,
    surfaceContainer = surfaceBaseLight.blend(Color.Black, 0.05f),
    surfaceContainerLow = surfaceBaseLight.blend(Color.Black, 0.06f),
    surfaceContainerLowest = surfaceBaseLight.blend(Color.Black, 0.04f),
    surfaceContainerHigh = surfaceBaseLight.blend(Color.Black, 0.08f),
    surfaceContainerHighest = surfaceBaseLight.blend(Color.Black, 0.12f),

    primaryFixed = customPrimary,
    primaryFixedDim = customPrimary.blend(Color.Black, 0.20f),
    onPrimaryFixed = Color.White,
    onPrimaryFixedVariant = Color.White.copy(alpha = 0.8f),

    secondaryFixed = customPrimary,
    secondaryFixedDim = customPrimary.blend(Color.Black, 0.20f),
    onSecondaryFixed = Color.Black,
    onSecondaryFixedVariant = Color.Black.copy(alpha = 0.7f),

    tertiaryFixed = customPrimary,
    tertiaryFixedDim = customPrimary.blend(Color.Black, 0.20f),
    onTertiaryFixed = Color.Black,
    onTertiaryFixedVariant = Color.Black.copy(alpha = 0.7f)
)
