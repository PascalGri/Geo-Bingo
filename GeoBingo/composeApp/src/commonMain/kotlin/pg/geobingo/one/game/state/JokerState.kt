package pg.geobingo.one.game.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class JokerState {
    var jokerMode by mutableStateOf(false)
    var myJokerUsed by mutableStateOf(false)
    var jokerLabels by mutableStateOf(mapOf<String, String>())
}
