package pg.geobingo.one.platform

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLAudioElement

actual object SoundPlayer {
    private val dataUris = mutableMapOf<String, String>()
    private val audioCache = mutableMapOf<String, HTMLAudioElement>()

    @OptIn(ExperimentalEncodingApi::class)
    actual fun preload(sounds: Map<String, ByteArray>) {
        for ((name, bytes) in sounds) {
            try {
                val b64 = Base64.encode(bytes)
                val uri = "data:audio/mpeg;base64,$b64"
                dataUris[name] = uri
                val audio = document.createElement("audio") as HTMLAudioElement
                audio.src = uri
                audio.volume = 0.5
                audioCache[name] = audio
            } catch (e: Exception) {
                println("[W] [SoundPlayer] Preload failed: $name: ${e.message}")
            }
        }
    }

    actual fun playFile(fileName: String) {
        val cached = audioCache[fileName] ?: return
        try {
            val clone = cached.cloneNode(false) as HTMLAudioElement
            clone.volume = 0.5
            clone.play()
        } catch (e: Exception) {
            println("[W] [SoundPlayer] Play failed: $fileName: ${e.message}")
        }
    }
}
