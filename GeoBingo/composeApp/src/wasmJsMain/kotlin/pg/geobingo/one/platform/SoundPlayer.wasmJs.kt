package pg.geobingo.one.platform

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@JsFun("""(dataUri, key) => {
    try {
        if (!window._sndCache) window._sndCache = {};
        if (!window._sndCache[key]) {
            var a = new Audio(dataUri);
            a.volume = 0.5;
            window._sndCache[key] = a;
        }
        var src = window._sndCache[key];
        var clone = src.cloneNode();
        clone.volume = 0.5;
        clone.play().catch(function(){});
    } catch(e) { console.warn('playSound failed:', e); }
}""")
external fun playAudioCached(dataUri: JsString, key: JsString)

actual object SoundPlayer {
    private val dataUris = mutableMapOf<String, String>()

    @OptIn(ExperimentalEncodingApi::class)
    actual fun preload(sounds: Map<String, ByteArray>) {
        for ((name, bytes) in sounds) {
            try {
                val b64 = Base64.encode(bytes)
                dataUris[name] = "data:audio/mpeg;base64,$b64"
            } catch (e: Exception) {
                println("[W] [SoundPlayer] Preload failed: $name: ${e.message}")
            }
        }
    }

    actual fun playFile(fileName: String) {
        val uri = dataUris[fileName] ?: return
        playAudioCached(uri.toJsString(), fileName.toJsString())
    }
}
