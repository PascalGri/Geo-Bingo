package pg.geobingo.one.platform

import java.io.File

actual object LocalPhotoStore {
    private val baseDir: File by lazy {
        File(System.getProperty("user.home"), ".katchit").also { it.mkdirs() }
    }

    actual fun savePhoto(gameId: String, playerId: String, categoryId: String, bytes: ByteArray) {
        val file = File(baseDir, "games/$gameId/photos/${playerId}_${categoryId}.jpg")
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
    }

    actual fun loadPhoto(gameId: String, playerId: String, categoryId: String): ByteArray? {
        val file = File(baseDir, "games/$gameId/photos/${playerId}_${categoryId}.jpg")
        return if (file.exists()) file.readBytes() else null
    }

    actual fun saveAvatar(playerId: String, bytes: ByteArray) {
        val file = File(baseDir, "avatars/${playerId}.jpg")
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
    }

    actual fun loadAvatar(playerId: String): ByteArray? {
        val file = File(baseDir, "avatars/${playerId}.jpg")
        return if (file.exists()) file.readBytes() else null
    }

    actual fun saveGameMeta(gameId: String, json: String) {
        val file = File(baseDir, "games/$gameId/meta.json")
        file.parentFile?.mkdirs()
        file.writeText(json)
    }

    actual fun loadGameMeta(gameId: String): String? {
        val file = File(baseDir, "games/$gameId/meta.json")
        return if (file.exists()) file.readText() else null
    }

    actual fun listGameIds(): List<String> {
        val gamesDir = File(baseDir, "games")
        return gamesDir.listFiles()
            ?.filter { it.isDirectory && File(it, "meta.json").exists() }
            ?.map { it.name }
            ?: emptyList()
    }
}
