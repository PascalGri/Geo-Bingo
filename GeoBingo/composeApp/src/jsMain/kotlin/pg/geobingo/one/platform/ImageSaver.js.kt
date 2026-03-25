package pg.geobingo.one.platform

import kotlinx.browser.document
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag

actual suspend fun saveImageToDevice(bytes: ByteArray, filename: String): Boolean = try {
    val blob = Blob(arrayOf(bytes), BlobPropertyBag(type = "image/jpeg"))
    val url = URL.createObjectURL(blob)
    val a = document.createElement("a") as HTMLAnchorElement
    a.href = url
    a.download = filename
    document.body?.appendChild(a)
    a.click()
    document.body?.removeChild(a)
    URL.revokeObjectURL(url)
    true
} catch (_: Exception) {
    false
}
