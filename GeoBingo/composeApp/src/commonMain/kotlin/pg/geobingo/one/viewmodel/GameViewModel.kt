package pg.geobingo.one.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pg.geobingo.one.data.Category
import pg.geobingo.one.game.GameConstants
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.i18n.S
import pg.geobingo.one.navigation.NavigationManager
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.VoteKeys
import pg.geobingo.one.network.withRetry
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.platform.getCurrentLocation
import pg.geobingo.one.platform.SoundPlayer
import pg.geobingo.one.util.Analytics
import pg.geobingo.one.util.AppLogger

/**
 * Contains all game-phase business logic previously embedded in GameScreen composable.
 * Manages timer, photo uploads, vote-to-end, sync subscriptions, and finish countdown.
 */
class GameViewModel(
    val gameState: GameState,
    private val nav: NavigationManager,
) : ViewModel() {

    var uploadSuccessCategory by mutableStateOf<String?>(null)
        private set

    var finishCountdownSeconds by mutableStateOf<Int?>(null)
        private set

    var jokerLabelInput by mutableStateOf("")

    private var timerJob: Job? = null
    private var realtimeGameJob: Job? = null
    private var realtimeVoteJob: Job? = null
    private var realtimeCaptureJob: Job? = null
    private var pollingJob: Job? = null
    private var finishCountdownJob: Job? = null

    // ── Lifecycle ────────────────────────────────────────────────────────

    fun startObserving() {
        val gameId = gameState.session.gameId ?: return
        val realtime = gameState.realtime

        startTimer()
        startFinishCountdown(gameId)
        startRealtimeGameUpdates(gameId, realtime)
        startRealtimeVoteSubmissions(gameId, realtime)
        startRealtimeCaptureUpdates(realtime)
        startFallbackPolling(gameId, realtime)
    }

    override fun onCleared() {
        timerJob?.cancel()
        realtimeGameJob?.cancel()
        realtimeVoteJob?.cancel()
        realtimeCaptureJob?.cancel()
        pollingJob?.cancel()
        finishCountdownJob?.cancel()
        super.onCleared()
    }

    // ── Photo capture + upload ────────────────────────────────────────────

    fun handlePhotoCaptured(
        bytes: ByteArray?,
        playerId: String,
        categoryId: String,
        onFeedbackCapture: () -> Unit,
    ) {
        if (bytes == null) return
        val gameId = gameState.session.gameId

        Analytics.track(Analytics.PHOTO_CAPTURED, mapOf("isJoker" to categoryId.startsWith("joker_").toString()))
        gameState.addPhoto(playerId, categoryId, bytes)
        val isJoker = categoryId.startsWith("joker_")

        if (gameId != null) {
            try { LocalPhotoStore.savePhoto(gameId, playerId, categoryId, bytes) } catch (e: Exception) {
                AppLogger.d("GameVM", "Local photo save failed", e)
            }
        }

        gameState.photo.startUpload(categoryId)

        if (gameId != null) {
            viewModelScope.launch {
                val location = try { getCurrentLocation() } catch (e: Exception) {
                    AppLogger.d("GameVM", "Location unavailable", e); null
                }

                val captureSuccess = try {
                    withRetry { GameRepository.recordCapture(gameId, playerId, categoryId, bytes, latitude = location?.latitude, longitude = location?.longitude) }
                    true
                } catch (e: Exception) {
                    AppLogger.e("GameVM", "Capture upload failed for $categoryId", e)
                    if (gameState.ui.soundEnabled) SoundPlayer.playError()
                    gameState.ui.pendingToast = S.current.uploadFailed
                    false
                }

                if (isJoker) {
                    try { withRetry { GameRepository.setJokerLabel(gameId, playerId, jokerLabelInput.trim()) } } catch (e: Exception) {
                        AppLogger.w("GameVM", "Joker label save failed", e)
                    }
                }

                if (captureSuccess) {
                    onFeedbackCapture()
                    uploadSuccessCategory = categoryId
                    delay(GameConstants.UPLOAD_SUCCESS_TOAST_MS)
                    uploadSuccessCategory = null
                }
                gameState.photo.finishUpload(categoryId)
            }
        } else {
            gameState.photo.finishUpload(categoryId)
        }

        if (isJoker) gameState.joker.myJokerUsed = true
    }

    fun addJokerCategory(playerId: String) {
        val label = jokerLabelInput.trim().ifEmpty { "Joker" }
        jokerLabelInput = label
        val existingJoker = gameState.gameplay.selectedCategories.find { it.id == "joker_$playerId" }
        if (existingJoker == null) {
            gameState.gameplay.selectedCategories = gameState.gameplay.selectedCategories + Category(
                id = "joker_$playerId",
                name = label,
                emoji = "joker",
            )
        }
    }

    // ── Vote to end ──────────────────────────────────────────────────────

    fun voteToEnd(onFeedback: (() -> Unit)? = null) {
        val gameId = gameState.session.gameId ?: return
        if (gameState.review.hasVotedToEnd) return
        val pid = gameState.session.myPlayerId ?: return

        gameState.review.hasVotedToEnd = true
        gameState.review.endVoteCount = gameState.review.endVoteCount + 1

        viewModelScope.launch {
            val success = try {
                withRetry { GameRepository.submitEndVote(gameId, pid) }
                true
            } catch (e: Exception) {
                AppLogger.e("GameVM", "End vote submission failed", e)
                gameState.ui.pendingToast = S.current.voteFailed
                false
            }
            if (!success) {
                gameState.review.hasVotedToEnd = false
                gameState.review.endVoteCount = (gameState.review.endVoteCount - 1).coerceAtLeast(0)
            } else {
                checkAllVotedToEnd(gameId)
            }
        }
    }

    // ── Private: Timer ───────────────────────────────────────────────────

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (gameState.gameplay.isGameRunning && gameState.gameplay.timeRemainingSeconds > 0 && !gameState.review.allCategoriesCaptured) {
                delay(GameConstants.TIMER_TICK_MS)
                if (gameState.gameplay.isGameRunning && !gameState.review.allCategoriesCaptured) {
                    gameState.gameplay.timeRemainingSeconds--
                    // Timer warning sound at 60s, 30s, 10s
                    val t = gameState.gameplay.timeRemainingSeconds
                    if (gameState.ui.soundEnabled && (t == 60 || t == 30 || t == 10)) {
                        SoundPlayer.playTimerWarning()
                    }
                }
            }
            if (!gameState.review.allCategoriesCaptured && gameState.gameplay.timeRemainingSeconds <= 0 && gameState.gameplay.isGameRunning) {
                transitionToVoting()
            }
        }
    }

    // ── Private: Finish countdown ────────────────────────────────────────

    private fun startFinishCountdown(gameId: String) {
        finishCountdownJob = viewModelScope.launch {
            snapshotFlow { gameState.review.allCategoriesCaptured || gameState.review.finishSignalDetected }
                .first { it }

            if (gameState.review.allCategoriesCaptured) {
                val pid = gameState.session.myPlayerId ?: ""
                try { withRetry { GameRepository.signalAllCaptured(gameId, pid) } } catch (e: Exception) {
                    AppLogger.w("GameVM", "AllCaptured signal failed", e)
                }
            }

            finishCountdownSeconds = GameConstants.FINISH_COUNTDOWN_SECONDS
            repeat(GameConstants.FINISH_COUNTDOWN_SECONDS) {
                delay(GameConstants.TIMER_TICK_MS)
                finishCountdownSeconds = (finishCountdownSeconds ?: 1) - 1
            }

            if (gameState.gameplay.isGameRunning) {
                transitionToVoting()
            }
        }
    }

    // ── Private: Realtime ────────────────────────────────────────────────

    private fun startRealtimeGameUpdates(gameId: String, realtime: pg.geobingo.one.network.GameRealtimeManager?) {
        if (realtime == null) return
        realtimeGameJob = viewModelScope.launch {
            realtime.gameUpdates.collect { game ->
                if (game.status == "voting" && nav.currentScreen == Screen.GAME) {
                    gameState.gameplay.isGameRunning = false
                    gameState.review.reviewCategoryIndex = game.review_category_index
                    nav.replaceCurrent(Screen.VOTE_TRANSITION)
                }
            }
        }
    }

    private fun startRealtimeVoteSubmissions(gameId: String, realtime: pg.geobingo.one.network.GameRealtimeManager?) {
        if (realtime == null) return
        realtimeVoteJob = viewModelScope.launch {
            realtime.voteSubmissionInserts.collect { submission ->
                if (submission.category_id == VoteKeys.END_VOTE) {
                    try {
                        val count = GameRepository.getEndVoteCount(gameId)
                        gameState.review.endVoteCount = count
                        if (count >= gameState.gameplay.players.size && gameState.gameplay.players.isNotEmpty()) {
                            gameState.gameplay.isGameRunning = false
                            GameRepository.endGameAsVoting(gameId)
                            gameState.review.reviewCategoryIndex = 0
                            nav.replaceCurrent(Screen.VOTE_TRANSITION)
                        }
                    } catch (e: Exception) { AppLogger.e("GameVM", "End vote count check failed", e) }
                }
                if (submission.category_id == VoteKeys.ALL_CAPTURED && !gameState.review.finishSignalDetected && !gameState.review.allCategoriesCaptured) {
                    gameState.review.finishSignalDetected = true
                }
            }
        }
    }

    private fun startRealtimeCaptureUpdates(realtime: pg.geobingo.one.network.GameRealtimeManager?) {
        if (realtime == null) return
        realtimeCaptureJob = viewModelScope.launch {
            realtime.captureInserts.collect { capture ->
                if (capture.player_id != gameState.session.myPlayerId) {
                    gameState.updateCaptures(capture.player_id, capture.category_id)
                }
            }
        }
    }

    // ── Private: Fallback polling ────────────────────────────────────────

    private fun startFallbackPolling(gameId: String, realtime: pg.geobingo.one.network.GameRealtimeManager?) {
        pollingJob = viewModelScope.launch {
            var interval = GameConstants.POLLING_INITIAL_INTERVAL_MS
            while (true) {
                delay(interval)
                try {
                    val game = GameRepository.getGameById(gameId)
                    if (game?.status == "voting" && nav.currentScreen == Screen.GAME) {
                        gameState.gameplay.isGameRunning = false
                        gameState.review.reviewCategoryIndex = game.review_category_index
                        nav.replaceCurrent(Screen.VOTE_TRANSITION)
                    }
                    if (gameState.gameplay.isGameRunning && realtime == null) {
                        pollCapturesAndVotes(gameId)
                    }
                    gameState.ui.consecutiveNetworkErrors = 0
                    interval = GameConstants.POLLING_INITIAL_INTERVAL_MS
                } catch (e: Exception) {
                    AppLogger.w("GameVM", "Fallback polling error", e)
                    gameState.ui.consecutiveNetworkErrors++
                    interval = (interval * GameConstants.POLLING_BACKOFF_FACTOR).toLong().coerceAtMost(GameConstants.POLLING_MAX_INTERVAL_MS)
                }
            }
        }
    }

    private suspend fun pollCapturesAndVotes(gameId: String) {
        try {
            val allCaptures = GameRepository.getCaptures(gameId)
            val captureMap = mutableMapOf<String, Set<String>>()
            allCaptures.forEach { capture ->
                captureMap[capture.player_id] = (captureMap[capture.player_id] ?: emptySet()) + capture.category_id
            }
            gameState.mergeCaptures(captureMap)
        } catch (e: Exception) { AppLogger.w("GameVM", "Capture polling failed", e) }

        val count = GameRepository.getEndVoteCount(gameId)
        gameState.review.endVoteCount = count
        checkAllVotedToEnd(gameId)

        if (!gameState.review.finishSignalDetected && !gameState.review.allCategoriesCaptured) {
            try {
                if (GameRepository.hasAllCapturedSignal(gameId)) {
                    gameState.review.finishSignalDetected = true
                }
            } catch (e: Exception) { AppLogger.d("GameVM", "AllCaptured signal check failed", e) }
        }
    }

    // ── Private: Helpers ─────────────────────────────────────────────────

    private suspend fun transitionToVoting() {
        val gameId = gameState.session.gameId
        gameState.gameplay.isGameRunning = false
        if (gameId != null) {
            try { GameRepository.endGameAsVoting(gameId) } catch (e: Exception) {
                AppLogger.e("GameVM", "Failed to end game as voting", e)
            }
        }
        gameState.review.reviewCategoryIndex = 0
        nav.replaceCurrent(Screen.VOTE_TRANSITION)
    }

    private suspend fun checkAllVotedToEnd(gameId: String) {
        val count = gameState.review.endVoteCount
        if (count >= gameState.gameplay.players.size && gameState.gameplay.players.isNotEmpty()) {
            gameState.gameplay.isGameRunning = false
            try { GameRepository.endGameAsVoting(gameId) } catch (e: Exception) {
                AppLogger.e("GameVM", "Failed to end game (all voted)", e)
            }
            gameState.review.reviewCategoryIndex = 0
            nav.replaceCurrent(Screen.VOTE_TRANSITION)
        }
    }
}
