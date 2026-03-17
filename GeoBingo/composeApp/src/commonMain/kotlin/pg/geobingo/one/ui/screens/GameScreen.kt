package pg.geobingo.one.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.draw.rotate
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
import pg.geobingo.one.data.getCategoryIconRotation
import pg.geobingo.one.game.*
import pg.geobingo.one.network.GameRealtimeManager
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.platform.rememberPhotoCapturer
import pg.geobingo.one.platform.toImageBitmap
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.ui.theme.PlayerAvatarView

@Composable
fun GameScreen(gameState: GameState) {
    val scope = rememberCoroutineScope()
    val gameId = gameState.gameId

    var photoTargetPlayerId by remember { mutableStateOf("") }
    var photoTargetCategoryId by remember { mutableStateOf("") }
    var jokerDialogVisible by remember { mutableStateOf(false) }
    var jokerLabelInput by remember { mutableStateOf("") }

    val photoCapturer = rememberPhotoCapturer { bytes ->
        if (bytes != null) {
            gameState.addPhoto(photoTargetPlayerId, photoTargetCategoryId, bytes)
            val pid = photoTargetPlayerId
            val cid = photoTargetCategoryId
            val isJoker = cid.startsWith("joker_")
            if (gameId != null) {
                scope.launch {
                    try {
                        GameRepository.recordCapture(gameId, pid, cid, bytes)
                        if (isJoker) GameRepository.setJokerLabel(gameId, pid, jokerLabelInput.trim())
                    } catch (_: Exception) {}
                }
            }
            if (isJoker) gameState.myJokerUsed = true
        }
    }

    if (jokerDialogVisible) {
        AlertDialog(
            onDismissRequest = { jokerDialogVisible = false },
            containerColor = ColorSurface,
            title = {
                Text(
                    "🃏 Joker verwenden",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorOnSurface,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Gib ein Thema für dein Joker-Foto ein:",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = jokerLabelInput,
                        onValueChange = { if (it.length <= 40) jokerLabelInput = it },
                        placeholder = { Text("z.B. Rote Tür", color = ColorOnSurfaceVariant) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorPrimary,
                            unfocusedBorderColor = ColorOutline,
                            focusedTextColor = ColorOnSurface,
                            unfocusedTextColor = ColorOnSurface,
                            cursorColor = ColorPrimary,
                        ),
                    )
                }
            },
            confirmButton = {
                GradientButton(
                    text = "Foto machen",
                    onClick = {
                        jokerDialogVisible = false
                        val myId = gameState.myPlayerId ?: return@GradientButton
                        photoTargetPlayerId = myId
                        photoTargetCategoryId = "joker_$myId"
                        // Add a virtual joker category so it shows in the grid
                        val label = jokerLabelInput.trim().ifEmpty { "Joker" }
                        jokerLabelInput = label
                        val existingJoker = gameState.selectedCategories.find { it.id == "joker_$myId" }
                        if (existingJoker == null) {
                            gameState.selectedCategories = gameState.selectedCategories + Category(
                                id = "joker_$myId",
                                name = "🃏 $label",
                                emoji = "joker",
                            )
                        }
                        photoCapturer.launch()
                    },
                    enabled = jokerLabelInput.trim().isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            dismissButton = {
                TextButton(onClick = { jokerDialogVisible = false }) {
                    Text("Abbrechen")
                }
            },
        )
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

    // Realtime: update other players' capture counts live
    LaunchedEffect(gameId) {
        if (gameId == null) return@LaunchedEffect
        realtime?.captureInserts?.collect { capture ->
            if (capture.player_id != gameState.myPlayerId) {
                val current = gameState.captures[capture.player_id] ?: emptySet()
                gameState.captures = gameState.captures + (capture.player_id to current + capture.category_id)
            }
        }
    }

    LaunchedEffect(gameId) {
        if (gameId == null) return@LaunchedEffect
        try { realtime?.subscribe() } catch (_: Exception) {}
        // Fallback poll every 3s
        while (true) {
            delay(3_000)
            try {
                val game = GameRepository.getGameById(gameId)
                if (game?.status == "voting" && gameState.currentScreen == Screen.GAME) {
                    gameState.isGameRunning = false
                    gameState.reviewCategoryIndex = game.review_category_index
                    gameState.currentScreen = Screen.REVIEW
                }
                // Poll end vote count
                if (gameState.isGameRunning) {
                    val count = GameRepository.getEndVoteCount(gameId)
                    gameState.endVoteCount = count
                    if (count >= gameState.players.size && gameState.players.isNotEmpty()) {
                        gameState.isGameRunning = false
                        GameRepository.endGameAsVoting(gameId)
                        gameState.reviewCategoryIndex = 0
                        gameState.currentScreen = Screen.REVIEW
                    }
                }
                // Poll for "all captured" signal from another player
                if (gameState.isGameRunning && !gameState.allCategoriesCaptured && !gameState.finishSignalDetected) {
                    try {
                        if (GameRepository.hasAllCapturedSignal(gameId)) {
                            gameState.finishSignalDetected = true
                        }
                    } catch (_: Exception) {}
                }
                gameState.consecutiveNetworkErrors = 0
            } catch (_: Exception) {
                gameState.consecutiveNetworkErrors++
            }
        }
    }

    DisposableEffect(gameId) {
        onDispose { scope.launch { try { realtime?.unsubscribe() } catch (_: Exception) {} } }
    }

    // Main countdown timer — stops ticking once all categories are captured
    LaunchedEffect(Unit) {
        while (gameState.isGameRunning && gameState.timeRemainingSeconds > 0 && !gameState.allCategoriesCaptured) {
            delay(1000L)
            if (gameState.isGameRunning && !gameState.allCategoriesCaptured) {
                gameState.timeRemainingSeconds--
            }
        }
        // Only end here if the finish-countdown is NOT driving the end
        if (!gameState.allCategoriesCaptured) {
            if (gameId != null) {
                try { GameRepository.endGameAsVoting(gameId) } catch (_: Exception) {}
            }
            gameState.reviewCategoryIndex = 0
            gameState.currentScreen = Screen.REVIEW
        }
    }

    // 30-second finish countdown when current player captures all categories
    var finishCountdownSeconds by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(gameState.allCategoriesCaptured) {
        if (!gameState.allCategoriesCaptured) return@LaunchedEffect
        // Signal to other players that someone finished
        if (gameId != null) {
            try { GameRepository.signalAllCaptured(gameId, gameState.myPlayerId ?: "") } catch (_: Exception) {}
        }
        finishCountdownSeconds = 30
        repeat(30) {
            delay(1000L)
            finishCountdownSeconds = (finishCountdownSeconds ?: 1) - 1
        }
        gameState.isGameRunning = false
        if (gameId != null) {
            try { GameRepository.endGameAsVoting(gameId) } catch (_: Exception) {}
        }
        gameState.reviewCategoryIndex = 0
        gameState.currentScreen = Screen.REVIEW
    }

    GameScreenContent(
        gameState = gameState,
        finishCountdownSeconds = finishCountdownSeconds,
        onJokerClick = { jokerDialogVisible = true },
        onVoteToEnd = {
            scope.launch {
                if (gameId != null && !gameState.hasVotedToEnd) {
                    gameState.hasVotedToEnd = true
                    try {
                        GameRepository.submitEndVote(gameId, gameState.myPlayerId ?: return@launch)
                        val count = GameRepository.getEndVoteCount(gameId)
                        gameState.endVoteCount = count
                        if (count >= gameState.players.size && gameState.players.isNotEmpty()) {
                            gameState.isGameRunning = false
                            GameRepository.endGameAsVoting(gameId)
                            gameState.reviewCategoryIndex = 0
                            gameState.currentScreen = Screen.REVIEW
                        }
                    } catch (_: Exception) {
                        gameState.hasVotedToEnd = false
                    }
                }
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
    finishCountdownSeconds: Int? = null,
    onJokerClick: () -> Unit = {},
    onVoteToEnd: () -> Unit = {},
    onCameraClick: (String, String) -> Unit = { _, _ -> },
) {
    val isLow = gameState.timeRemainingSeconds in 1..60
    val timerColor by animateColorAsState(
        targetValue = if (isLow) ColorError else ColorPrimary,
        animationSpec = tween(500),
    )

    val myPlayer = gameState.players.find { it.id == gameState.myPlayerId }

    Scaffold(containerColor = ColorBackground) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        Column(modifier = Modifier.fillMaxSize()) {

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
                                photoBytes = gameState.playerAvatarBytes[player.id],
                                onClick = {},
                            )
                        }
                    }
                }
            }

            // Offline banner
            if (gameState.consecutiveNetworkErrors >= 3) {
                Surface(modifier = Modifier.fillMaxWidth(), color = ColorError.copy(alpha = 0.15f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            "Keine Verbindung – versuche erneut zu verbinden…",
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorError,
                        )
                    }
                }
            }

            // Finish signal banner for players who haven't finished yet
            if (gameState.finishSignalDetected && !gameState.allCategoriesCaptured) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ColorPrimaryContainer,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        AnimatedGradientText(
                            text = "Ein Spieler hat alle Fotos! Beeil dich! \uD83C\uDFC3",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            gradientColors = GradientPrimary,
                            durationMillis = 1000,
                        )
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
                        PlayerAvatarView(player = myPlayer, size = 32.dp, fontSize = 13.sp, photoBytes = gameState.playerAvatarBytes[myPlayer.id])
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
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (gameState.jokerMode && !gameState.myJokerUsed) {
                            OutlinedButton(
                                onClick = onJokerClick,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorPrimary),
                                border = BorderStroke(1.dp, ColorPrimary.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    "🃏 Joker",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ColorPrimary,
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onVoteToEnd,
                            enabled = !gameState.hasVotedToEnd,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (gameState.hasVotedToEnd) ColorOnSurfaceVariant else ColorError
                            ),
                            border = BorderStroke(1.dp, if (gameState.hasVotedToEnd) ColorOutlineVariant else ColorError.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text(
                                if (gameState.hasVotedToEnd) "Abgestimmt ✓" else "Beenden",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (gameState.hasVotedToEnd) ColorOnSurfaceVariant else ColorError,
                            )
                        }
                        if (gameState.endVoteCount > 0 || gameState.hasVotedToEnd) {
                            Text(
                                "${gameState.endVoteCount}/${gameState.players.size} wollen beenden",
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorOnSurfaceVariant,
                            )
                        }
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
                        val otherCapturers = remember(gameState.captures) {
                            gameState.players.filter { player ->
                                player.id != myPlayer.id && (gameState.captures[player.id]?.contains(category.id) == true)
                            }
                        }
                        DarkBingoCategoryCard(
                            category = category,
                            isCaptured = captured,
                            playerColor = myPlayer.color,
                            thumbnail = thumbnail,
                            otherCapturingPlayers = otherCapturers,
                            onCameraClick = { onCameraClick(myPlayer.id, category.id) },
                        )
                    }
                }
            }
        } // end Column

        // Finish countdown overlay
        if (finishCountdownSeconds != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ColorSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.padding(32.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AnimatedGradientText(
                            text = "Alle Fotos gemacht!",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            gradientColors = GradientPrimary,
                            durationMillis = 1500,
                        )
                        Text(
                            "Spiel endet in",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorOnSurfaceVariant,
                        )
                        AnimatedGradientText(
                            text = "$finishCountdownSeconds",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                            ),
                            gradientColors = if (finishCountdownSeconds <= 10) GradientHot else GradientPrimary,
                            durationMillis = 800,
                        )
                        Text(
                            "Anderen Spielern noch Zeit lassen",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorOnSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        } // end outer Box
    }
}

@Composable
private fun GamePlayerTab(player: Player, isActive: Boolean, captureCount: Int, photoBytes: ByteArray? = null, onClick: () -> Unit) {
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
            PlayerAvatarView(player = player, size = 18.dp, fontSize = 8.sp, photoBytes = photoBytes)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DarkBingoCategoryCard(
    category: Category,
    isCaptured: Boolean,
    playerColor: Color,
    thumbnail: ImageBitmap?,
    otherCapturingPlayers: List<Player> = emptyList(),
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
                    modifier = Modifier.size(32.dp).rotate(getCategoryIconRotation(category.id)),
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
                    Text(if (isCaptured) "Neu aufnehmen" else "Foto machen")
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
                        modifier = Modifier.size(28.dp).rotate(getCategoryIconRotation(category.id)),
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

            // Other capturers dots - bottom left
            if (otherCapturingPlayers.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    otherCapturingPlayers.take(4).forEach { player ->
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(player.color)
                        )
                    }
                    if (otherCapturingPlayers.size > 4) {
                        Text(
                            "+${otherCapturingPlayers.size - 4}",
                            fontSize = 6.sp,
                            color = ColorOnSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
