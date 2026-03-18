package pg.geobingo.one.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.useContents
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
private class ImagePickerDelegate :
    NSObject(),
    UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol {

    var onResult: ((ByteArray?) -> Unit)? = null

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val originalImage = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        val image = originalImage?.let { resizeImage(it, maxWidth = 1200.0) }
        val jpegData = image?.let { UIImageJPEGRepresentation(it, 0.7) }
        val bytes = jpegData?.let { data ->
            data.bytes?.readBytes(data.length.toInt())
        }
        onResult?.invoke(bytes)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onResult?.invoke(null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun resizeImage(image: UIImage, maxWidth: Double): UIImage {
    val w = image.size.useContents { width }
    val h = image.size.useContents { height }
    if (w <= maxWidth) return image
    val ratio = maxWidth / w
    val newSize = CGSizeMake(maxWidth, h * ratio)
    UIGraphicsBeginImageContextWithOptions(newSize, true, 1.0)
    image.drawInRect(CGRectMake(0.0, 0.0, maxWidth, h * ratio))
    val resized = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    return resized ?: image
}

@Composable
actual fun rememberPhotoCapturer(onResult: (ByteArray?) -> Unit): PhotoCapturer {
    val currentOnResult = rememberUpdatedState(onResult)
    val delegate = remember { ImagePickerDelegate() }

    return remember {
        object : PhotoCapturer {
            override fun launch() {
                delegate.onResult = { bytes -> currentOnResult.value(bytes) }

                val sourceType = if (
                    UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)
                ) UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                else UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary

                val picker = UIImagePickerController()
                picker.sourceType = sourceType
                picker.allowsEditing = false
                picker.delegate = delegate

                val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
                rootVC?.presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}

actual fun ByteArray.toImageBitmap(): ImageBitmap? = try {
    Image.makeFromEncoded(this).toComposeImageBitmap()
} catch (_: Exception) { null }
