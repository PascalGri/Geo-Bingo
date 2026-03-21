package pg.geobingo.one.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import pg.geobingo.one.ui.theme.rememberFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import pg.geobingo.one.data.Category
import pg.geobingo.one.data.Player
import pg.geobingo.one.data.getCategoryIcon
import pg.geobingo.one.data.getCategoryIconRotation
import pg.geobingo.one.game.*
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.VoteKeys
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.platform.RequestLocationPermission
import pg.geobingo.one.platform.getCurrentLocation
import pg.geobingo.one.platform.rememberPhotoCapturer
import pg.geobingo.one.platform.toImageBitmap
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.ui.theme.PlayerAvatarView
import pg.geobingo.one.ui.theme.Spacing

@Composable
fun GameScreen(gameState: GameState) {
    // Request location permission once when entering the game
    RequestLocationPermission()

    val scope = rememberCoroutineScope()
    val gameId = gameState.gameId
    val realtime = gameId?.let { gameState.realtime }

    var photoTargetPlayerId by remember { mutableStateOf("") }
    var photoTargetCategoryId by remember { mutableStateOf("") }
    var jokerDialogVisible by remember { mutableStateOf(false) }
    var jokerLabelInput by remember { mutableStateOf("") }
    var uploadSuccessCategory by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current
    val feedback = rememberFeedback(gameState)

    val photoCapturer = rememberPhotoCapturer { bytes ->
        if (bytes != null) {
            gameState.addPhoto(photoTargetPlayerId, photoTargetCategoryId, bytes)
            val pid = photoTargetPlayerId
            val cid = photoTargetCategoryId
            val isJoker = cid.startsWith("joker_")

            // Save locally for game history
            if (gameId != null) {
                try { LocalPhotoStore.savePhoto(gameId, pid, cid, bytes) } catch (_: Exception) {}
            }

            gameState.uploadingCategories = gameState.uploadingCategories + cid

            if (gameId != null) {
                scope.launch {
                    // Get GPS location (best-effort, don't block on failure)
                    val location = try { getCurrentLocation() } catch (_: Exception) { null }

                    // Upload capture with retries
                    var attempt = 0
                    var captureSuccess = false
                    while (attempt < 3 && !captureSuccess) {
                        try {
                            if (attempt > 0) delay(2_000L * attempt)
                            GameRepository.recordCapture(gameId, pid, cid, bytes, latitude = location?.latitude, longitude = location?.longitude)
                            captureSuccess = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                            attempt++
                        }
                    }
                    // Save joker label separately (so capture retry doesn't block it)
                    if (isJoker) {
                        var jokerAttempt = 0
                        while (jokerAttempt < 3) {
                            try {
                                if (jokerAttempt > 0) delay(1_000L * jokerAttempt)
                                GameRepository.setJokerLabel(gameId, pid, jokerLabelInput.trim())
                                break
                            } catch (e: Exception) {
                                e.printStackTrace()
                                jokerAttempt++
                            }
                        }
                    }
                    if (captureSuccess) {
                        feedback.capture()
                        uploadSuccessCategory = cid
                        delay(1500)
                        uploadSuccessCategory = null
                    }
                    gameState.uploadingCategories = gameState.uploadingCategories - cid
                }
            } else {
                gameState.uploadingCategories = gameState.uploadingCategories - cid
            }
            if (isJoker) gameState.myJokerUsed = true
        }
    }

    if (jokerDialogVisible) {
        AlertDialog(
            onDismissRequest = { jokerDialogVisible = false },
            containerColor = ColorSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Style, null, modifier = Modifier.size(20.dp), tint = ColorPrimary)
                    Text(
                        "Joker verwenden",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorOnSurface,
                    )
                }
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
                        val label = jokerLabelInput.trim().ifEmpty { "Joker" }
                        jokerLabelInput = label
                        val existingJoker = gameState.selectedCategories.find { it.id == "joker_$myId" }
                        if (existingJoker == null) {
                            gameState.selectedCategories = gameState.selectedCategories + Category(
                                id = "joker_$myId",
                                name = label,
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

    // Realtime: game status and end-votes
    LaunchedEffect(gameId) {
        if (realtime == null) return@LaunchedEffect
        realtime.gameUpdates.collect { game ->
            if (game.status == "voting" && gameState.currentScreen == Screen.GAME) {
                gameState.isGameRunning = false
                gameState.reviewCategoryIndex = game.review_category_index
                gameState.currentScreen = Screen.VOTE_TRANSITION
            }
        }
    }

    LaunchedEffect(gameId) {
        if (realtime == null || gameId == null) return@LaunchedEffect
        val gid = gameId
        realtime.voteSubmissionInserts.collect { submission ->
            if (submission.category_id == VoteKeys.END_VOTE) {
                try {
                    val count = GameRepository.getEndVoteCount(gid)
                    gameState.endVoteCount = count
                    if (count >= gameState.players.size && gameState.players.isNotEmpty()) {
                        gameState.isGameRunning = false
                        GameRepository.endGameAsVoting(gameId)
                        gameState.reviewCategoryIndex = 0
                        gameState.currentScreen = Screen.VOTE_TRANSITION
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
            // Also detect finish signal via realtime
            if (submission.category_id == VoteKeys.ALL_CAPTURED && !gameState.finishSignalDetected && !gameState.allCategoriesCaptured) {
                gameState.finishSignalDetected = true
            }
        }
    }

    // Realtime: update other players' capture counts
    LaunchedEffect(gameId) {
        if (realtime == null) return@LaunchedEffect
        realtime.captureInserts.collect { capture ->
            if (capture.player_id != gameState.myPlayerId) {
                val current = gameState.captures[capture.player_id] ?: emptySet()
                gameState.captures = gameState.captures + (capture.player_id to current + capture.category_id)
            }
        }
    }

    // Fallback polling
    LaunchedEffect(gameId) {
        if (gameId == null) return@LaunchedEffect
        while (true) {
            delay(3_000)
            try {
                val game = GameRepository.getGameById(gameId)
                if (game?.status == "voting" && gameState.currentScreen == Screen.GAME) {
                    gameState.isGameRunning = false
                    gameState.reviewCategoryIndex = game.review_category_index
                    gameState.currentScreen = Screen.VOTE_TRANSITION
                }
                if (gameState.isGameRunning) {
                    // Poll other players' captures
                    try {
                        val allCaptures = GameRepository.getCaptures(gameId)
                        val updatedCaptures = gameState.captures.toMutableMap()
                        allCaptures.forEach { capture ->
                            val current = updatedCaptures[capture.player_id] ?: emptySet()
                            updatedCaptures[capture.player_id] = current + capture.category_id
                        }
                        gameState.captures = updatedCaptures
                    } catch (_: Exception) {}

                    val count = GameRepository.getEndVoteCount(gameId)
                    gameState.endVoteCount = count
                    if (count >= gameState.players.size && gameState.players.isNotEmpty()) {
                        gameState.isGameRunning = false
                        GameRepository.endGameAsVoting(gameId)
                        gameState.reviewCategoryIndex = 0
                        gameState.currentScreen = Screen.VOTE_TRANSITION
                    }

                    // Check if any player completed all categories (for countdown)
                    if (!gameState.finishSignalDetected && !gameState.allCategoriesCaptured) {
                        try {
                            if (GameRepository.hasAllCapturedSignal(gameId)) {
                                gameState.finishSignalDetected = true
                            }
                        } catch (_: Exception) {}
                    }
                }
                gameState.consecutiveNetworkErrors = 0
            } catch (e: Exception) {
                e.printStackTrace()
                gameState.consecutiveNetworkErrors++
            }
        }
    }

    // Download avatar photos for all players not yet cached or tried
    LaunchedEffect(gameState.players) {
        gameState.players
            .filter { it.id !in gameState.playerAvatarBytes && it.id !in gameState.triedAvatarDownloads }
            .forEach { player ->
                scope.launch {
                    gameState.triedAvatarDownloads = gameState.triedAvatarDownloads + player.id
                    val bytes = GameRepository.downloadAvatarPhoto(player.id)
                    if (bytes != null) {
                        gameState.playerAvatarBytes = gameState.playerAvatarBytes + (player.id to bytes)
                    }
                }
            }
    }

    // Retry avatar downloads for players still missing avatars (handles iOS failures)
    LaunchedEffect(gameId) {
        var retries = 0
        while (retries < 5) {
            delay(5_000)
            val missing = gameState.players.filter { it.id !in gameState.playerAvatarBytes }
            if (missing.isEmpty()) break
            missing.forEach { player ->
                scope.launch {
                    val bytes = GameRepository.downloadAvatarPhoto(player.id)
                    if (bytes != null) {
                        gameState.playerAvatarBytes = gameState.playerAvatarBytes + (player.id to bytes)
                    }
                }
            }
            retries++
        }
    }

    // Timer logic
    LaunchedEffect(Unit) {
        while (gameState.isGameRunning && gameState.timeRemainingSeconds > 0 && !gameState.allCategoriesCaptured) {
            delay(1000L)
            if (gameState.isGameRunning && !gameState.allCategoriesCaptured) {
                gameState.timeRemainingSeconds--
                if (gameState.timeRemainingSeconds in 1..10) feedback.countdownTick()
            }
        }
        if (!gameState.allCategoriesCaptured && gameState.timeRemainingSeconds <= 0 && gameState.isGameRunning) {
            feedback.gameEnd()
            if (gameId != null) {
                try { GameRepository.endGameAsVoting(gameId) } catch (e: Exception) { e.printStackTrace() }
            }
            gameState.reviewCategoryIndex = 0
            gameState.currentScreen = Screen.VOTE_TRANSITION
        }
    }

    // Detect remote "all captured" signal from other players
    LaunchedEffect(gameId) {
        if (gameId == null) return@LaunchedEffect
        while (gameState.isGameRunning && !gameState.allCategoriesCaptured && !gameState.finishSignalDetected) {
            delay(2_000)
            try {
                if (GameRepository.hasAllCapturedSignal(gameId)) {
                    gameState.finishSignalDetected = true
                }
            } catch (_: Exception) {}
        }
    }

    // Finish countdown – triggered by local completion OR remote signal
    var finishCountdownSeconds by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        // Wait until either the local player or a remote player signals "all captured"
        snapshotFlow { gameState.allCategoriesCaptured || gameState.finishSignalDetected }
            .first { it }
        // Signal to server if we were the one who captured all
        if (gameState.allCategoriesCaptured && gameId != null) {
            val pid = gameState.myPlayerId ?: ""
            for (attempt in 0 until 3) {
                try {
                    if (attempt > 0) delay(1_000L * attempt)
                    GameRepository.signalAllCaptured(gameId, pid)
                    break
                } catch (_: Exception) {}
            }
        }
        finishCountdownSeconds = 30
        repeat(30) {
            delay(1000L)
            val remaining = (finishCountdownSeconds ?: 1) - 1
            finishCountdownSeconds = remaining
            if (remaining in 1..10) feedback.countdownTick()
        }
        if (gameState.isGameRunning) {
            gameState.isGameRunning = false
            if (gameId != null) {
                try { GameRepository.endGameAsVoting(gameId) } catch (_: Exception) {}
            }
            gameState.reviewCategoryIndex = 0
            gameState.currentScreen = Screen.VOTE_TRANSITION
        }
    }

    GameScreenContent(
        gameState = gameState,
        finishCountdownSeconds = finishCountdownSeconds,
        uploadSuccessCategory = uploadSuccessCategory,
        onJokerClick = { jokerDialogVisible = true },
        onVoteToEnd = {
            scope.launch {
                if (gameId != null && !gameState.hasVotedToEnd) {
                    val pid = gameState.myPlayerId ?: return@launch
                    gameState.hasVotedToEnd = true
                    gameState.endVoteCount = gameState.endVoteCount + 1
                    var attempt = 0
                    var success = false
                    while (attempt < 3) {
                        try {
                            if (attempt > 0) delay(1_000L * attempt)
                            GameRepository.submitEndVote(gameId, pid)
                            success = true
                            break
                        } catch (e: Exception) {
                            e.printStackTrace()
                            attempt++
                        }
                    }
                    if (!success) {
                        gameState.hasVotedToEnd = false
                        gameState.endVoteCount = (gameState.endVoteCount - 1).coerceAtLeast(0)
                    } else {
                        // Check if all players voted to end
                        val count = gameState.endVoteCount
                        if (count >= gameState.players.size && gameState.players.isNotEmpty()) {
                            gameState.isGameRunning = false
                            try { GameRepository.endGameAsVoting(gameId) } catch (_: Exception) {}
                            gameState.reviewCategoryIndex = 0
                            gameState.currentScreen = Screen.VOTE_TRANSITION
                        }
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
    uploadSuccessCategory: String? = null,
    onJokerClick: () -> Unit = {},
    onVoteToEnd: () -> Unit = {},
    onCameraClick: (String, String) -> Unit = { _, _ -> },
) {
    // Fade-in animation
    val contentAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) { contentAlpha.animateTo(1f, tween(400)) }

    val isLow = gameState.timeRemainingSeconds in 1..60
    val isCritical = gameState.timeRemainingSeconds in 1..30
    val timerColor by animateColorAsState(
        targetValue = if (isLow) ColorError else ColorPrimary,
        animationSpec = tween(500),
    )

    // Timer pulse animation when <30s
    val pulseTransition = rememberInfiniteTransition(label = "timerPulse")
    val pulseDuration = when {
        gameState.timeRemainingSeconds in 1..10 -> 500
        gameState.timeRemainingSeconds in 11..20 -> 1000
        else -> 1500
    }
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = if (isCritical) 0.4f else 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isCritical) 1.03f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    val myPlayer = gameState.players.find { it.id == gameState.myPlayerId }

    Scaffold(containerColor = ColorBackground) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).graphicsLayer { alpha = contentAlpha.value }) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar: timer
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
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            },
                        ) {
                            // Pulsing glow behind timer when critical
                            if (isCritical) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ColorError.copy(alpha = pulseAlpha)),
                                )
                            }
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
                        }

                        if (finishCountdownSeconds != null) {
                            Spacer(Modifier.height(10.dp))
                            AnimatedGradientBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)),
                                gradientColors = GradientHot,
                                durationMillis = 800,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        if (gameState.allCategoriesCaptured) "Du hast alle gefunden!"
                                        else "Jemand hat alle gefunden!",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        "Noch ${finishCountdownSeconds}s",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            gameState.players.forEach { player ->
                                val isMe = player.id == gameState.myPlayerId
                                val captured = gameState.captures[player.id]?.size ?: 0
                                GamePlayerTab(
                                    player = player,
                                    isActive = isMe,
                                    captureCount = captured,
                                    totalCategories = gameState.selectedCategories.size,
                                    photoBytes = gameState.playerAvatarBytes[player.id],
                                    onClick = {},
                                )
                            }
                        }
                    }
                }

                // Player info + controls
                if (myPlayer != null) {
                    val myCount = gameState.captures[myPlayer.id]?.size ?: 0
                    val totalCats = gameState.selectedCategories.size
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(Spacing.screenHorizontal),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Animated progress ring around avatar
                            val ringProgress = remember { Animatable(0f) }
                            val targetProgress = if (totalCats > 0) myCount.toFloat() / totalCats else 0f
                            LaunchedEffect(myCount) {
                                ringProgress.animateTo(targetProgress, tween(500))
                            }
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { ringProgress.value },
                                    modifier = Modifier.size(40.dp),
                                    color = myPlayer.color,
                                    trackColor = ColorSurfaceVariant,
                                    strokeWidth = 3.dp,
                                )
                                PlayerAvatarView(player = myPlayer, size = 30.dp, fontSize = 12.sp, photoBytes = gameState.playerAvatarBytes[myPlayer.id])
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(myPlayer.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = ColorOnSurface)
                                Text("$myCount/$totalCats gefunden", style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (gameState.jokerMode && !gameState.myJokerUsed) {
                                OutlinedButton(
                                    onClick = onJokerClick,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorPrimary),
                                    border = BorderStroke(1.dp, ColorPrimary.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                ) {
                                    Icon(Icons.Default.Style, null, modifier = Modifier.size(14.dp), tint = ColorPrimary)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Joker", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            OutlinedButton(
                                onClick = onVoteToEnd,
                                enabled = !gameState.hasVotedToEnd,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorError),
                                border = BorderStroke(1.dp, ColorError.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                val needed = gameState.players.size
                                Text(
                                    if (gameState.hasVotedToEnd) "Abgestimmt (${gameState.endVoteCount}/$needed)"
                                    else "Vorzeitig beenden (${gameState.endVoteCount}/$needed)",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = ColorOutlineVariant)
                }

                // Bingo grid
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
                            val otherCapturers = gameState.players.filter { p ->
                                p.id != myPlayer.id && (gameState.captures[p.id]?.contains(category.id) == true)
                            }
                            val isUploading = category.id in gameState.uploadingCategories
                            val showUploadSuccess = uploadSuccessCategory == category.id
                            DarkBingoCategoryCard(
                                category = category,
                                isCaptured = captured,
                                isUploading = isUploading,
                                showUploadSuccess = showUploadSuccess,
                                playerColor = myPlayer.color,
                                thumbnail = thumbnail,
                                otherCapturingPlayers = otherCapturers,
                                onCameraClick = { onCameraClick(myPlayer.id, category.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GamePlayerTab(player: Player, isActive: Boolean, captureCount: Int, totalCategories: Int, photoBytes: ByteArray? = null, onClick: () -> Unit) {
    val bg = if (isActive)
        Brush.linearGradient(listOf(player.color.copy(alpha = 0.2f), player.color.copy(alpha = 0.1f)))
    else
        Brush.linearGradient(listOf(ColorSurfaceVariant, ColorSurfaceVariant))

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .then(if (isActive) Modifier.border(1.dp, player.color.copy(alpha = 0.4f), RoundedCornerShape(16.dp)) else Modifier)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlayerAvatarView(player = player, size = 18.dp, fontSize = 8.sp, photoBytes = photoBytes)
            Spacer(Modifier.width(6.dp))
            Text(player.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (isActive) ColorOnSurface else ColorOnSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            Text(
                "$captureCount/$totalCategories",
                fontSize = 11.sp,
                color = if (captureCount >= totalCategories) ColorPrimary
                    else (if (isActive) player.color else ColorOnSurfaceVariant).copy(alpha = 0.8f),
                fontWeight = if (captureCount >= totalCategories) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DarkBingoCategoryCard(
    category: Category,
    isCaptured: Boolean,
    isUploading: Boolean,
    showUploadSuccess: Boolean = false,
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

    val containerColor by animateColorAsState(if (isCaptured) playerColor.copy(alpha = 0.15f) else ColorSurface)
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

    Card(
        modifier = Modifier.aspectRatio(0.9f).fillMaxWidth().combinedClickable(onClick = { onCameraClick() }, onLongClick = { showInfo = true }),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (thumbnail != null && !isUploading) {
                // Photo fills the entire card
                Image(
                    bitmap = thumbnail,
                    contentDescription = null,
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
                Text(
                    category.name,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 12.sp,
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
                    Text(category.name, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 12.sp)
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
