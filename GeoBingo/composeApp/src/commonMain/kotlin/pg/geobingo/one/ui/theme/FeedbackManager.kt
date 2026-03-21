package pg.geobingo.one.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import pg.geobingo.one.game.GameState
import pg.geobingo.one.platform.SoundPlayer

class FeedbackManager(
    private val gameState: GameState,
    private val haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
) {
    private fun hapticTick() {
        if (gameState.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun tap() {
        hapticTick()
        if (gameState.soundEnabled) SoundPlayer.playTap()
    }

    fun capture() {
        hapticTick()
        if (gameState.soundEnabled) SoundPlayer.playCapture()
    }

    fun vote() {
        hapticTick()
        if (gameState.soundEnabled) SoundPlayer.playVote()
    }

    fun countdownTick() {
        if (gameState.soundEnabled) SoundPlayer.playCountdownTick()
    }

    fun gameStart() {
        hapticTick()
        if (gameState.soundEnabled) SoundPlayer.playGameStart()
    }

    fun gameEnd() {
        hapticTick()
        if (gameState.soundEnabled) SoundPlayer.playGameEnd()
    }

    fun success() {
        hapticTick()
        if (gameState.soundEnabled) SoundPlayer.playSuccess()
    }
}

@Composable
fun rememberFeedback(gameState: GameState): FeedbackManager {
    val haptic = LocalHapticFeedback.current
    return remember(gameState, haptic) { FeedbackManager(gameState, haptic) }
}
