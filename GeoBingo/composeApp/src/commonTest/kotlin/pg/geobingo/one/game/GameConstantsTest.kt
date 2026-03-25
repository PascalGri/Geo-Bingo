package pg.geobingo.one.game

import kotlin.test.Test
import kotlin.test.assertTrue

class GameConstantsTest {

    @Test
    fun pollingIntervalsAreOrdered() {
        assertTrue(GameConstants.POLLING_INITIAL_INTERVAL_MS < GameConstants.POLLING_MAX_INTERVAL_MS)
    }

    @Test
    fun retryIntervalsAreOrdered() {
        assertTrue(GameConstants.RETRY_INITIAL_DELAY_MS < GameConstants.RETRY_MAX_DELAY_MS)
    }

    @Test
    fun gameDurationRangeIsValid() {
        assertTrue(GameConstants.MIN_GAME_DURATION_MINUTES < GameConstants.MAX_GAME_DURATION_MINUTES)
        assertTrue(GameConstants.DEFAULT_GAME_DURATION_MINUTES in GameConstants.MIN_GAME_DURATION_MINUTES..GameConstants.MAX_GAME_DURATION_MINUTES)
    }

    @Test
    fun finishCountdownIsPositive() {
        assertTrue(GameConstants.FINISH_COUNTDOWN_SECONDS > 0)
    }

    @Test
    fun lobbyTimeoutIsPositive() {
        assertTrue(GameConstants.LOBBY_TIMEOUT_SECONDS > 0)
    }
}
