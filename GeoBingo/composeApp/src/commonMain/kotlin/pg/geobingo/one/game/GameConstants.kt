package pg.geobingo.one.game

import kotlin.time.Duration.Companion.seconds

object GameConstants {
    // Polling intervals
    const val POLLING_INITIAL_INTERVAL_MS = 3_000L
    const val POLLING_MAX_INTERVAL_MS = 15_000L
    const val POLLING_BACKOFF_FACTOR = 1.5

    // Lobby
    const val LOBBY_TIMEOUT_SECONDS = 300

    // Game timer
    const val DEFAULT_GAME_DURATION_MINUTES = 15
    const val MIN_GAME_DURATION_MINUTES = 5
    const val MAX_GAME_DURATION_MINUTES = 60
    const val TIMER_TICK_MS = 1_000L
    const val FINISH_COUNTDOWN_SECONDS = 30

    // Waiting / Review
    const val WAITING_POLL_INTERVAL_MS = 1_500L
    const val SELF_VOTE_TOAST_DELAY_MS = 1_200L

    // Upload / UI feedback
    const val UPLOAD_SUCCESS_TOAST_MS = 1_500L

    // Photo cache
    const val PHOTO_CACHE_MAX_ENTRIES = 30

    // Storage URL signature durations
    val AVATAR_URL_EXPIRY = 3600.seconds
    val CAPTURE_URL_EXPIRY = 86400.seconds

    // Placeholder for "no timestamp"
    const val INFINITY_TIME = "9999-99-99T99:99:99Z"

    // Results
    const val RESULTS_CLEANUP_DELAY_MS = 10_000L

    // Retry defaults
    const val RETRY_MAX_ATTEMPTS = 3
    const val RETRY_INITIAL_DELAY_MS = 1_000L
    const val RETRY_MAX_DELAY_MS = 10_000L
    const val RETRY_BACKOFF_FACTOR = 2.0
    const val RETRY_JITTER_FACTOR = 0.2
}
