package pg.geobingo.one.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GreenPrimary = Color(0xFF2E7D32)
val GreenDark = Color(0xFF1B5E20)
val GreenLight = Color(0xFFA5D6A7)
val GreenContainer = Color(0xFFE8F5E9)
val AmberAccent = Color(0xFFFFB300)
val OrangeAccent = Color(0xFFE65100)
val OrangeContainer = Color(0xFFFFE0B2)
val GradientStart = Color(0xFF1A3A2A)
val GradientMid = Color(0xFF1B5E20)
val GradientEnd = Color(0xFF388E3C)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenLight,
    onPrimaryContainer = GreenDark,
    secondary = OrangeAccent,
    onSecondary = Color.White,
    secondaryContainer = OrangeContainer,
    onSecondaryContainer = Color(0xFF3E1E00),
    tertiary = Color(0xFF0288D1),
    onTertiary = Color.White,
    background = GreenContainer,
    onBackground = Color(0xFF1C1C1C),
    surface = Color.White,
    onSurface = Color(0xFF1C1C1C),
    surfaceVariant = Color(0xFFF1F8E9),
    onSurfaceVariant = Color(0xFF444444),
    error = Color(0xFFB71C1C),
    onError = Color.White,
)

@Composable
fun GeoBingoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
