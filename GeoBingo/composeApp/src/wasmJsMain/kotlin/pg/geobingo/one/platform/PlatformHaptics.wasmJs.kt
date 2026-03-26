package pg.geobingo.one.platform

@JsFun("""(ms) => {
    try {
        if (navigator.vibrate) { navigator.vibrate(ms); }
    } catch(e) {}
}""")
private external fun vibrateJs(ms: Int)

actual object PlatformHaptics {
    actual fun vibrate(durationMs: Int) {
        vibrateJs(durationMs)
    }
}
