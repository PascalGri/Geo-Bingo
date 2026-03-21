package pg.geobingo.one.platform

import android.media.AudioAttributes
import android.media.SoundPool

actual object SoundPlayer {
    private var soundPool: SoundPool? = null
    private var tapId = 0
    private var captureId = 0
    private var successId = 0
    private var loaded = false

    private fun pool(): SoundPool {
        soundPool?.let { return it }
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val sp = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build()
        soundPool = sp
        // We use Android's built-in system sounds via ToneGenerator as a fallback
        // Since SoundPool needs actual files, we'll use ToneGenerator instead
        return sp
    }

    private fun playTone(toneType: Int, durationMs: Int = 150) {
        try {
            val tg = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 80)
            tg.startTone(toneType, durationMs)
            // Release after tone finishes
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ tg.release() }, durationMs + 50L)
        } catch (_: Exception) {}
    }

    actual fun playCapture() {
        playTone(android.media.ToneGenerator.TONE_PROP_ACK, 200)
    }

    actual fun playVote() {
        playTone(android.media.ToneGenerator.TONE_PROP_BEEP, 100)
    }

    actual fun playCountdownTick() {
        playTone(android.media.ToneGenerator.TONE_CDMA_PIP, 80)
    }

    actual fun playGameStart() {
        playTone(android.media.ToneGenerator.TONE_PROP_ACK, 300)
    }

    actual fun playGameEnd() {
        playTone(android.media.ToneGenerator.TONE_PROP_ACK, 400)
    }

    actual fun playSuccess() {
        playTone(android.media.ToneGenerator.TONE_PROP_ACK, 250)
    }

    actual fun playTap() {
        playTone(android.media.ToneGenerator.TONE_PROP_BEEP, 50)
    }
}
