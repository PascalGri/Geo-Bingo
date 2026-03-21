package pg.geobingo.one.platform

@JsFun("""(freq, duration, type, volume) => {
    try {
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
    } catch(e) {}
}""")
external fun playWebTone(freq: Float, duration: Float, type: String, volume: Float)

actual object SoundPlayer {
    actual fun playCapture() {
        playWebTone(880f, 200f, "sine", 0.3f)
        playWebTone(1100f, 150f, "sine", 0.2f)
    }

    actual fun playVote() {
        playWebTone(660f, 100f, "sine", 0.25f)
    }

    actual fun playCountdownTick() {
        playWebTone(1000f, 60f, "square", 0.15f)
    }

    actual fun playGameStart() {
        playWebTone(523f, 150f, "sine", 0.3f)
        playWebTone(659f, 150f, "sine", 0.3f)
        playWebTone(784f, 200f, "sine", 0.3f)
    }

    actual fun playGameEnd() {
        playWebTone(784f, 200f, "sine", 0.3f)
        playWebTone(659f, 200f, "sine", 0.3f)
    }

    actual fun playSuccess() {
        playWebTone(880f, 250f, "sine", 0.3f)
    }

    actual fun playTap() {
        playWebTone(800f, 40f, "square", 0.15f)
    }
}
