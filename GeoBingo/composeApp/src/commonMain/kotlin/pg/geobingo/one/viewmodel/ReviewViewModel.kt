package pg.geobingo.one.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import pg.geobingo.one.data.Category
import pg.geobingo.one.data.Player
import pg.geobingo.one.game.GameConstants
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.navigation.NavigationManager
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.VoteKeys
import pg.geobingo.one.network.withRetry
import pg.geobingo.one.util.AppLogger

/**
 * Contains all review-phase business logic.
 * Supports both individual mode (player x category) and team mode (team x category).
 */
class ReviewViewModel(
    val gameState: GameState,
    private val nav: NavigationManager,
) : ViewModel() {

    var selfVoteToast by mutableStateOf(false)
        private set

    private var realtimeGameJob: Job? = null
    private var realtimeVoteJob: Job? = null
    private var pollingJob: Job? = null

    val isTeamMode: Boolean get() = gameState.gameplay.teamModeEnabled

    val sortedPlayers: List<Player>
        get() = gameState.gameplay.players.sortedBy { it.id }

    val sortedTeams: List<Int>
        get() = gameState.getTeamNumbers()

    val categories: List<Category>
        get() = gameState.gameplay.selectedCategories

    /** Number of entities being reviewed (players or teams). */
    val reviewEntityCount: Int
        get() = if (isTeamMode) sortedTeams.size else sortedPlayers.size

    val totalSteps: Int
        get() = categories.size * reviewEntityCount

    // ── Lifecycle ────────────────────────────────────────────────────────

    fun startObserving() {
        val gameId = gameState.session.gameId ?: return
        loadJokerCategories(gameId)
        loadCaptures(gameId)
        startRealtimeGameUpdates(gameId)
        startRealtimeVoteSubmissions(gameId)
        startFallbackPolling(gameId)
    }

    override fun onCleared() {
        realtimeGameJob?.cancel()
        realtimeVoteJob?.cancel()
        pollingJob?.cancel()
        super.onCleared()
    }

    // ── Current step info ────────────────────────────────────────────────

    /** Get the step key for the current review step. */
    fun currentStepKey(): String? {
        val stepIndex = gameState.review.reviewCategoryIndex
        val entityCount = reviewEntityCount
        val categoryIndex = stepIndex / entityCount
        val targetIndex = stepIndex % entityCount
        if (categoryIndex >= categories.size || targetIndex >= entityCount) return null

        val currentCategory = categories[categoryIndex]
        return if (isTeamMode) {
            VoteKeys.stepKey(currentCategory.id, "team_${sortedTeams[targetIndex]}")
        } else {
            VoteKeys.stepKey(currentCategory.id, sortedPlayers[targetIndex].id)
        }
    }

    /** Is the current step about my own team/player? */
    fun isCurrentStepSelf(): Boolean {
        val stepIndex = gameState.review.reviewCategoryIndex
        val entityCount = reviewEntityCount
        val targetIndex = stepIndex % entityCount
        return if (isTeamMode) {
            val myTeam = gameState.getMyTeamNumber() ?: return false
            targetIndex < sortedTeams.size && sortedTeams[targetIndex] == myTeam
        } else {
            targetIndex < sortedPlayers.size && sortedPlayers[targetIndex].id == gameState.session.myPlayerId
        }
    }

    /** Number of votes required for current step to advance. */
    fun requiredVotesForCurrentStep(): Int {
        val stepIndex = gameState.review.reviewCategoryIndex
        val entityCount = reviewEntityCount
        val targetIndex = stepIndex % entityCount
        return if (isTeamMode) {
            val targetTeam = sortedTeams.getOrNull(targetIndex) ?: return 0
            // All players NOT on the target team must vote
            gameState.gameplay.players.count { gameState.gameplay.teamAssignments[it.id] != targetTeam }
        } else {
            sortedPlayers.size - 1
        }
    }

    // ── Vote submission ──────────────────────────────────────────────────

    fun submitVote(rating: Int) {
        val gameId = gameState.session.gameId ?: return
        val myPlayerId = gameState.session.myPlayerId ?: return
        val stepIndex = gameState.review.reviewCategoryIndex
        val entityCount = reviewEntityCount
        val categoryIndex = stepIndex / entityCount
        val targetIndex = stepIndex % entityCount

        if (categoryIndex >= categories.size || targetIndex >= entityCount) return

        val currentCategory = categories[categoryIndex]

        gameState.review.hasSubmittedCurrentCategory = true

        viewModelScope.launch {
            try {
                if (isTeamMode) {
                    val targetTeam = sortedTeams[targetIndex]
                    val capturer = gameState.getTeamCapturer(targetTeam, currentCategory.id)
                    val targetPlayerId = capturer?.id ?: return@launch
                    val stepKey = VoteKeys.stepKey(currentCategory.id, "team_$targetTeam")
                    withRetry { GameRepository.submitStepVote(gameId, myPlayerId, targetPlayerId, currentCategory.id, stepKey, rating) }
                } else {
                    val targetPlayer = sortedPlayers[targetIndex]
                    val stepKey = VoteKeys.stepKey(currentCategory.id, targetPlayer.id)
                    withRetry { GameRepository.submitStepVote(gameId, myPlayerId, targetPlayer.id, currentCategory.id, stepKey, rating) }
                }
            } catch (e: Exception) { AppLogger.e("ReviewVM", "Vote submit failed", e) }
            advanceStep()
        }
    }

    fun submitNoPhoto() {
        val gameId = gameState.session.gameId ?: return
        val myPlayerId = gameState.session.myPlayerId ?: return
        val stepKey = currentStepKey() ?: return

        gameState.review.hasSubmittedCurrentCategory = true

        viewModelScope.launch {
            try { withRetry { GameRepository.submitStepSubmission(gameId, myPlayerId, stepKey) } } catch (e: Exception) {
                AppLogger.e("ReviewVM", "Step submission failed", e)
            }
            advanceStep()
        }
    }

    // ── Self-vote auto-skip ──────────────────────────────────────────────

    fun handleSelfVoteSkip() {
        viewModelScope.launch {
            selfVoteToast = true
            delay(GameConstants.SELF_VOTE_TOAST_DELAY_MS)
            selfVoteToast = false
            gameState.review.hasSubmittedCurrentCategory = true
            advanceStep()
        }
    }

    // ── Force advance (host only) ────────────────────────────────────────

    fun forceAdvance() {
        val gameId = gameState.session.gameId ?: return
        val stepIndex = gameState.review.reviewCategoryIndex

        viewModelScope.launch {
            try {
                val nextStep = stepIndex + 1
                if (nextStep >= totalSteps) GameRepository.setGameStatus(gameId, "results")
                else GameRepository.setReviewCategoryIndex(gameId, nextStep)
            } catch (e: Exception) { AppLogger.e("ReviewVM", "Force advance failed", e) }
        }
    }

    // ── Private: Step advancement ────────────────────────────────────────

    private suspend fun advanceStep() {
        val gameId = gameState.session.gameId ?: return
        val stepIndex = gameState.review.reviewCategoryIndex
        val stepKey = currentStepKey() ?: return
        val requiredVotes = requiredVotesForCurrentStep()

        try {
            val submissionCount = GameRepository.getVoteSubmissionCount(gameId, stepKey)
            if (submissionCount >= requiredVotes) {
                val nextStep = stepIndex + 1
                if (nextStep >= totalSteps) {
                    transitionToResults(gameId)
                } else {
                    val serverUpdated = try {
                        withRetry { GameRepository.setReviewCategoryIndex(gameId, nextStep) }
                        true
                    } catch (e: Exception) { AppLogger.e("ReviewVM", "Set review index failed", e); false }
                    if (serverUpdated) {
                        gameState.review.reviewCategoryIndex = nextStep
                        gameState.review.hasSubmittedCurrentCategory = false
                    }
                }
            }
        } catch (e: Exception) { AppLogger.e("ReviewVM", "advanceStep failed", e) }
    }

    private suspend fun transitionToResults(gameId: String) {
        try { withRetry { GameRepository.setGameStatus(gameId, "results") } } catch (e: Exception) {
            AppLogger.e("ReviewVM", "Set results status failed", e)
        }
        try { gameState.review.allVotes = GameRepository.getVotes(gameId) } catch (e: Exception) {
            AppLogger.w("ReviewVM", "Votes fetch failed", e)
        }
        nav.replaceCurrent(Screen.RESULTS_TRANSITION)
    }

    // ── Private: Data loading ────────────────────────────────────────────

    private fun loadJokerCategories(gameId: String) {
        if (!gameState.joker.jokerMode) return
        viewModelScope.launch {
            try {
                val labels = GameRepository.getJokerLabels(gameId)
                gameState.joker.jokerLabels = labels
                val jokerCats = labels.entries.map { (playerId, label) ->
                    Category(id = "joker_$playerId", name = label, emoji = "joker")
                }.filter { jokerCat -> categories.none { it.id == jokerCat.id } }
                if (jokerCats.isNotEmpty()) {
                    gameState.gameplay.selectedCategories = gameState.gameplay.selectedCategories + jokerCats
                }
            } catch (e: Exception) { AppLogger.w("ReviewVM", "Joker labels fetch failed", e) }
        }
    }

    private fun loadCaptures(gameId: String) {
        viewModelScope.launch {
            try { gameState.review.allCaptures = GameRepository.getCaptures(gameId) } catch (e: Exception) {
                AppLogger.w("ReviewVM", "Captures fetch failed", e)
            }
        }
    }

    // ── Private: Realtime ────────────────────────────────────────────────

    private fun startRealtimeGameUpdates(gameId: String) {
        val realtime = gameState.realtime ?: return
        realtimeGameJob = viewModelScope.launch {
            realtime.gameUpdates.collect { game ->
                val newStep = game.review_category_index
                if (newStep != gameState.review.reviewCategoryIndex) {
                    gameState.review.reviewCategoryIndex = newStep
                    gameState.review.hasSubmittedCurrentCategory = false
                }
                if (game.status == "results") {
                    try { gameState.review.allVotes = GameRepository.getVotes(gameId) } catch (e: Exception) {
                        AppLogger.w("ReviewVM", "Votes fetch failed", e)
                    }
                    nav.replaceCurrent(Screen.RESULTS_TRANSITION)
                }
            }
        }
    }

    private fun startRealtimeVoteSubmissions(gameId: String) {
        val realtime = gameState.realtime ?: return
        realtimeVoteJob = viewModelScope.launch {
            realtime.voteSubmissionInserts.collect { _ ->
                if (gameState.review.hasSubmittedCurrentCategory) {
                    advanceStep()
                }
            }
        }
    }

    // ── Private: Fallback polling ────────────────────────────────────────

    private fun startFallbackPolling(gameId: String) {
        pollingJob = viewModelScope.launch {
            var interval = GameConstants.POLLING_INITIAL_INTERVAL_MS
            while (isActive) {
                delay(interval)
                try {
                    val game = withTimeoutOrNull(10_000L) { GameRepository.getGameById(gameId) }
                    val newStep = game?.review_category_index ?: 0
                    if (newStep != gameState.review.reviewCategoryIndex) {
                        gameState.review.reviewCategoryIndex = newStep
                        gameState.review.hasSubmittedCurrentCategory = false
                    }
                    if (game?.status == "results" && nav.currentScreen == Screen.REVIEW) {
                        gameState.review.allVotes = GameRepository.getVotes(gameId)
                        nav.replaceCurrent(Screen.RESULTS_TRANSITION)
                    }
                    interval = GameConstants.POLLING_INITIAL_INTERVAL_MS
                } catch (e: Exception) {
                    AppLogger.w("ReviewVM", "Fallback polling error", e)
                    interval = (interval * GameConstants.POLLING_BACKOFF_FACTOR).toLong().coerceAtMost(GameConstants.POLLING_MAX_INTERVAL_MS)
                }
            }
        }
    }
}
