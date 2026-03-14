package pg.geobingo.one.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

interface PhotoCapturer {
    fun launch()
}

@Composable
expect fun rememberPhotoCapturer(onResult: (ByteArray?) -> Unit): PhotoCapturer

expect fun ByteArray.toImageBitmap(): ImageBitmap?
