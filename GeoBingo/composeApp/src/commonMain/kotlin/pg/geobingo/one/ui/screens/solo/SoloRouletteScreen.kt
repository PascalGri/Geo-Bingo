package pg.geobingo.one.ui.screens.solo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import pg.geobingo.one.data.CategoryGroup
import pg.geobingo.one.data.ROULETTE_GROUPS
import pg.geobingo.one.data.rouletteCategories
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.game.state.SoloMode
import pg.geobingo.one.i18n.Language
import pg.geobingo.one.i18n.S
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.SoundEffect
import pg.geobingo.one.platform.SoundPlayer
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.platform.play
import pg.geobingo.one.ui.theme.*

private val RouletteGradient = listOf(Color(0xFFA855F7), Color(0xFF22D3EE))

private fun groupLabel(group: CategoryGroup): String =
    if (S.language == Language.DE) group.labelDe else group.labelEn

@Composable
fun SoloRouletteScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val groups = remember { ROULETTE_GROUPS }
    val target = remember { groups.randomOrNull() }
    var displayIndex by remember { mutableStateOf(0) }
    var landed by remember { mutableStateOf(false) }
    val landScale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        if (groups.isEmpty() || target == null) {
            // Defensive: ROULETTE_GROUPS is derived from real data and never
            // empty in practice, but never strand the player on a dead screen.
            nav.resetTo(Screen.HOME)
            return@LaunchedEffect
        }
        // Spin: cycle the reel three full laps plus the offset to the target so
        // the final frame lands exactly on it. Quadratic ease-out on the delay.
        val targetIdx = groups.indexOf(target)
        val steps = groups.size * 3 + targetIdx
        for (i in 0..steps) {
            displayIndex = i % groups.size
            val progress = if (steps == 0) 1f else i.toFloat() / steps
            val d = (45 + progress * progress * 230).toLong()
            if (progress > 0.7f && gameState.ui.soundEnabled) SoundPlayer.play(SoundEffect.CategorySelect)
            delay(d)
        }
        displayIndex = targetIdx
        landed = true
        if (gameState.ui.soundEnabled) SoundPlayer.play(SoundEffect.Success)
        landScale.animateTo(1.15f, tween(180))
        landScale.animateTo(1f, tween(160))
        delay(900L)

        // Hand off to the shared countdown → SoloGameScreen with a roulette set.
        gameState.solo.reset()
        gameState.solo.mode = SoloMode.CATEGORY_ROULETTE
        gameState.solo.isOutdoor = true
        gameState.solo.categoryCount = 5
        gameState.solo.categories = rouletteCategories(target, 5)
        gameState.solo.totalDurationSeconds = 300
        gameState.solo.timeRemainingSeconds = 300
        gameState.solo.playerName = AppSettings.getString("last_player_name", "Player")
        nav.replaceCurrent(Screen.SOLO_START_TRANSITION)
    }

    SystemBackHandler { nav.resetTo(Screen.HOME) }

    Box(
        modifier = Modifier.fillMaxSize().background(ColorBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Casino, null, tint = RouletteGradient.first(), modifier = Modifier.size(26.dp))
                AnimatedGradientText(
                    text = S.current.modeRoulette,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    gradientColors = RouletteGradient,
                )
            }

            // Reel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .graphicsLayer { scaleX = landScale.value; scaleY = landScale.value }
                    .clip(RoundedCornerShape(20.dp))
                    .then(
                        if (landed) Modifier.background(Brush.linearGradient(RouletteGradient))
                        else Modifier.background(ColorSurface)
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(RouletteGradient),
                        shape = RoundedCornerShape(20.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val current = groups.getOrNull(displayIndex)
                Text(
                    text = current?.let { groupLabel(it) } ?: "",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = if (landed) Color.White else ColorOnSurface,
                    textAlign = TextAlign.Center,
                )
            }

            AnimatedVisibility(visible = landed, enter = fadeIn()) {
                Text(
                    S.current.rouletteYourCategory,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = RouletteGradient.first(),
                )
            }
            if (!landed) {
                Text(
                    S.current.rouletteRolling,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ColorOnSurfaceVariant,
                )
            }
        }
    }
}
