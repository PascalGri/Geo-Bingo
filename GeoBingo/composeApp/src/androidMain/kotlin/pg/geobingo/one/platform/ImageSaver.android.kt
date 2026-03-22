package pg.geobingo.one.platform

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Global holders set from MainActivity
lateinit var appContext: Context
var currentActivity: android.app.Activity? = null

actual suspend fun saveImageToDevice(bytes: ByteArray, filename: String): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/KatchIt")
            }
            val uri = appContext.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
            ) ?: return@withContext false
            appContext.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            true
        } catch (_: Exception) {
            false
        }
    }
