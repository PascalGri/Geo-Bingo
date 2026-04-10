package pg.geobingo.one.network

import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import pg.geobingo.one.util.AppLogger

/**
 * Per-user cosmetic snapshot used to render the PlayerBanner across the app.
 * Cached in-memory to avoid hammering the profiles table on every leaderboard /
 * lobby render.
 */
object PlayerCosmeticsRepository {
    private const val TAG = "PlayerCosmeticsRepository"
    private const val CACHE_TTL_MS = 60_000L

    @Serializable
    private data class CosmeticRow(
        val id: String = "",
        val equipped_frame: String = "frame_none",
        val equipped_name_effect: String = "name_none",
        val equipped_title: String = "title_none",
        val equipped_banner_background: String = "banner_none",
    )

    private data class CacheEntry(val cosmetics: PlayerCosmetics, val timestamp: Long)

    private val cache = mutableMapOf<String, CacheEntry>()

    private fun nowMs(): Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()

    /** Returns the cached cosmetics for [userId], or [PlayerCosmetics.NONE] if not loaded. */
    fun cached(userId: String): PlayerCosmetics =
        cache[userId]?.cosmetics ?: PlayerCosmetics.NONE

    /** Forcibly invalidates the cache (e.g. when the user equips something new). */
    fun invalidate(userId: String) {
        cache.remove(userId)
    }

    fun invalidateAll() {
        cache.clear()
    }

    /**
     * Fetch cosmetics for a single user, returning the cached value if fresh.
     */
    suspend fun get(userId: String): PlayerCosmetics {
        val entry = cache[userId]
        if (entry != null && nowMs() - entry.timestamp < CACHE_TTL_MS) {
            return entry.cosmetics
        }
        return try {
            val row = supabase.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<CosmeticRow>()
            val cosmetics = if (row != null) {
                PlayerCosmetics(
                    frameId = row.equipped_frame,
                    nameEffectId = row.equipped_name_effect,
                    titleId = row.equipped_title,
                    bannerBackgroundId = row.equipped_banner_background,
                )
            } else {
                PlayerCosmetics.NONE
            }
            cache[userId] = CacheEntry(cosmetics, nowMs())
            cosmetics
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to fetch cosmetics for $userId", e)
            entry?.cosmetics ?: PlayerCosmetics.NONE
        }
    }

    /**
     * Batch-fetch cosmetics for many users in a single query. Returns a map of
     * user_id -> PlayerCosmetics. Users without a profile row are omitted.
     */
    suspend fun getMany(userIds: Collection<String>): Map<String, PlayerCosmetics> {
        if (userIds.isEmpty()) return emptyMap()
        val now = nowMs()
        val (fresh, stale) = userIds.distinct().partition { id ->
            cache[id]?.let { now - it.timestamp < CACHE_TTL_MS } == true
        }
        val result = mutableMapOf<String, PlayerCosmetics>()
        fresh.forEach { result[it] = cache[it]!!.cosmetics }

        if (stale.isEmpty()) return result

        try {
            val rows = supabase.from("profiles")
                .select { filter { isIn("id", stale) } }
                .decodeList<CosmeticRow>()
            rows.forEach { row ->
                val cosmetics = PlayerCosmetics(
                    frameId = row.equipped_frame,
                    nameEffectId = row.equipped_name_effect,
                    titleId = row.equipped_title,
                    bannerBackgroundId = row.equipped_banner_background,
                )
                cache[row.id] = CacheEntry(cosmetics, now)
                result[row.id] = cosmetics
            }
            // Cache misses (no profile row) → still cache as NONE so we don't refetch
            stale.filter { it !in result }.forEach { id ->
                cache[id] = CacheEntry(PlayerCosmetics.NONE, now)
                result[id] = PlayerCosmetics.NONE
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Batch fetch failed for ${stale.size} users", e)
        }
        return result
    }
}
