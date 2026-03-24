package pg.geobingo.one.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// === Neon Night Color Palette (Rose → Fuchsia → Purple) ===
val ColorPrimary             = Color(0xFFD946EF) // Fuchsia 500
val ColorOnPrimary           = Color(0xFF1F0026)
val ColorPrimaryContainer    = Color(0xFF330040) // Deep fuchsia
val ColorOnPrimaryContainer  = Color(0xFFF5D0FE) // Light fuchsia

val ColorSecondary           = Color(0xFFA855F7) // Purple 500
val ColorOnSecondary         = Color(0xFF1A0033)
val ColorSecondaryContainer  = Color(0xFF280047) // Deep purple
val ColorOnSecondaryContainer= Color(0xFFE9D5FF)

val ColorTertiary            = Color(0xFFF43F5E) // Rose 500
val ColorOnTertiary          = Color(0xFF2D0010)
val ColorTertiaryContainer   = Color(0xFF420018) // Deep rose
val ColorOnTertiaryContainer = Color(0xFFFFD9E2)

val ColorBackground          = Color(0xFF050508) // Neutral pure dark
val ColorSurface             = Color(0xFF0C0B15) // Very dark purple-black
val ColorSurfaceVariant      = Color(0xFF15132A) // Dark purple

val ColorOnBackground        = Color(0xFFF8F5FF) // Near white, subtle purple tint
val ColorOnSurface           = Color(0xFFF0ECFF) // Bright near-white
val ColorOnSurfaceVariant    = Color(0xFFCBB8F0) // Light purple — readable on dark surfaces

val ColorOutline             = Color(0xFF5A3D8A)
val ColorOutlineVariant      = Color(0xFF2E1F50)

val ColorError               = Color(0xFFFF4D6D)
val ColorErrorContainer      = Color(0xFF4A0017)
val ColorOnError             = Color(0xFFFFFFFF)
val ColorOnErrorContainer    = Color(0xFFFFD9E2)

// === Gradient Color Sets (Neon Night: Rose → Fuchsia → Purple) ===
val GradientPrimary = listOf(
    Color(0xFFF43F5E), // Rose 500
    Color(0xFFD946EF), // Fuchsia 500
    Color(0xFFA855F7), // Purple 500
)
val GradientHot = listOf(
    Color(0xFFFF6B6B), // Coral
    Color(0xFFF43F5E), // Rose 500
    Color(0xFFD946EF), // Fuchsia 500
)
val GradientWarm = listOf(
    Color(0xFFFB7185), // Rose 400
    Color(0xFFE879F9), // Fuchsia 400
    Color(0xFFC026D3), // Fuchsia 700
)
val GradientCool = listOf(
    Color(0xFFA855F7), // Purple 500
    Color(0xFF7C3AED), // Violet 600
    Color(0xFF6366F1), // Indigo 500
)
val GradientGold = listOf(
    Color(0xFFF59E0B), // Amber 500
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
fun KatchItTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
