package pg.geobingo.one.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pg.geobingo.one.platform.toImageBitmap
import pg.geobingo.one.ui.theme.*

@Composable
fun SelfiePicker(
    avatarBytes: ByteArray?,
    onTakePhoto: () -> Unit,
    onClear: () -> Unit,
) {
    var imageBitmap by remember(avatarBytes) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(avatarBytes) {
        imageBitmap = if (avatarBytes != null) kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { avatarBytes.toImageBitmap() } else null
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (imageBitmap != null) {
            GradientBorderCard(
                modifier = Modifier.size(68.dp),
                cornerRadius = 34.dp,
                borderColors = GradientPrimary,
                backgroundColor = Color.Transparent,
                borderWidth = 2.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .clickable { onTakePhoto() },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = imageBitmap!!,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(22.dp), tint = Color.White)
                    }
                }
            }
        } else {
            AnimatedGradientBox(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .clickable { onTakePhoto() },
                gradientColors = listOf(ColorSurfaceVariant, ColorOutline, ColorSurfaceVariant),
                durationMillis = 3000,
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(28.dp), tint = ColorOnSurfaceVariant)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (imageBitmap != null) "Selfie aufgenommen" else "Selfie aufnehmen",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = ColorOnSurface,
            )
            Text(
                if (imageBitmap != null) "Tippen zum Neuaufnehmen" else "Optional · wird als Profilbild angezeigt",
                style = MaterialTheme.typography.bodySmall,
                color = ColorOnSurfaceVariant,
            )
            if (imageBitmap != null) {
                TextButton(
                    onClick = onClear,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(24.dp),
                ) {
                    Text(
                        "Entfernen",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorError,
                    )
                }
            }
        }
    }
}
