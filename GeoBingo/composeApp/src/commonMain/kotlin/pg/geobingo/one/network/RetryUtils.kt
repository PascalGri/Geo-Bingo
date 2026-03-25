package pg.geobingo.one.network

import kotlinx.coroutines.delay
import pg.geobingo.one.game.GameConstants
import kotlin.random.Random

/**
 * Retries [block] with exponential backoff and jitter.
 * Only retries on network/unknown errors; non-recoverable errors are rethrown immediately.
 */
suspend fun <T> withRetry(
    maxAttempts: Int = GameConstants.RETRY_MAX_ATTEMPTS,
    initialDelay: Long = GameConstants.RETRY_INITIAL_DELAY_MS,
    maxDelay: Long = GameConstants.RETRY_MAX_DELAY_MS,
    factor: Double = GameConstants.RETRY_BACKOFF_FACTOR,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(maxAttempts - 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            val type = classifyError(e)
            if (type != ErrorType.NETWORK && type != ErrorType.UNKNOWN) throw e
        }
        val jitter = (currentDelay * GameConstants.RETRY_JITTER_FACTOR * Random.nextDouble()).toLong()
        delay(currentDelay + jitter)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return block()
}
