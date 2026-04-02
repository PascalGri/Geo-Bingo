package pg.geobingo.one.game.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pg.geobingo.one.game.GameMode
import pg.geobingo.one.game.Screen
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.SettingsKeys

class SessionState {
    var currentScreen by mutableStateOf(
        if (AppSettings.getBoolean(SettingsKeys.ONBOARDING_COMPLETED)) Screen.HOME else Screen.ONBOARDING
    )
    var gameId by mutableStateOf<String?>(null)
    var gameCode by mutableStateOf<String?>(null)
    var isHost by mutableStateOf(false)
    var myPlayerId by mutableStateOf<String?>(null)
    var gameMode by mutableStateOf(GameMode.CLASSIC)
    var quickStartOutdoor by mutableStateOf(true)
    var quickStartDurationMinutes by mutableStateOf(15)
    var aiJudgeOutdoor by mutableStateOf(true)
    @Deprecated("Difficulty removed – single pool per environment")
    var quickStartDifficulty by mutableStateOf("medium")
}
