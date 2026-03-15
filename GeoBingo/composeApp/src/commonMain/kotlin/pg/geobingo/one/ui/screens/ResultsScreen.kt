package pg.geobingo.one.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.data.Player
import pg.geobingo.one.game.*
import pg.geobingo.one.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(gameState: GameState) {
    val ranked = gameState.getRankedPlayers()
    val winner = ranked.firstOrNull()?.first

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = "Ergebnisse",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        gradientColors = GradientGold,
                        durationMillis = 2500,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = ColorSurface) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    GradientButton(
                        text = "Nochmal spielen",
                        onClick = { gameState.resetGame() },
                        modifier = Modifier.fillMaxWidth(),
                        gradientColors = GradientPrimary,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Replay,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White,
                            )
                        },
                    )
                }
            }
        },
        containerColor = ColorBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Winner banner
            if (winner != null) {
                AnimatedGradientBox(
                    modifier = Modifier.fillMaxWidth(),
                    gradientColors = GradientGold,
                    durationMillis = 4000,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = Color.White,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            winner.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            "gewinnt! 🎉",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            }

            // Podium
            if (ranked.size >= 2) {
                Spacer(Modifier.height(20.dp))
                DarkPodiumSection(ranked = ranked.take(3))
            }

            // Full ranking
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Alle Ergebnisse",
                    style = MaterialTheme.typography.labelLarge,
                    color = ColorOnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                ranked.forEachIndexed { index, (player, score) ->
                    DarkRankCard(
                        rank = index + 1,
                        player = player,
                        score = score,
                        totalCategories = gameState.selectedCategories.size,
                        captures = gameState.getPlayerCaptures(player.id).map { it.name },
                        isWinner = index == 0,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DarkPodiumSection(ranked: List<Pair<Player, Int>>) {
    val heights = listOf(100.dp, 72.dp, 56.dp)
    val podiumOrder = when (ranked.size) {
        1 -> listOf(ranked[0] to 0)
        2 -> listOf(ranked[1] to 1, ranked[0] to 0)
        else -> listOf(ranked[1] to 1, ranked[0] to 0, ranked[2] to 2)
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom,
    ) {
        podiumOrder.forEach { (playerScore, rank) ->
            val (player, score) = playerScore
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = when (rank) { 0 -> "1." ; 1 -> "2."; else -> "3." },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (rank) {
                        0 -> Color(0xFFFBBF24) // Gold
                        1 -> Color(0xFF94A3B8) // Silver
                        else -> Color(0xFFCD7F32) // Bronze
                    },
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(player.color),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(player.name.take(1).uppercase(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    player.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = ColorOnBackground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "$score Pkt.",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = player.color,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heights[rank])
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            when (rank) {
                                0 -> Brush.linearGradient(GradientGold)
                                1 -> Brush.linearGradient(listOf(Color(0xFF64748B), Color(0xFF94A3B8)))
                                else -> Brush.linearGradient(listOf(Color(0xFF92400E), Color(0xFFCD7F32)))
                            }
                        ),
                )
            }
        }
    }
}

@Composable
private fun DarkRankCard(
    rank: Int,
    player: Player,
    score: Int,
    totalCategories: Int,
    captures: List<String>,
    isWinner: Boolean,
) {
    val cardBg = if (isWinner) ColorPrimaryContainer else ColorSurface
    val borderColor = if (isWinner) ColorPrimary.copy(alpha = 0.5f) else ColorOutlineVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(if (isWinner) 1.5.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Rank number
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        when (rank) {
                            1 -> Brush.linearGradient(GradientGold)
                            2 -> Brush.linearGradient(listOf(Color(0xFF64748B), Color(0xFF94A3B8)))
                            3 -> Brush.linearGradient(listOf(Color(0xFF92400E), Color(0xFFCD7F32)))
                            else -> Brush.linearGradient(listOf(ColorSurfaceVariant, ColorSurfaceVariant))
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$rank",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            Spacer(Modifier.width(10.dp))

            // Avatar
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).background(player.color),
                contentAlignment = Alignment.Center,
            ) {
                Text(player.name.take(1).uppercase(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    player.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorOnSurface,
                )
                if (captures.isNotEmpty()) {
                    Text(
                        captures.take(3).joinToString(", ") + if (captures.size > 3) " …" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorOnSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            // Score
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$score",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = player.color,
                )
                Text(
                    "/ $totalCategories",
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant,
                )
            }
        }
    }
}
