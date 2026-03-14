package pg.geobingo.one.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.game.*

@Composable
fun HomeScreen(gameState: GameState) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A3A2A),
                        Color(0xFF1B5E20),
                        Color(0xFF2E7D32),
                        Color(0xFF388E3C),
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(72.dp))

            // Title section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "📍",
                    fontSize = 84.sp,
                    modifier = Modifier.scale(pulseScale)
                )
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "GEO BINGO",
                    color = Color(0xFFFFB300),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 6.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Erkunde die Stadt · Mach Fotos · Gewinne",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(44.dp))

                // Feature list
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeatureRow("👥", "2–8 Spieler in einer Runde")
                    FeatureRow("🗂️", "Kategorien frei wählen")
                    FeatureRow("⏱️", "Timer für die Runde festlegen")
                    FeatureRow("📸", "Fotos in der Stadt machen")
                    FeatureRow("📍", "GPS-Standort wird gespeichert")
                    FeatureRow("🗳️", "Gemeinsam abstimmen & Punkte zählen")
                }
            }

            // Bottom section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 56.dp)
            ) {
                Button(
                    onClick = { gameState.currentScreen = Screen.CREATE_GAME },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB300),
                        contentColor = Color(0xFF1A3000)
                    ),
                    shape = RoundedCornerShape(29.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text(
                        text = "NEUES SPIEL STARTEN",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "GeoBingo v1.0",
                    color = Color.White.copy(alpha = 0.28f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(emoji: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.09f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(text = emoji, fontSize = 18.sp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
