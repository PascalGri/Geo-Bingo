package pg.geobingo.one.game

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pg.geobingo.one.data.*
import pg.geobingo.one.game.state.*
import pg.geobingo.one.network.CaptureDto
import pg.geobingo.one.network.GameRealtimeManager
import pg.geobingo.one.network.GameSyncManager
import pg.geobingo.one.network.PlayerDto
import pg.geobingo.one.network.VoteDto
import pg.geobingo.one.network.toHex
import pg.geobingo.one.util.AppLogger

enum class Screen {
    ONBOARDING, HOME, HOW_TO_PLAY, SELECT_MODE, CREATE_GAME, JOIN_GAME, LOBBY, GAME_START_TRANSITION, GAME, VOTE_TRANSITION, REVIEW, RESULTS_TRANSITION, RESULTS, HISTORY, SETTINGS, STATS, SOLO_START_TRANSITION, SOLO_GAME, SOLO_RESULTS, SOLO_LEADERBOARD, SHOP, COSMETIC_SHOP, PROFILE_SETUP, ACCOUNT, FRIENDS, MP_LEADERBOARD, PROFILE, ACTIVITY_FEED
}

enum class GameMode { CLASSIC, BLIND_BINGO, WEIRD_CORE, QUICK_START }

data class HistoryPlayer(
    val id: String,
    val name: String,
    val score: Int,
    val colorHex: String,
)

