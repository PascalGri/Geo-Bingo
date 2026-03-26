package pg.geobingo.one.ui.screens.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameMode
import pg.geobingo.one.game.GameState
import pg.geobingo.one.network.VoteKeys
import pg.geobingo.one.i18n.S
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.viewmodel.ReviewViewModel

@Composable
fun ReviewScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val vm = viewModel { ReviewViewModel(gameState, nav) }

    val modeColor = when (gameState.session.gameMode) {
        GameMode.CLASSIC     -> GradientPrimary.first()
        GameMode.BLIND_BINGO -> GradientCool.first()
        GameMode.WEIRD_CORE  -> GradientWeird.first()
        GameMode.QUICK_START -> GradientQuickStart.first()
    }
    val gameId = gameState.session.gameId ?: return
    val categories = vm.categories
    val myPlayerId = gameState.session.myPlayerId ?: return
    val sortedPlayers = vm.sortedPlayers
    val numPlayers = sortedPlayers.size

    // Start observing realtime/polling
    LaunchedEffect(gameId) {
        vm.startObserving()
    }

    // Reload captures when review step changes
    LaunchedEffect(gameState.review.reviewCategoryIndex) {
        // Captures are reloaded inside the VM via startObserving
    }

    if (numPlayers == 0 || categories.isEmpty()) {
        Box(Modifier.fillMaxSize().background(ColorBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = modeColor)
        }
        return
    }

    val stepIndex = gameState.review.reviewCategoryIndex
    val totalSteps = categories.size * numPlayers

    if (stepIndex >= totalSteps) {
        Box(Modifier.fillMaxSize().background(ColorBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = modeColor)
        }
        return
    }

    val categoryIndex = stepIndex / numPlayers
    val targetPlayerIndex = stepIndex % numPlayers
    val currentCategory = categories[categoryIndex]
    val targetPlayer = sortedPlayers[targetPlayerIndex]
    val stepKey = VoteKeys.stepKey(currentCategory.id, targetPlayer.id)

    // Auto-skip self-voting
    val isSelf = targetPlayer.id == myPlayerId
    LaunchedEffect(stepIndex, isSelf) {
        if (isSelf && !gameState.review.hasSubmittedCurrentCategory) {
            vm.handleSelfVoteSkip()
        }
    }

    key(stepIndex) {
        // Self-vote toast overlay
        if (vm.selfVoteToast) {
            Box(
                modifier = Modifier.fillMaxSize().background(ColorBackground),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = modeColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, modeColor.copy(alpha = 0.4f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = modeColor,
                        )
                        Text(
                            S.current.yourPhotoBeingRated,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorOnPrimaryContainer,
                        )
                    }
                }
            }
        } else if (isSelf || gameState.review.hasSubmittedCurrentCategory) {
            DarkWaitingScreen(
                gameId = gameId,
                stepKey = stepKey,
                categoryName = currentCategory.name,
                playerName = targetPlayer.name,
                categoryIndex = categoryIndex,
                totalCategories = categories.size,
                playerIndex = targetPlayerIndex,
                totalPlayers = numPlayers,
                isHost = gameState.session.isHost,
                isSelf = isSelf,
                onReadyToAdvance = { vm.submitNoPhoto() },
                onForceAdvance = { vm.forceAdvance() },
            )
        } else {
            DarkSinglePhotoVotingScreen(
                gameId = gameId,
                currentCategory = currentCategory,
                categoryIndex = categoryIndex,
                totalCategories = categories.size,
                targetPlayer = targetPlayer,
                targetPlayerIndex = targetPlayerIndex,
                totalPlayers = numPlayers,
                stepIndex = stepIndex,
                playerAvatarBytes = gameState.photo.playerAvatarBytes[targetPlayer.id],
                hapticEnabled = gameState.ui.hapticEnabled,
                soundEnabled = gameState.ui.soundEnabled,
                onVote = { rating -> vm.submitVote(rating) },
                onNoPhoto = { vm.submitNoPhoto() },
            )
        }
    }
}
