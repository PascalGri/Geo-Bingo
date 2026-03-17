package pg.geobingo.one.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import pg.geobingo.one.data.Category
import pg.geobingo.one.data.Player
import pg.geobingo.one.data.getCategoryIcon
import pg.geobingo.one.game.*
import pg.geobingo.one.network.GameRealtimeManager
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.platform.rememberPhotoCapturer
import pg.geobingo.one.platform.toImageBitmap
import pg.geobingo.one.ui.theme.*

@Composable
fun GameScreen(gameState: GameState) {
    val scope = rememberCoroutineScope()
    val gameId = gameState.gameId

    var photoTargetPlayerId by remember { mutableStateOf("") }
    var photoTargetCategoryId by remember { mutableStateOf("") }

    val photoCapturer = rememberPhotoCapturer { bytes ->
        if (bytes != null) {
            gameState.addPhoto(photoTargetPlayerId, photoTargetCategoryId, bytes)
            val pid = photoTargetPlayerId
            val cid = photoTargetCategoryId
            if (gameId != null) {
                scope.launch {
                    try { GameRepository.recordCapture(gameId, pid, cid, bytes) } catch (_: Exception) {}
                }
            }
        }
    }

    val realtime = remember(gameId) { gameId?.let { GameRealtimeManager(it) } }

    // Realtime: detect when another player ends the game early
    LaunchedEffect(gameId) {
        if (gameId == null) return@LaunchedEffect
        realtime?.gameUpdates?.collect { game ->
            if (game.status == "voting" && gameState.currentScreen == Screen.GAME) {
                gameState.isGameRunning = false
                gameState.reviewCategoryIndex = game.review_category_index
                gameState.currentScreen = Screen.REVIEW
            }
        }
    }

    LaunchedEffect(gameId) {
        if (gameId == null) return@LaunchedEffect
        try { realtime?.subscribe() } catch (_: Exception) {}
        // Fallback poll every 15s
        while (true) {
            delay(3_000)
            try {
                val game = GameRepository.getGameById(gameId)
                if (game?.status == "voting" && gameState.currentScreen == Screen.GAME) {
                    gameState.isGameRunning = false
                    gameState.reviewCategoryIndex = game.review_category_index
                    gameState.currentScreen = Screen.REVIEW
                }
            } catch (_: Exception) {}
        }
    }

    DisposableEffect(gameId) {
        onDispose { scope.launch { try { realtime?.unsubscribe() } catch (_: Exception) {} } }
    }

    LaunchedEffect(Unit) {
        while (gameState.isGameRunning && gameState.timeRemainingSeconds > 0) {
            delay(1000L)
            if (gameState.isGameRunning) {
                gameState.timeRemainingSeconds--
            }
        }
        if (gameId != null) {
            try { GameRepository.endGameAsVoting(gameId) } catch (_: Exception) {}
        }
        gameState.reviewCategoryIndex = 0
        gameState.currentScreen = Screen.REVIEW
    }

    GameScreenContent(
        gameState = gameState,
        onEndGame = {
            gameState.isGameRunning = false
            scope.launch {
                if (gameId != null) {
                    try { GameRepository.endGameAsVoting(gameId) } catch (_: Exception) {}
                }
                gameState.reviewCategoryIndex = 0
                gameState.currentScreen = Screen.REVIEW
            }
        },
        onCameraClick = { playerId, catId ->
            photoTargetPlayerId = playerId
            photoTargetCategoryId = catId
            photoCapturer.launch()
        },
    )
}

