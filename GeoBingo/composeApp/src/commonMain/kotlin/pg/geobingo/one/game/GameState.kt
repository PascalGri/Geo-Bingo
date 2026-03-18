package pg.geobingo.one.game

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pg.geobingo.one.data.*
import pg.geobingo.one.network.CaptureDto
import pg.geobingo.one.network.GameRealtimeManager
import pg.geobingo.one.network.PlayerDto
import pg.geobingo.one.network.VoteDto

enum class Screen {
    HOME, HOW_TO_PLAY, CREATE_GAME, JOIN_GAME, LOBBY, GAME, REVIEW, RESULTS, HISTORY
}

data class GameHistoryEntry(
    val gameCode: String,
    val playerName: String,
    val score: Int,
    val totalCategories: Int,
    val players: List<Pair<String, Int>>, // name to score
    val jokerMode: Boolean,
)

class GameState {
    var currentScreen by mutableStateOf(Screen.HOME)

    // Shared realtime manager – created once per game, survives screen transitions
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

    // Multiplayer
    var gameId by mutableStateOf<String?>(null)
    var gameCode by mutableStateOf<String?>(null)
    var isHost by mutableStateOf(false)
    var myPlayerId by mutableStateOf<String?>(null)
    var lobbyPlayers by mutableStateOf(listOf<PlayerDto>())

    // Setup
    var players by mutableStateOf(listOf<Player>())
    var selectedCategories by mutableStateOf(listOf<Category>())
    var gameDurationMinutes by mutableStateOf(15)

    // Game
    var timeRemainingSeconds by mutableStateOf(0)
    var isGameRunning by mutableStateOf(false)
    var currentPlayerIndex by mutableStateOf(0)

    // Captures: playerId -> Set of captured categoryIds
    var captures by mutableStateOf(mapOf<String, Set<String>>())

    // Photos: playerId -> (categoryId -> ByteArray)
    var photos by mutableStateOf(mapOf<String, Map<String, ByteArray>>())

    // Votes: "$playerId-$categoryId" -> list of approvals
    var votes by mutableStateOf(mapOf<String, List<Boolean>>())

    // Review
    var reviewPlayerIndex by mutableStateOf(0)

    // Review sync state
    var reviewCategoryIndex by mutableStateOf(0)
    var allCaptures by mutableStateOf(listOf<CaptureDto>())
    var categoryVotes by mutableStateOf(mapOf<String, Boolean>()) // targetPlayerId -> approved
    var hasSubmittedCurrentCategory by mutableStateOf(false)
    var hasVotedToEnd by mutableStateOf(false)
    var endVoteCount by mutableStateOf(0)
    var allCategoriesCaptured by mutableStateOf(false)
    var finishSignalDetected by mutableStateOf(false)
    var allVotes by mutableStateOf(listOf<VoteDto>())

    // Joker mode
    var jokerMode by mutableStateOf(false)
    var myJokerUsed by mutableStateOf(false)
    var jokerLabels by mutableStateOf(mapOf<String, String>()) // playerId → custom label

    // One-shot message shown on the next screen (e.g. "Lobby was closed")
    var pendingToast by mutableStateOf<String?>(null)

    // In-memory game history (persists within app session)
    var gameHistory by mutableStateOf(listOf<GameHistoryEntry>())

    // Offline detection: number of consecutive poll failures
    var consecutiveNetworkErrors by mutableStateOf(0)

    // Avatar photos: playerId → ByteArray (in-memory cache, populated from selfie upload/download)
    var playerAvatarBytes by mutableStateOf(mapOf<String, ByteArray>())
    // Track players we already tried downloading avatars for (to avoid retrying every poll)
    var triedAvatarDownloads by mutableStateOf(setOf<String>())

    // Uploading state
    var uploadingCategories by mutableStateOf(setOf<String>())

    val currentPlayer: Player? get() = players.getOrNull(currentPlayerIndex)
    val reviewPlayer: Player? get() = players.getOrNull(reviewPlayerIndex)

    fun startGame() {
        timeRemainingSeconds = gameDurationMinutes * 60
        isGameRunning = true
        currentPlayerIndex = 0
        captures = players.associate { it.id to emptySet() }
        photos = players.associate { it.id to emptyMap() }
        votes = emptyMap()
        currentScreen = Screen.GAME
    }

    fun toggleCapture(playerId: String, categoryId: String) {
        val updated = captures.toMutableMap()
        val current = (updated[playerId] ?: emptySet()).toMutableSet()
        if (current.contains(categoryId)) current.remove(categoryId) else current.add(categoryId)
        updated[playerId] = current
        captures = updated
    }

    fun isCaptured(playerId: String, categoryId: String): Boolean =
        captures[playerId]?.contains(categoryId) == true

    fun addPhoto(playerId: String, categoryId: String, bytes: ByteArray) {
        val updated = photos.toMutableMap()
        val playerPhotos = (updated[playerId] ?: emptyMap()).toMutableMap()
        playerPhotos[categoryId] = bytes
        updated[playerId] = playerPhotos
        photos = updated
        // Auto-mark as captured when photo is taken
        if (!isCaptured(playerId, categoryId)) {
            toggleCapture(playerId, categoryId)
        }
        // Signal countdown when this player has photographed every category
        if (isGameRunning && selectedCategories.isNotEmpty() && !allCategoriesCaptured) {
            val playerPhotos2 = updated[playerId] ?: emptyMap()
            if (selectedCategories.all { it.id in playerPhotos2 }) {
                allCategoriesCaptured = true
            }
        }
    }

