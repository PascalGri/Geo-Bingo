package pg.geobingo.one.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.BillingManager
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.ui.theme.ColorPrimary
import pg.geobingo.one.ui.theme.PlayerAvatarViewRaw

@Composable
fun TopBarStarsAndProfile(
    gameState: GameState,
    onNavigate: (Screen) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(end = 4.dp),
    ) {
        StarsChip(
            count = gameState.stars.starCount,
            onClick = { onNavigate(if (BillingManager.isBillingSupported) Screen.SHOP else Screen.SETTINGS) },
        )
        val name = AppSettings.getString("last_player_name", "")
        val avatarBytes = LocalPhotoStore.loadAvatar("profile")
        PlayerAvatarViewRaw(
            name = name.ifBlank { "?" },
            color = ColorPrimary,
            size = 28.dp,
            photoBytes = avatarBytes,
            modifier = Modifier.clickable { onNavigate(Screen.SETTINGS) },
        )
    }
}
