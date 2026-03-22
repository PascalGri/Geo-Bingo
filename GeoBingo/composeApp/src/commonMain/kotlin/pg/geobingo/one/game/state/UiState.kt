package pg.geobingo.one.game.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pg.geobingo.one.game.GameHistoryEntry

class UiState {
    var soundEnabled by mutableStateOf(true)
    var hapticEnabled by mutableStateOf(true)
    var pendingToast by mutableStateOf<String?>(null)
    var consecutiveNetworkErrors by mutableStateOf(0)
    var gameHistory by mutableStateOf(listOf<GameHistoryEntry>())
}
