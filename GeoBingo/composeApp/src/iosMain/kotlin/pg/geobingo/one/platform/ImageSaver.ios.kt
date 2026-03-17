package pg.geobingo.one.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toCValues
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImageWriteToSavedPhotosAlbum

@OptIn(ExperimentalForeignApi::class)
actual suspend fun saveImageToDevice(bytes: ByteArray, filename: String): Boolean = try {
    val data = NSData.create(bytes = bytes.toCValues(), length = bytes.size.toULong())
    val image = UIImage(data = data) ?: return false
    UIImageWriteToSavedPhotosAlbum(image, null, null, null)
    true
} catch (_: Exception) {
    false
}
