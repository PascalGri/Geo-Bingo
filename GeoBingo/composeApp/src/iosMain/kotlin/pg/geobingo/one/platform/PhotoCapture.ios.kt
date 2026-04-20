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
import platform.CoreGraphics.CGAffineTransformIdentity
import platform.CoreGraphics.CGAffineTransformMakeRotation
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreGraphics.CGAffineTransformMakeTranslation
import platform.CoreGraphics.CGAffineTransformConcat
import platform.CoreGraphics.CGContextConcatCTM
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIDevice
import platform.UIKit.popoverPresentationController
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImageOrientation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UIModalPresentationFullScreen
import platform.UIKit.UIModalPresentationPopover
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.darwin.NSObject
import kotlin.math.PI

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
        if (originalImage == null) {
            println("[PhotoCapture] step=cast original-image=null")
            onResult?.invoke(null)
            return
        }
        // Resize FIRST to reduce memory pressure before orientation normalization.
        // On 12MP iPhone photos, normalizing at full size can silently OOM.
        val resized = resizeImage(originalImage, maxWidth = 1200.0)
        val normalized = normalizeOrientation(resized)
        val bytes = encodeJpegWithFallback(normalized)
        if (bytes == null) println("[PhotoCapture] step=encode result=null — all fallbacks failed")
        else println("[PhotoCapture] step=done bytes=${bytes.size}")
        onResult?.invoke(bytes)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onResult?.invoke(null)
    }
}

/**
 * Re-draws the image with orientation applied so the pixel data matches visual appearance.
 * This fixes mirrored selfies and rotated camera photos.
 */
@OptIn(ExperimentalForeignApi::class)
private fun normalizeOrientation(image: UIImage): UIImage {
    if (image.imageOrientation == UIImageOrientation.UIImageOrientationUp) return image
    val w = image.size.useContents { width }
    val h = image.size.useContents { height }
    UIGraphicsBeginImageContextWithOptions(image.size, false, image.scale)
    image.drawInRect(CGRectMake(0.0, 0.0, w, h))
    val normalized = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    if (normalized == null) println("[PhotoCapture] step=normalize fallback-to-original")
    return normalized ?: image
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
    if (resized == null) println("[PhotoCapture] step=resize fallback-to-original")
    return resized ?: image
}

@OptIn(ExperimentalForeignApi::class)
private fun encodeJpegWithFallback(image: UIImage): ByteArray? {
    for (quality in listOf(0.7, 0.5, 0.3)) {
        val data = UIImageJPEGRepresentation(image, quality)
        if (data == null) {
            println("[PhotoCapture] step=jpeg quality=$quality result=null")
            continue
        }
        val length = data.length.toInt()
        if (length <= 0) {
            println("[PhotoCapture] step=jpeg quality=$quality length=$length")
            continue
        }
        val bytes = data.bytes?.readBytes(length)
        if (bytes != null) {
            println("[PhotoCapture] step=jpeg quality=$quality bytes=${bytes.size}")
            return bytes
        }
        println("[PhotoCapture] step=jpeg quality=$quality readBytes=null")
    }
    return null
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberPhotoCapturer(onResult: (ByteArray?) -> Unit): PhotoCapturer {
    val currentOnResult = rememberUpdatedState(onResult)
    val delegate = remember { ImagePickerDelegate() }

    return remember {
        object : PhotoCapturer {
            override fun launch() {
                delegate.onResult = { bytes -> currentOnResult.value(bytes) }

                val cameraType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                val libraryType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
                val sourceType = if (UIImagePickerController.isSourceTypeAvailable(cameraType))
                    cameraType else libraryType

                val picker = UIImagePickerController()
                picker.sourceType = sourceType
                picker.allowsEditing = false
                picker.delegate = delegate

                // Resolve the presenting window via connectedScenes instead of
                // the deprecated keyWindow — required for iPad Stage Manager
                // and multi-scene setups or the picker silently fails to show.
                val rootVC = activeRootViewController() ?: return
                val isPad = UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad

                if (sourceType == libraryType && isPad) {
                    // PhotoLibrary on iPad MUST present as a popover with an
                    // anchor, otherwise UIKit throws NSInvalidArgumentException.
                    picker.modalPresentationStyle = UIModalPresentationPopover
                    val rootView = rootVC.view
                    val midX = rootView.bounds.useContents { origin.x + size.width / 2.0 }
                    val midY = rootView.bounds.useContents { origin.y + size.height / 2.0 }
                    val popover = picker.popoverPresentationController()
                    popover?.setSourceView(rootView)
                    popover?.setSourceRect(CGRectMake(midX, midY, 0.0, 0.0))
                } else {
                    picker.modalPresentationStyle = UIModalPresentationFullScreen
                }

                rootVC.presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}

actual fun ByteArray.toImageBitmap(): ImageBitmap? = try {
    Image.makeFromEncoded(this).toComposeImageBitmap()
} catch (e: Exception) {
    println("[W] [PhotoCapture] toImageBitmap failed: ${e.message}")
    null
}
