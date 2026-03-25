package pg.geobingo.one.game

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pg.geobingo.one.data.*
import pg.geobingo.one.game.state.*
import pg.geobingo.one.network.CaptureDto
import pg.geobingo.one.network.GameRealtimeManager
import pg.geobingo.one.network.PlayerDto
import pg.geobingo.one.network.VoteDto
import pg.geobingo.one.network.toHex

enum class Screen {
    HOME, HOW_TO_PLAY, SELECT_MODE, CREATE_GAME, JOIN_GAME, LOBBY, GAME, VOTE_TRANSITION, REVIEW, RESULTS_TRANSITION, RESULTS, HISTORY, SETTINGS
}

enum class GameMode { CLASSIC, BLIND_BINGO, WEIRD_CORE, QUICK_START }

data class HistoryPlayer(
    val id: String,
    val name: String,
    val score: Int,
    val colorHex: String,
)

data class GameHistoryEntry(
    val gameCode: String,
    val playerName: String,
    val score: Int,
    val totalCategories: Int,
    val players: List<HistoryPlayer>,
    val jokerMode: Boolean,
    val date: String = "",
)

class GameState {
    // ── Sub-state holders ────────────────────────────────────────────────
    val session = SessionState()
    val gameplay = GamePlayState()
    val photo = PhotoState()
    val review = ReviewState()
    val joker = JokerState()
    val ui = UiState()

    // ── Shared realtime manager ──────────────────────────────────────────
    var realtime: GameRealtimeManager? = null
        private set

    fun ensureRealtime(gameId: String): GameRealtimeManager {
        val existing = realtime
        if (existing != null) return existing
        val mgr = GameRealtimeManager(gameId)
        realtime = mgr
        return mgr
    }

    private fun cleanupRealtime() {
        val rt = realtime ?: return
        realtime = null
        CoroutineScope(Dispatchers.Default).launch {
            try { rt.unsubscribe() } catch (_: Exception) {}
        }
    }

    // ── Convenience accessors (keep common read patterns short) ─────────
    val reviewPlayer: Player? get() = gameplay.players.getOrNull(review.reviewPlayerIndex)

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

    // ── Captures ────────────────────────────────────────────────────────
    fun toggleCapture(playerId: String, categoryId: String) {
        val updated = gameplay.captures.toMutableMap()
        val current = (updated[playerId] ?: emptySet()).toMutableSet()
        if (current.contains(categoryId)) current.remove(categoryId) else current.add(categoryId)
        updated[playerId] = current
        gameplay.captures = updated
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
            if (gameplay.selectedCategories.all { photo.photoCache.contains(playerId, it.id) }) {
                review.allCategoriesCaptured = true
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

    fun getVoteResult(playerId: String, categoryId: String): Boolean? {
        val key = "$playerId-$categoryId"
        val v = review.votes[key] ?: return null
        if (v.isEmpty()) return null
        return v.count { it } > v.size / 2
    }

    // ── Scoring ─────────────────────────────────────────────────────────
    fun getFirstCapturers(): Map<String, String> {
        if (review.allCaptures.isEmpty()) return emptyMap()
        return gameplay.selectedCategories.mapNotNull { category ->
            val first = review.allCaptures
                .filter { it.category_id == category.id && it.created_at.isNotEmpty() }
                .minByOrNull { it.created_at }
            if (first != null) category.id to first.player_id else null
        }.toMap()
    }

    fun getSpeedBonusCount(playerId: String): Int {
        val firstCapturers = getFirstCapturers()
        return firstCapturers.values.count { it == playerId }
    }

    fun getCategoryAverageRating(playerId: String, categoryId: String): Double? {
        val votesForThis = review.allVotes.filter { it.target_player_id == playerId && it.category_id == categoryId }
        if (votesForThis.isEmpty()) return null
        return votesForThis.map { it.rating }.average()
    }

    fun getPlayerAverageRating(playerId: String): Double? {
        val ratings = gameplay.selectedCategories.mapNotNull { getCategoryAverageRating(playerId, it.id) }
        if (ratings.isEmpty()) return null
        return ratings.average()
    }

    fun getPlayerScore(playerId: String): Int {
        val starScore: Int
        if (review.allVotes.isNotEmpty()) {
            var sum = 0.0
            for (category in gameplay.selectedCategories) {
                val capturesForPlayer = review.allCaptures.filter { it.player_id == playerId && it.category_id == category.id }
                if (capturesForPlayer.isEmpty()) continue
                val avg = getCategoryAverageRating(playerId, category.id) ?: continue
                sum += avg
            }
            starScore = (sum + 0.5).toInt()
        } else {
            starScore = gameplay.selectedCategories.count { category ->
                if (!isCaptured(playerId, category.id)) return@count false
                getVoteResult(playerId, category.id) ?: true
            }
        }
        return starScore + getSpeedBonusCount(playerId)
    }

    fun getPlayerCaptures(playerId: String): List<Category> {
        val capturedIds = gameplay.captures[playerId] ?: emptySet()
        return gameplay.selectedCategories.filter { it.id in capturedIds }
    }

    fun getLastCaptureTime(playerId: String): String {
        return review.allCaptures
            .filter { it.player_id == playerId && it.created_at.isNotEmpty() }
            .maxOfOrNull { it.created_at } ?: "9999-99-99T99:99:99Z"
    }

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
        val myId = session.myPlayerId ?: return
        val myPlayer = gameplay.players.find { it.id == myId } ?: return
        val now = kotlinx.datetime.Clock.System.now().toString()
        val entry = GameHistoryEntry(
            gameCode = session.gameCode ?: "?",
            playerName = myPlayer.name,
            score = getPlayerScore(myId),
            totalCategories = gameplay.selectedCategories.size,
            players = rankedPlayers.map { (p, s) -> HistoryPlayer(id = p.id, name = p.name, score = s, colorHex = p.color.toHex()) },
            jokerMode = joker.jokerMode,
            date = now,
        )
        ui.gameHistory = listOf(entry) + ui.gameHistory
    }

    // ── Reset ───────────────────────────────────────────────────────────
    private fun clearGameplayState() {
        gameplay.players = listOf()
        gameplay.lobbyPlayers = listOf()
        gameplay.timeRemainingSeconds = 0
        gameplay.isGameRunning = false
        gameplay.currentPlayerIndex = 0
        gameplay.captures = mapOf()
        photo.photoCache.clear()
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
        joker.myJokerUsed = false
        joker.jokerLabels = mapOf()
        ui.consecutiveNetworkErrors = 0
        photo.playerAvatarBytes = mapOf()
        photo.triedAvatarDownloads = setOf()
        photo.uploadingCategories = setOf()
    }

    fun resetGame() {
        cleanupRealtime()
        clearGameplayState()
        gameplay.selectedCategories = listOf()
        gameplay.gameDurationMinutes = 15
        session.gameId = null
        session.gameCode = null
        session.isHost = false
        session.myPlayerId = null
        joker.jokerMode = false
        session.gameMode = GameMode.CLASSIC
        session.currentScreen = Screen.HOME
    }

    fun resetForRematch(newGameId: String, newGameCode: String, newPlayerId: String) {
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
