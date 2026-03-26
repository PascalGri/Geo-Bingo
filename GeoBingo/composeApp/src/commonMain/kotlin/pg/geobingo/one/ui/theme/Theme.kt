package pg.geobingo.one.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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

// === Extended Surface Hierarchy (Material 3 tonal elevation) ===
val ColorSurfaceContainerLowest  = Color(0xFF080714) // Deepest container
val ColorSurfaceContainerLow     = Color(0xFF0E0D1A) // Low container
val ColorSurfaceContainer        = Color(0xFF121120) // Default container
val ColorSurfaceContainerHigh    = Color(0xFF1A1830) // Elevated container
val ColorSurfaceContainerHighest = Color(0xFF221F3A) // Highest container

val ColorOnBackground        = Color(0xFFF8F5FF) // Near white, subtle purple tint
val ColorOnSurface           = Color(0xFFF0ECFF) // Bright near-white
val ColorOnSurfaceVariant    = Color(0xFFCBB8F0) // Light purple — readable on dark surfaces

val ColorOutline             = Color(0xFF5A3D8A)
val ColorOutlineVariant      = Color(0xFF2E1F50)

val ColorError               = Color(0xFFFF4D6D)
val ColorErrorContainer      = Color(0xFF4A0017)
val ColorOnError             = Color(0xFFFFFFFF)
val ColorOnErrorContainer    = Color(0xFFFFD9E2)

// === Semantic Colors (Success / Warning / Info) ===
val ColorSuccess             = Color(0xFF22C55E) // Green 500
val ColorSuccessContainer    = Color(0xFF052E16) // Deep green
val ColorOnSuccess           = Color(0xFFFFFFFF)
val ColorOnSuccessContainer  = Color(0xFFBBF7D0) // Light green

val ColorWarning             = Color(0xFFFBBF24) // Amber 400
val ColorWarningContainer    = Color(0xFF3D2800) // Deep amber
val ColorOnWarning           = Color(0xFF1C1500)
val ColorOnWarningContainer  = Color(0xFFFDE68A) // Light amber

val ColorInfo                = Color(0xFF38BDF8) // Sky 400
val ColorInfoContainer       = Color(0xFF0C2D48) // Deep sky
val ColorOnInfo              = Color(0xFF082F49)
val ColorOnInfoContainer     = Color(0xFFBAE6FD) // Light sky

// === Gradient Color Sets (Neon Night: Rose → Fuchsia → Purple) ===
// === Mode gradients — distinct per mode, ordered warm → cool on the screen ===
val GradientQuickStart = listOf( // Quick Start: Orange
    Color(0xFFFBBF24), // Amber 400
    Color(0xFFF59E0B), // Amber 500
    Color(0xFFF97316), // Orange 500
)
val GradientPrimary = listOf(    // Classic: Pink/Fuchsia
    Color(0xFFF43F5E), // Rose 500
    Color(0xFFD946EF), // Fuchsia 500
    Color(0xFFA855F7), // Purple 500
)
val GradientCool = listOf(       // Blind Bingo: Purple/Indigo
    Color(0xFFA855F7), // Purple 500
    Color(0xFF7C3AED), // Violet 600
    Color(0xFF6366F1), // Indigo 500
)
val GradientWeird = listOf(      // Weird Core: Lime/Green
    Color(0xFF84CC16), // Lime 500
    Color(0xFF22C55E), // Green 500
    Color(0xFF10B981), // Emerald 500
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

// === Central Typography ===
val KatchItTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

// === App-specific text styles (for recurring patterns) ===
object AppTextStyles {
    /** Hero title "KatchIt!" */
    val heroTitle = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 58.sp,
        letterSpacing = (-2).sp,
    )
    /** Game code display (Lobby) */
    val gameCode = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        letterSpacing = 8.sp,
    )
    /** Code input field text */
    val codeInput = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 4.sp,
    )
    /** Timer display */
    val timer = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        letterSpacing = 1.sp,
    )
    /** Countdown number */
    val countdown = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
    )
    /** Score display */
    val score = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
    )
    /** Section header (e.g. settings section title) */
    val sectionHeader = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
    )
}

// === Reduce-motion accessibility ===
val LocalReduceMotion = compositionLocalOf { false }

@Composable
fun KatchItTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}
