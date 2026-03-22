package pg.geobingo.one.game.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pg.geobingo.one.data.PhotoCache

class PhotoState {
    val photoCache = PhotoCache()
    var uploadingCategories by mutableStateOf(setOf<String>())
    var playerAvatarBytes by mutableStateOf(mapOf<String, ByteArray>())
    var triedAvatarDownloads by mutableStateOf(setOf<String>())
}
