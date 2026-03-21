package pg.geobingo.one.platform

actual object SoundPlayer {
    private fun playTone(freq: Double, duration: Int, type: String = "sine", volume: Double = 0.25) {
        try {
            js("""
                (function() {
                    var ctx = new (window.AudioContext || window.webkitAudioContext)();
                    var osc = ctx.createOscillator();
                    var gain = ctx.createGain();
                    osc.connect(gain);
                    gain.connect(ctx.destination);
                    osc.type = type;
                    osc.frequency.value = freq;
                    gain.gain.setValueAtTime(volume, ctx.currentTime);
                    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + duration / 1000.0);
                    osc.start(ctx.currentTime);
                    osc.stop(ctx.currentTime + duration / 1000.0);
                })()
            """)
        } catch (_: Exception) {}
    }

    actual fun playCapture() { playTone(880.0, 200) }
    actual fun playVote() { playTone(660.0, 100) }
    actual fun playCountdownTick() { playTone(1000.0, 60, "square", 0.15) }
    actual fun playGameStart() { playTone(523.0, 150) }
    actual fun playGameEnd() { playTone(784.0, 200) }
    actual fun playSuccess() { playTone(880.0, 250) }
    actual fun playTap() { playTone(800.0, 40, "square", 0.15) }
}
