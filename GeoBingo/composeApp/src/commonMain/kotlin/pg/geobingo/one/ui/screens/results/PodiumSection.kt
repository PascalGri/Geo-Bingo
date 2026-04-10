package pg.geobingo.one.ui.screens.results

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.data.Player
import pg.geobingo.one.game.GameState
import pg.geobingo.one.i18n.S
import pg.geobingo.one.network.PlayerCosmetics
import pg.geobingo.one.ui.components.CosmeticPlayerName
import pg.geobingo.one.ui.theme.*

@Composable
internal fun DarkPodiumSection(
    ranked: List<Pair<Player, Int>>,
    playerAvatarBytes: Map<String, ByteArray>,
    gameState: GameState,
    cosmeticsByUserId: Map<String, PlayerCosmetics> = emptyMap(),
) {
    val heights = listOf(100.dp, 72.dp, 56.dp)
    val podiumOrder = when (ranked.size) {
        1 -> listOf(ranked[0] to 0)
        2 -> listOf(ranked[1] to 1, ranked[0] to 0)
        else -> listOf(ranked[1] to 1, ranked[0] to 0, ranked[2] to 2)
    }

    // Podium grow animations
    val podiumHeights = podiumOrder.map { (_, rank) ->
        val anim = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(rank * 200L + 300L)
            anim.animateTo(1f, tween(600))
        }
        anim
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom,
    ) {
        podiumOrder.forEachIndexed { i, (playerScore, rank) ->
            val (player, score) = playerScore
            val rankColor = when (rank) {
                0 -> Color(0xFFFBBF24).copy(alpha = 0.7f)
                1 -> Color(0xFF94A3B8).copy(alpha = 0.5f)
                else -> Color(0xFFCD7F32).copy(alpha = 0.5f)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = when (rank) { 0 -> "1." ; 1 -> "2."; else -> "3." },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = rankColor,
                )
                Spacer(Modifier.height(4.dp))
                PlayerAvatarView(player = player, size = 40.dp, fontSize = 16.sp, photoBytes = playerAvatarBytes[player.id])
                Spacer(Modifier.height(4.dp))
                val playerCosmetics = player.userId?.let { cosmeticsByUserId[it] } ?: PlayerCosmetics.NONE
                CosmeticPlayerName(
                    name = player.name,
                    nameEffectId = playerCosmetics.nameEffectId,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    fallbackColor = ColorOnBackground,
                )
                // Animated score counter
                val animatedScore = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    // Stagger: rank 2 (3rd place) first, then rank 1, then rank 0 (1st place)
                    kotlinx.coroutines.delay(rank * 400L + 500L)
                    animatedScore.animateTo(score.toFloat(), tween(1500, easing = FastOutSlowInEasing))
                }
                Text(
                    "${animatedScore.value.toInt()} ${S.current.pointsAbbrev}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = player.color,
                )
                val avgRating = gameState.scoring.getPlayerAverageRating(player.id)
                if (avgRating != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, modifier = Modifier.size(11.dp), tint = Color(0xFFFBBF24))
                        Text(
                            " ${formatRating(avgRating)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFBBF24),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                // Animated podium bar growing from bottom
                val targetHeight = heights[rank]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(targetHeight * podiumHeights[i].value)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(ColorSurfaceVariant),
                )
            }
        }
    }
}
