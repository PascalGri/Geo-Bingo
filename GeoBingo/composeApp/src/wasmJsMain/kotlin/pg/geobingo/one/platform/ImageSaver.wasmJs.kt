package pg.geobingo.one.platform

import org.khronos.webgl.Uint8Array
import org.khronos.webgl.set

@JsFun("""(bytes, filename) => {
    try {
        var blob = new Blob([bytes], { type: 'image/jpeg' });
        var url = URL.createObjectURL(blob);
        var a = document.createElement('a');
        a.href = url;
        a.setAttribute('download', filename);
        a.style.display = 'none';
        document.body.appendChild(a);
        a.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: false, view: window }));
        setTimeout(function() {
            URL.revokeObjectURL(url);
            if (a.parentNode) a.parentNode.removeChild(a);
        }, 2000);
    } catch(e) {
        console.error('KatchIt download failed:', e);
    }
}""")
external fun triggerWebDownload(bytes: Uint8Array, filename: String)

actual suspend fun saveImageToDevice(bytes: ByteArray, filename: String): Boolean = try {
    val jsArray = Uint8Array(bytes.size)
    for (i in bytes.indices) {
        jsArray[i] = (bytes[i].toInt() and 0xFF).toByte()
    }
    triggerWebDownload(jsArray, filename)
    true
} catch (_: Exception) {
    false
}
