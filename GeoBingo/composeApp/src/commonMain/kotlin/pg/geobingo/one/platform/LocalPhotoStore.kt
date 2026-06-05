package pg.geobingo.one.platform

/** Platform-specific local file storage for game photos and metadata. */
expect object LocalPhotoStore {
    fun savePhoto(gameId: String, playerId: String, categoryId: String, bytes: ByteArray)
    fun loadPhoto(gameId: String, playerId: String, categoryId: String): ByteArray?
    fun saveAvatar(playerId: String, bytes: ByteArray)
    fun loadAvatar(playerId: String): ByteArray?
    /**
     * Completely removes the cached avatar file. Preferred over writing empty
     * bytes when signing out — leaves no trace on disk, and loadAvatar() will
     * cleanly return null on the next read (no half-empty decode attempts).
     */
    fun deleteAvatar(playerId: String)
    fun saveGameMeta(gameId: String, json: String)
    fun loadGameMeta(gameId: String): String?
    fun listGameIds(): List<String>
    /**
     * Recursively removes a single game's directory (`games/<gameId>/` — all
     * its photos + meta.json). Used by the 7-day history retention prune to
     * free disk for games that have aged out of the Spielverlauf. No-op if the
     * directory doesn't exist. Avatars live outside `games/` and are untouched.
     */
    fun deleteGame(gameId: String)
    /**
     * Recursively removes the entire `games/` directory — all photos and
     * meta.json files for every game. Called on sign-out / user-switch so
     * the previous user's per-game personal data (photos contain location +
     * faces) doesn't sit on disk for the next user. No-op if the directory
     * doesn't exist.
     */
    fun deleteAllGameData()
}
