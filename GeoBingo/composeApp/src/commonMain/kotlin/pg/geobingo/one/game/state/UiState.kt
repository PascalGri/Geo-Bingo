package pg.geobingo.one.game.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pg.geobingo.one.game.GameHistoryEntry
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.SettingsKeys

class UiState {
    var soundEnabled by mutableStateOf(AppSettings.getBoolean(SettingsKeys.SOUND_ENABLED, true))
        private set
    var hapticEnabled by mutableStateOf(AppSettings.getBoolean(SettingsKeys.HAPTIC_ENABLED, true))
        private set

    fun updateSoundEnabled(value: Boolean) {
        soundEnabled = value
        AppSettings.setBoolean(SettingsKeys.SOUND_ENABLED, value)
    }

    fun updateHapticEnabled(value: Boolean) {
        hapticEnabled = value
        AppSettings.setBoolean(SettingsKeys.HAPTIC_ENABLED, value)
    }
    var pendingToast by mutableStateOf<String?>(null)
    var pendingGameInviteCode by mutableStateOf<String?>(null)
    var consecutiveNetworkErrors by mutableStateOf(0)
    var gameHistory by mutableStateOf(listOf<GameHistoryEntry>())
}
