package pg.geobingo.one.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import pg.geobingo.one.game.GameState
import pg.geobingo.one.platform.SoundPlayer
import pg.geobingo.one.platform.PlatformHaptics

class FeedbackManager(
    private val gameState: GameState,
    private val haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
) {
    private fun hapticTick() {
        if (gameState.ui.hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            PlatformHaptics.vibrate()
        }
    }

    fun tap() {
        hapticTick()
        if (gameState.ui.soundEnabled) SoundPlayer.playTap()
    }

    fun capture() {
        hapticTick()
        if (gameState.ui.soundEnabled) SoundPlayer.playCapture()
    }

    fun vote() {
        hapticTick()
        if (gameState.ui.soundEnabled) SoundPlayer.playVote()
    }

    fun countdownTick() {
        if (gameState.ui.soundEnabled) SoundPlayer.playCountdownTick()
    }

    fun gameStart() {
        hapticTick()
        if (gameState.ui.soundEnabled) SoundPlayer.playGameStart()
    }

    fun gameEnd() {
        hapticTick()
        if (gameState.ui.soundEnabled) SoundPlayer.playGameEnd()
    }

    fun success() {
        hapticTick()
        if (gameState.ui.soundEnabled) SoundPlayer.playSuccess()
    }
}

@Composable
fun rememberFeedback(gameState: GameState): FeedbackManager {
    val haptic = LocalHapticFeedback.current
    return remember(gameState, haptic) { FeedbackManager(gameState, haptic) }
}
