package pg.geobingo.one.ui.screens.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pg.geobingo.one.data.Category
import pg.geobingo.one.game.GameConstants
import pg.geobingo.one.game.GameMode
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.util.AppLogger
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.VoteKeys
import pg.geobingo.one.network.withRetry
import pg.geobingo.one.ui.theme.*

@Composable
fun ReviewScreen(gameState: GameState) {
    val scope = rememberCoroutineScope()
    val modeColor = when (gameState.session.gameMode) {
        GameMode.CLASSIC     -> GradientPrimary.first()
        GameMode.BLIND_BINGO -> GradientCool.first()
        GameMode.WEIRD_CORE  -> GradientWeird.first()
        GameMode.QUICK_START -> GradientQuickStart.first()
    }
    val gameId = gameState.session.gameId ?: return
    val categories = gameState.gameplay.selectedCategories
    val myPlayerId = gameState.session.myPlayerId ?: return

    val sortedPlayers = remember(gameState.gameplay.players) { gameState.gameplay.players.sortedBy { it.id } }
    val numPlayers = sortedPlayers.size

    val realtime = gameState.realtime

    LaunchedEffect(gameId) {
        if (gameState.joker.jokerMode) {
            try {
                val labels = GameRepository.getJokerLabels(gameId)
                gameState.joker.jokerLabels = labels
                val jokerCats = labels.entries.map { (playerId, label) ->
                    Category(id = "joker_$playerId", name = label, emoji = "joker")
                }.filter { jokerCat -> gameState.gameplay.selectedCategories.none { it.id == jokerCat.id } }
                if (jokerCats.isNotEmpty()) gameState.gameplay.selectedCategories = gameState.gameplay.selectedCategories + jokerCats
            } catch (e: Exception) { AppLogger.w("Review", "Joker labels fetch failed", e) }
        }
    }

    LaunchedEffect(gameState.review.reviewCategoryIndex) {
        try { gameState.review.allCaptures = GameRepository.getCaptures(gameId) } catch (e: Exception) { AppLogger.w("Review", "Captures fetch failed", e) }
    }

    LaunchedEffect(gameId) {
        if (realtime == null) return@LaunchedEffect
        realtime.gameUpdates.collect { game ->
            val newStep = game.review_category_index
            if (newStep != gameState.review.reviewCategoryIndex) {
                gameState.review.reviewCategoryIndex = newStep
                gameState.review.hasSubmittedCurrentCategory = false
            }
            if (game.status == "results") {
                try { gameState.review.allVotes = GameRepository.getVotes(gameId) } catch (e: Exception) { AppLogger.w("Review", "Votes fetch failed", e) }
                gameState.session.currentScreen = Screen.RESULTS_TRANSITION
            }
        }
    }

    // Realtime: detect vote submissions to trigger faster advancement
    LaunchedEffect(gameId) {
        if (realtime == null) return@LaunchedEffect
        realtime.voteSubmissionInserts.collect { _ ->
            // When any vote submission arrives, check if we can advance
            if (gameState.review.hasSubmittedCurrentCategory) {
                val currentStepIndex = gameState.review.reviewCategoryIndex
                val currentCatIndex = currentStepIndex / numPlayers
                val currentPlayerIdx = currentStepIndex % numPlayers
                if (currentCatIndex < categories.size && currentPlayerIdx < sortedPlayers.size) {
                    val currentStepKey = VoteKeys.stepKey(categories[currentCatIndex].id, sortedPlayers[currentPlayerIdx].id)
                    try {
                        val count = GameRepository.getVoteSubmissionCount(gameId, currentStepKey)
                        if (count >= numPlayers - 1) {
                            val nextStep = currentStepIndex + 1
                            val totalSteps = categories.size * numPlayers
                            if (nextStep >= totalSteps) {
                                try { withRetry { GameRepository.setGameStatus(gameId, "results") } } catch (e: Exception) { AppLogger.e("Review", "Set game results failed", e) }
                                try { gameState.review.allVotes = GameRepository.getVotes(gameId) } catch (e: Exception) { AppLogger.w("Review", "Votes fetch failed", e) }
                                gameState.session.currentScreen = Screen.RESULTS_TRANSITION
                            } else {
                                try { withRetry { GameRepository.setReviewCategoryIndex(gameId, nextStep) } } catch (e: Exception) { AppLogger.e("Review", "Set review index failed", e) }
                                gameState.review.reviewCategoryIndex = nextStep
                                gameState.review.hasSubmittedCurrentCategory = false
                            }
                        }
                    } catch (e: Exception) { AppLogger.w("Review", "Vote submission check failed", e) }
                }
            }
        }
    }

    // Fallback polling (with backoff on errors)
    LaunchedEffect(gameId) {
        var interval = GameConstants.POLLING_INITIAL_INTERVAL_MS
        while (true) {
            delay(interval)
            try {
                val game = GameRepository.getGameById(gameId)
                val newStep = game?.review_category_index ?: 0
                if (newStep != gameState.review.reviewCategoryIndex) {
                    gameState.review.reviewCategoryIndex = newStep
                    gameState.review.hasSubmittedCurrentCategory = false
                }
                if (game?.status == "results" && gameState.session.currentScreen == Screen.REVIEW) {
                    gameState.review.allVotes = GameRepository.getVotes(gameId)
                    gameState.session.currentScreen = Screen.RESULTS_TRANSITION
                }
                interval = GameConstants.POLLING_INITIAL_INTERVAL_MS
            } catch (e: Exception) {
                AppLogger.w("Review", "Fallback polling error", e)
                interval = (interval * GameConstants.POLLING_BACKOFF_FACTOR).toLong().coerceAtMost(GameConstants.POLLING_MAX_INTERVAL_MS)
            }
        }
    }

    if (numPlayers == 0 || categories.isEmpty()) {
        Box(Modifier.fillMaxSize().background(ColorBackground), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = modeColor) }
        return
    }

    val stepIndex = gameState.review.reviewCategoryIndex
    val totalSteps = categories.size * numPlayers

    if (stepIndex >= totalSteps) {
        Box(Modifier.fillMaxSize().background(ColorBackground), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = modeColor) }
        return
    }

    val categoryIndex = stepIndex / numPlayers
    val targetPlayerIndex = stepIndex % numPlayers
    val currentCategory = categories[categoryIndex]
    val targetPlayer = sortedPlayers[targetPlayerIndex]
    val stepKey = VoteKeys.stepKey(currentCategory.id, targetPlayer.id)

    val requiredVotes = numPlayers - 1 // target player doesn't vote on their own image

    suspend fun advanceStep() {
        try {
            val submissionCount = GameRepository.getVoteSubmissionCount(gameId, stepKey)
            if (submissionCount >= requiredVotes) {
                val nextStep = stepIndex + 1
                if (nextStep >= totalSteps) {
                    try { withRetry { GameRepository.setGameStatus(gameId, "results") } } catch (e: Exception) { AppLogger.e("Review", "Set results status failed", e) }
                    try { gameState.review.allVotes = GameRepository.getVotes(gameId) } catch (e: Exception) { AppLogger.w("Review", "Votes fetch failed", e) }
                    gameState.session.currentScreen = Screen.RESULTS_TRANSITION
                } else {
                    val serverUpdated = try {
                        withRetry { GameRepository.setReviewCategoryIndex(gameId, nextStep) }
                        true
                    } catch (e: Exception) { AppLogger.e("Review", "Set review index failed", e); false }
                    if (serverUpdated) {
                        gameState.review.reviewCategoryIndex = nextStep
                        gameState.review.hasSubmittedCurrentCategory = false
                    }
                }
            }
        } catch (e: Exception) { AppLogger.e("Review", "advanceStep failed", e) }
    }

    // Auto-skip self-voting: show brief toast, then wait for others
    val isSelf = targetPlayer.id == myPlayerId
    var selfVoteToast by remember(stepIndex) { mutableStateOf(false) }
    LaunchedEffect(stepIndex, isSelf) {
        if (isSelf && !gameState.review.hasSubmittedCurrentCategory) {
            selfVoteToast = true
            delay(GameConstants.SELF_VOTE_TOAST_DELAY_MS)
            selfVoteToast = false
            gameState.review.hasSubmittedCurrentCategory = true
            advanceStep()
        }
    }

    key(stepIndex) {
        // Self-vote toast overlay
        if (selfVoteToast) {
            Box(
                modifier = Modifier.fillMaxSize().background(ColorBackground),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = modeColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, modeColor.copy(alpha = 0.4f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = modeColor,
                        )
                        Text(
                            "Dein Bild wird bewertet...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorOnPrimaryContainer,
                        )
                    }
                }
            }
        } else if (isSelf || gameState.review.hasSubmittedCurrentCategory) {
            DarkWaitingScreen(
                gameId = gameId,
                stepKey = stepKey,
                categoryName = currentCategory.name,
                playerName = targetPlayer.name,
                categoryIndex = categoryIndex,
                totalCategories = categories.size,
                playerIndex = targetPlayerIndex,
                totalPlayers = numPlayers,
                isHost = gameState.session.isHost,
                isSelf = isSelf,
                onReadyToAdvance = { scope.launch { advanceStep() } },
                onForceAdvance = {
                    scope.launch {
                        try {
                            val nextStep = stepIndex + 1
                            if (nextStep >= totalSteps) GameRepository.setGameStatus(gameId, "results")
                            else GameRepository.setReviewCategoryIndex(gameId, nextStep)
                        } catch (e: Exception) { AppLogger.e("Review", "Force advance failed", e) }
                    }
                },
            )
        } else {
            DarkSinglePhotoVotingScreen(
                gameId = gameId,
                currentCategory = currentCategory,
                categoryIndex = categoryIndex,
                totalCategories = categories.size,
                targetPlayer = targetPlayer,
                targetPlayerIndex = targetPlayerIndex,
                totalPlayers = numPlayers,
                stepIndex = stepIndex,
                playerAvatarBytes = gameState.photo.playerAvatarBytes[targetPlayer.id],
                hapticEnabled = gameState.ui.hapticEnabled,
                soundEnabled = gameState.ui.soundEnabled,
                onVote = { rating ->
                    scope.launch {
                        gameState.review.hasSubmittedCurrentCategory = true
                        try { withRetry { GameRepository.submitStepVote(gameId, myPlayerId, targetPlayer.id, currentCategory.id, stepKey, rating) } } catch (e: Exception) { AppLogger.e("Review", "Vote submit failed", e) }
                        // Always try to advance, even if vote submission had issues
                        advanceStep()
                    }
                },
                onNoPhoto = {
                    scope.launch {
                        gameState.review.hasSubmittedCurrentCategory = true
                        try { withRetry { GameRepository.submitStepSubmission(gameId, myPlayerId, stepKey) } } catch (e: Exception) { AppLogger.e("Review", "Step submission failed", e) }
                        advanceStep()
                    }
                },
            )
        }
    }
}
