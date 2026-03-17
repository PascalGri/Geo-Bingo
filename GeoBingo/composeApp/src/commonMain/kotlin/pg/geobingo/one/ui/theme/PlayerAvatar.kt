package pg.geobingo.one.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.data.Player
import pg.geobingo.one.platform.toImageBitmap

/**
 * Reusable player avatar: shows selfie photo if available, otherwise first letter on colored circle.
 */
@Composable
fun PlayerAvatarView(
    player: Player,
    size: Dp = 40.dp,
    fontSize: TextUnit = 16.sp,
    modifier: Modifier = Modifier,
    photoBytes: ByteArray? = null,
) {
    val imageBitmap = remember(photoBytes) { photoBytes?.toImageBitmap() }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (imageBitmap == null) player.color else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
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
    photoBytes: ByteArray? = null,
) {
    val imageBitmap = remember(photoBytes) { photoBytes?.toImageBitmap() }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (imageBitmap == null) color else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
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
