package pg.geobingo.one.game.state

import androidx.compose.runtime.mutableStateMapOf
import pg.geobingo.one.data.PhotoCache

class PhotoState {
    val photoCache = PhotoCache()

    /** Categories currently being uploaded. Using SnapshotStateMap for granular recomposition. */
    val uploadingCategories = mutableStateMapOf<String, Boolean>()

    /** Downloaded player avatar bytes, keyed by playerId. */
    val playerAvatarBytes = mutableStateMapOf<String, ByteArray>()

    /** Player IDs for which avatar download was already attempted. */
    val triedAvatarDownloads = mutableStateMapOf<String, Boolean>()

    fun isUploading(categoryId: String): Boolean = uploadingCategories[categoryId] == true

    fun startUpload(categoryId: String) { uploadingCategories[categoryId] = true }

    fun finishUpload(categoryId: String) { uploadingCategories.remove(categoryId) }

    fun setAvatar(playerId: String, bytes: ByteArray) { playerAvatarBytes[playerId] = bytes }

    fun getAvatar(playerId: String): ByteArray? = playerAvatarBytes[playerId]

    fun markAvatarTried(playerId: String) { triedAvatarDownloads[playerId] = true }

    fun isAvatarTried(playerId: String): Boolean = triedAvatarDownloads[playerId] == true

    fun clear() {
        photoCache.clear()
        uploadingCategories.clear()
        playerAvatarBytes.clear()
        triedAvatarDownloads.clear()
    }
}
