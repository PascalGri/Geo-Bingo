package pg.geobingo.one.game

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pg.geobingo.one.data.*
import pg.geobingo.one.game.state.*
import pg.geobingo.one.network.GameRealtimeManager
import pg.geobingo.one.network.GameSyncManager
import pg.geobingo.one.util.AppLogger

enum class Screen {
    ONBOARDING, HOME, HOW_TO_PLAY, SELECT_MODE, CREATE_GAME, JOIN_GAME, LOBBY, GAME_START_TRANSITION, GAME, VOTE_TRANSITION, REVIEW, RESULTS_TRANSITION, RESULTS, HISTORY, SETTINGS, STATS, SOLO_START_TRANSITION, SOLO_GAME, SOLO_RESULTS, SOLO_LEADERBOARD, SHOP, COSMETIC_SHOP, PROFILE_SETUP, ACCOUNT, FRIENDS, MP_LEADERBOARD, PROFILE, ACTIVITY_FEED, ACHIEVEMENTS, DIRECT_MESSAGE, MATCH_DETAIL
}

enum class GameMode { CLASSIC, BLIND_BINGO, WEIRD_CORE, QUICK_START, AI_JUDGE }

@kotlinx.serialization.Serializable
data class HistoryPlayer(
    val id: String,
    val name: String,
    val score: Int,
    val colorHex: String,
)

@kotlinx.serialization.Serializable
data class HistoryCategory(
    val id: String,
    val name: String,
)

@kotlinx.serialization.Serializable
data class GameHistoryEntry(
    val gameCode: String,
    val playerName: String,
    val score: Int,
    val totalCategories: Int,
    val players: List<HistoryPlayer>,
    val jokerMode: Boolean,
    val date: String = "",
    val gameId: String = "",
    val categories: List<HistoryCategory> = emptyList(),
)

class GameState {
    // ── Sub-state holders ────────────────────────────────────────────────
    val session = SessionState()
    val gameplay = GamePlayState()
    val photo = PhotoState()
    val review = ReviewState()
    val joker = JokerState()
    val ui = UiState()
    val solo = SoloState()
    val stars = StarsState()

    // ── Mutex for thread-safe state mutations ────────────────────────────
    private val stateMutex = Mutex()

    // ── Feature managers (single source of truth for logic) ──────────────
    val scoring = ScoringManager(gameplay, review)
    val teams = TeamManager(session, gameplay, review, scoring)
    val history = HistoryManager(session, gameplay, ui, scoring)

    // ── Shared realtime + sync managers ─────────────────────────────────
    var realtime: GameRealtimeManager? = null
        private set

    var syncManager: GameSyncManager? = null
        private set

    fun ensureRealtime(gameId: String): GameRealtimeManager {
        val existing = realtime
        if (existing != null) return existing
        val mgr = GameRealtimeManager(gameId)
        realtime = mgr
        return mgr
    }

    fun ensureSyncManager(gameId: String, scope: CoroutineScope): GameSyncManager {
        val existing = syncManager
        if (existing != null) return existing
        val rt = ensureRealtime(gameId)
        val mgr = GameSyncManager(gameId, rt, scope)
        syncManager = mgr
        mgr.start()
        return mgr
    }

    private fun cleanupRealtime() {
        syncManager?.stop()
        syncManager = null
        val rt = realtime ?: return
        realtime = null
        CoroutineScope(Dispatchers.Default).launch {
            try { rt.unsubscribe() } catch (e: Exception) {
                AppLogger.w("GameState", "Realtime cleanup failed", e)
            }
        }
    }

    // ── Convenience accessors ───────────────────────────────────────────
    val reviewPlayer: Player? get() = gameplay.players.getOrNull(review.reviewPlayerIndex)

    // ── Game lifecycle ──────────────────────────────────────────────────
    fun startGame() {
        gameplay.timeRemainingSeconds = gameplay.gameDurationMinutes * 60
        gameplay.isGameRunning = true
        gameplay.currentPlayerIndex = 0
        gameplay.captures = gameplay.players.associate { it.id to emptySet() }
        photo.photoCache.clear()
        review.votes = emptyMap()
    }

    fun endGame() {
        gameplay.isGameRunning = false
    }

    // ── Captures (thread-safe via mutex) ─────────────────────────────────

    /**
     * Toggle a capture for a player. Safe for non-concurrent contexts (tests, initial setup).
     * For concurrent access from coroutines, prefer [updateCapturesSafe].
     */
    fun toggleCapture(playerId: String, categoryId: String) {
        val updated = gameplay.captures.toMutableMap()
        val current = (updated[playerId] ?: emptySet()).toMutableSet()
        if (current.contains(categoryId)) current.remove(categoryId) else current.add(categoryId)
        updated[playerId] = current
        gameplay.captures = updated
    }

    /**
     * Thread-safe capture update from concurrent sources (realtime, polling).
     */
    suspend fun updateCapturesSafe(playerId: String, categoryId: String) {
        stateMutex.withLock {
            val current = gameplay.captures[playerId] ?: emptySet()
            if (categoryId !in current) {
                gameplay.captures = gameplay.captures + (playerId to current + categoryId)
                checkAllCategoriesCaptured(playerId)
            }
        }
    }

