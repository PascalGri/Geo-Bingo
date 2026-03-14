package pg.geobingo.one.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap

@Composable
actual fun rememberPhotoCapturer(onResult: (ByteArray?) -> Unit): PhotoCapturer =
    remember { object : PhotoCapturer { override fun launch() { onResult(null) } } }

actual fun ByteArray.toImageBitmap(): ImageBitmap? = null
