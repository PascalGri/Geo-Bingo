package pg.geobingo.one.util

import kotlinx.datetime.Clock

/**
 * Simple client-side rate limiter to prevent spam/abuse.
 * Tracks last action time per key and enforces a cooldown.
 */
object RateLimiter {
    private val timestamps = mutableMapOf<String, Long>()

    /**
     * Returns true if the action is allowed (cooldown has passed).
     * Returns false if rate-limited.
     */
    fun allow(key: String, cooldownMs: Long): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        val last = timestamps[key] ?: 0L
        return if (now - last >= cooldownMs) {
            timestamps[key] = now
            true
        } else {
            false
        }
    }

    // Predefined rate limits
    const val KEY_CREATE_GAME = "create_game"
    const val KEY_SOLO_SUBMIT = "solo_submit"
    const val KEY_JOIN_GAME = "join_game"

    const val GAME_CREATE_COOLDOWN_MS = 10_000L // 10 seconds between game creates
    const val SOLO_SUBMIT_COOLDOWN_MS = 5_000L  // 5 seconds between solo score submits
    const val JOIN_COOLDOWN_MS = 3_000L          // 3 seconds between join attempts
}
