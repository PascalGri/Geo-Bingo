package pg.geobingo.one.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.browser.document
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader

@Composable
actual fun rememberPhotoCapturer(onResult: (ByteArray?) -> Unit): PhotoCapturer {
    val scope = rememberCoroutineScope()
    return remember {
        object : PhotoCapturer {
            override fun launch() {
                val channel = Channel<ByteArray?>(1)
                scope.launch {
                    onResult(channel.receive())
                    channel.close()
                }

                val input = document.createElement("input") as HTMLInputElement
                input.type = "file"
                input.accept = "image/*"
                input.setAttribute("capture", "environment")
                input.style.display = "none"

                input.onchange = {
                    val file = input.files?.item(0)
                    if (file == null) {
                        scope.launch { channel.trySend(null) }
                    } else {
                        val reader = FileReader()
                        reader.onload = {
                            val buffer = reader.result.unsafeCast<ArrayBuffer>()
                            val int8 = Int8Array(buffer)
                            val bytes = ByteArray(int8.length) { i -> int8[i] }
                            scope.launch { channel.trySend(bytes) }
                            null
                        }
                        reader.onerror = {
                            scope.launch { channel.trySend(null) }
                            null
                        }
                        reader.readAsArrayBuffer(file)
                    }
                    null
                }

                document.body?.appendChild(input)
                input.click()
                // Clean up after 60 s
                scope.launch {
                    kotlinx.coroutines.delay(60_000)
                    input.parentNode?.removeChild(input)
                }
            }
        }
    }
}

actual fun ByteArray.toImageBitmap(): ImageBitmap? = try {
    Image.makeFromEncoded(this).toComposeImageBitmap()
} catch (_: Exception) { null }
