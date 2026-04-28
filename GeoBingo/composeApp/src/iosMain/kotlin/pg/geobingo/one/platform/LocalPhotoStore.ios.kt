package pg.geobingo.one.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.usePinned
import platform.Foundation.*
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual object LocalPhotoStore {
    private val baseDir: String by lazy {
        val docs = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String
        "$docs/katchit"
    }

    private fun ensureDir(path: String) {
        NSFileManager.defaultManager.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null)
    }

    actual fun savePhoto(gameId: String, playerId: String, categoryId: String, bytes: ByteArray) {
        val dir = "$baseDir/games/$gameId/photos"
        ensureDir(dir)
        val data = bytes.toNSData()
        data.writeToFile("$dir/${playerId}_${categoryId}.jpg", atomically = true)
    }

    actual fun loadPhoto(gameId: String, playerId: String, categoryId: String): ByteArray? {
        val path = "$baseDir/games/$gameId/photos/${playerId}_${categoryId}.jpg"
        val data = NSData.dataWithContentsOfFile(path) ?: return null
        return data.toByteArray()
    }

    actual fun saveAvatar(playerId: String, bytes: ByteArray) {
        val dir = "$baseDir/avatars"
        ensureDir(dir)
        val data = bytes.toNSData()
        data.writeToFile("$dir/${playerId}.jpg", atomically = true)
    }

    actual fun loadAvatar(playerId: String): ByteArray? {
        val path = "$baseDir/avatars/${playerId}.jpg"
        val data = NSData.dataWithContentsOfFile(path) ?: return null
        // A leftover zero-length file (from a past "clear by writing empty bytes"
        // approach) must NOT be treated as a valid avatar — callers would feed
        // the 0-byte array to the decoder, which races against the placeholder
        // and visually flickers. Return null so the UI cleanly shows the
        // initial letter.
        if (data.length.toInt() == 0) return null
        return data.toByteArray()
    }

    actual fun deleteAvatar(playerId: String) {
        val path = "$baseDir/avatars/${playerId}.jpg"
        NSFileManager.defaultManager.removeItemAtPath(path, error = null)
    }

    actual fun saveGameMeta(gameId: String, json: String) {
        val dir = "$baseDir/games/$gameId"
        ensureDir(dir)
        (json as NSString).writeToFile("$dir/meta.json", atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }

    actual fun loadGameMeta(gameId: String): String? {
        val path = "$baseDir/games/$gameId/meta.json"
        return NSString.stringWithContentsOfFile(path, encoding = NSUTF8StringEncoding, error = null) as? String
    }

    @Suppress("UNCHECKED_CAST")
    actual fun listGameIds(): List<String> {
        val gamesDir = "$baseDir/games"
        val fm = NSFileManager.defaultManager
        val contents = fm.contentsOfDirectoryAtPath(gamesDir, error = null) as? List<String> ?: return emptyList()
        return contents.filter { fm.fileExistsAtPath("$gamesDir/$it/meta.json") }
    }

    actual fun deleteAllGameData() {
        val gamesDir = "$baseDir/games"
        val fm = NSFileManager.defaultManager
        // removeItemAtPath recursively deletes the directory + everything
        // under it. No-op (returns false + sets error) when the path is
        // missing — we ignore both because the goal is "make sure nothing
        // is there afterwards".
        fm.removeItemAtPath(gamesDir, error = null)
    }

    private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }

    private fun NSData.toByteArray(): ByteArray {
        val bytes = this.bytes ?: return ByteArray(0)
        return bytes.readBytes(this.length.toInt())
    }
}
