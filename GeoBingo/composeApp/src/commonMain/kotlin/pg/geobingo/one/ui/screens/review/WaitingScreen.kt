package pg.geobingo.one.ui.screens.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import pg.geobingo.one.game.GameConstants
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.util.AppLogger
import pg.geobingo.one.i18n.S
import pg.geobingo.one.ui.theme.*

@Composable
internal fun DarkWaitingScreen(
    gameId: String,
    stepKey: String,
    categoryName: String,
    playerName: String,
    categoryIndex: Int,
    totalCategories: Int,
    playerIndex: Int,
    totalPlayers: Int,
    requiredVotes: Int = totalPlayers - 1,
    isHost: Boolean,
    isSelf: Boolean = false,
    isTeamMode: Boolean = false,
    modeGradient: List<Color> = GradientPrimary,
    onReadyToAdvance: () -> Unit,
    onForceAdvance: () -> Unit,
) {
    var submittedCount by remember(stepKey) { mutableStateOf(0) }
    var advanceCalled by remember(stepKey) { mutableStateOf(false) }
    LaunchedEffect(stepKey) {
        while (true) {
            try {
                submittedCount = GameRepository.getVoteSubmissionCount(gameId, stepKey)
                if (submittedCount >= requiredVotes && !advanceCalled) {
                    advanceCalled = true
                    onReadyToAdvance()
                }
            } catch (e: Exception) { AppLogger.w("Waiting", "Vote count poll failed", e) }
            delay(GameConstants.WAITING_POLL_INTERVAL_MS)
        }
    }
    Box(modifier = Modifier.fillMaxSize().background(ColorBackground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = ColorPrimary)
            AnimatedGradientText(
                text = if (isSelf) S.current.yourPhotoBeingRated else S.current.voting,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                gradientColors = modeGradient,
            )
            Text(
                if (isSelf) S.current.othersAreVoting
                else S.current.votedOf(submittedCount, requiredVotes),
                style = MaterialTheme.typography.bodyMedium,
                color = ColorOnSurfaceVariant,
            )
            if (isHost) {
                OutlinedButton(
                    onClick = onForceAdvance,
                    modifier = Modifier.padding(top = 16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorOnSurfaceVariant),
                    border = BorderStroke(1.dp, ColorOutlineVariant),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text(S.current.skipHostOption, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
