package pg.geobingo.one.platform

/**
 * Lightweight platform sound player.
 * Each method plays a short feedback sound appropriate for the event.
 */
expect object SoundPlayer {
    fun playCapture()
    fun playVote()
    fun playCountdownTick()
    fun playGameStart()
    fun playGameEnd()
    fun playSuccess()
    fun playTap()
}
