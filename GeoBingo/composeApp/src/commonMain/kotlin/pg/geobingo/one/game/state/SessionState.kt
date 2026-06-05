package pg.geobingo.one.game.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pg.geobingo.one.game.GameMode

class SessionState {
    var gameId by mutableStateOf<String?>(null)
    var gameCode by mutableStateOf<String?>(null)
    var isHost by mutableStateOf(false)
    var myPlayerId by mutableStateOf<String?>(null)
    var gameMode by mutableStateOf(GameMode.CLASSIC)
    var quickStartOutdoor by mutableStateOf(true)
    var quickStartDurationMinutes by mutableStateOf(15)

    // Universal indoor/outdoor selection for all modes (except QUICK_START
    // which has its own quickStartOutdoor). Drives which preset pool gets
    // shown / drawn from on CreateGameScreen. Default outdoor — that's the
    // primary value proposition of the app.
    var playOutdoor by mutableStateOf(true)

    // Universal random-pool mode: the DEFAULT for every mode except
    // QUICK_START (which is intrinsically random already). When enabled,
    // the host skips the manual preset/custom picker on CreateGameScreen
    // and the round runs against [randomCategoriesCount] categories drawn
    // from the indoor or outdoor preset pool at start time. The host can
    // opt out via "Eigene Kategorien" to hand-pick instead.
    var randomCategoriesEnabled by mutableStateOf(true)
    var randomCategoriesCount by mutableStateOf(5)
}
