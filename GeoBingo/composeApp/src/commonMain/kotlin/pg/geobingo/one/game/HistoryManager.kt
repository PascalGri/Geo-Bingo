package pg.geobingo.one.game

import pg.geobingo.one.data.Player
import pg.geobingo.one.game.state.GamePlayState
import pg.geobingo.one.game.state.SessionState
import pg.geobingo.one.game.state.SoloState
import pg.geobingo.one.game.state.UiState
import pg.geobingo.one.network.toHex

/**
 * Manages game history: saving completed game results.
 */
class HistoryManager(
    private val session: SessionState,
    private val gameplay: GamePlayState,
    private val ui: UiState,
    private val scoring: ScoringManager,
) {
    fun saveToHistory(jokerMode: Boolean, rankedPlayers: List<Pair<Player, Int>>) {
        val myId = session.myPlayerId ?: return
        val myPlayer = gameplay.players.find { it.id == myId } ?: return
        val now = kotlinx.datetime.Clock.System.now().toString()
        val entry = GameHistoryEntry(
            gameCode = session.gameCode ?: "?",
            playerName = myPlayer.name,
            score = rankedPlayers.find { it.first.id == myId }?.second ?: 0,
            totalCategories = gameplay.selectedCategories.size,
            players = rankedPlayers.map { (p, s) -> HistoryPlayer(id = p.id, name = p.name, score = s, colorHex = p.color.toHex()) },
            jokerMode = jokerMode,
            date = now,
            gameId = session.gameId ?: "",
            categories = gameplay.selectedCategories.map { HistoryCategory(id = it.id, name = it.name) },
        )
        ui.gameHistory = listOf(entry) + ui.gameHistory
    }

    /**
     * Append a completed solo round to the shared game-history list. Solo
     * rounds used to only update `SoloStatsManager` (for the profile-page
     * aggregates) and never showed up in the per-round "Spielverlauf"
     * history — so the user's history looked empty for solo-only players.
     * Uses the "SOLO" gameCode sentinel so HistoryScreen can render it
     * differently if needed.
     */
    fun saveSoloToHistory(solo: SoloState, playerName: String) {
        if (solo.categories.isEmpty()) return
        val now = kotlinx.datetime.Clock.System.now().toString()
        val entry = GameHistoryEntry(
            gameCode = "SOLO",
            playerName = playerName,
            score = solo.totalScore,
            totalCategories = solo.categoryCount,
            players = listOf(
                HistoryPlayer(
                    id = session.myPlayerId ?: "solo",
                    name = playerName,
                    score = solo.totalScore,
                    colorHex = "#6366F1",
                )
            ),
            jokerMode = false,
            date = now,
            gameId = "",
            categories = solo.categories.map { HistoryCategory(id = it.id, name = it.name) },
        )
        ui.gameHistory = listOf(entry) + ui.gameHistory
    }
}
