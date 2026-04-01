package pg.geobingo.one.data

import androidx.compose.runtime.mutableStateMapOf
import pg.geobingo.one.game.GameConstants

/**
 * Size-capped photo cache with Compose state integration.
 * Uses [mutableStateMapOf] for granular recomposition (only readers of changed keys recompose).
 * Evicts oldest entries (FIFO) when [maxEntries] is exceeded.
 */
class PhotoCache(private val maxEntries: Int = GameConstants.PHOTO_CACHE_MAX_ENTRIES) {
    // Granular Compose-reactive map; only readers of specific keys recompose on mutation.
    private val _photos = mutableStateMapOf<String, ByteArray>()
    private val insertOrder = ArrayDeque<String>()

    private fun key(playerId: String, categoryId: String) = "$playerId:$categoryId"

    fun put(playerId: String, categoryId: String, bytes: ByteArray) {
        val k = key(playerId, categoryId)
        if (k !in _photos) insertOrder.addLast(k)
        _photos[k] = bytes
        while (_photos.size > maxEntries && insertOrder.isNotEmpty()) {
            _photos.remove(insertOrder.removeAt(0))
        }
    }

    fun get(playerId: String, categoryId: String): ByteArray? =
        _photos[key(playerId, categoryId)]

    fun contains(playerId: String, categoryId: String): Boolean =
        key(playerId, categoryId) in _photos

    fun clear() {
        _photos.clear()
        insertOrder.clear()
    }

    val size: Int get() = _photos.size
}
