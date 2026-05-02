package pg.geobingo.one.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameMode
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.i18n.S
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.SettingsKeys
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.ui.theme.rememberFeedback
import pg.geobingo.one.util.AppLogger

@Composable
fun VoteTransitionScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val feedback = rememberFeedback(gameState)
    val isAiJudge = gameState.session.gameMode == GameMode.AI_JUDGE

    val modeGradient = when (gameState.session.gameMode) {
        GameMode.CLASSIC     -> GradientPrimary
        GameMode.BLIND_BINGO -> GradientCool
        GameMode.WEIRD_CORE  -> GradientWeird
        GameMode.QUICK_START -> GradientQuickStart
        GameMode.AI_JUDGE    -> GradientAiJudge
    }

    if (isAiJudge) {
        AiJudgeTransition(gameState, modeGradient, feedback)
    } else {
        NormalVoteTransition(modeGradient, feedback, nav)
    }
}

@Composable
private fun NormalVoteTransition(
    modeGradient: List<Color>,
    feedback: pg.geobingo.one.ui.theme.FeedbackManager,
    nav: pg.geobingo.one.navigation.NavigationManager,
) {
    var countdown by remember { mutableStateOf(3) }

    LaunchedEffect(Unit) {
        feedback.gameEnd()
        repeat(3) {
            delay(1000L)
            countdown--
            feedback.countdownTick()
        }
        nav.replaceCurrent(Screen.REVIEW)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            AnimatedGradientText(
                text = S.current.voting,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                ),
                gradientColors = modeGradient,
            )

            Text(
                text = S.current.reviewInProgress,
                style = MaterialTheme.typography.bodyLarge,
                color = ColorOnSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))

            AnimatedGradientBox(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp)),
                gradientColors = modeGradient,
                durationMillis = 600,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$countdown",
                        style = AppTextStyles.countdown,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun AiJudgeTransition(
    gameState: GameState,
    modeGradient: List<Color>,
    feedback: pg.geobingo.one.ui.theme.FeedbackManager,
) {
    val nav = remember { ServiceLocator.navigation }
    var progressCurrent by remember { mutableStateOf(0) }
    var progressTotal by remember { mutableStateOf(0) }
    var isDone by remember { mutableStateOf(false) }

    // AI consent for multiplayer — host needs consent before sending photos to AI
    var aiConsentAccepted by remember { mutableStateOf(AppSettings.getBoolean(SettingsKeys.AI_CONSENT_ACCEPTED, false)) }
    var showAiConsentDialog by remember { mutableStateOf(false) }
    // true once consent flow is resolved (accepted or declined)
    var consentResolved by remember { mutableStateOf(aiConsentAccepted) }
    var consentDeclined by remember { mutableStateOf(false) }

    // Show consent dialog on first launch if not already accepted
    LaunchedEffect(Unit) {
        if (!aiConsentAccepted && gameState.session.isHost) {
            showAiConsentDialog = true
        }
    }

    if (showAiConsentDialog) {
        AlertDialog(
            onDismissRequest = {
                showAiConsentDialog = false
                consentDeclined = true
                consentResolved = true
            },
            icon = { Icon(Icons.Default.PhotoCamera, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(28.dp)) },
            title = { Text(S.current.aiConsentTitle, fontWeight = FontWeight.Bold) },
            text = { pg.geobingo.one.ui.components.AiConsentDialogText() },
            confirmButton = {
                TextButton(onClick = {
                    AppSettings.setBoolean(SettingsKeys.AI_CONSENT_ACCEPTED, true)
                    aiConsentAccepted = true
                    showAiConsentDialog = false
                    consentResolved = true
                }) {
                    Text(S.current.aiConsentAccept)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAiConsentDialog = false
                    consentDeclined = true
                    consentResolved = true
                }) {
                    Text(S.current.aiConsentDecline)
                }
            },
        )
    }

    val transition = rememberInfiniteTransition(label = "aiJudge")
    val sparkleRotation by transition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sparkleRot",
    )
    val sparkleScale by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sparkleScale",
    )

    LaunchedEffect(consentResolved) {
        if (!consentResolved) return@LaunchedEffect
        feedback.gameEnd()
        val gameId = gameState.session.gameId ?: return@LaunchedEffect
        val isHost = gameState.session.isHost

        if (isHost) {
            // Host runs AI validation for all captures (only if consent was given)
            if (!consentDeclined) {
                try {
                    GameRepository.validateMultiplayerCaptures(
                        gameId = gameId,
                        categories = gameState.gameplay.selectedCategories,
                        onProgress = { current, total ->
                            progressCurrent = current
                            progressTotal = total
                        },
                    )
                } catch (e: Exception) {
                    AppLogger.e("VoteTransition", "AI validation failed", e)
                }
            }

            // Set game status to results
            try {
                GameRepository.setGameStatus(gameId, "results")
            } catch (e: Exception) {
                AppLogger.e("VoteTransition", "Failed to set results status", e)
            }

            // Load votes and navigate
            try {
                gameState.review.allVotes = GameRepository.getVotes(gameId)
            } catch (e: Exception) {
                AppLogger.w("VoteTransition", "Votes fetch failed", e)
            }
            isDone = true
            nav.replaceCurrent(Screen.RESULTS_TRANSITION)
        } else {
            // Non-host: poll for game status change to "results"
            while (true) {
                delay(2000L)
                try {
                    val game = GameRepository.getGameById(gameId)
                    if (game?.status == "results") {
                        try {
                            gameState.review.allVotes = GameRepository.getVotes(gameId)
                        } catch (e: Exception) {
                            AppLogger.w("VoteTransition", "Votes fetch failed", e)
                        }
                        isDone = true
                        nav.replaceCurrent(Screen.RESULTS_TRANSITION)
                        break
                    }
                } catch (e: Exception) {
                    AppLogger.w("VoteTransition", "Polling error", e)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            AnimatedGradientText(
                text = S.current.aiAnalyzing,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                ),
                gradientColors = modeGradient,
            )

            Text(
                text = S.current.aiAnalyzingDesc,
                style = MaterialTheme.typography.bodyLarge,
                color = ColorOnSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))

            // Animated sparkle icon
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        rotationZ = sparkleRotation
                        scaleX = sparkleScale
                        scaleY = sparkleScale
                    },
                tint = modeGradient.first(),
            )

            Spacer(Modifier.height(8.dp))

            // Progress indicator
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = modeGradient.first(),
                strokeWidth = 3.dp,
            )

            if (progressTotal > 0) {
                Text(
                    text = S.current.aiAnalyzingProgress
                        .replace("%1", "$progressCurrent")
                        .replace("%2", "$progressTotal"),
                    style = MaterialTheme.typography.labelMedium,
                    color = ColorOnSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
