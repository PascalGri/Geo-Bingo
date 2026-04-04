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
import pg.geobingo.one.i18n.S
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.viewmodel.ReviewViewModel

@Composable
fun ReviewScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val vm = viewModel { ServiceLocator.createReviewViewModel() }

    val modeGradient = when (gameState.session.gameMode) {
        GameMode.CLASSIC     -> GradientPrimary
        GameMode.BLIND_BINGO -> GradientCool
        GameMode.WEIRD_CORE  -> GradientWeird
        GameMode.QUICK_START -> GradientQuickStart
        GameMode.AI_JUDGE    -> GradientAiJudge
    }
    val modeColor = modeGradient.first()
    val gameId = gameState.session.gameId ?: return
    val categories = vm.categories
    val myPlayerId = gameState.session.myPlayerId ?: return
    val entityCount = vm.reviewEntityCount

    LaunchedEffect(gameId) {
        pg.geobingo.one.game.ActiveSession.save(gameState)
        vm.startObserving()
    }

    if (entityCount == 0 || categories.isEmpty()) {
        Box(Modifier.fillMaxSize().background(ColorBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = modeColor)
        }
        return
    }

    val stepIndex = gameState.review.reviewCategoryIndex
    val totalSteps = vm.totalSteps

    if (stepIndex >= totalSteps) {
        Box(Modifier.fillMaxSize().background(ColorBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = modeColor)
        }
        return
    }

    val categoryIndex = stepIndex / entityCount
    val targetIndex = stepIndex % entityCount
    val currentCategory = categories[categoryIndex]
    val stepKey = vm.currentStepKey() ?: return
    val isSelf = vm.isCurrentStepSelf()

    // Determine target info for display
    val targetDisplayName: String
    val targetPlayerId: String? // the player whose photo to show
    val targetPlayerForAvatar: pg.geobingo.one.data.Player?

    if (vm.isTeamMode) {
        val targetTeam = vm.sortedTeams[targetIndex]
        val teamName = gameState.gameplay.teamNames[targetTeam] ?: S.current.teamName(targetTeam)
        val capturer = gameState.teams.getTeamCapturer(targetTeam, currentCategory.id)
        targetDisplayName = teamName
        targetPlayerId = capturer?.id
        targetPlayerForAvatar = capturer
    } else {
        val targetPlayer = vm.sortedPlayers[targetIndex]
        targetDisplayName = targetPlayer.name
        targetPlayerId = targetPlayer.id
        targetPlayerForAvatar = targetPlayer
    }

    // Auto-skip self-voting
    LaunchedEffect(stepIndex, isSelf) {
        if (isSelf && !gameState.review.hasSubmittedCurrentCategory) {
            vm.handleSelfVoteSkip()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
    // Progress bar
    val progress = if (totalSteps > 0) (stepIndex + 1).toFloat() / totalSteps else 0f
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().height(4.dp),
        color = modeColor,
        trackColor = ColorSurfaceVariant,
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            S.current.categoryOfTotal(categoryIndex + 1, categories.size),
            style = MaterialTheme.typography.labelSmall,
            color = ColorOnSurfaceVariant,
        )
        Text(
            S.current.playerOfTotal(targetIndex + 1, entityCount),
            style = MaterialTheme.typography.labelSmall,
            color = ColorOnSurfaceVariant,
        )
    }

    Box(modifier = Modifier.weight(1f)) {
    key(stepIndex) {
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
                playerName = targetDisplayName,
                categoryIndex = categoryIndex,
                totalCategories = categories.size,
                playerIndex = targetIndex,
                totalPlayers = entityCount,
                requiredVotes = vm.requiredVotesForCurrentStep(),
                isHost = gameState.session.isHost,
                isSelf = isSelf,
                isTeamMode = vm.isTeamMode,
                modeGradient = modeGradient,
                onReadyToAdvance = { vm.submitNoPhoto() },
                onForceAdvance = { vm.forceAdvance() },
            )
        } else if (targetPlayerForAvatar != null) {
            DarkSinglePhotoVotingScreen(
                gameId = gameId,
                currentCategory = currentCategory,
                categoryIndex = categoryIndex,
                totalCategories = categories.size,
                targetPlayer = targetPlayerForAvatar,
                targetPlayerIndex = targetIndex,
                totalPlayers = entityCount,
                stepIndex = stepIndex,
                playerAvatarBytes = gameState.photo.playerAvatarBytes[targetPlayerForAvatar.id],
                hapticEnabled = gameState.ui.hapticEnabled,
                soundEnabled = gameState.ui.soundEnabled,
                teamName = if (vm.isTeamMode) targetDisplayName else null,
                modeGradient = modeGradient,
                onVote = { rating -> vm.submitVote(rating) },
                onNoPhoto = { vm.submitNoPhoto() },
            )
        } else {
            // No capturer found for this team/category - skip
            LaunchedEffect(stepIndex) { vm.submitNoPhoto() }
        }
    }
    } // end Box
    } // end Column
}
