package pg.geobingo.one.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImageWriteToSavedPhotosAlbum

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual suspend fun saveImageToDevice(bytes: ByteArray, filename: String): Boolean = try {
    val data = bytes.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
    }
    val image = UIImage(data = data) ?: return false
    UIImageWriteToSavedPhotosAlbum(image, null, null, null)
    true
} catch (_: Exception) {
    false
}
