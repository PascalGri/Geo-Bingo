package pg.geobingo.one.platform

import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.kSystemSoundID_Vibrate

actual object SoundPlayer {
    // iOS system sound IDs
    private const val SOUND_TAP: UInt = 1104u        // Tock
    private const val SOUND_CAPTURE: UInt = 1108u     // Camera shutter-like
    private const val SOUND_VOTE: UInt = 1105u        // Tap
    private const val SOUND_TICK: UInt = 1103u        // Tink
    private const val SOUND_SUCCESS: UInt = 1025u     // Positive
    private const val SOUND_START: UInt = 1025u       // Positive
    private const val SOUND_END: UInt = 1026u         // Tri-tone

    private fun play(soundId: UInt) {
        try {
            AudioServicesPlaySystemSound(soundId)
        } catch (_: Exception) {}
    }

    actual fun playCapture() = play(SOUND_CAPTURE)
    actual fun playVote() = play(SOUND_VOTE)
    actual fun playCountdownTick() = play(SOUND_TICK)
    actual fun playGameStart() = play(SOUND_START)
    actual fun playGameEnd() = play(SOUND_END)
    actual fun playSuccess() = play(SOUND_SUCCESS)
    actual fun playTap() = play(SOUND_TAP)
    actual fun playTimerWarning() = play(1029u)  // Alarm (ascending)
    actual fun playResultsReveal() = play(1025u) // Positive tri-tone
    actual fun playSpeedBonus() = play(1115u)    // Short positive chirp
    actual fun playError() = play(1073u)         // Negative/Error beep
}
