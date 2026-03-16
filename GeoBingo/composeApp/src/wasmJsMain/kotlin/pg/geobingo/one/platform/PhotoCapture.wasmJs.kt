package pg.geobingo.one.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
actual fun rememberPhotoCapturer(onResult: (ByteArray?) -> Unit): PhotoCapturer {
    val scope = rememberCoroutineScope()
    return remember {
        object : PhotoCapturer {
            override fun launch() {
                startWebFilePicker()
                scope.launch {
                    var elapsed = 0
                    while (elapsed < 60_000) {
                        if (isWebFilePickerDone()) {
                            onResult(consumeWebPickerResult())
                            return@launch
                        }
                        delay(300)
                        elapsed += 300
                    }
                    onResult(null)
                }
            }
        }
    }
}

@JsFun("""() => {
    window._katchit_done = false;
    window._katchit_bytes = null;
    var input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.style.display = 'none';
    input.addEventListener('change', function() {
        var file = input.files && input.files[0];
        if (!file) { window._katchit_done = true; return; }
        file.arrayBuffer().then(function(buf) {
            window._katchit_bytes = new Uint8Array(buf);
            window._katchit_done = true;
        })['catch'](function() { window._katchit_done = true; });
    });
    document.body.appendChild(input);
    input.click();
    setTimeout(function() { if (input.parentNode) input.parentNode.removeChild(input); }, 60000);
}""")
private external fun startWebFilePicker()

@JsFun("() => window._katchit_done === true")
private external fun isWebFilePickerDone(): Boolean

private fun consumeWebPickerResult(): ByteArray? {
    val arr = getWebPickerBytes() ?: run { clearWebPickerState(); return null }
    val len = getJsArrayLength(arr)
    val bytes = ByteArray(len) { i -> getJsArrayByte(arr, i) }
    clearWebPickerState()
    return bytes
}

@JsFun("() => window._katchit_bytes || null")
private external fun getWebPickerBytes(): JsAny?

@JsFun("(arr) => arr.length")
private external fun getJsArrayLength(arr: JsAny): Int

@JsFun("(arr, i) => arr[i]")
private external fun getJsArrayByte(arr: JsAny, i: Int): Byte

@JsFun("() => { window._katchit_done = false; window._katchit_bytes = null; }")
private external fun clearWebPickerState()

actual fun ByteArray.toImageBitmap(): ImageBitmap? = null
