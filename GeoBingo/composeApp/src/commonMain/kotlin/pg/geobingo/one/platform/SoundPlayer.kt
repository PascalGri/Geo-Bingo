package pg.geobingo.one.platform

/**
 * Lightweight platform sound player.
 * Each method plays a short feedback sound appropriate for the event.
 * Sound is only played if the user has sound enabled (checked by callers).
 */
expect object SoundPlayer {
    fun playCapture()
    fun playVote()
    fun playCountdownTick()
    fun playGameStart()
    fun playGameEnd()
    fun playSuccess()
    fun playTap()
    fun playTimerWarning()
    fun playResultsReveal()
    fun playSpeedBonus()
    fun playError()
}
