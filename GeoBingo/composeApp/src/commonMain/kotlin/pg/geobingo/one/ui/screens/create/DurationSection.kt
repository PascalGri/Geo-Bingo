package pg.geobingo.one.ui.screens.create

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import pg.geobingo.one.game.GameConstants
import pg.geobingo.one.ui.theme.ColorOnSurfaceVariant
import pg.geobingo.one.ui.theme.ColorPrimary
import pg.geobingo.one.ui.theme.ColorSurfaceVariant

@Composable
internal fun DurationSection(
    durationMinutes: Float,
    onDurationChange: (Float) -> Unit,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    DarkSectionCard(
        title = "Spielzeit — ${durationMinutes.toInt()} Min",
        modifier = modifier,
        gradientColors = gradientColors,
    ) {
        Slider(
            value = durationMinutes,
            onValueChange = onDurationChange,
            valueRange = GameConstants.MIN_GAME_DURATION_MINUTES.toFloat()..GameConstants.MAX_GAME_DURATION_MINUTES.toFloat(),
            steps = 10,
            colors = SliderDefaults.colors(
                thumbColor = ColorPrimary,
                activeTrackColor = ColorPrimary,
                inactiveTrackColor = ColorSurfaceVariant,
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${GameConstants.MIN_GAME_DURATION_MINUTES} Min", style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant)
            Text("${GameConstants.MAX_GAME_DURATION_MINUTES} Min", style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant)
        }
    }
}
