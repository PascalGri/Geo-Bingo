package pg.geobingo.one.platform

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@JsFun("""(dataUri) => {
    try {
        var a = new Audio(dataUri);
        a.volume = 0.5;
        a.play().catch(function(){});
    } catch(e) { console.warn('playSound failed:', e); }
}""")
external fun playAudioDataUri(dataUri: JsString)

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
        playAudioDataUri(uri.toJsString())
    }
}
