package pg.geobingo.one.platform

/**
 * Saves image bytes to the device gallery / downloads folder.
 * Returns true on success, false on failure.
 */
expect suspend fun saveImageToDevice(bytes: ByteArray, filename: String): Boolean
