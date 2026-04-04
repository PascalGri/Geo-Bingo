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
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
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
                        compressJvmPhoto(chooser.selectedFile.readBytes())
                    } else null
                    withContext(Dispatchers.Main) { currentOnResult.value(bytes) }
                }
            }
        }
    }
}

private fun compressJvmPhoto(raw: ByteArray): ByteArray = try {
    val decoded = ImageIO.read(ByteArrayInputStream(raw)) ?: return raw
    val maxWidth = 1200
    val img = if (decoded.width > maxWidth) {
        val ratio = maxWidth.toDouble() / decoded.width
        val newH = (decoded.height * ratio).toInt()
        val scaled = BufferedImage(maxWidth, newH, BufferedImage.TYPE_INT_RGB)
        val g = scaled.createGraphics()
        g.drawImage(decoded.getScaledInstance(maxWidth, newH, java.awt.Image.SCALE_SMOOTH), 0, 0, null)
        g.dispose()
        scaled
    } else decoded
    val out = ByteArrayOutputStream()
    val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
    val param = writer.defaultWriteParam
    param.compressionMode = ImageWriteParam.MODE_EXPLICIT
    param.compressionQuality = 0.7f
    writer.output = ImageIO.createImageOutputStream(out)
    writer.write(null, IIOImage(img, null, null), param)
    writer.dispose()
    out.toByteArray()
} catch (_: Exception) { raw }

actual fun ByteArray.toImageBitmap(): ImageBitmap? = try {
    Image.makeFromEncoded(this).toComposeImageBitmap()
} catch (_: Exception) { null }
