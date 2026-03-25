package pg.geobingo.one.ui.screens.results

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.data.Player
import pg.geobingo.one.ui.theme.*

@Composable
internal fun DarkRankCard(
    rank: Int,
    player: Player,
    score: Int,
    capturedCount: Int,
    totalCategories: Int,
    captures: List<String>,
    isWinner: Boolean,
    speedBonus: Int = 0,
    averageRating: Double? = null,
    photoBytes: ByteArray? = null,
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
                            1 -> Color(0xFF3A3010) // Muted gold bg
                            2 -> Color(0xFF2A2E34) // Muted silver bg
                            3 -> Color(0xFF2E2218) // Muted bronze bg
                            else -> ColorSurfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$rank",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (rank) {
                        1 -> Color(0xFFFBBF24)
                        2 -> Color(0xFF94A3B8)
                        3 -> Color(0xFFCD7F32)
                        else -> ColorOnSurfaceVariant
                    },
                )
            }

            Spacer(Modifier.width(10.dp))

            // Avatar
            PlayerAvatarView(player = player, size = 38.dp, fontSize = 15.sp, photoBytes = photoBytes)

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

            // Score with animated counter
            Column(horizontalAlignment = Alignment.End) {
                val animatedScore = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    // Stagger by rank: lower rank starts later
                    kotlinx.coroutines.delay((rank - 1) * 300L + 800L)
                    animatedScore.animateTo(score.toFloat(), tween(1500, easing = FastOutSlowInEasing))
                }
                Text(
                    "${animatedScore.value.toInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = player.color,
                )
                Text(
                    "Pkt.",
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant,
                )
                if (averageRating != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, modifier = Modifier.size(13.dp), tint = Color(0xFFFBBF24))
                        Text(
                            " ${formatRating(averageRating)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFBBF24),
                        )
                    }
                }
                if (capturedCount > score) {
                    Text(
                        "$capturedCount gefunden",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorOnSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
                if (speedBonus > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, null, modifier = Modifier.size(12.dp), tint = Color(0xFFFBBF24))
                        Text(
                            " +$speedBonus Tempo",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFBBF24),
                        )
                    }
                }
            }
        }
    }
}
