package pg.geobingo.one.ui.screens

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.ui.theme.*

@Composable
fun VoteTransitionScreen(gameState: GameState) {
    var countdown by remember { mutableStateOf(3) }

    LaunchedEffect(Unit) {
        repeat(3) {
            delay(1000L)
            countdown--
        }
        gameState.currentScreen = Screen.REVIEW
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
                text = "Abstimmung",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                ),
                gradientColors = GradientPrimary,
            )

            Text(
                text = "Stimmt jetzt über die Fotos ab!",
                style = MaterialTheme.typography.bodyLarge,
                color = ColorOnSurfaceVariant,
                textAlign = TextAlign.Center,
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
                        text = "$countdown",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
