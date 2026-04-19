package pg.geobingo.one.platform

expect object SoundPlayer {
    fun preload(sounds: Map<String, ByteArray>)
    fun playFile(fileName: String)
}

/**
 * Plays a sound effect — unless the user has disabled sound in Settings.
 *
 * The setting is read from AppSettings directly (not from gameState.ui) so that
 * even call sites that forget to add a `if (gameState.ui.soundEnabled)` guard
 * around the play() call still respect the toggle. This keeps the "Sound aus"
 * toggle genuinely silent everywhere, not just in the places the original
 * author remembered to guard.
 */
fun SoundPlayer.play(effect: SoundEffect) {
    if (!AppSettings.getBoolean(SettingsKeys.SOUND_ENABLED, true)) return
    playFile(effect.fileName)
}