data class HistoryCategory(
    val id: String,
    val name: String,
)

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

    // ── Convenience accessors (delegate to ScoringManager) ──────────────
    val reviewPlayer: Player? get() = gameplay.players.getOrNull(review.reviewPlayerIndex)

    fun getFirstCapturers(): Map<String, String> = scoring.getFirstCapturers()
    fun getSpeedBonusCount(playerId: String): Int = scoring.getSpeedBonusCount(playerId)
    fun getCategoryAverageRating(playerId: String, categoryId: String): Double? = scoring.getCategoryAverageRating(playerId, categoryId)
    fun getPlayerAverageRating(playerId: String): Double? = scoring.getPlayerAverageRating(playerId)
    fun getPlayerScore(playerId: String): Int = scoring.getPlayerScore(playerId, review.votes)
    fun getPlayerCaptures(playerId: String): List<Category> = scoring.getPlayerCaptures(playerId)
    fun getLastCaptureTime(playerId: String): String = scoring.getLastCaptureTime(playerId)

    // ── Game lifecycle ──────────────────────────────────────────────────
    fun startGame() {
        gameplay.timeRemainingSeconds = gameplay.gameDurationMinutes * 60
        gameplay.isGameRunning = true
        gameplay.currentPlayerIndex = 0
        gameplay.captures = gameplay.players.associate { it.id to emptySet() }
        photo.photoCache.clear()
        review.votes = emptyMap()
        session.currentScreen = Screen.GAME
    }

    fun endGame() {
        gameplay.isGameRunning = false
    }

    // ── Captures (thread-safe via mutex) ─────────────────────────────────

    fun toggleCapture(playerId: String, categoryId: String) {
        val updated = gameplay.captures.toMutableMap()
        val current = (updated[playerId] ?: emptySet()).toMutableSet()
        if (current.contains(categoryId)) current.remove(categoryId) else current.add(categoryId)
        updated[playerId] = current
        gameplay.captures = updated
    }

    /**
     * Thread-safe capture update from concurrent sources (realtime, polling).
     * Uses mutex to prevent race conditions between coroutines.
     */
    suspend fun updateCapturesSafe(playerId: String, categoryId: String) {
        stateMutex.withLock {
            val current = gameplay.captures[playerId] ?: emptySet()
            if (categoryId !in current) {
                gameplay.captures = gameplay.captures + (playerId to current + categoryId)
            }
        }
    }

    /** Non-suspending version for use from main thread where races are unlikely. */
    fun updateCaptures(playerId: String, categoryId: String) {
        val current = gameplay.captures[playerId] ?: emptySet()
        if (categoryId !in current) {
            gameplay.captures = gameplay.captures + (playerId to current + categoryId)
        }
    }

    /**
     * Thread-safe bulk capture update from polling.
     * Uses mutex to prevent race conditions.
     */
    suspend fun mergeCapturesSafe(allCaptures: Map<String, Set<String>>) {
        stateMutex.withLock {
            val merged = gameplay.captures.toMutableMap()
            allCaptures.forEach { (pid, cats) ->
                val existing = merged[pid] ?: emptySet()
                merged[pid] = existing + cats
            }
            gameplay.captures = merged
        }
    }

    /** Non-suspending version for backward compatibility. */
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
    fun addPhoto(playerId: String, categoryId: String, bytes: ByteArray) {
        photo.photoCache.put(playerId, categoryId, bytes)
        if (!isCaptured(playerId, categoryId)) {
            toggleCapture(playerId, categoryId)
        }
        if (gameplay.isGameRunning && gameplay.selectedCategories.isNotEmpty() && !review.allCategoriesCaptured) {
            if (gameplay.teamModeEnabled) {
                val myTeam = gameplay.teamAssignments[playerId] ?: return
                val teamCaptures = getTeamCaptures(myTeam)
                if (gameplay.selectedCategories.all { it.id in teamCaptures }) {
                    review.allCategoriesCaptured = true
                }
            } else {
                if (gameplay.selectedCategories.all { photo.photoCache.contains(playerId, it.id) }) {
                    review.allCategoriesCaptured = true
                }
            }
        }
    }

    fun getPhoto(playerId: String, categoryId: String): ByteArray? =
        photo.photoCache.get(playerId, categoryId)

    // ── Voting ──────────────────────────────────────────────────────────
    fun submitVotes(targetPlayerId: String, approvedCategoryIds: Set<String>) {
        val updated = review.votes.toMutableMap()
        getPlayerCaptures(targetPlayerId).forEach { category ->
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

    // ── Team helpers ─────────────────────────────────────────────────────

    fun getTeamNumbers(): List<Int> =
        gameplay.teamAssignments.values.toSet().sorted()

    fun getTeamPlayers(teamNumber: Int): List<Player> =
        gameplay.players.filter { gameplay.teamAssignments[it.id] == teamNumber }

    /** All captures from any member of a team, merged. */
    fun getTeamCaptures(teamNumber: Int): Set<String> {
        val teamPlayerIds = getTeamPlayers(teamNumber).map { it.id }
        return teamPlayerIds.flatMap { gameplay.captures[it] ?: emptySet() }.toSet()
    }

    fun isTeamCaptured(teamNumber: Int, categoryId: String): Boolean =
        getTeamCaptures(teamNumber).contains(categoryId)

    /** Find which player on a team captured a specific category. */
    fun getTeamCapturer(teamNumber: Int, categoryId: String): Player? {
        val teamPlayers = getTeamPlayers(teamNumber)
        // Prefer allCaptures (server truth) if available
        if (review.allCaptures.isNotEmpty()) {
            val teamPlayerIds = teamPlayers.map { it.id }.toSet()
            val capture = review.allCaptures
                .filter { it.category_id == categoryId && it.player_id in teamPlayerIds }
                .minByOrNull { it.created_at }
            if (capture != null) return teamPlayers.find { it.id == capture.player_id }
        }
        // Fallback to local captures
        return teamPlayers.firstOrNull { gameplay.captures[it.id]?.contains(categoryId) == true }
    }

    fun getMyTeamNumber(): Int? =
        gameplay.teamAssignments[session.myPlayerId]

    /** Team score: sum of star ratings + speed bonuses. */
    fun getTeamScore(teamNumber: Int): Int {
        if (review.allVotes.isNotEmpty()) {
            var starScore = 0.0
            for (category in gameplay.selectedCategories) {
                val capturer = getTeamCapturer(teamNumber, category.id) ?: continue
                val avg = getCategoryAverageRating(capturer.id, category.id) ?: continue
                starScore += avg
            }
            val speedBonus = getTeamSpeedBonusCount(teamNumber)
            return (starScore + 0.5).toInt() + speedBonus
        }
        // Fallback: count captured categories
        return getTeamCaptures(teamNumber).size
    }

    /** Speed bonuses at team level. */
    fun getTeamSpeedBonusCount(teamNumber: Int): Int {
        val firstCapturers = getFirstCapturers()
        val teamPlayerIds = getTeamPlayers(teamNumber).map { it.id }.toSet()
        return firstCapturers.values.count { it in teamPlayerIds }
    }

    /** Ranked teams by score. Returns (teamNumber, teamName, score). */
    val rankedTeams: List<Triple<Int, String, Int>>
        get() {
            if (!gameplay.teamModeEnabled) return emptyList()
            return getTeamNumbers().map { teamNum ->
                val teamName = gameplay.teamNames[teamNum] ?: "Team $teamNum"
                Triple(teamNum, teamName, getTeamScore(teamNum))
            }.sortedByDescending { it.third }
        }

    // ── Ranked players (derived state) ───────────────────────────────────
    val rankedPlayers: List<Pair<Player, Int>> by derivedStateOf {
        gameplay.players.map { it to getPlayerScore(it.id) }
            .sortedWith(
                compareByDescending<Pair<Player, Int>> { it.second }
                    .thenByDescending { getSpeedBonusCount(it.first.id) }
                    .thenBy { getLastCaptureTime(it.first.id) }
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
        session.currentScreen = Screen.HOME
    }

    fun resetForRematch(newGameId: String, newGameCode: String, newPlayerId: String) {
        ActiveSession.clear()
        cleanupRealtime()
        clearGameplayState()
        session.gameId = newGameId
        session.gameCode = newGameCode
        session.myPlayerId = newPlayerId
        session.isHost = true
        session.currentScreen = Screen.LOBBY
    }

    fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    }
}
