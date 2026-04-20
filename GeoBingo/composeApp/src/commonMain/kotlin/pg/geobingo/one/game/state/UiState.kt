package pg.geobingo.one.game.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.Json
import pg.geobingo.one.game.GameHistoryEntry
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.SettingsKeys
import pg.geobingo.one.util.AppLogger

private const val MAX_HISTORY_ENTRIES = 50
private val historyJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/**
 * Short-lived reward notification driving the global RewardEarnedOverlay.
 * `stars` shows a star badge when > 0. `label` is the user-facing headline
 * (e.g. the cosmetic name, "Daily Bonus", "Dankeschön"). Consumed exactly
 * once — whoever reads it must set `pendingReward = null` when the animation
 * finishes.
 */
data class RewardEvent(
    val label: String,
    val stars: Int = 0,
    val emoji: String? = null,
) {
    val triggerKey: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
}

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
    var pendingReward by mutableStateOf<RewardEvent?>(null)
    var showDailyBonusBanner by mutableStateOf(false)
    var pendingGameInviteCode by mutableStateOf<String?>(null)
    var consecutiveNetworkErrors by mutableStateOf(0)
    var interstitialShown by mutableStateOf(false)

    // Game history is persisted in AppSettings as JSON for signed-in users.
    // Guests (no account) see an empty in-memory history that never hits disk
    // — per product decision 2026-04-14, guest sessions keep zero local state.
    private var _gameHistory by mutableStateOf(
        if (pg.geobingo.one.network.AccountManager.isLoggedIn) loadHistoryFromStorage() else emptyList()
    )
    var gameHistory: List<GameHistoryEntry>
        get() = _gameHistory
        set(value) {
            val trimmed = if (value.size > MAX_HISTORY_ENTRIES) value.take(MAX_HISTORY_ENTRIES) else value
            _gameHistory = trimmed
            // Only persist for signed-in users; guests keep history in-memory only.
            if (pg.geobingo.one.network.AccountManager.isLoggedIn) {
                saveHistoryToStorage(trimmed)
            }
        }

    /** Called on sign-out / user-switch so the previous identity's history is wiped. */
    fun clearGameHistory() {
        _gameHistory = emptyList()
        AppSettings.setString(SettingsKeys.GAME_HISTORY_JSON, "")
    }

    var selectedDmFriendId by mutableStateOf<String?>(null)
    var selectedDmFriendName by mutableStateOf("")
    var selectedMatchGameId by mutableStateOf<String?>(null)
    var selectedMatchEntry by mutableStateOf<GameHistoryEntry?>(null)
}

private fun loadHistoryFromStorage(): List<GameHistoryEntry> {
    val raw = AppSettings.getString(SettingsKeys.GAME_HISTORY_JSON, "")
    if (raw.isBlank()) return emptyList()
    return try {
        historyJson.decodeFromString(kotlinx.serialization.builtins.ListSerializer(GameHistoryEntry.serializer()), raw)
    } catch (e: Exception) {
        AppLogger.w("UiState", "gameHistory JSON decode failed; resetting", e)
        emptyList()
    }
}

private fun saveHistoryToStorage(entries: List<GameHistoryEntry>) {
    try {
        AppSettings.setString(
            SettingsKeys.GAME_HISTORY_JSON,
            historyJson.encodeToString(kotlinx.serialization.builtins.ListSerializer(GameHistoryEntry.serializer()), entries),
        )
    } catch (e: Exception) {
        AppLogger.w("UiState", "gameHistory JSON encode failed", e)
    }
}
