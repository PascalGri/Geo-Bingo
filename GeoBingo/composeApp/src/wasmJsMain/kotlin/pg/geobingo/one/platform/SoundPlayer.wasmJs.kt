package pg.geobingo.one.platform

/**
 * Initializes a shared AudioContext on first user interaction (required by mobile browsers).
 * Reuses the same context for all subsequent sounds.
 */
@JsFun("""() => {
    if (!window._katchAudioCtx) {
        window._katchAudioCtx = new (window.AudioContext || window.webkitAudioContext)();
    }
    if (window._katchAudioCtx.state === 'suspended') {
        window._katchAudioCtx.resume();
    }
    return window._katchAudioCtx;
}""")
external fun getOrCreateAudioContext(): JsAny

@JsFun("""(freq, duration, type, volume) => {
    try {
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
    } catch(e) {}
}""")
external fun playWebTone(freq: Float, duration: Float, type: String, volume: Float)

@JsFun("""(freq1, dur1, freq2, dur2, type, volume) => {
    try {
        if (!window._katchAudioCtx) {
            window._katchAudioCtx = new (window.AudioContext || window.webkitAudioContext)();
        }
        var ctx = window._katchAudioCtx;
        if (ctx.state === 'suspended') { ctx.resume(); }
        var osc1 = ctx.createOscillator();
        var gain1 = ctx.createGain();
        osc1.connect(gain1);
        gain1.connect(ctx.destination);
        osc1.type = type;
        osc1.frequency.value = freq1;
        gain1.gain.setValueAtTime(volume, ctx.currentTime);
        gain1.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + dur1 / 1000.0);
        osc1.start(ctx.currentTime);
        osc1.stop(ctx.currentTime + dur1 / 1000.0);
        var osc2 = ctx.createOscillator();
        var gain2 = ctx.createGain();
        osc2.connect(gain2);
        gain2.connect(ctx.destination);
        osc2.type = type;
        osc2.frequency.value = freq2;
        var delay = dur1 / 1000.0 * 0.7;
        gain2.gain.setValueAtTime(0.001, ctx.currentTime);
        gain2.gain.setValueAtTime(volume, ctx.currentTime + delay);
        gain2.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + delay + dur2 / 1000.0);
        osc2.start(ctx.currentTime + delay);
        osc2.stop(ctx.currentTime + delay + dur2 / 1000.0);
    } catch(e) {}
}""")
external fun playWebToneSequence(freq1: Float, dur1: Float, freq2: Float, dur2: Float, type: String, volume: Float)

@JsFun("""(ms) => {
    try {
        if (navigator.vibrate) { navigator.vibrate(ms); }
    } catch(e) {}
}""")
external fun vibrateWeb(ms: Int)

actual object SoundPlayer {
    actual fun playCapture() {
        playWebToneSequence(880f, 200f, 1100f, 150f, "sine", 0.3f)
    }

    actual fun playVote() {
        playWebTone(660f, 100f, "sine", 0.25f)
    }

    actual fun playCountdownTick() {
        playWebTone(1000f, 60f, "square", 0.15f)
    }

    actual fun playGameStart() {
        playWebToneSequence(523f, 150f, 784f, 200f, "sine", 0.3f)
    }

    actual fun playGameEnd() {
        playWebToneSequence(784f, 200f, 523f, 200f, "sine", 0.3f)
    }

    actual fun playSuccess() {
        playWebTone(880f, 250f, "sine", 0.3f)
    }

    actual fun playTap() {
        playWebTone(800f, 40f, "square", 0.15f)
    }
}
