package pg.geobingo.one.platform

import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File

actual object SoundPlayer {
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val soundIds = mutableMapOf<String, Int>()

    actual fun preload(sounds: Map<String, ByteArray>) {
        val cacheDir = appContext.cacheDir
        for ((name, bytes) in sounds) {
            try {
                val file = File(cacheDir, "snd_$name")
                file.writeBytes(bytes)
                val id = soundPool.load(file.absolutePath, 1)
                soundIds[name] = id
            } catch (e: Exception) {
                println("[W] [SoundPlayer] Preload failed: $name: ${e.message}")
            }
        }
    }

    actual fun playFile(fileName: String) {
        val id = soundIds[fileName] ?: return
        soundPool.play(id, 1f, 1f, 1, 0, 1f)
    }
}
