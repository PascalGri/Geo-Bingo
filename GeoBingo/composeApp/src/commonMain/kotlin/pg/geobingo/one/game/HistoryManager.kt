package pg.geobingo.one.game

import pg.geobingo.one.data.Player
import pg.geobingo.one.game.state.GamePlayState
import pg.geobingo.one.game.state.SessionState
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
        )
        ui.gameHistory = listOf(entry) + ui.gameHistory
    }
}
