package pg.geobingo.one.network

import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Retries [block] with exponential backoff and jitter.
 * Only retries on network/unknown errors; non-recoverable errors are rethrown immediately.
 */
suspend fun <T> withRetry(
    maxAttempts: Int = 3,
    initialDelay: Long = 1_000L,
    maxDelay: Long = 10_000L,
    factor: Double = 2.0,
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
        val jitter = (currentDelay * 0.2 * Random.nextDouble()).toLong()
        delay(currentDelay + jitter)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return block()
}
