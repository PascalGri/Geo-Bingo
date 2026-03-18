package pg.geobingo.one.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.ui.theme.*

@Composable
fun ResultsTransitionScreen(gameState: GameState) {
    var countdown by remember { mutableStateOf(3) }
    var revealText by remember { mutableStateOf("Wer hat gewonnen?") }

    LaunchedEffect(Unit) {
        delay(1000L)
        countdown = 2
        revealText = "Gleich wisst ihr es..."
        delay(1000L)
        countdown = 1
        revealText = "Und der Gewinner ist..."
        delay(1000L)
        countdown = 0
        gameState.currentScreen = Screen.RESULTS
    }

    val pulseScale by rememberInfiniteTransition().animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
    )

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
                text = "Ergebnis",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                ),
                gradientColors = GradientPrimary,
                durationMillis = 800,
                modifier = Modifier.scale(pulseScale),
            )

            Text(
                text = revealText,
                style = MaterialTheme.typography.bodyLarge,
                color = ColorOnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.scale(pulseScale),
            )

            Spacer(Modifier.height(16.dp))

            AnimatedGradientBox(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp)),
                gradientColors = GradientPrimary,
                durationMillis = 600,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (countdown > 0) "$countdown" else "!",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
