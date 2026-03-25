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
                        // Use Canvas to resize + compress as JPEG
                        val url = js("URL.createObjectURL(file)").unsafeCast<String>()
                        val img = js("new Image()").asDynamic()
                        img.onload = {
                            val maxW = 1200
                            var w: Int = img.width as Int
                            var h: Int = img.height as Int
                            if (w > maxW) { h = (h * maxW.toDouble() / w).toInt(); w = maxW }
                            val canvas = document.createElement("canvas").asDynamic()
                            canvas.width = w; canvas.height = h
                            val ctx = canvas.getContext("2d")
                            ctx.drawImage(img, 0, 0, w, h)
                            js("URL.revokeObjectURL(url)")
                            canvas.toBlob({ blob: dynamic ->
                                if (blob == null) {
                                    scope.launch { channel.trySend(null) }
                                } else {
                                    blob.arrayBuffer().then { buf: dynamic ->
                                        val arr = js("new Int8Array(buf)").asDynamic()
                                        val len = (arr.length as Int)
                                        val bytes = ByteArray(len) { i -> (arr[i] as Byte) }
                                        scope.launch { channel.trySend(bytes) }
                                    }
                                }
                            }, "image/jpeg", 0.7)
                            null
                        }
                        img.onerror = {
                            scope.launch { channel.trySend(null) }
                            null
                        }
                        img.src = url
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
