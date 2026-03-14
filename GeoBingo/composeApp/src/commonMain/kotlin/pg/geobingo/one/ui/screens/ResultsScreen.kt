package pg.geobingo.one.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.data.Player
import pg.geobingo.one.game.*

@Composable
fun ResultsScreen(gameState: GameState) {
    val ranked = gameState.getRankedPlayers()
    val winner = ranked.firstOrNull()?.first

    val infiniteTransition = rememberInfiniteTransition()
    val trophyScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8F5E9))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1A3A2A), Color(0xFF1B5E20), Color(0xFF2E7D32))
                    )
                )
                .padding(top = 44.dp, bottom = 24.dp, start = 20.dp, end = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "🏆",
                    fontSize = 64.sp,
                    modifier = Modifier.scale(trophyScale)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "ERGEBNISSE",
                    color = Color(0xFFFFB300),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp
                )
                if (winner != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "🥇 ${winner.name} gewinnt!",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Podium (top 3)
        if (ranked.size >= 2) {
            Spacer(Modifier.height(16.dp))
            PodiumSection(ranked = ranked.take(3))
        }

        // Full ranking list
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Alle Ergebnisse",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            ranked.forEachIndexed { index, (player, score) ->
                RankCard(
                    rank = index + 1,
                    player = player,
                    score = score,
                    totalCategories = gameState.selectedCategories.size,
                    captures = gameState.getPlayerCaptures(player.id).map { it.name },
                    isWinner = index == 0
                )
            }
        }

        // Bottom buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { gameState.resetGame() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB300),
                    contentColor = Color(0xFF1A3000)
                ),
                shape = RoundedCornerShape(27.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    "🔄  Nochmal spielen",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun PodiumSection(ranked: List<Pair<Player, Int>>) {
    val medals = listOf("🥇", "🥈", "🥉")
    val heights = listOf(90.dp, 70.dp, 55.dp)
    // Reorder for podium: 2nd, 1st, 3rd
    val podiumOrder = when (ranked.size) {
        1 -> listOf(ranked[0] to 0)
        2 -> listOf(ranked[1] to 1, ranked[0] to 0)
        else -> listOf(ranked[1] to 1, ranked[0] to 0, ranked[2] to 2)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom
    ) {
        podiumOrder.forEach { (playerScore, rank) ->
            val (player, score) = playerScore
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(medals.getOrElse(rank) { "" }, fontSize = 20.sp)
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(player.color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        player.name.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    player.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121),
                    textAlign = TextAlign.Center
                )
                Text(
                    "$score Pkt.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = player.color
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heights[rank])
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            when (rank) {
                                0 -> Color(0xFFFFB300)
                                1 -> Color(0xFFB0BEC5)
                                else -> Color(0xFFCD7F32)
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun RankCard(
    rank: Int,
    player: Player,
    score: Int,
    totalCategories: Int,
    captures: List<String>,
    isWinner: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isWinner) Color(0xFFFFFDE7) else Color.White
        ),
        border = if (isWinner) BorderStroke(2.dp, Color(0xFFFFB300)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isWinner) 5.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        when (rank) {
                            1 -> Color(0xFFFFB300)
                            2 -> Color(0xFFB0BEC5)
                            3 -> Color(0xFFCD7F32)
                            else -> Color(0xFFEEEEEE)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "#$rank" },
                    fontSize = if (rank <= 3) 18.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Player avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(player.color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    player.name.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    player.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
                if (captures.isNotEmpty()) {
                    Text(
                        captures.take(3).joinToString(", ") + if (captures.size > 3) " ..." else "",
                        fontSize = 11.sp,
                        color = Color(0xFF9E9E9E),
                        maxLines = 1
                    )
                }
            }

            // Score
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$score",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = player.color
                )
                Text(
                    "von $totalCategories",
                    fontSize = 10.sp,
                    color = Color(0xFF9E9E9E)
                )
            }
        }
    }
}
