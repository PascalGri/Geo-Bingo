package pg.geobingo.one.platform

/** Platform-specific local file storage for game photos and metadata. */
expect object LocalPhotoStore {
    fun savePhoto(gameId: String, playerId: String, categoryId: String, bytes: ByteArray)
    fun loadPhoto(gameId: String, playerId: String, categoryId: String): ByteArray?
    fun saveAvatar(playerId: String, bytes: ByteArray)
    fun loadAvatar(playerId: String): ByteArray?
    fun saveGameMeta(gameId: String, json: String)
    fun loadGameMeta(gameId: String): String?
    fun listGameIds(): List<String>
}
