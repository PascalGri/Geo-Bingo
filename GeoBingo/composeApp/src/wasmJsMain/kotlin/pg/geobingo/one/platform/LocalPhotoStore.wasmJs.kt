package pg.geobingo.one.platform

/** Web: no persistent local photo storage. */
actual object LocalPhotoStore {
    actual fun savePhoto(gameId: String, playerId: String, categoryId: String, bytes: ByteArray) {}
    actual fun loadPhoto(gameId: String, playerId: String, categoryId: String): ByteArray? = null
    actual fun saveAvatar(playerId: String, bytes: ByteArray) {}
    actual fun loadAvatar(playerId: String): ByteArray? = null
    actual fun saveGameMeta(gameId: String, json: String) {}
    actual fun loadGameMeta(gameId: String): String? = null
    actual fun listGameIds(): List<String> = emptyList()
}
