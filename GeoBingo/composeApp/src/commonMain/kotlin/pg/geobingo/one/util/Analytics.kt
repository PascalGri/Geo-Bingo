package pg.geobingo.one.util

import pg.geobingo.one.platform.AppSettings

/**
 * Lightweight analytics tracker. Counts events locally via AppSettings
 * and logs them for debugging. Can be extended to send events to a backend.
 */
object Analytics {
    private const val PREFIX = "analytics_"

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

    /**
     * Track an event. Increments a persistent counter and logs it.
     * [params] are optional key-value metadata for debugging.
     */
    fun track(event: String, params: Map<String, String> = emptyMap()) {
        val count = AppSettings.getInt("$PREFIX$event", 0) + 1
        AppSettings.setInt("$PREFIX$event", count)
        val paramStr = if (params.isEmpty()) "" else " ${params.entries.joinToString { "${it.key}=${it.value}" }}"
        AppLogger.d("Analytics", "$event (#$count)$paramStr")
    }

    /** Get the total count for a specific event. */
    fun getCount(event: String): Int = AppSettings.getInt("$PREFIX$event", 0)
}
