package pg.geobingo.one.game.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.days
import pg.geobingo.one.game.GameConstants
import pg.geobingo.one.game.GameHistoryEntry
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.platform.SettingsKeys
import pg.geobingo.one.util.AppLogger

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
        if (pg.geobingo.one.network.AccountManager.isLoggedIn) loadAndPruneHistory() else emptyList()
    )
    var gameHistory: List<GameHistoryEntry>
        get() = _gameHistory
        set(value) {
            val previous = _gameHistory
            // Keep only games from the last 7 days (plus a safety cap). Newest first.
            val kept = retainHistory(value)
            _gameHistory = kept
            // Only persist for signed-in users; guests keep history in-memory only.
            if (pg.geobingo.one.network.AccountManager.isLoggedIn) {
                saveHistoryToStorage(kept)
            }
            // Free disk for any game that dropped out — aged past 7 days, pushed
            // past the cap, or swiped away by the user.
            deletePhotosForGames(previous.map { it.gameId } - kept.map { it.gameId }.toSet())
        }

    /** Called on sign-out / user-switch so the previous identity's history is wiped. */
    fun clearGameHistory() {
        _gameHistory = emptyList()
        AppSettings.setString(SettingsKeys.GAME_HISTORY_JSON, "")
    }

    /**
     * Re-read persisted history from storage once auth has settled.
     *
     * UiState is constructed at app launch, BEFORE Supabase finishes restoring
     * the session asynchronously — so the constructor's load sees
     * isLoggedIn=false and leaves history empty even though the signed-in
     * user's history is on disk. handleAppStartup() calls this after
     * awaitInitialization() (mirroring StarsState.reload()) so the history
     * survives a restart instead of vanishing. Guarded on isEmpty so it only
     * recovers the lost-on-startup case and never clobbers entries already
     * added this session.
     */
    fun reloadGameHistory() {
        if (pg.geobingo.one.network.AccountManager.isLoggedIn && _gameHistory.isEmpty()) {
            _gameHistory = loadAndPruneHistory()
        }
    }

    var selectedDmFriendId by mutableStateOf<String?>(null)
    var selectedDmFriendName by mutableStateOf("")
    var selectedMatchGameId by mutableStateOf<String?>(null)
    var selectedMatchEntry by mutableStateOf<GameHistoryEntry?>(null)
}

/**
 * Keep only games played within the retention window (last 7 days), newest
 * first, capped at a generous safety limit. Entries with an unparseable/blank
 * date are kept (legacy rows pre-dating the timestamp field).
 */
private fun retainHistory(entries: List<GameHistoryEntry>): List<GameHistoryEntry> {
    val cutoff = Clock.System.now() - GameConstants.HISTORY_RETENTION_DAYS.days
    val fresh = entries.filter { e ->
        val ts = parseHistoryDate(e.date) ?: return@filter true
        ts >= cutoff
    }
    return if (fresh.size > GameConstants.HISTORY_MAX_ENTRIES) fresh.take(GameConstants.HISTORY_MAX_ENTRIES) else fresh
}

private fun parseHistoryDate(raw: String): Instant? =
    if (raw.isBlank()) null else try { Instant.parse(raw) } catch (e: Exception) { null }

/** Load persisted history, prune anything older than the window, and persist + GC if it shrank. */
private fun loadAndPruneHistory(): List<GameHistoryEntry> {
    val raw = loadHistoryFromStorage()
    val kept = retainHistory(raw)
    if (kept.size != raw.size) {
        saveHistoryToStorage(kept)
        deletePhotosForGames(raw.map { it.gameId } - kept.map { it.gameId }.toSet())
    }
    return kept
}

/** Best-effort, off-thread deletion of the on-disk photo folders for dropped games. */
private fun deletePhotosForGames(gameIds: Collection<String>) {
    val ids = gameIds.filter { it.isNotBlank() }.distinct()
    if (ids.isEmpty()) return
    CoroutineScope(Dispatchers.Default).launch {
        ids.forEach {
            try { LocalPhotoStore.deleteGame(it) } catch (e: Exception) {
                AppLogger.w("UiState", "history photo prune failed for $it", e)
            }
        }
    }
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
