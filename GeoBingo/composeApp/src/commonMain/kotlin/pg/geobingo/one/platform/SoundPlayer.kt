package pg.geobingo.one.platform

expect object SoundPlayer {
    fun preload(sounds: Map<String, ByteArray>)
    fun playFile(fileName: String)
}

fun SoundPlayer.play(effect: SoundEffect) {
    playFile(effect.fileName)
}
