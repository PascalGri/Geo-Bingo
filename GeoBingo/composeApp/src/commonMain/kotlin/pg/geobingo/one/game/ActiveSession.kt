package pg.geobingo.one.game

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pg.geobingo.one.data.Category
import pg.geobingo.one.game.state.SoloState
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.toCategory
import pg.geobingo.one.network.toPlayer
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.util.AppLogger
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Serializable
private data class SoloCategoryDto(val id: String, val name: String, val emoji: String, val description: String = "")

@Serializable
private data class SoloSessionSnapshot(
    val categories: List<SoloCategoryDto>,
    val captured: List<String>,
    val captureTimestamps: Map<String, Long>,
    val ratings: Map<String, Int>,
    val reasons: Map<String, String>,
    val playerName: String,
    val isOutdoor: Boolean,
    val categoryCount: Int,
    val totalDurationSeconds: Int,
    val startTimeMillis: Long,
    val savedAtMillis: Long,
)

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
    private const val KEY_SOLO_SNAPSHOT = "active_session_solo_snapshot"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

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
        AppSettings.setString(KEY_SOLO_SNAPSHOT, "")
        AppLogger.d("ActiveSession", "Cleared session")
    }

    /** Check if there's a multiplayer session to rejoin. */
    fun exists(): Boolean = AppSettings.getString(KEY_GAME_ID).isNotBlank()

    /** Get the persisted game code for display. */
    fun getGameCode(): String = AppSettings.getString(KEY_GAME_CODE)

    /** Get the persisted player name for display. */
    fun getPlayerName(): String = AppSettings.getString(KEY_PLAYER_NAME)

    // ── Solo-mode persistence ─────────────────────────────────────────────
    //
    // Solo rounds are time-boxed (5–10 min) and entirely client-side, so we
    // snapshot the SoloState to disk on every capture. On relaunch we restore
    // the same categories, captured set, ratings, player name and remaining
    // time — continuing exactly where the user left off.

    /** Snapshot the in-progress solo round to disk. */
    fun saveSolo(solo: SoloState) {
        if (solo.categories.isEmpty() || solo.startTimeMillis == 0L) return
        val snap = SoloSessionSnapshot(
            categories = solo.categories.map { SoloCategoryDto(it.id, it.name, it.emoji, it.description) },
            captured = solo.capturedCategories.toList(),
            captureTimestamps = solo.captureTimestamps,
            ratings = solo.categoryRatings,
            reasons = solo.categoryReasons,
            playerName = solo.playerName,
            isOutdoor = solo.isOutdoor,
            categoryCount = solo.categoryCount,
            totalDurationSeconds = solo.totalDurationSeconds,
            startTimeMillis = solo.startTimeMillis,
            savedAtMillis = Clock.System.now().toEpochMilliseconds(),
        )
        try {
            AppSettings.setString(KEY_SOLO_SNAPSHOT, json.encodeToString(SoloSessionSnapshot.serializer(), snap))
        } catch (e: Exception) {
            AppLogger.w("ActiveSession", "saveSolo failed", e)
        }
    }

    /** Wipe any solo snapshot (round finished, results shown, or manual leave). */
    fun clearSolo() {
        AppSettings.setString(KEY_SOLO_SNAPSHOT, "")
    }

    /** Does a (non-expired) solo snapshot exist? */
    fun hasSoloSession(): Boolean = loadSoloSnapshot()?.let { it.remainingSeconds() > 10 } ?: false

    /** Display metadata for the rejoin banner. */
    data class SoloSessionMeta(
        val playerName: String,
        val capturedCount: Int,
        val totalCategories: Int,
        val remainingSeconds: Int,
    )

    fun getSoloSessionMeta(): SoloSessionMeta? {
        val snap = loadSoloSnapshot() ?: return null
        return SoloSessionMeta(
            playerName = snap.playerName,
            capturedCount = snap.captured.size,
            totalCategories = snap.categories.size,
            remainingSeconds = snap.remainingSeconds(),
        )
    }

    /**
     * Restore the snapshot into gameState.solo and return the SOLO_GAME screen
     * so the caller can navigate. Returns null if the snapshot is missing or
     * already expired (more time elapsed than the round's duration).
     */
    fun rejoinSolo(gameState: GameState): Screen? {
        val snap = loadSoloSnapshot() ?: return null
        val remaining = snap.remainingSeconds()
        if (remaining <= 10) {
            clearSolo()
            return null
        }
        val solo = gameState.solo
        solo.categories = snap.categories.map { Category(it.id, it.name, it.emoji, it.description) }
        solo.capturedCategories = snap.captured.toSet()
        solo.captureTimestamps = snap.captureTimestamps
        solo.categoryRatings = snap.ratings
        solo.categoryReasons = snap.reasons
        solo.validatingCategories = emptySet()
        solo.playerName = snap.playerName
        solo.isOutdoor = snap.isOutdoor
        solo.categoryCount = snap.categoryCount
        solo.totalDurationSeconds = snap.totalDurationSeconds
        solo.startTimeMillis = snap.startTimeMillis
        solo.timeRemainingSeconds = remaining
        solo.isRunning = true
        return Screen.SOLO_GAME
    }

    private fun loadSoloSnapshot(): SoloSessionSnapshot? {
        val raw = AppSettings.getString(KEY_SOLO_SNAPSHOT)
        if (raw.isBlank()) return null
        return try {
            json.decodeFromString(SoloSessionSnapshot.serializer(), raw)
        } catch (e: Exception) {
            AppLogger.w("ActiveSession", "loadSoloSnapshot decode failed — clearing", e)
            clearSolo()
            null
        }
    }

    private fun SoloSessionSnapshot.remainingSeconds(): Int {
        val elapsed = ((Clock.System.now().toEpochMilliseconds() - startTimeMillis) / 1000L).toInt()
        return (totalDurationSeconds - elapsed).coerceAtLeast(0)
    }

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

            // Validate the player is still in the game
            if (playerDtos.none { it.id == playerId }) {
                AppLogger.w("ActiveSession", "Player $playerId no longer in game $gameId")
                clear()
                return null
            }

            gameState.gameplay.players = playerDtos.map { it.toPlayer() }
            gameState.gameplay.lobbyPlayers = playerDtos
            gameState.gameplay.selectedCategories = categoryDtos.map { it.toCategory() }
            gameState.gameplay.currentPlayerIndex = playerDtos.indexOfFirst { it.id == playerId }
                .takeIf { it >= 0 } ?: 0

            // Load team assignments and names
            try {
                val teams = GameRepository.getTeamAssignments(gameId)
                if (teams.isNotEmpty()) {
                    gameState.gameplay.teamModeEnabled = true
                    gameState.gameplay.teamAssignments = teams
                    try {
                        val names = GameRepository.getTeamNames(gameId)
                        if (names.isNotEmpty()) gameState.gameplay.teamNames = names
                    } catch (e: Exception) {
                        AppLogger.w("ActiveSession", "Team names load failed", e)
                    }
                }
            } catch (e: Exception) {
                AppLogger.w("ActiveSession", "Team assignment load failed", e)
            }

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

                    // Calculate remaining time from server timestamp
                    gameState.gameplay.timeRemainingSeconds = estimateRemainingTime(game.created_at, game.duration_s)
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

    /**
     * Calculate remaining game time from the server's created_at timestamp.
     * Falls back to half the duration if parsing fails.
     */
    private fun estimateRemainingTime(createdAt: String, durationSeconds: Int): Int {
        if (createdAt.isBlank()) return durationSeconds / 2
        return try {
            val startInstant = Instant.parse(createdAt)
            val elapsed = (Clock.System.now() - startInstant).inWholeSeconds.toInt()
            (durationSeconds - elapsed).coerceIn(10, durationSeconds)
        } catch (_: Exception) {
            durationSeconds / 2
        }
    }
}
