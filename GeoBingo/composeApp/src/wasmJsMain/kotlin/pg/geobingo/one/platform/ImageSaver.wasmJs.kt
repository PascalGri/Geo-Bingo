package pg.geobingo.one.platform

import org.khronos.webgl.Uint8Array
import org.khronos.webgl.set

@JsFun("""(bytes, filename) => {
    var blob = new Blob([bytes], { type: 'image/jpeg' });
    var url = URL.createObjectURL(blob);
    var a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    setTimeout(function() { URL.revokeObjectURL(url); document.body.removeChild(a); }, 1000);
}""")
external fun triggerWebDownload(bytes: Uint8Array, filename: String)

actual suspend fun saveImageToDevice(bytes: ByteArray, filename: String): Boolean = try {
    val jsArray = Uint8Array(bytes.size)
    for (i in bytes.indices) {
        jsArray[i] = bytes[i]
    }
    triggerWebDownload(jsArray, filename)
    true
} catch (_: Exception) {
    false
}