    fun getPhoto(playerId: String, categoryId: String): ByteArray? =
        photos[playerId]?.get(categoryId)

    fun endGame() {
        isGameRunning = false
        // Navigation handled by GameScreen after Supabase update
    }

    fun submitVotes(targetPlayerId: String, approvedCategoryIds: Set<String>) {
        val updated = votes.toMutableMap()
        getPlayerCaptures(targetPlayerId).forEach { category ->
            val key = "$targetPlayerId-${category.id}"
            val approved = category.id in approvedCategoryIds
            val existing = (updated[key] ?: emptyList()).toMutableList()
            existing.add(approved)
            updated[key] = existing
        }
        votes = updated
    }

    fun getVoteResult(playerId: String, categoryId: String): Boolean? {
        val key = "$playerId-$categoryId"
        val v = votes[key] ?: return null
        if (v.isEmpty()) return null
        return v.count { it } > v.size / 2
    }

    // Returns the playerId who captured each categoryId first (based on created_at)
    fun getFirstCapturers(): Map<String, String> {
        if (allCaptures.isEmpty()) return emptyMap()
        return selectedCategories.mapNotNull { category ->
            val first = allCaptures
                .filter { it.category_id == category.id && it.created_at.isNotEmpty() }
                .minByOrNull { it.created_at }
            if (first != null) category.id to first.player_id else null
        }.toMap()
    }

    fun getSpeedBonusCount(playerId: String): Int {
        val firstCapturers = getFirstCapturers()
        return firstCapturers.values.count { it == playerId }
    }

    fun getPlayerScore(playerId: String): Int {
        val baseScore: Int
        // If we have server votes, use them
        if (allVotes.isNotEmpty()) {
            baseScore = selectedCategories.count { category ->
                val capturesForPlayer = allCaptures.filter { it.player_id == playerId && it.category_id == category.id }
                if (capturesForPlayer.isEmpty()) return@count false
                val votesForThis = allVotes.filter { it.target_player_id == playerId && it.category_id == category.id }
                if (votesForThis.isEmpty()) return@count true // no votes = approved by default
                votesForThis.count { it.approved } > votesForThis.size / 2
            }
        } else {
            // Fallback: local votes
            baseScore = selectedCategories.count { category ->
                if (!isCaptured(playerId, category.id)) return@count false
                getVoteResult(playerId, category.id) ?: true
            }
        }
        return baseScore + getSpeedBonusCount(playerId)
    }

    fun getPlayerCaptures(playerId: String): List<Category> {
        val capturedIds = captures[playerId] ?: emptySet()
        return selectedCategories.filter { it.id in capturedIds }
    }

    fun getRankedPlayers(): List<Pair<Player, Int>> =
        players.map { it to getPlayerScore(it.id) }
            .sortedWith(compareByDescending<Pair<Player, Int>> { it.second }.thenByDescending { getSpeedBonusCount(it.first.id) })

    fun saveToHistory() {
        val myId = myPlayerId ?: return
        val myPlayer = players.find { it.id == myId } ?: return
        val entry = GameHistoryEntry(
            gameCode = gameCode ?: "?",
            playerName = myPlayer.name,
            score = getPlayerScore(myId),
            totalCategories = selectedCategories.size,
            players = getRankedPlayers().map { (p, s) -> p.name to s },
            jokerMode = jokerMode,
        )
        gameHistory = listOf(entry) + gameHistory
    }

    private fun clearGameplayState() {
        players = listOf()
        lobbyPlayers = listOf()
        timeRemainingSeconds = 0
        isGameRunning = false
        currentPlayerIndex = 0
        captures = mapOf()
        photos = mapOf()
        votes = mapOf()
        reviewPlayerIndex = 0
        reviewCategoryIndex = 0
        allCaptures = listOf()
        categoryVotes = mapOf()
        hasSubmittedCurrentCategory = false
        allVotes = listOf()
        hasVotedToEnd = false
        endVoteCount = 0
        allCategoriesCaptured = false
        finishSignalDetected = false
        myJokerUsed = false
        jokerLabels = mapOf()
        consecutiveNetworkErrors = 0
        playerAvatarBytes = mapOf()
        triedAvatarDownloads = setOf()
        uploadingCategories = setOf()
    }

    fun resetGame() {
        cleanupRealtime()
        clearGameplayState()
        selectedCategories = listOf()
        gameDurationMinutes = 15
        gameId = null
        gameCode = null
        isHost = false
        myPlayerId = null
        jokerMode = false
        currentScreen = Screen.HOME
    }

    fun resetForRematch(newGameId: String, newGameCode: String, newPlayerId: String) {
        cleanupRealtime()
        clearGameplayState()
        gameId = newGameId
        gameCode = newGameCode
        myPlayerId = newPlayerId
        isHost = true
        // selectedCategories and gameDurationMinutes are intentionally kept for rematch
        currentScreen = Screen.LOBBY
    }

    fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    }
}
