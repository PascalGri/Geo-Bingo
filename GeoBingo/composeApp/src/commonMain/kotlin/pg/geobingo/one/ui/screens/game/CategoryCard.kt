package pg.geobingo.one.ui.screens.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.data.Category
import pg.geobingo.one.data.Player
import pg.geobingo.one.data.getCategoryIcon
import pg.geobingo.one.data.getCategoryIconRotation
import pg.geobingo.one.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DarkBingoCategoryCard(
    category: Category,
    isCaptured: Boolean,
    isUploading: Boolean,
    showUploadSuccess: Boolean = false,
    playerColor: Color,
    thumbnail: ImageBitmap?,
    otherCapturingPlayers: List<Player> = emptyList(),
    translucentForCardDesign: Boolean = false,
    onCameraClick: () -> Unit,
) {
    var showInfo by remember { mutableStateOf(false) }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            containerColor = ColorSurface,
            title = { Text(category.name, fontWeight = FontWeight.Bold) },
            text = { Text(category.description) },
            confirmButton = {
                TextButton(onClick = { showInfo = false; onCameraClick() }) {
                    Text(if (isCaptured) "Neu aufnehmen" else "Foto machen")
                }
            },
            dismissButton = { TextButton(onClick = { showInfo = false }) { Text("Schließen") } }
        )
    }

    val containerColor by animateColorAsState(
        when {
            isCaptured -> playerColor.copy(alpha = 0.15f)
            // A card design is equipped → keep the tile semi-transparent so the
            // board gradient behind it shows through as this category's
            // background. Captured tiles are already translucent (above), so
            // this just brings the un-captured ones in line.
            translucentForCardDesign -> ColorSurface.copy(alpha = 0.5f)
            else -> ColorSurface
        }
    )
    val borderColor = if (isCaptured) playerColor.copy(alpha = 0.5f) else ColorOutlineVariant

    // Upload success checkmark scale animation
    val checkScale = remember { Animatable(0f) }
    LaunchedEffect(showUploadSuccess) {
        if (showUploadSuccess) {
            checkScale.animateTo(1.2f, tween(200))
            checkScale.animateTo(1f, tween(150))
        } else {
            checkScale.snapTo(0f)
        }
    }

    val a11yLabel = buildString {
        append(category.name)
        if (isCaptured) append(", captured")
        if (isUploading) append(", uploading")
        if (showUploadSuccess) append(", upload complete")
    }

    Card(
        modifier = Modifier.aspectRatio(0.9f).fillMaxWidth()
            .semantics { contentDescription = a11yLabel }
            .combinedClickable(onClick = { onCameraClick() }, onLongClick = { showInfo = true }),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (thumbnail != null && !isUploading) {
                // Photo fills the entire card
                Image(
                    bitmap = thumbnail,
                    contentDescription = "${category.name} photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                // Gradient scrim at bottom for text readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxSize(0.45f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        ),
                )
                // Category name overlay at bottom
                AutoResizeCategoryName(
                    text = category.name,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                )
                // Checkmark badge top-right
                if (isCaptured) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(playerColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp), tint = Color.White)
                    }
                }
            } else {
                // No photo yet or uploading — show icon/spinner layout
                Column(
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = playerColor)
                    } else {
                        Icon(imageVector = getCategoryIcon(category.id), contentDescription = null, modifier = Modifier.size(26.dp).rotate(getCategoryIconRotation(category.id)), tint = if (isCaptured) playerColor else ColorOnSurfaceVariant)
                    }
                    Spacer(Modifier.height(4.dp))
                    AutoResizeCategoryName(text = category.name, modifier = Modifier.fillMaxWidth())
                    if (isCaptured) Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp), tint = playerColor)
                }
            }
            // Upload success overlay with animated checkmark
            if (showUploadSuccess) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(playerColor.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .graphicsLayer {
                                scaleX = checkScale.value
                                scaleY = checkScale.value
                            },
                        tint = playerColor,
                    )
                }
            }
        }
    }
}

/**
 * Category name that auto-shrinks to fit the bingo tile.
 *
 * A base iPhone (e.g. iPhone 16, 393pt wide) makes each tile only a few points
 * narrower than a Pro (402pt). At a fixed font size that tiny per-tile delta tipped
 * long German category names from one clean line into a crammed, mid-word-broken
 * second line — fine on the wider device, "broken" on the narrower one. Shrinking
 * the font to fit (down to a readable floor) makes every tile render the same way,
 * independent of the exact device width.
 */
@Composable
private fun AutoResizeCategoryName(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    val baseStyle = MaterialTheme.typography.labelSmall
    val minSize = 8f
    // Start at labelSmall's size (11sp); fontSize overrides the style's own size
    // while the style still supplies weight/letter-spacing.
    var fontSize by remember(text) { mutableStateOf(11.sp) }
    var readyToDraw by remember(text) { mutableStateOf(false) }
    Text(
        text = text,
        color = color,
        style = baseStyle,
        fontSize = fontSize,
        lineHeight = (fontSize.value * 1.15f).sp,
        textAlign = TextAlign.Center,
        maxLines = 2,
        softWrap = true,
        overflow = TextOverflow.Clip,
        modifier = modifier.drawWithContent { if (readyToDraw) drawContent() },
        onTextLayout = { result ->
            if (result.hasVisualOverflow && fontSize.value > minSize) {
                fontSize = (fontSize.value * 0.9f).coerceAtLeast(minSize).sp
            } else if (!readyToDraw) {
                readyToDraw = true
            }
        },
    )
}
