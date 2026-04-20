package pg.geobingo.one.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pg.geobingo.one.game.GameState
import pg.geobingo.one.i18n.S
import pg.geobingo.one.platform.BillingManager
import pg.geobingo.one.ui.theme.*

private data class QuickStarPkg(val stars: Int, val price: String, val productId: String)

private val QUICK_PACKAGES = listOf(
    QuickStarPkg(50, "0,99 \u20AC", "pg.geobingo.one.stars_50"),
    QuickStarPkg(150, "1,99 \u20AC", "pg.geobingo.one.stars_150"),
    QuickStarPkg(400, "3,99 \u20AC", "pg.geobingo.one.stars_400"),
)

/**
 * A compact in-game shop popup that appears when the player doesn't have enough stars.
 * Does NOT navigate away from the current screen.
 */
@Composable
fun MiniShopPopup(
    gameState: GameState,
    neededStars: Int,
    onDismiss: () -> Unit,
    onPurchased: () -> Unit,
) {
    var purchaseLoading by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Default.Star, null, tint = ColorWarning, modifier = Modifier.size(20.dp))
                    Text(S.current.notEnoughStars, fontWeight = FontWeight.Bold, color = ColorOnSurface)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = ColorOnSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    S.current.needMoreStars(neededStars, gameState.stars.starCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorOnSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                QUICK_PACKAGES.forEach { pkg ->
                    GradientBorderCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = purchaseLoading == null) {
                                purchaseLoading = pkg.productId
                                BillingManager.purchaseProduct(
                                    productId = pkg.productId,
                                    onSuccess = {
                                        gameState.stars.add(pkg.stars)
                                        gameState.ui.pendingReward = pg.geobingo.one.game.state.RewardEvent(
                                            label = pg.geobingo.one.i18n.S.current.starsEarned,
                                            stars = pkg.stars,
                                        )
                                        purchaseLoading = null
                                        onPurchased()
                                    },
                                    onError = { purchaseLoading = null },
                                )
                            },
                        cornerRadius = 12.dp,
                        borderColors = GradientGold,
                        backgroundColor = ColorSurface,
                        borderWidth = 1.dp,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Default.Star, null, tint = ColorWarning, modifier = Modifier.size(16.dp))
                                Text(
                                    "${pkg.stars} ${S.current.stars}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorOnSurface,
                                )
                            }
                            if (purchaseLoading == pkg.productId) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ColorWarning)
                            } else {
                                Text(
                                    pkg.price,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorPrimary,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
    )
}
