package pg.geobingo.one.network

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * Simple in-memory cache with TTL for network responses.
 */
class ResponseCache<T>(private val ttlMs: Long = 30_000L) {
    private var cachedValue: T? = null
    private var cachedAt: Long = 0L
    private val mutex = Mutex()

    suspend fun getOrFetch(fetch: suspend () -> T): T {
        mutex.withLock {
            val now = Clock.System.now().toEpochMilliseconds()
            val cached = cachedValue
            if (cached != null && (now - cachedAt) < ttlMs) {
                return cached
            }
            val fresh = fetch()
            cachedValue = fresh
            cachedAt = now
            return fresh
        }
    }

    fun invalidate() {
        cachedValue = null
        cachedAt = 0L
    }
}
