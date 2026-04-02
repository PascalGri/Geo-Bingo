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
            } catch (_: Exception) {}
        }
    }

    actual fun playFile(fileName: String) {
        val filePath = soundPaths[fileName] ?: return
        try {
            val url = NSURL.fileURLWithPath(filePath)
            val player = AVAudioPlayer(contentsOfURL = url, error = null)
            player?.prepareToPlay()
            player?.play()
        } catch (_: Exception) {}
    }
}
