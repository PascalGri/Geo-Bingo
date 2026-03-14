package pg.geobingo.one.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

@Composable
actual fun rememberPhotoCapturer(onResult: (ByteArray?) -> Unit): PhotoCapturer =
    remember { object : PhotoCapturer { override fun launch() { onResult(null) } } }

actual fun ByteArray.toImageBitmap(): ImageBitmap? = try {
    Image.makeFromEncoded(this).toComposeImageBitmap()
} catch (_: Exception) { null }
