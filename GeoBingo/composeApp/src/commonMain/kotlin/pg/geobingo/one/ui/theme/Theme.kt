package pg.geobingo.one.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// === Dark Neon Color Palette ===
val ColorPrimary             = Color(0xFFA78BFA) // Violet 400
val ColorOnPrimary           = Color(0xFF1A0060)
val ColorPrimaryContainer    = Color(0xFF2D1B6B) // Deep violet
val ColorOnPrimaryContainer  = Color(0xFFDDD6FE) // Light violet

val ColorSecondary           = Color(0xFF22D3EE) // Cyan 400
val ColorOnSecondary         = Color(0xFF003344)
val ColorSecondaryContainer  = Color(0xFF0C3040) // Deep cyan
val ColorOnSecondaryContainer= Color(0xFFA5F3FC)

val ColorTertiary            = Color(0xFFF472B6) // Pink 400
val ColorOnTertiary          = Color(0xFF3A0020)
val ColorTertiaryContainer   = Color(0xFF5C1035) // Deep pink
val ColorOnTertiaryContainer = Color(0xFFFBCFE8)

val ColorBackground          = Color(0xFF080816) // Ultra dark navy
val ColorSurface             = Color(0xFF111127) // Dark blue-purple
val ColorSurfaceVariant      = Color(0xFF1C1C38) // Medium dark

val ColorOnBackground        = Color(0xFFF0EFFE) // Near white
val ColorOnSurface           = Color(0xFFE5E1FF)
val ColorOnSurfaceVariant    = Color(0xFF8B85B0) // Muted slate

val ColorOutline             = Color(0xFF3D3568)
val ColorOutlineVariant      = Color(0xFF25213E)

val ColorError               = Color(0xFFFF6B6B)
val ColorErrorContainer      = Color(0xFF4A0E0E)
val ColorOnError             = Color(0xFFFFFFFF)
val ColorOnErrorContainer    = Color(0xFFFFCDD2)

// === Gradient Color Sets ===
val GradientPrimary = listOf(
    Color(0xFF8B5CF6), // Violet 500
    Color(0xFF3B82F6), // Blue 500
    Color(0xFF22D3EE), // Cyan 400
)
val GradientHot = listOf(
    Color(0xFFEC4899), // Pink 500
    Color(0xFF8B5CF6), // Violet 500
    Color(0xFF3B82F6), // Blue 500
)
val GradientWarm = listOf(
    Color(0xFFF59E0B), // Amber 500
    Color(0xFFEC4899), // Pink 500
    Color(0xFF8B5CF6), // Violet 500
)
val GradientCool = listOf(
    Color(0xFF22D3EE), // Cyan 400
    Color(0xFF3B82F6), // Blue 500
    Color(0xFF8B5CF6), // Violet 500
)
val GradientGold = listOf(
    Color(0xFFF59E0B), // Amber
    Color(0xFFFBBF24), // Yellow
    Color(0xFFF97316), // Orange
)

private val DarkColorScheme = darkColorScheme(
    primary = ColorPrimary,
    onPrimary = ColorOnPrimary,
    primaryContainer = ColorPrimaryContainer,
    onPrimaryContainer = ColorOnPrimaryContainer,
    secondary = ColorSecondary,
    onSecondary = ColorOnSecondary,
    secondaryContainer = ColorSecondaryContainer,
    onSecondaryContainer = ColorOnSecondaryContainer,
    tertiary = ColorTertiary,
    onTertiary = ColorOnTertiary,
    tertiaryContainer = ColorTertiaryContainer,
    onTertiaryContainer = ColorOnTertiaryContainer,
    error = ColorError,
    errorContainer = ColorErrorContainer,
    onError = ColorOnError,
    onErrorContainer = ColorOnErrorContainer,
    background = ColorBackground,
    onBackground = ColorOnBackground,
    surface = ColorSurface,
    onSurface = ColorOnSurface,
    surfaceVariant = ColorSurfaceVariant,
    onSurfaceVariant = ColorOnSurfaceVariant,
    outline = ColorOutline,
    outlineVariant = ColorOutlineVariant,
    scrim = Color(0xFF000000),
)

@Composable
fun GotchaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
