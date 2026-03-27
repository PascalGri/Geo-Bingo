package pg.geobingo.one.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pg.geobingo.one.game.state.StarsState
import pg.geobingo.one.i18n.S
import pg.geobingo.one.ui.theme.*

@Composable
fun RerollDialog(
    title: String,
    starsCost: Int,
    starsState: StarsState,
    onPayStars: () -> Unit,
    onWatchAd: () -> Unit,
    onUseSkipCard: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorSurface,
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ColorOnSurface,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Option 1: Pay Stars
                TextButton(
                    onClick = onPayStars,
                    enabled = starsState.starCount >= starsCost,
                ) {
                    Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp), tint = ColorWarning)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        S.current.rerollCost(starsCost),
                        color = if (starsState.starCount >= starsCost) ColorWarning else ColorOnSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.weight(1f))
                    StarsChip(count = starsState.starCount)
                }

                // Option 2: Watch Ad
                TextButton(onClick = onWatchAd) {
                    Icon(Icons.Default.PlayCircle, null, modifier = Modifier.size(16.dp), tint = ColorPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        S.current.watchVideo,
                        color = ColorPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Option 3: Skip Card (if available)
                if (starsState.skipCardsCount > 0) {
                    TextButton(onClick = onUseSkipCard) {
                        Icon(Icons.Default.Style, null, modifier = Modifier.size(16.dp), tint = ColorSuccess)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${S.current.useSkipCard} (${starsState.skipCardsCount})",
                            color = ColorSuccess,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(S.current.cancel, color = ColorOnSurfaceVariant)
            }
        },
    )
}
