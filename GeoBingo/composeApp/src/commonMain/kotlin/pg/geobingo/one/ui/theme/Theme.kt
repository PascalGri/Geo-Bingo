package pg.geobingo.one.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// === Summer Dark Color Palette (Yellow → Lime → Sky Blue) ===
val ColorPrimary             = Color(0xFF84CC16) // Lime 500
val ColorOnPrimary           = Color(0xFF0D1A00)
val ColorPrimaryContainer    = Color(0xFF1A2E05) // Deep lime
val ColorOnPrimaryContainer  = Color(0xFFD9F99D) // Light lime

val ColorSecondary           = Color(0xFF38BDF8) // Sky 400
val ColorOnSecondary         = Color(0xFF002A40)
val ColorSecondaryContainer  = Color(0xFF083248) // Deep sky
val ColorOnSecondaryContainer= Color(0xFFBAE6FD)

val ColorTertiary            = Color(0xFFFBBF24) // Amber 400
val ColorOnTertiary          = Color(0xFF2A1A00)
val ColorTertiaryContainer   = Color(0xFF3A2800) // Deep amber
val ColorOnTertiaryContainer = Color(0xFFFEF08A)

val ColorBackground          = Color(0xFF060E08) // Ultra dark green-black
val ColorSurface             = Color(0xFF0E1A10) // Dark forest surface
val ColorSurfaceVariant      = Color(0xFF182A1A) // Medium dark green

val ColorOnBackground        = Color(0xFFF0FFF4) // Near white with green tint
val ColorOnSurface           = Color(0xFFE2F5E6)
val ColorOnSurfaceVariant    = Color(0xFF6B9E75) // Muted sage

val ColorOutline             = Color(0xFF2E4D34)
val ColorOutlineVariant      = Color(0xFF1A3020)

val ColorError               = Color(0xFFFF6B6B)
val ColorErrorContainer      = Color(0xFF4A0E0E)
val ColorOnError             = Color(0xFFFFFFFF)
val ColorOnErrorContainer    = Color(0xFFFFCDD2)

// === Gradient Color Sets (Summer: Yellow → Lime → Sky Blue) ===
val GradientPrimary = listOf(
    Color(0xFFFBBF24), // Amber 400
    Color(0xFF84CC16), // Lime 500
    Color(0xFF38BDF8), // Sky 400
)
val GradientHot = listOf(
    Color(0xFFFBBF24), // Amber 400
    Color(0xFFA3E635), // Lime 400
    Color(0xFF4ADE80), // Green 400
)
val GradientWarm = listOf(
    Color(0xFFF97316), // Orange 500
    Color(0xFFFBBF24), // Amber 400
    Color(0xFF84CC16), // Lime 500
)
val GradientCool = listOf(
    Color(0xFF38BDF8), // Sky 400
    Color(0xFF4ADE80), // Green 400
    Color(0xFF84CC16), // Lime 500
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
fun GotchaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
