package pg.geobingo.one.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.awt.EventQueue
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberPhotoCapturer(onResult: (ByteArray?) -> Unit): PhotoCapturer {
    val currentOnResult = rememberUpdatedState(onResult)
    val scope = rememberCoroutineScope()
    return remember {
        object : PhotoCapturer {
            override fun launch() {
                scope.launch(Dispatchers.IO) {
                    val chooser = JFileChooser()
                    chooser.dialogTitle = "Foto auswählen"
                    chooser.fileFilter = FileNameExtensionFilter(
                        "Bilder", "jpg", "jpeg", "png", "gif", "bmp", "webp"
                    )
                    var result: Int = JFileChooser.CANCEL_OPTION
                    EventQueue.invokeAndWait { result = chooser.showOpenDialog(null) }
                    val bytes = if (result == JFileChooser.APPROVE_OPTION) {
                        chooser.selectedFile.readBytes()
                    } else null
                    withContext(Dispatchers.Main) { currentOnResult.value(bytes) }
                }
            }
        }
    }
}

actual fun ByteArray.toImageBitmap(): ImageBitmap? = try {
    Image.makeFromEncoded(this).toComposeImageBitmap()
} catch (_: Exception) { null }
