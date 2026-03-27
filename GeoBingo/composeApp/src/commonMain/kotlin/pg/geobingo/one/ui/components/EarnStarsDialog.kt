package pg.geobingo.one.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.game.state.StarsState
import pg.geobingo.one.i18n.S
import pg.geobingo.one.ui.theme.*

@Composable
fun EarnStarsDialog(
    starsState: StarsState,
    onWatchAd: () -> Unit,
    onDismiss: () -> Unit,
) {
    val remaining = starsState.adsRemainingToday

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.Star, null, tint = ColorWarning, modifier = Modifier.size(22.dp))
                Text(
                    S.current.earnStars,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorOnSurface,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    S.current.earnStarsDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorOnSurfaceVariant,
                )

                // Slots indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(5) { index ->
                        val filled = index < (5 - remaining)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (filled) ColorOutlineVariant
                                    else ColorWarning.copy(alpha = 0.7f)
                                ),
                        )
                    }
                }
                Text(
                    S.current.adsRemaining(remaining),
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant,
                )

                if (starsState.skipCardsCount > 0) {
                    Text(
                        S.current.skipCardsRemaining(starsState.skipCardsCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorPrimary,
                    )
                }
            }
        },
        confirmButton = {
            if (remaining > 0) {
                TextButton(onClick = onWatchAd) {
                    Icon(Icons.Default.PlayCircle, null, modifier = Modifier.size(16.dp), tint = ColorWarning)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${S.current.watchVideo} (+10)",
                        color = ColorWarning,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(S.current.close, color = ColorOnSurfaceVariant)
            }
        },
    )
}
