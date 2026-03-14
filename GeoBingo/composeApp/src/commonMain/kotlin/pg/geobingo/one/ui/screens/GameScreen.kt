package pg.geobingo.one.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pg.geobingo.one.data.Category
import pg.geobingo.one.data.Player
import pg.geobingo.one.game.*
import pg.geobingo.one.platform.rememberPhotoCapturer
import pg.geobingo.one.platform.toImageBitmap

@Composable
fun GameScreen(gameState: GameState) {
    var photoTargetPlayerId by remember { mutableStateOf("") }
    var photoTargetCategoryId by remember { mutableStateOf("") }

    val photoCapturer = rememberPhotoCapturer { bytes ->
        if (bytes != null) {
            gameState.addPhoto(photoTargetPlayerId, photoTargetCategoryId, bytes)
        }
    }

    LaunchedEffect(Unit) {
        while (gameState.isGameRunning && gameState.timeRemainingSeconds > 0) {
            delay(1000L)
            if (gameState.isGameRunning) {
                gameState.timeRemainingSeconds--
                if (gameState.timeRemainingSeconds <= 0) gameState.endGame()
            }
        }
    }

    val isLow = gameState.timeRemainingSeconds in 1..60
    val timerColor by animateColorAsState(
        targetValue = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        animationSpec = tween(500)
    )

    val currentPlayer = gameState.currentPlayer

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Top bar: timer + player tabs
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Timer
                    Text(
                        text = gameState.formatTime(gameState.timeRemainingSeconds),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = timerColor,
                        letterSpacing = 3.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    // Player tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        gameState.players.forEachIndexed { index, player ->
                            val isActive = index == gameState.currentPlayerIndex
                            val captured = gameState.captures[player.id]?.size ?: 0
                            PlayerTab(
                                player = player,
                                isActive = isActive,
                                captureCount = captured,
                                onClick = { gameState.currentPlayerIndex = index }
                            )
                        }
                    }
                }
            }

            // Player info + end button
            if (currentPlayer != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(30.dp).clip(CircleShape).background(currentPlayer.color),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                currentPlayer.name.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                currentPlayer.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            val count = gameState.captures[currentPlayer.id]?.size ?: 0
                            Text(
                                "$count/${gameState.selectedCategories.size} gefunden",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { gameState.endGame() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Beenden", style = MaterialTheme.typography.labelMedium)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            // Bingo grid
            if (currentPlayer != null) {
                val cols = if (gameState.selectedCategories.size <= 9) 3 else 4
                LazyVerticalGrid(
                    columns = GridCells.Fixed(cols),
                    modifier = Modifier.weight(1f).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(gameState.selectedCategories) { category ->
                        val captured = gameState.isCaptured(currentPlayer.id, category.id)
                        val photoBytes = gameState.getPhoto(currentPlayer.id, category.id)
                        val thumbnail: ImageBitmap? = remember(photoBytes) { photoBytes?.toImageBitmap() }
                        BingoCategoryCard(
                            category = category,
                            isCaptured = captured,
                            playerColor = currentPlayer.color,
                            thumbnail = thumbnail,
                            onToggle = { gameState.toggleCapture(currentPlayer.id, category.id) },
                            onCameraClick = {
                                photoTargetPlayerId = currentPlayer.id
                                photoTargetCategoryId = category.id
                                photoCapturer.launch()
                            }
                        )
                    }
                }

                // Navigation row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (gameState.currentPlayerIndex > 0) {
                        OutlinedButton(
                            onClick = { gameState.currentPlayerIndex-- },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text("← Zurück")
                        }
                    }
                    if (gameState.currentPlayerIndex < gameState.players.size - 1) {
                        Button(
                            onClick = { gameState.currentPlayerIndex++ },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Weiter →")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerTab(player: Player, isActive: Boolean, captureCount: Int, onClick: () -> Unit) {
    val containerColor = if (isActive) player.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isActive) player.color else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = if (isActive) BorderStroke(1.dp, player.color.copy(alpha = 0.4f)) else null,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Box(
                modifier = Modifier.size(16.dp).clip(CircleShape).background(player.color),
                contentAlignment = Alignment.Center
            ) {
                Text(player.name.take(1).uppercase(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(5.dp))
            Text(player.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = contentColor)
            if (captureCount > 0) {
                Spacer(Modifier.width(3.dp))
                Text("($captureCount)", fontSize = 11.sp, color = contentColor.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun BingoCategoryCard(
    category: Category,
    isCaptured: Boolean,
    playerColor: Color,
    thumbnail: ImageBitmap?,
    onToggle: () -> Unit,
    onCameraClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (isCaptured) playerColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(300)
    )
    val borderColor = if (isCaptured) playerColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = Modifier.aspectRatio(0.9f).fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCaptured) 2.dp else 1.dp),
        border = BorderStroke(if (isCaptured) 1.5.dp else 1.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main tap area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onToggle() }
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (thumbnail != null) {
                    // Show photo thumbnail
                    Image(
                        bitmap = thumbnail,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(text = category.emoji, fontSize = 24.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isCaptured) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isCaptured) playerColor else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 13.sp
                )
                if (isCaptured) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "✓",
                        fontSize = 10.sp,
                        color = playerColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Camera button overlay (bottom-right)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (thumbnail != null) playerColor else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onCameraClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "📷",
                    fontSize = 10.sp
                )
            }
        }
    }
}
