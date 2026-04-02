package pg.geobingo.one.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import pg.geobingo.one.game.GameState
import pg.geobingo.one.platform.SoundEffect
import pg.geobingo.one.platform.SoundPlayer
import pg.geobingo.one.platform.PlatformHaptics
import pg.geobingo.one.platform.play

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

    private fun sound(effect: SoundEffect) {
        if (gameState.ui.soundEnabled) SoundPlayer.play(effect)
    }

    // UI feedback
    fun tap() { hapticTick(); sound(SoundEffect.Tap) }
    fun toggle() { hapticTick(); sound(SoundEffect.Toggle) }
    fun swipe() { sound(SoundEffect.Swipe) }
    fun categorySelect() { hapticTick(); sound(SoundEffect.CategorySelect) }

    // Camera/photo
    fun capture() { hapticTick(); sound(SoundEffect.Capture) }
    fun photoValidated() { hapticTick(); sound(SoundEffect.PhotoValidated) }
    fun photoRejected() { hapticTick(); sound(SoundEffect.PhotoRejected) }

    // Game flow
    fun countdownTick() { sound(SoundEffect.CountdownTick) }
    fun gameStart() { hapticTick(); sound(SoundEffect.GameStart) }
    fun gameEnd() { hapticTick(); sound(SoundEffect.GameEnd) }
    fun timerWarning() { hapticTick(); sound(SoundEffect.TimerWarning) }

    // Achievements
    fun success() { hapticTick(); sound(SoundEffect.Success) }
    fun speedBonus() { hapticTick(); sound(SoundEffect.SpeedBonus) }
    fun resultsReveal() { hapticTick(); sound(SoundEffect.ResultsReveal) }
    fun challengeComplete() { hapticTick(); sound(SoundEffect.ChallengeComplete) }
    fun confetti() { sound(SoundEffect.Confetti) }

    // Social
    fun playerJoined() { sound(SoundEffect.PlayerJoined) }
    fun friendRequest() { sound(SoundEffect.FriendRequest) }
    fun vote() { hapticTick(); sound(SoundEffect.Vote) }

    // Shop/items
    fun purchaseSuccess() { hapticTick(); sound(SoundEffect.PurchaseSuccess) }
    fun powerUp() { hapticTick(); sound(SoundEffect.PowerUp) }

    // Error
    fun error() { hapticTick(); sound(SoundEffect.Error) }
}

@Composable
fun rememberFeedback(gameState: GameState): FeedbackManager {
    val haptic = LocalHapticFeedback.current
    return remember(gameState, haptic) { FeedbackManager(gameState, haptic) }
}