    /**
     * Thread-safe bulk capture update from polling.
     */
    suspend fun mergeCapturesSafe(allCaptures: Map<String, Set<String>>) {
        stateMutex.withLock {
            val merged = gameplay.captures.toMutableMap()
            allCaptures.forEach { (pid, cats) ->
                val existing = merged[pid] ?: emptySet()
                merged[pid] = existing + cats
            }
            gameplay.captures = merged
            allCaptures.keys.forEach { pid -> checkAllCategoriesCaptured(pid) }
        }
    }

    /** Non-suspending version for initial setup (main thread, no concurrent access). */
    fun mergeCaptures(allCaptures: Map<String, Set<String>>) {
        val merged = gameplay.captures.toMutableMap()
        allCaptures.forEach { (pid, cats) ->
            val existing = merged[pid] ?: emptySet()
            merged[pid] = existing + cats
        }
        gameplay.captures = merged
    }

    fun isCaptured(playerId: String, categoryId: String): Boolean =
        gameplay.captures[playerId]?.contains(categoryId) == true

    // ── Photos ──────────────────────────────────────────────────────────
    suspend fun addPhoto(playerId: String, categoryId: String, bytes: ByteArray) {
        stateMutex.withLock {
            photo.photoCache.put(playerId, categoryId, bytes)
            if (!isCaptured(playerId, categoryId)) {
                toggleCapture(playerId, categoryId)
            }
            checkAllCategoriesCaptured(playerId)
        }
    }

    /** Recheck the allCategoriesCaptured flag after captures change. Call inside stateMutex. */
    private fun checkAllCategoriesCaptured(playerId: String) {
        if (!gameplay.isGameRunning || gameplay.selectedCategories.isEmpty() || review.allCategoriesCaptured) return
        if (gameplay.teamModeEnabled) {
            val myTeam = gameplay.teamAssignments[playerId] ?: return
            val teamCaptures = teams.getTeamCaptures(myTeam)
            if (gameplay.selectedCategories.all { it.id in teamCaptures }) {
                review.allCategoriesCaptured = true
            }
        } else {
            val playerCaptures = gameplay.captures[playerId] ?: emptySet()
            if (gameplay.selectedCategories.all { it.id in playerCaptures }) {
                review.allCategoriesCaptured = true
            }
        }
    }

    fun getPhoto(playerId: String, categoryId: String): ByteArray? =
        photo.photoCache.get(playerId, categoryId)

    // ── Voting ──────────────────────────────────────────────────────────
    fun submitVotes(targetPlayerId: String, approvedCategoryIds: Set<String>) {
        val updated = review.votes.toMutableMap()
        scoring.getPlayerCaptures(targetPlayerId).forEach { category ->
            val key = "$targetPlayerId-${category.id}"
            val approved = category.id in approvedCategoryIds
            val existing = (updated[key] ?: emptyList()).toMutableList()
            existing.add(approved)
            updated[key] = existing
        }
        review.votes = updated
    }

    fun getVoteResult(playerId: String, categoryId: String): Boolean? =
        scoring.getVoteResult(playerId, categoryId, review.votes)

    // ── Ranked players (derived state) ───────────────────────────────────
    val rankedPlayers: List<Pair<Player, Int>> by derivedStateOf {
        gameplay.players.map { it to scoring.getPlayerScore(it.id, review.votes) }
            .sortedWith(
                compareByDescending<Pair<Player, Int>> { it.second }
                    .thenByDescending { scoring.getSpeedBonusCount(it.first.id) }
                    .thenBy { scoring.getLastCaptureTime(it.first.id) }
                    .thenBy { it.first.name }
            )
    }

    // ── History ──────────────────────────────────────────────────────────
    fun saveToHistory() {
        history.saveToHistory(joker.jokerMode, rankedPlayers)
    }

    // ── Reset ───────────────────────────────────────────────────────────
    private fun clearGameplayState() {
        gameplay.players = listOf()
        gameplay.lobbyPlayers = listOf()
        gameplay.timeRemainingSeconds = 0
        gameplay.isGameRunning = false
        gameplay.currentPlayerIndex = 0
        gameplay.captures = mapOf()
        photo.clear()
        review.votes = mapOf()
        review.reviewPlayerIndex = 0
        review.reviewCategoryIndex = 0
        review.allCaptures = listOf()
        review.categoryVotes = mapOf()
        review.hasSubmittedCurrentCategory = false
        review.allVotes = listOf()
        review.hasVotedToEnd = false
        review.endVoteCount = 0
        review.allCategoriesCaptured = false
        review.finishSignalDetected = false
        gameplay.teamAssignments = mapOf()
        gameplay.teamNames = mapOf()
        gameplay.teamModeEnabled = false
        gameplay.nextTeamNumber = 1
        joker.myJokerUsed = false
        joker.jokerLabels = mapOf()
        ui.consecutiveNetworkErrors = 0
    }

    fun resetGame() {
        ActiveSession.clear()
        cleanupRealtime()
        clearGameplayState()
        gameplay.selectedCategories = listOf()
        gameplay.gameDurationMinutes = GameConstants.DEFAULT_GAME_DURATION_MINUTES
        session.gameId = null
        session.gameCode = null
        session.isHost = false
        session.myPlayerId = null
        joker.jokerMode = false
        session.gameMode = GameMode.CLASSIC
        ui.interstitialShown = false
    }

    fun resetForRematch(newGameId: String, newGameCode: String, newPlayerId: String) {
        ActiveSession.clear()
        cleanupRealtime()
        clearGameplayState()
        session.gameId = newGameId
        session.gameCode = newGameCode
        session.myPlayerId = newPlayerId
        session.isHost = true
    }

    fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    }
}
