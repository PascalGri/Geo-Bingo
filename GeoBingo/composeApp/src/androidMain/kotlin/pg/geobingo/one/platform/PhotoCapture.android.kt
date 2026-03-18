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

actual fun ByteArray.toImageBitmap(): ImageBitmap? = try {
    BitmapFactory.decodeByteArray(this, 0, size)?.asImageBitmap()
} catch (_: Exception) { null }
