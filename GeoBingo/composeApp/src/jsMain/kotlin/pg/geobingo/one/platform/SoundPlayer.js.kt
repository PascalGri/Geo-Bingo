package pg.geobingo.one.platform

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.browser.document
import org.w3c.dom.HTMLAudioElement

actual object SoundPlayer {
    private val dataUris = mutableMapOf<String, String>()

    @OptIn(ExperimentalEncodingApi::class)
    actual fun preload(sounds: Map<String, ByteArray>) {
        for ((name, bytes) in sounds) {
            try {
                val b64 = Base64.encode(bytes)
                dataUris[name] = "data:audio/mpeg;base64,$b64"
            } catch (_: Exception) {}
        }
    }

    actual fun playFile(fileName: String) {
        val uri = dataUris[fileName] ?: return
        try {
            val audio = document.createElement("audio") as HTMLAudioElement
            audio.src = uri
            audio.volume = 0.5
            audio.play()
        } catch (_: Exception) {}
    }
}
