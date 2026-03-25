package pg.geobingo.one.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pg.geobingo.one.game.GameConstants

/**
 * Size-capped photo cache with Compose state integration.
 * Evicts oldest entries (FIFO) when [maxEntries] is exceeded.
 */
class PhotoCache(private val maxEntries: Int = GameConstants.PHOTO_CACHE_MAX_ENTRIES) {
    // Compose-reactive map; any mutation triggers recomposition for readers.
    private var _photos by mutableStateOf(mapOf<String, ByteArray>())
    private val insertOrder = ArrayDeque<String>()

    private fun key(playerId: String, categoryId: String) = "$playerId:$categoryId"

    fun put(playerId: String, categoryId: String, bytes: ByteArray) {
        val k = key(playerId, categoryId)
        if (k !in _photos) insertOrder.addLast(k)
        val updated = _photos.toMutableMap()
        updated[k] = bytes
        while (updated.size > maxEntries && insertOrder.isNotEmpty()) {
            updated.remove(insertOrder.removeFirst())
        }
        _photos = updated
    }

    fun get(playerId: String, categoryId: String): ByteArray? =
        _photos[key(playerId, categoryId)]

    fun contains(playerId: String, categoryId: String): Boolean =
        key(playerId, categoryId) in _photos

    fun clear() {
        _photos = mapOf()
        insertOrder.clear()
    }

    val size: Int get() = _photos.size
}
