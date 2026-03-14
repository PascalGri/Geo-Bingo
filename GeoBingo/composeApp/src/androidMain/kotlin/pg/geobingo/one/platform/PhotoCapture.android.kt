package pg.geobingo.one.platform

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberPhotoCapturer(onResult: (ByteArray?) -> Unit): PhotoCapturer {
    val context = LocalContext.current
    val currentOnResult = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            currentOnResult.value(bytes)
        } else {
            currentOnResult.value(null)
        }
    }
    return remember { object : PhotoCapturer { override fun launch() { launcher.launch("image/*") } } }
}

actual fun ByteArray.toImageBitmap(): ImageBitmap? = try {
    BitmapFactory.decodeByteArray(this, 0, size)?.asImageBitmap()
} catch (_: Exception) { null }
