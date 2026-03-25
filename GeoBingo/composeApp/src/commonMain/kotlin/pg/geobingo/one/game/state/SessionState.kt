package pg.geobingo.one.game.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pg.geobingo.one.game.GameMode
import pg.geobingo.one.game.Screen

class SessionState {
    var currentScreen by mutableStateOf(Screen.HOME)
    var gameId by mutableStateOf<String?>(null)
    var gameCode by mutableStateOf<String?>(null)
    var isHost by mutableStateOf(false)
    var myPlayerId by mutableStateOf<String?>(null)
    var gameMode by mutableStateOf(GameMode.CLASSIC)
}
