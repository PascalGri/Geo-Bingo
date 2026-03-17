package pg.geobingo.one.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.data.Player

/**
 * Reusable player avatar: shows emoji if set, otherwise first letter of name on colored circle.
 */
@Composable
fun PlayerAvatarView(
    player: Player,
    size: Dp = 40.dp,
    fontSize: TextUnit = 16.sp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (player.avatar.isEmpty()) player.color else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        if (player.avatar.isNotEmpty()) {
            Text(
                text = player.avatar,
                fontSize = fontSize,
            )
        } else {
            Text(
                text = player.name.take(1).uppercase(),
                color = Color.White,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Same as above but for PlayerDto (lobby phase, before full Player object exists). */
@Composable
fun PlayerAvatarViewRaw(
    name: String,
    color: Color,
    avatar: String = "",
    size: Dp = 40.dp,
    fontSize: TextUnit = 16.sp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (avatar.isEmpty()) color else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        if (avatar.isNotEmpty()) {
            Text(text = avatar, fontSize = fontSize)
        } else {
            Text(
                text = name.take(1).uppercase(),
                color = Color.White,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
