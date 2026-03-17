package pg.geobingo.one.platform

actual suspend fun saveImageToDevice(bytes: ByteArray, filename: String): Boolean = try {
    triggerWebDownload(bytes, filename)
    true
} catch (_: Exception) {
    false
}

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
private external fun triggerWebDownload(bytes: ByteArray, filename: String)
