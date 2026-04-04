package pg.geobingo.one.ui.screens.results

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
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
import pg.geobingo.one.game.state.CosmeticsManager
import pg.geobingo.one.i18n.S
import pg.geobingo.one.ui.components.CosmeticPlayerName
import pg.geobingo.one.ui.components.PlayerTitleBadge
import pg.geobingo.one.ui.theme.*

/**
 * Per-category breakdown data for a single player/category combination.
 */
internal data class CategoryBreakdownItem(
    val categoryName: String,
    val averageRating: Double?,
    val hasSpeedBonus: Boolean,
    val isControversial: Boolean, // true when vote standard deviation > 1.5
)

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
    categoryBreakdown: List<CategoryBreakdownItem> = emptyList(),
) {
    var expanded by remember { mutableStateOf(false) }

    val cardBg = if (isWinner) ColorPrimaryContainer else ColorSurface
    val borderColor = if (isWinner) ColorPrimary.copy(alpha = 0.5f) else ColorOutlineVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(if (isWinner) 1.5.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CosmeticPlayerName(
                            name = player.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        val titleId = CosmeticsManager.getEquippedTitleId()
                        if (titleId != "title_none") {
                            Spacer(Modifier.width(6.dp))
                            PlayerTitleBadge(titleId = titleId)
                        }
                    }
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

                // Expand/collapse indicator
                if (categoryBreakdown.isNotEmpty()) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = ColorOnSurfaceVariant,
                    )
                }
            }

            // Expandable category breakdown
            AnimatedVisibility(
                visible = expanded && categoryBreakdown.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                ) {
                    HorizontalDivider(
                        color = ColorOutlineVariant.copy(alpha = 0.4f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Text(
                        S.current.categoryBreakdown,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorOnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    categoryBreakdown.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Category name
                            Text(
                                item.categoryName,
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorOnSurface,
                                modifier = Modifier.weight(1f),
                            )

                            // Controversial badge
                            if (item.isControversial) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF442222),
                                    modifier = Modifier.padding(end = 6.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            modifier = Modifier.size(10.dp),
                                            tint = Color(0xFFEF4444),
                                        )
                                        Spacer(Modifier.width(2.dp))
                                        Text(
                                            S.current.controversialPhoto,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFEF4444),
                                            fontSize = 9.sp,
                                        )
                                    }
                                }
                            }

                            // Speed bonus icon
                            if (item.hasSpeedBonus) {
                                Icon(
                                    Icons.Default.Bolt,
                                    contentDescription = S.current.speedBonusLabel,
                                    modifier = Modifier.size(14.dp).padding(end = 2.dp),
                                    tint = Color(0xFFFBBF24),
                                )
                            }

                            // Star rating
                            if (item.averageRating != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val starColor = when {
                                        item.averageRating >= 4.0 -> Color(0xFFFBBF24)
                                        item.averageRating >= 2.5 -> Color(0xFFF59E0B)
                                        else -> Color(0xFF78716C)
                                    }
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = starColor,
                                    )
                                    Spacer(Modifier.width(2.dp))
                                    Text(
                                        formatRating(item.averageRating),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = starColor,
                                    )
                                }
                            } else {
                                Text(
                                    "—",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ColorOnSurfaceVariant.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }

                    // Average rating footer
                    if (averageRating != null) {
                        HorizontalDivider(
                            color = ColorOutlineVariant.copy(alpha = 0.3f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                S.current.averageRating,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorOnSurfaceVariant,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(0xFFFBBF24),
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    formatRating(averageRating),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
