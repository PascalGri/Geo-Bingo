package pg.geobingo.one.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.data.Player
import pg.geobingo.one.game.state.CosmeticsManager
import pg.geobingo.one.ui.components.CosmeticPlayerName
import pg.geobingo.one.ui.components.PlayerTitleBadge

/**
 * Reusable player display: Avatar (with frame) + Name (with effect) + Title badge + optional status text.
 * Automatically applies all equipped cosmetics.
 */
@Composable
fun PlayerCard(
    player: Player,
    modifier: Modifier = Modifier,
    photoBytes: ByteArray? = null,
    statusText: String? = null,
    statusColor: androidx.compose.ui.graphics.Color = ColorOnSurfaceVariant,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerAvatarView(
            player = player,
            size = 38.dp,
            fontSize = 15.sp,
            photoBytes = photoBytes,
        )
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
            if (statusText != null) {
                Text(
                    statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                )
            }
        }
        trailingContent?.invoke()
    }
}
