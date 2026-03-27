package pg.geobingo.one.ui.screens.solo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.i18n.S
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.util.Analytics

private val SoloGradient = listOf(Color(0xFF22D3EE), Color(0xFF6366F1))

@Composable
fun SoloStartTransitionScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    var countdown by remember { mutableStateOf(3) }
    var revealText by remember { mutableStateOf(S.current.getReady) }
    val feedback = rememberFeedback(gameState)

    LaunchedEffect(Unit) {
        feedback.countdownTick()
        delay(1000L)
        countdown = 2
        feedback.countdownTick()
        delay(1000L)
        countdown = 1
        revealText = S.current.gameStartsNow
        feedback.countdownTick()
        delay(1000L)
        countdown = 0
        feedback.gameStart()
        // Now start the solo game
        gameState.solo.isRunning = true
        Analytics.track(Analytics.SOLO_GAME_STARTED)
        nav.replaceCurrent(Screen.SOLO_GAME)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            AnimatedGradientText(
                text = revealText,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                ),
                gradientColors = SoloGradient,
            )

            Spacer(Modifier.height(16.dp))

            AnimatedGradientBox(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp)),
                gradientColors = SoloGradient,
                durationMillis = 600,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (countdown > 0) "$countdown" else "!",
                        style = AppTextStyles.countdown,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