@Composable
fun GameScreenContent(
    gameState: GameState,
    onEndGame: () -> Unit = {},
    onCameraClick: (String, String) -> Unit = { _, _ -> },
) {
    val isLow = gameState.timeRemainingSeconds in 1..60
    val timerColor by animateColorAsState(
        targetValue = if (isLow) ColorError else ColorPrimary,
        animationSpec = tween(500),
    )

    val myPlayer = gameState.players.find { it.id == gameState.myPlayerId }

    Scaffold(containerColor = ColorBackground) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Top bar: timer + player tabs (status only, not interactive)
            Surface(
                color = ColorSurface,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, ColorOutlineVariant),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Timer with gradient when low
                    if (isLow) {
                        AnimatedGradientText(
                            text = gameState.formatTime(gameState.timeRemainingSeconds),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp,
                                fontSize = 40.sp,
                            ),
                            gradientColors = GradientHot,
                            durationMillis = 600,
                        )
                    } else {
                        Text(
                            text = gameState.formatTime(gameState.timeRemainingSeconds),
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = timerColor,
                            letterSpacing = 3.sp,
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Player tabs — read-only status indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        gameState.players.forEach { player ->
                            val isMe = player.id == gameState.myPlayerId
                            val captured = gameState.captures[player.id]?.size ?: 0
                            GamePlayerTab(
                                player = player,
                                isActive = isMe,
                                captureCount = captured,
                                onClick = {},
                            )
                        }
                    }
                }
            }

            // Player info + end button
            if (myPlayer != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(myPlayer.color),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                myPlayer.name.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                myPlayer.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorOnSurface,
                            )
                            val count = gameState.captures[myPlayer.id]?.size ?: 0
                            Text(
                                "$count/${gameState.selectedCategories.size} gefunden",
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorOnSurfaceVariant,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onEndGame,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorError),
                        border = BorderStroke(1.dp, ColorError.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text("Beenden", style = MaterialTheme.typography.labelMedium, color = ColorError)
                    }
                }
                HorizontalDivider(color = ColorOutlineVariant)
            }

            // Bingo grid — always shows only own player
            if (myPlayer != null) {
                val cols = when {
                    gameState.selectedCategories.size <= 4 -> 2
                    gameState.selectedCategories.size <= 9 -> 3
                    else -> 4
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(cols),
                    modifier = Modifier.weight(1f).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(gameState.selectedCategories) { category ->
                        val captured = gameState.isCaptured(myPlayer.id, category.id)
                        val photoBytes = gameState.getPhoto(myPlayer.id, category.id)
                        val thumbnail: ImageBitmap? = remember(photoBytes) { photoBytes?.toImageBitmap() }
                        DarkBingoCategoryCard(
                            category = category,
                            isCaptured = captured,
                            playerColor = myPlayer.color,
                            thumbnail = thumbnail,
                            onCameraClick = { onCameraClick(myPlayer.id, category.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GamePlayerTab(player: Player, isActive: Boolean, captureCount: Int, onClick: () -> Unit) {
    val bg = if (isActive)
        Brush.linearGradient(listOf(player.color.copy(alpha = 0.25f), player.color.copy(alpha = 0.15f)))
    else
        Brush.linearGradient(listOf(ColorSurfaceVariant, ColorSurfaceVariant))

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .then(
                if (isActive) Modifier.border(1.dp, player.color.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                else Modifier
            )
            .clickable { onClick() },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(player.color),
                contentAlignment = Alignment.Center,
            ) {
                Text(player.name.take(1).uppercase(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(5.dp))
            Text(
                player.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isActive) player.color else ColorOnSurfaceVariant,
            )
            if (captureCount > 0) {
                Spacer(Modifier.width(3.dp))
                Text(
                    "($captureCount)",
                    fontSize = 11.sp,
                    color = (if (isActive) player.color else ColorOnSurfaceVariant).copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun DarkBingoCategoryCard(
    category: Category,
    isCaptured: Boolean,
    playerColor: Color,
    thumbnail: ImageBitmap?,
    onCameraClick: () -> Unit,
) {
    var showInfo by remember { mutableStateOf(false) }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            containerColor = ColorSurface,
            icon = {
                Icon(
                    imageVector = getCategoryIcon(category.id),
                    contentDescription = null,
                    tint = if (isCaptured) playerColor else ColorOnSurfaceVariant,
                    modifier = Modifier.size(32.dp),
                )
            },
            title = {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorOnSurface,
                    textAlign = TextAlign.Center,
                )
            },
            text = if (category.description.isNotBlank()) {
                {
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorOnSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else null,
            confirmButton = {
                TextButton(onClick = { showInfo = false; onCameraClick() }) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Foto machen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text("Schließen")
                }
            },
        )
    }

    val containerColor by animateColorAsState(
        targetValue = if (isCaptured) playerColor.copy(alpha = 0.18f) else ColorSurface,
        animationSpec = tween(300),
    )
    val borderColor = if (isCaptured) playerColor.copy(alpha = 0.5f) else ColorOutlineVariant

    Card(
        modifier = Modifier
            .aspectRatio(0.9f)
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onCameraClick() },
                onLongClick = { showInfo = true },
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(if (isCaptured) 1.5.dp else 1.dp, borderColor),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = getCategoryIcon(category.id),
                        contentDescription = category.name,
                        modifier = Modifier.size(28.dp),
                        tint = if (isCaptured) playerColor else ColorOnSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isCaptured) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isCaptured) playerColor else ColorOnSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 13.sp,
                )
                if (isCaptured) {
                    Spacer(Modifier.height(2.dp))
                    Text("✓", fontSize = 10.sp, color = playerColor, fontWeight = FontWeight.Bold)
                }
            }

            // Camera icon (visual indicator, whole card is already clickable)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCaptured)
                            Brush.linearGradient(listOf(playerColor, playerColor))
                        else
                            Brush.linearGradient(listOf(ColorSurfaceVariant, ColorSurfaceVariant))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = if (thumbnail != null) Color.White else ColorOnSurfaceVariant,
                )
            }
        }
    }
}
