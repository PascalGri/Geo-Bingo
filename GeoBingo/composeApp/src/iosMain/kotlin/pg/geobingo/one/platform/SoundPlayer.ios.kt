package pg.geobingo.one.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class)
actual object SoundPlayer {
    private val soundPaths = mutableMapOf<String, String>()
    /** Retain active players so ARC doesn't deallocate them mid-playback. */
    private val activePlayers = mutableListOf<AVAudioPlayer>()

    actual fun preload(sounds: Map<String, ByteArray>) {
        val tmpDir = NSTemporaryDirectory()
        for ((name, bytes) in sounds) {
            try {
                val filePath = "${tmpDir}snd_$name"
                val data = bytes.usePinned { pinned ->
                    NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
                }
                data.writeToFile(filePath, atomically = true)
                soundPaths[name] = filePath
            } catch (e: Exception) {
                println("[W] [SoundPlayer] Preload failed: $name: ${e.message}")
            }
        }
    }

    actual fun playFile(fileName: String) {
        val filePath = soundPaths[fileName] ?: return
        try {
            // Remove finished players to avoid unbounded growth
            activePlayers.removeAll { !it.isPlaying() }
            val url = NSURL.fileURLWithPath(filePath)
            val player = AVAudioPlayer(contentsOfURL = url, error = null) ?: return
            player.prepareToPlay()
            player.play()
            activePlayers.add(player)
        } catch (e: Exception) {
            println("[W] [SoundPlayer] Play failed: $fileName: ${e.message}")
        }
    }
}
