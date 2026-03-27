package pg.geobingo.one.game

import pg.geobingo.one.data.Category
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.toCategory
import pg.geobingo.one.network.toPlayer
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.util.AppLogger

/**
 * Persists the active game session to AppSettings so the player can
 * rejoin if they accidentally close the app/browser during a round.
 */
object ActiveSession {
    private const val KEY_GAME_ID = "active_session_game_id"
    private const val KEY_GAME_CODE = "active_session_game_code"
    private const val KEY_PLAYER_ID = "active_session_player_id"
    private const val KEY_PLAYER_NAME = "active_session_player_name"
    private const val KEY_IS_HOST = "active_session_is_host"
    private const val KEY_GAME_MODE = "active_session_game_mode"

    /** Save session when entering the game phase. */
    fun save(gameState: GameState) {
        val gid = gameState.session.gameId ?: return
        val pid = gameState.session.myPlayerId ?: return
        AppSettings.setString(KEY_GAME_ID, gid)
        AppSettings.setString(KEY_GAME_CODE, gameState.session.gameCode ?: "")
        AppSettings.setString(KEY_PLAYER_ID, pid)
        AppSettings.setString(KEY_PLAYER_NAME,
            gameState.gameplay.players.find { it.id == pid }?.name ?: "")
        AppSettings.setBoolean(KEY_IS_HOST, gameState.session.isHost)
        AppSettings.setString(KEY_GAME_MODE, gameState.session.gameMode.name)
        AppLogger.d("ActiveSession", "Saved session gameId=$gid playerId=$pid")
    }

    /** Clear persisted session (game ended, results shown, or manual leave). */
    fun clear() {
        AppSettings.setString(KEY_GAME_ID, "")
        AppSettings.setString(KEY_GAME_CODE, "")
        AppSettings.setString(KEY_PLAYER_ID, "")
        AppSettings.setString(KEY_PLAYER_NAME, "")
        AppSettings.setBoolean(KEY_IS_HOST, false)
        AppSettings.setString(KEY_GAME_MODE, "")
        AppLogger.d("ActiveSession", "Cleared session")
    }

    /** Check if there's a persisted session to rejoin. */
    fun exists(): Boolean = AppSettings.getString(KEY_GAME_ID).isNotBlank()

    /** Get the persisted game code for display. */
    fun getGameCode(): String = AppSettings.getString(KEY_GAME_CODE)

    /** Get the persisted player name for display. */
    fun getPlayerName(): String = AppSettings.getString(KEY_PLAYER_NAME)

    /**
     * Attempt to rejoin the persisted session.
     * Returns the target Screen if successful, null if the game is no longer active.
     */
    suspend fun rejoin(gameState: GameState): Screen? {
        val gameId = AppSettings.getString(KEY_GAME_ID)
        val playerId = AppSettings.getString(KEY_PLAYER_ID)
        val isHost = AppSettings.getBoolean(KEY_IS_HOST)
        val gameModeStr = AppSettings.getString(KEY_GAME_MODE)

        if (gameId.isBlank() || playerId.isBlank()) {
            clear()
            return null
        }

        return try {
            // Fetch game from server
            val game = GameRepository.getGameById(gameId)
            if (game == null || game.status == "ended" || game.status == "closed") {
                clear()
                return null
            }

            // Restore session state
            gameState.session.gameId = gameId
            gameState.session.gameCode = game.code
            gameState.session.myPlayerId = playerId
            gameState.session.isHost = isHost
            gameState.session.gameMode = try { GameMode.valueOf(game.game_mode) } catch (_: Exception) {
                if (gameModeStr.isNotBlank()) try { GameMode.valueOf(gameModeStr) } catch (_: Exception) { GameMode.CLASSIC }
                else GameMode.CLASSIC
            }
            gameState.gameplay.gameDurationMinutes = game.duration_s / 60
            gameState.joker.jokerMode = game.joker_mode

            // Fetch players and categories
            val playerDtos = GameRepository.getPlayers(gameId)
            val categoryDtos = GameRepository.getCategories(gameId)
            gameState.gameplay.players = playerDtos.map { it.toPlayer() }
            gameState.gameplay.lobbyPlayers = playerDtos
            gameState.gameplay.selectedCategories = categoryDtos.map { it.toCategory() }
            gameState.gameplay.currentPlayerIndex = playerDtos.indexOfFirst { it.id == playerId }
                .takeIf { it >= 0 } ?: 0

            // Load team assignments
            try {
                val teams = GameRepository.getTeamAssignments(gameId)
                if (teams.isNotEmpty()) {
                    gameState.gameplay.teamModeEnabled = true
                    gameState.gameplay.teamAssignments = teams
                }
            } catch (_: Exception) {}

            when (game.status) {
                "lobby" -> {
                    Screen.LOBBY
                }
                "running" -> {
                    // Restore captures from server
                    val captures = GameRepository.getCaptures(gameId)
                    val captureMap = mutableMapOf<String, Set<String>>()
                    captures.forEach { c ->
                        captureMap[c.player_id] = (captureMap[c.player_id] ?: emptySet()) + c.category_id
                    }
                    // Initialize all players with empty captures, then merge server data
                    gameState.gameplay.captures = playerDtos.associate { it.id to emptySet<String>() }
                    gameState.mergeCaptures(captureMap)

                    // Estimate remaining time based on game duration
                    // We don't have exact start time, so use a safe estimate
                    gameState.gameplay.timeRemainingSeconds = game.duration_s / 2 // conservative: half time left
                    gameState.gameplay.isGameRunning = true

                    Screen.GAME
                }
                "voting" -> {
                    // Load all captures and votes for review phase
                    gameState.review.allCaptures = GameRepository.getCaptures(gameId)
                    gameState.review.allVotes = GameRepository.getVotes(gameId)
                    gameState.review.reviewCategoryIndex = game.review_category_index
                    gameState.gameplay.captures = playerDtos.associate { it.id to emptySet<String>() }
                    val captureMap = mutableMapOf<String, Set<String>>()
                    gameState.review.allCaptures.forEach { c ->
                        captureMap[c.player_id] = (captureMap[c.player_id] ?: emptySet()) + c.category_id
                    }
                    gameState.mergeCaptures(captureMap)

                    Screen.REVIEW
                }
                else -> {
                    clear()
                    null
                }
            }
        } catch (e: Exception) {
            AppLogger.w("ActiveSession", "Rejoin failed", e)
            clear()
            null
        }
    }
}
