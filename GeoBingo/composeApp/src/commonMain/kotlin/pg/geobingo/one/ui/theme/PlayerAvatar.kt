package pg.geobingo.one.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.data.Player
import pg.geobingo.one.game.state.CosmeticsManager
import pg.geobingo.one.platform.toImageBitmap

/**
 * Reusable player avatar: shows selfie photo if available, otherwise first letter on colored circle.
 * Automatically applies the equipped profile frame cosmetic.
 */
@Composable
fun PlayerAvatarView(
    player: Player,
    size: Dp = 40.dp,
    fontSize: TextUnit = 16.sp,
    modifier: Modifier = Modifier,
    photoBytes: ByteArray? = null,
    showFrame: Boolean = true,
) {
    // Key on equippedRevision so switching cosmetics live updates every
    // avatar on screen. Previously `remember { ... }` without a key cached
    // the initial frame forever and the avatar kept showing the old ring.
    val equipRev by CosmeticsManager.equippedRevision.collectAsState()
    val frame = if (showFrame) remember(equipRev) { CosmeticsManager.getEquippedFrame() } else null
    val hasFrame = frame != null && frame.id != "frame_none" && frame.borderColors.any { it != Color.Transparent }

    var imageBitmap by remember(photoBytes) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(photoBytes) {
        imageBitmap = if (photoBytes != null) withContext(Dispatchers.Default) { photoBytes.toImageBitmap() } else null
    }

    if (hasFrame && frame != null) {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .border(
                    width = frame.borderWidth.dp,
                    brush = Brush.linearGradient(frame.borderColors),
                    shape = CircleShape,
                )
                .padding(frame.borderWidth.dp),
        ) {
            AvatarContent(imageBitmap, player.color, player.name, size, fontSize)
        }
    } else {
        AvatarContent(imageBitmap, player.color, player.name, size, fontSize, modifier)
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
    showFrame: Boolean = true,
) {
    // Key on equippedRevision so switching cosmetics live updates every
    // avatar on screen. Previously `remember { ... }` without a key cached
    // the initial frame forever and the avatar kept showing the old ring.
    val equipRev by CosmeticsManager.equippedRevision.collectAsState()
    val frame = if (showFrame) remember(equipRev) { CosmeticsManager.getEquippedFrame() } else null
    val hasFrame = frame != null && frame.id != "frame_none" && frame.borderColors.any { it != Color.Transparent }

    var imageBitmap by remember(photoBytes) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(photoBytes) {
        imageBitmap = if (photoBytes != null) withContext(Dispatchers.Default) { photoBytes.toImageBitmap() } else null
    }

    if (hasFrame && frame != null) {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .border(
                    width = frame.borderWidth.dp,
                    brush = Brush.linearGradient(frame.borderColors),
                    shape = CircleShape,
                )
                .padding(frame.borderWidth.dp),
        ) {
            AvatarContent(imageBitmap, color, name, size, fontSize)
        }
    } else {
        AvatarContent(imageBitmap, color, name, size, fontSize, modifier)
    }
}

@Composable
private fun AvatarContent(
    imageBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    color: Color,
    name: String,
    size: Dp,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (imageBitmap == null) color else Color.Transparent)
            .semantics { contentDescription = "Avatar $name" },
        contentAlignment = Alignment.Center,
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Foto $name",
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
