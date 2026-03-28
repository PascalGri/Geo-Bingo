package pg.geobingo.one.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import pg.geobingo.one.network.AccountManager
import pg.geobingo.one.network.supabase
import pg.geobingo.one.platform.AppSettings
import io.github.jan.supabase.postgrest.postgrest

/**
 * Analytics tracker. Counts events locally and sends them to Supabase
 * for aggregated reporting across all users and platforms.
 */
object Analytics {
    private const val PREFIX = "analytics_"
    private val scope = CoroutineScope(Dispatchers.Default)

    // ── Event names ─────────────────────────────────────────────────────
    const val GAME_CREATED = "game_created"
    const val GAME_JOINED = "game_joined"
    const val GAME_STARTED = "game_started"
    const val GAME_COMPLETED = "game_completed"
    const val PHOTO_CAPTURED = "photo_captured"
    const val VOTE_CAST = "vote_cast"
    const val REMATCH_SAME = "rematch_same"
    const val REMATCH_NEW = "rematch_new"
    const val SHARE_RESULTS = "share_results"
    const val AD_WATCHED = "ad_watched"
    const val MODE_SELECTED = "mode_selected"
    const val SOLO_GAME_STARTED = "solo_game_started"
    const val SOLO_GAME_COMPLETED = "solo_game_completed"
    const val APP_OPENED = "app_opened"
    const val STARS_PURCHASED = "stars_purchased"
    const val NO_ADS_PURCHASED = "no_ads_purchased"
    const val SIGN_UP = "sign_up"
    const val SIGN_IN = "sign_in"

    /** Platform identifier, set at app startup by each platform. */
    var platform: String = "unknown"

    /**
     * Track an event. Increments a persistent local counter and sends to Supabase.
     * [params] are optional key-value metadata.
     */
    fun track(event: String, params: Map<String, String> = emptyMap()) {
        val count = AppSettings.getInt("$PREFIX$event", 0) + 1
        AppSettings.setInt("$PREFIX$event", count)
        val paramStr = if (params.isEmpty()) "" else " ${params.entries.joinToString { "${it.key}=${it.value}" }}"
        AppLogger.d("Analytics", "$event (#$count)$paramStr")

        // Send to Supabase in background
        scope.launch {
            try {
                supabase.postgrest["analytics_events"].insert(
                    AnalyticsEvent(
                        event = event,
                        platform = platform,
                        user_id = AccountManager.currentUserId,
                        params = params,
                    )
                )
            } catch (_: Exception) {
                // Silently fail - analytics should never block the app
            }
        }
    }

    /** Get the total local count for a specific event. */
    fun getCount(event: String): Int = AppSettings.getInt("$PREFIX$event", 0)
}

@Serializable
data class AnalyticsEvent(
    val event: String,
    val platform: String,
    val user_id: String? = null,
    val params: Map<String, String> = emptyMap(),
)
