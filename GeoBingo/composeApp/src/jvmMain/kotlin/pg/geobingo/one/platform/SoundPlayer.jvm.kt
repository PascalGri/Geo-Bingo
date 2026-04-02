package pg.geobingo.one.platform

actual object SoundPlayer {
    actual fun preload(sounds: Map<String, ByteArray>) {}
    actual fun playFile(fileName: String) {}
}
