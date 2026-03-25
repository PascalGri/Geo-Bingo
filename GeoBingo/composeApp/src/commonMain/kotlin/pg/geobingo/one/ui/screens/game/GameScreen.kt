package pg.geobingo.one.ui.screens.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pg.geobingo.one.data.Category
import pg.geobingo.one.game.GameConstants
import pg.geobingo.one.game.*
import pg.geobingo.one.util.AppLogger
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.VoteKeys
import pg.geobingo.one.network.withRetry
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.platform.RequestLocationPermission
import pg.geobingo.one.platform.getCurrentLocation
import pg.geobingo.one.platform.rememberPhotoCapturer
import pg.geobingo.one.i18n.S
import pg.geobingo.one.ui.theme.*

@Composable
fun GameScreen(gameState: GameState) {
    // Request location permission once when entering the game
    RequestLocationPermission()

    val scope = rememberCoroutineScope()
    val gameId = gameState.session.gameId
    val realtime = gameId?.let { gameState.realtime }

    // Ensure SyncManager is running for this game
    val sync = gameId?.let { remember(it) { gameState.ensureSyncManager(it, scope) } }

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
                try { LocalPhotoStore.savePhoto(gameId, pid, cid, bytes) } catch (e: Exception) { AppLogger.d("Game", "Local photo save failed", e) }
            }

            gameState.photo.startUpload(cid)

            if (gameId != null) {
                scope.launch {
                    // Get GPS location (best-effort, don't block on failure)
                    val location = try { getCurrentLocation() } catch (e: Exception) { AppLogger.d("Game", "Location unavailable", e); null }

                    // Upload capture with retries
                    val captureSuccess = try {
                        withRetry { GameRepository.recordCapture(gameId, pid, cid, bytes, latitude = location?.latitude, longitude = location?.longitude) }
                        true
                    } catch (e: Exception) {
                        AppLogger.e("Game", "Capture upload failed for $cid", e)
                        gameState.ui.pendingToast = S.current.uploadFailed
                        false
                    }
                    // Save joker label separately (so capture retry doesn't block it)
                    if (isJoker) {
                        try { withRetry { GameRepository.setJokerLabel(gameId, pid, jokerLabelInput.trim()) } } catch (e: Exception) { AppLogger.w("Game", "Joker label save failed", e) }
                    }
                    if (captureSuccess) {
                        feedback.capture()
                        uploadSuccessCategory = cid
                        delay(GameConstants.UPLOAD_SUCCESS_TOAST_MS)
                        uploadSuccessCategory = null
                    }
                    gameState.photo.finishUpload(cid)
                }
            } else {
                gameState.photo.finishUpload(cid)
            }
            if (isJoker) gameState.joker.myJokerUsed = true
        }
    }

    val modeGradient = when (gameState.session.gameMode) {
        GameMode.CLASSIC     -> GradientPrimary
        GameMode.BLIND_BINGO -> GradientCool
        GameMode.WEIRD_CORE  -> GradientWeird
        GameMode.QUICK_START -> GradientQuickStart
    }
    val modeColor = modeGradient.first()

    if (jokerDialogVisible) {
        AlertDialog(
            onDismissRequest = { jokerDialogVisible = false },
            containerColor = ColorSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Style, null, modifier = Modifier.size(20.dp), tint = modeColor)
                    Text(
                        S.current.useJoker,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorOnSurface,
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        S.current.jokerTopicPrompt,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = jokerLabelInput,
                        onValueChange = { if (it.length <= 40) jokerLabelInput = it },
                        placeholder = { Text(S.current.jokerTopicPlaceholder, color = ColorOnSurfaceVariant) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = modeColor,
                            unfocusedBorderColor = ColorOutline,
                            focusedTextColor = ColorOnSurface,
                            unfocusedTextColor = ColorOnSurface,
                            cursorColor = modeColor,
                        ),
                    )
                }
            },
            confirmButton = {
                GradientButton(
                    text = S.current.takePhoto,
                    onClick = {
                        jokerDialogVisible = false
                        val myId = gameState.session.myPlayerId ?: return@GradientButton
                        photoTargetPlayerId = myId
                        photoTargetCategoryId = "joker_$myId"
                        val label = jokerLabelInput.trim().ifEmpty { "Joker" }
                        jokerLabelInput = label
                        val existingJoker = gameState.gameplay.selectedCategories.find { it.id == "joker_$myId" }
                        if (existingJoker == null) {
                            gameState.gameplay.selectedCategories = gameState.gameplay.selectedCategories + Category(
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
                    Text(S.current.cancel)
                }
            },
        )
    }

    // Realtime: game status and end-votes
    LaunchedEffect(gameId) {
        if (realtime == null) return@LaunchedEffect
        realtime.gameUpdates.collect { game ->
            if (game.status == "voting" && gameState.session.currentScreen == Screen.GAME) {
                gameState.gameplay.isGameRunning = false
                gameState.review.reviewCategoryIndex = game.review_category_index
                gameState.session.currentScreen = Screen.VOTE_TRANSITION
            }
        }
    }

    LaunchedEffect(gameId) {
        if (realtime == null) return@LaunchedEffect
        val gid = gameId ?: return@LaunchedEffect
        realtime.voteSubmissionInserts.collect { submission ->
            if (submission.category_id == VoteKeys.END_VOTE) {
                try {
                    val count = GameRepository.getEndVoteCount(gid)
                    gameState.review.endVoteCount = count
                    if (count >= gameState.gameplay.players.size && gameState.gameplay.players.isNotEmpty()) {
                        gameState.gameplay.isGameRunning = false
                        GameRepository.endGameAsVoting(gid)
                        gameState.review.reviewCategoryIndex = 0
                        gameState.session.currentScreen = Screen.VOTE_TRANSITION
                    }
                } catch (e: Exception) { AppLogger.e("Game", "End vote count check failed", e) }
            }
            // Also detect finish signal via realtime
            if (submission.category_id == VoteKeys.ALL_CAPTURED && !gameState.review.finishSignalDetected && !gameState.review.allCategoriesCaptured) {
                gameState.review.finishSignalDetected = true
            }
        }
    }

    // Realtime: update other players' capture counts
    LaunchedEffect(gameId) {
        if (realtime == null) return@LaunchedEffect
        realtime.captureInserts.collect { capture ->
            if (capture.player_id != gameState.session.myPlayerId) {
                gameState.updateCaptures(capture.player_id, capture.category_id)
            }
        }
    }

    // Fallback polling (skips most work when realtime is active)
    LaunchedEffect(gameId) {
        if (gameId == null) return@LaunchedEffect
        var interval = GameConstants.POLLING_INITIAL_INTERVAL_MS
        while (true) {
            delay(interval)
            try {
                val game = GameRepository.getGameById(gameId)
                if (game?.status == "voting" && gameState.session.currentScreen == Screen.GAME) {
                    gameState.gameplay.isGameRunning = false
                    gameState.review.reviewCategoryIndex = game.review_category_index
                    gameState.session.currentScreen = Screen.VOTE_TRANSITION
                }
                // Only do heavy polling when realtime is not active
                if (gameState.gameplay.isGameRunning && realtime == null) {
                    try {
                        val allCaptures = GameRepository.getCaptures(gameId)
                        val captureMap = mutableMapOf<String, Set<String>>()
                        allCaptures.forEach { capture ->
                            captureMap[capture.player_id] = (captureMap[capture.player_id] ?: emptySet()) + capture.category_id
                        }
                        gameState.mergeCaptures(captureMap)
                    } catch (e: Exception) { AppLogger.w("Game", "Capture polling failed", e) }

                    val count = GameRepository.getEndVoteCount(gameId)
                    gameState.review.endVoteCount = count
                    if (count >= gameState.gameplay.players.size && gameState.gameplay.players.isNotEmpty()) {
                        gameState.gameplay.isGameRunning = false
                        GameRepository.endGameAsVoting(gameId)
                        gameState.review.reviewCategoryIndex = 0
                        gameState.session.currentScreen = Screen.VOTE_TRANSITION
                    }

                    if (!gameState.review.finishSignalDetected && !gameState.review.allCategoriesCaptured) {
                        try {
                            if (GameRepository.hasAllCapturedSignal(gameId)) {
                                gameState.review.finishSignalDetected = true
                            }
                        } catch (e: Exception) { AppLogger.d("Game", "AllCaptured signal check failed", e) }
                    }
                }
                gameState.ui.consecutiveNetworkErrors = 0
                interval = GameConstants.POLLING_INITIAL_INTERVAL_MS
            } catch (e: Exception) {
                AppLogger.w("Game", "Fallback polling error", e)
                gameState.ui.consecutiveNetworkErrors++
                interval = (interval * GameConstants.POLLING_BACKOFF_FACTOR).toLong().coerceAtMost(GameConstants.POLLING_MAX_INTERVAL_MS)
            }
        }
    }

    // Timer logic
    LaunchedEffect(Unit) {
        while (gameState.gameplay.isGameRunning && gameState.gameplay.timeRemainingSeconds > 0 && !gameState.review.allCategoriesCaptured) {
            delay(GameConstants.TIMER_TICK_MS)
            if (gameState.gameplay.isGameRunning && !gameState.review.allCategoriesCaptured) {
                gameState.gameplay.timeRemainingSeconds--
                if (gameState.gameplay.timeRemainingSeconds in 1..10) feedback.countdownTick()
            }
        }
        if (!gameState.review.allCategoriesCaptured && gameState.gameplay.timeRemainingSeconds <= 0 && gameState.gameplay.isGameRunning) {
            feedback.gameEnd()
            if (gameId != null) {
                try { GameRepository.endGameAsVoting(gameId) } catch (e: Exception) { AppLogger.e("Game", "Failed to end game as voting", e) }
            }
            gameState.review.reviewCategoryIndex = 0
            gameState.session.currentScreen = Screen.VOTE_TRANSITION
        }
    }

    // Finish countdown – triggered by local completion OR remote signal
    var finishCountdownSeconds by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        // Wait until either the local player or a remote player signals "all captured"
        snapshotFlow { gameState.review.allCategoriesCaptured || gameState.review.finishSignalDetected }
            .first { it }
        // Signal to server if we were the one who captured all
        if (gameState.review.allCategoriesCaptured && gameId != null) {
            val pid = gameState.session.myPlayerId ?: ""
            try { withRetry { GameRepository.signalAllCaptured(gameId, pid) } } catch (e: Exception) { AppLogger.w("Game", "AllCaptured signal failed", e) }
        }
        finishCountdownSeconds = GameConstants.FINISH_COUNTDOWN_SECONDS
        repeat(GameConstants.FINISH_COUNTDOWN_SECONDS) {
            delay(GameConstants.TIMER_TICK_MS)
            val remaining = (finishCountdownSeconds ?: 1) - 1
            finishCountdownSeconds = remaining
            if (remaining in 1..10) feedback.countdownTick()
        }
        if (gameState.gameplay.isGameRunning) {
            gameState.gameplay.isGameRunning = false
            if (gameId != null) {
                try { GameRepository.endGameAsVoting(gameId) } catch (e: Exception) { AppLogger.e("Game", "Failed to end game (finish countdown)", e) }
            }
            gameState.review.reviewCategoryIndex = 0
            gameState.session.currentScreen = Screen.VOTE_TRANSITION
        }
    }

    GameScreenContent(
        gameState = gameState,
        finishCountdownSeconds = finishCountdownSeconds,
        uploadSuccessCategory = uploadSuccessCategory,
        onJokerClick = { jokerDialogVisible = true },
        onVoteToEnd = {
            scope.launch {
                if (gameId != null && !gameState.review.hasVotedToEnd) {
                    val pid = gameState.session.myPlayerId ?: return@launch
                    gameState.review.hasVotedToEnd = true
                    gameState.review.endVoteCount = gameState.review.endVoteCount + 1
                    val success = try {
                        withRetry { GameRepository.submitEndVote(gameId, pid) }
                        true
                    } catch (e: Exception) {
                        AppLogger.e("Game", "End vote submission failed", e)
                        gameState.ui.pendingToast = S.current.voteFailed
                        false
                    }
                    if (!success) {
                        gameState.review.hasVotedToEnd = false
                        gameState.review.endVoteCount = (gameState.review.endVoteCount - 1).coerceAtLeast(0)
                    } else {
                        // Check if all players voted to end
                        val count = gameState.review.endVoteCount
                        if (count >= gameState.gameplay.players.size && gameState.gameplay.players.isNotEmpty()) {
                            gameState.gameplay.isGameRunning = false
                            try { GameRepository.endGameAsVoting(gameId) } catch (e: Exception) { AppLogger.e("Game", "Failed to end game (all voted)", e) }
                            gameState.review.reviewCategoryIndex = 0
                            gameState.session.currentScreen = Screen.VOTE_TRANSITION
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
