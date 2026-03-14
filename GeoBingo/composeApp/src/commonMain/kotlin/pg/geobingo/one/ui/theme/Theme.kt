package pg.geobingo.one.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ColorPrimary          = Color(0xFF3A6B52)
val ColorOnPrimary        = Color(0xFFFFFFFF)
val ColorPrimaryContainer = Color(0xFFCDEADB)
val ColorOnPrimaryContainer = Color(0xFF1A3D2B)

val ColorSecondary          = Color(0xFF5A7268)
val ColorOnSecondary        = Color(0xFFFFFFFF)
val ColorSecondaryContainer = Color(0xFFDCEDE4)

val ColorBackground    = Color(0xFFF4F6F5)
val ColorSurface       = Color(0xFFFFFFFF)
val ColorSurfaceVariant = Color(0xFFECF0EE)
val ColorOnSurface     = Color(0xFF1A2420)
val ColorOnSurfaceVariant = Color(0xFF627168)
val ColorOutline       = Color(0xFFB8C9C0)
val ColorOutlineVariant = Color(0xFFDAE5DE)

val ColorError          = Color(0xFFBA1A1A)
val ColorErrorContainer = Color(0xFFFFDAD6)

private val LightColorScheme = lightColorScheme(
    primary = ColorPrimary,
    onPrimary = ColorOnPrimary,
    primaryContainer = ColorPrimaryContainer,
    onPrimaryContainer = ColorOnPrimaryContainer,
    secondary = ColorSecondary,
    onSecondary = ColorOnSecondary,
    secondaryContainer = ColorSecondaryContainer,
    onSecondaryContainer = ColorOnSurface,
    tertiary = Color(0xFF5E6E97),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDDE1FF),
    onTertiaryContainer = Color(0xFF111F52),
    error = ColorError,
    errorContainer = ColorErrorContainer,
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),
    background = ColorBackground,
    onBackground = ColorOnSurface,
    surface = ColorSurface,
    onSurface = ColorOnSurface,
    surfaceVariant = ColorSurfaceVariant,
    onSurfaceVariant = ColorOnSurfaceVariant,
    outline = ColorOutline,
    outlineVariant = ColorOutlineVariant,
    scrim = Color(0xFF000000),
)

@Composable
fun GeoBingoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
