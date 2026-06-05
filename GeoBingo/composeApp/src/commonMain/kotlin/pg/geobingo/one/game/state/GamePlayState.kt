package pg.geobingo.one.game.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pg.geobingo.one.data.Category
import pg.geobingo.one.data.Player
import pg.geobingo.one.game.GameConstants
import pg.geobingo.one.network.PlayerDto

class GamePlayState {
    var players by mutableStateOf(listOf<Player>())
    var lobbyPlayers by mutableStateOf(listOf<PlayerDto>())
    var selectedCategories by mutableStateOf(listOf<Category>())
    var gameDurationMinutes by mutableStateOf(GameConstants.DEFAULT_GAME_DURATION_MINUTES)
    var timeRemainingSeconds by mutableStateOf(0)
    var isGameRunning by mutableStateOf(false)
    var currentPlayerIndex by mutableStateOf(0)
    var captures by mutableStateOf(mapOf<String, Set<String>>())

    val currentPlayer: Player? get() = players.getOrNull(currentPlayerIndex)
}
