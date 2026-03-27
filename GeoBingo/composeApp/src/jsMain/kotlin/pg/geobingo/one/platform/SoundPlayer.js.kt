package pg.geobingo.one.platform

actual object SoundPlayer {
    /**
     * Plays a tone using a shared AudioContext (reused across calls).
     * Mobile browsers require AudioContext to be created/resumed after a user gesture.
     * By reusing the context, we avoid the iOS Safari limit of ~6 contexts.
     */
    private fun playTone(freq: Double, duration: Int, type: String = "sine", volume: Double = 0.25) {
        try {
            js("""
                (function() {
                    if (!window._katchAudioCtx) {
                        window._katchAudioCtx = new (window.AudioContext || window.webkitAudioContext)();
                    }
                    var ctx = window._katchAudioCtx;
                    if (ctx.state === 'suspended') { ctx.resume(); }
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
    actual fun playTimerWarning() { playTone(600.0, 300, "sawtooth", 0.25) }
    actual fun playResultsReveal() { playTone(440.0, 300) }
    actual fun playSpeedBonus() { playTone(1320.0, 150) }
    actual fun playError() { playTone(250.0, 200, "square", 0.2) }
}
