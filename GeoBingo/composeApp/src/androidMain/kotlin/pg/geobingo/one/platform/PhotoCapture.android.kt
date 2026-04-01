package pg.geobingo.one.platform

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

@Composable
actual fun rememberPhotoCapturer(onResult: (ByteArray?) -> Unit): PhotoCapturer {
    val currentOnResult = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val maxWidth = 1200
            val scaled = if (bitmap.width > maxWidth) {
                val ratio = maxWidth.toFloat() / bitmap.width
                Bitmap.createScaledBitmap(bitmap, maxWidth, (bitmap.height * ratio).toInt(), true)
            } else bitmap
            val stream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 70, stream)
            if (scaled !== bitmap) scaled.recycle()
            currentOnResult.value(stream.toByteArray())
        } else {
            currentOnResult.value(null)
        }
    }
    return remember { object : PhotoCapturer { override fun launch() { launcher.launch(null) } } }
}

actual fun ByteArray.toImageBitmap(): ImageBitmap? = toImageBitmap(maxWidth = 0)

/**
 * Decodes a JPEG/PNG byte array into an ImageBitmap, optionally down-sampling
 * so the resulting bitmap width does not exceed [maxWidth] pixels.
 * Pass 0 for no limit.
 */
fun ByteArray.toImageBitmap(maxWidth: Int): ImageBitmap? = try {
    if (maxWidth > 0) {
        // First pass: decode bounds only
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(this, 0, size, opts)
        // Calculate inSampleSize (power of 2)
        var sampleSize = 1
        while (opts.outWidth / sampleSize > maxWidth * 2) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        BitmapFactory.decodeByteArray(this, 0, size, decodeOpts)?.asImageBitmap()
    } else {
        BitmapFactory.decodeByteArray(this, 0, size)?.asImageBitmap()
    }
} catch (_: Exception) { null }
