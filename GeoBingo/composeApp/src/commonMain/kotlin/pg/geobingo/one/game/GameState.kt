package pg.geobingo.one.game

import androidx.compose.runtime.*
import pg.geobingo.one.data.*
import pg.geobingo.one.network.CaptureDto
import pg.geobingo.one.network.PlayerDto
import pg.geobingo.one.network.VoteDto

enum class Screen {
    HOME, CREATE_GAME, JOIN_GAME, LOBBY, GAME, REVIEW, RESULTS
}

class GameState {
    var currentScreen by mutableStateOf(Screen.HOME)

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
    var allVotes by mutableStateOf(listOf<VoteDto>())

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
        // End game if this player has now photographed every category
        if (isGameRunning && selectedCategories.isNotEmpty()) {
            val playerPhotos2 = updated[playerId] ?: emptyMap()
            if (selectedCategories.all { it.id in playerPhotos2 }) {
                endGame()
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

    fun getPlayerScore(playerId: String): Int {
        // If we have server votes, use them
        if (allVotes.isNotEmpty()) {
            return selectedCategories.count { category ->
                val capturesForPlayer = allCaptures.filter { it.player_id == playerId && it.category_id == category.id }
                if (capturesForPlayer.isEmpty()) return@count false
                val votesForThis = allVotes.filter { it.target_player_id == playerId && it.category_id == category.id }
                if (votesForThis.isEmpty()) return@count true // no votes = approved by default
                votesForThis.count { it.approved } > votesForThis.size / 2
            }
        }
        // Fallback: local votes
        return selectedCategories.count { category ->
            if (!isCaptured(playerId, category.id)) return@count false
            getVoteResult(playerId, category.id) ?: true
        }
    }

    fun getPlayerCaptures(playerId: String): List<Category> {
        val capturedIds = captures[playerId] ?: emptySet()
        return selectedCategories.filter { it.id in capturedIds }
    }

    fun getRankedPlayers(): List<Pair<Player, Int>> =
        players.map { it to getPlayerScore(it.id) }.sortedByDescending { it.second }

    fun resetGame() {
        currentScreen = Screen.HOME
        players = listOf()
        selectedCategories = listOf()
        gameDurationMinutes = 15
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
        gameId = null
        gameCode = null
        isHost = false
        myPlayerId = null
        lobbyPlayers = listOf()
    }

    fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    }
}
