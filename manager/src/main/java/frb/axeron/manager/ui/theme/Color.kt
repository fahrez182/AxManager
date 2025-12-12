package frb.axeron.manager.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
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

public val AMOLED_BLACK: Color = Color(0xFF000000)

private val basePrimary = Color(0xFFF56A1C)
private val baseSecondary = Color(0xFFDE5900)
private val baseTertiary = Color(0xFFAD6804)
private val baseError = Color(0xFFFF452C)

// Surface logic → tipikal dark mode M3
private val surfaceBase = Color(0xFF151515)
private val surfaceBright = surfaceBase.blend(Color.White, 0.10f)
private val surfaceDim = surfaceBase.blend(Color.Black, 0.10f)

private val surfaceBaseLight = Color(0xFFFFFFFF)
private val surfaceBrightLight = surfaceBaseLight.blend(Color.Black, 0.05f)
private val surfaceDimLight = surfaceBaseLight.blend(Color.Black, 0.10f)


fun vortexDarkColorScheme() = darkColorScheme(

    // PRIMARY
    primary = basePrimary,
    onPrimary = Color.White,
    primaryContainer = basePrimary.blend(surfaceBase, 0.35f),
    onPrimaryContainer = Color.White,
    inversePrimary = basePrimary.blend(Color.White, 0.40f),

    // SECONDARY
    secondary = baseSecondary.blend(Color.White, 0.9f).blend(surfaceBase, 0.4f),
    onSecondary = Color.Black,
    secondaryContainer = baseSecondary.blend(Color.White, 0.55f).blend(surfaceBase, 0.7f),
    onSecondaryContainer = Color.White,

    // TERTIARY
    tertiary = baseTertiary,
    onTertiary = Color.Black,
    tertiaryContainer = baseTertiary.blend(surfaceBase, 0.90f),
    onTertiaryContainer = Color.White,

    // BACKGROUND / SURFACE
    background = surfaceBase,
    onBackground = Color.White,
    surface = surfaceBase,
    onSurface = Color.White,
    surfaceVariant = surfaceBase.blend(Color.White, 0.15f),
    onSurfaceVariant = Color.White.copy(alpha = 0.7f),
    surfaceTint = basePrimary,
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
    primaryFixed = basePrimary,
    primaryFixedDim = basePrimary.blend(Color.Black, 0.25f),
    onPrimaryFixed = Color.White,
    onPrimaryFixedVariant = Color.White.copy(alpha = 0.7f),

    secondaryFixed = baseSecondary,
    secondaryFixedDim = baseSecondary.blend(Color.Black, 0.25f),
    onSecondaryFixed = Color.Black,
    onSecondaryFixedVariant = Color.Black.copy(alpha = 0.7f),

    tertiaryFixed = baseTertiary,
    tertiaryFixedDim = baseTertiary.blend(Color.Black, 0.25f),
    onTertiaryFixed = Color.Black,
    onTertiaryFixedVariant = Color.Black.copy(alpha = 0.7f)
)


fun vortexLightColorScheme() = lightColorScheme(
    // PRIMARY
    primary = basePrimary,
    onPrimary = Color.White,
    primaryContainer = basePrimary.blend(surfaceBaseLight, 0.15f),
    onPrimaryContainer = Color.White,

    // Inverse
    inversePrimary = basePrimary.blend(surfaceBaseLight, 0.40f),

    // SECONDARY
    secondary = baseSecondary.blend(Color.Black, 0.85f).blend(surfaceBaseLight, 0.4f),
    onSecondary = Color.Black,
    secondaryContainer = baseSecondary.blend(Color.Black, 0.45f).blend(surfaceBaseLight, 0.7f),
    onSecondaryContainer = Color.Black,

    // TERTIARY
    tertiary = baseTertiary,
    onTertiary = Color.Black,
    tertiaryContainer = baseTertiary.blend(surfaceBaseLight, 0.25f),
    onTertiaryContainer = Color.Black,

    // BACKGROUND / SURFACE
    background = surfaceBaseLight,
    onBackground = Color.Black,

    surface = surfaceBaseLight,
    onSurface = Color.Black,

    surfaceVariant = surfaceBaseLight.blend(Color.Black, 0.12f),
    onSurfaceVariant = Color.Black.copy(alpha = 0.75f),

    surfaceTint = basePrimary,

    inverseSurface = Color.Black,
    inverseOnSurface = Color.White,

    // ERROR
    error = baseError,
    onError = Color.White,
    errorContainer = baseError.blend(surfaceBaseLight, 0.25f),
    onErrorContainer = Color.White,

    // OUTLINE
    outline = Color(0xFF7F7F7F),
    outlineVariant = Color(0xFFBDBDBD),

    // SCRIM
    scrim = Color(0xFF000000),

    // SURFACE MULTI-LAYER (Material 3 elevations)
    surfaceBright = surfaceBrightLight,
    surfaceDim = surfaceDimLight,
    surfaceContainer = surfaceBaseLight.blend(Color.Black, 0.05f),
    surfaceContainerLow = surfaceBaseLight.blend(Color.Black, 0.03f),
    surfaceContainerLowest = surfaceBaseLight.blend(Color.Black, 0.01f),
    surfaceContainerHigh = surfaceBaseLight.blend(Color.Black, 0.08f),
    surfaceContainerHighest = surfaceBaseLight.blend(Color.Black, 0.12f),

    // FIXED TONES (untuk stabilitas tone light/dark)
    primaryFixed = basePrimary,
    primaryFixedDim = basePrimary.blend(Color.Black, 0.20f),
    onPrimaryFixed = Color.White,
    onPrimaryFixedVariant = Color.White.copy(alpha = 0.8f),

    secondaryFixed = baseSecondary,
    secondaryFixedDim = baseSecondary.blend(Color.Black, 0.20f),
    onSecondaryFixed = Color.Black,
    onSecondaryFixedVariant = Color.Black.copy(alpha = 0.7f),

    tertiaryFixed = baseTertiary,
    tertiaryFixedDim = baseTertiary.blend(Color.Black, 0.20f),
    onTertiaryFixed = Color.Black,
    onTertiaryFixedVariant = Color.Black.copy(alpha = 0.7f)
)
