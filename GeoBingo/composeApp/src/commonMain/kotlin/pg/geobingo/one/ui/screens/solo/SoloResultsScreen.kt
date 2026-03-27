package pg.geobingo.one.ui.screens.solo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.i18n.S
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.SettingsKeys
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.util.Analytics
import pg.geobingo.one.util.AppLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoloResultsScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val solo = gameState.solo
    val scope = rememberCoroutineScope()
    var submitted by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf(false) }

    // Submit score to leaderboard
    LaunchedEffect(Unit) {
        Analytics.track(Analytics.SOLO_GAME_COMPLETED, mapOf(
            "score" to solo.totalScore.toString(),
            "captured" to solo.capturedCategories.size.toString(),
            "timeBonus" to solo.timeBonus.toString(),
        ))
        // Update persistent stats
        val gamesPlayed = AppSettings.getInt(SettingsKeys.GAMES_PLAYED, 0) + 1
        AppSettings.setInt(SettingsKeys.GAMES_PLAYED, gamesPlayed)

        // Submit to server (rate-limited)
        if (!pg.geobingo.one.util.RateLimiter.allow(pg.geobingo.one.util.RateLimiter.KEY_SOLO_SUBMIT, pg.geobingo.one.util.RateLimiter.SOLO_SUBMIT_COOLDOWN_MS)) return@LaunchedEffect
        try {
            GameRepository.submitSoloScore(
                playerName = solo.playerName,
                score = solo.totalScore,
                categoriesCount = solo.capturedCategories.size,
                timeBonus = solo.timeBonus,
                durationSeconds = solo.totalDurationSeconds,
            )
            submitted = true
        } catch (e: Exception) {
            AppLogger.w("SoloResults", "Score submission failed", e)
            submitError = true
        }
    }

    SystemBackHandler {
        solo.reset()
        nav.resetTo(Screen.HOME)
    }

    val allCaptured = solo.capturedCategories.size == solo.categories.size
    var showConfetti by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (allCaptured) {
            kotlinx.coroutines.delay(600)
            showConfetti = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = S.current.soloChallengeComplete,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        gradientColors = GradientPrimary,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        bottomBar = {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                OutlinedButton(
                    onClick = { nav.navigateTo(Screen.SOLO_LEADERBOARD) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, ColorPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorPrimary),
                ) {
                    Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(S.current.soloLeaderboard, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                GradientButton(
                    text = S.current.newGame,
                    onClick = {
                        solo.reset()
                        nav.resetTo(Screen.HOME)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    gradientColors = GradientPrimary,
                    leadingIcon = {
                        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    },
                )
            }
        },
        containerColor = ColorBackground,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))

            // Trophy
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = S.current.soloChallengeComplete,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFFBBF24),
            )

            Spacer(Modifier.height(16.dp))

            // Score
            Text(
                "${solo.totalScore}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = ColorOnSurface,
            )
            Text(
                S.current.soloTotalScore,
                style = MaterialTheme.typography.bodyLarge,
                color = ColorOnSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))

            // Breakdown
            GradientBorderCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                borderColors = GradientPrimary,
                backgroundColor = ColorSurface,
                borderWidth = 1.5.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ScoreRow(
                        label = "${solo.capturedCategories.size}/${solo.categories.size} ${S.current.categories}",
                        value = "${solo.capturedCategories.size * solo.basePointsPerCategory} ${S.current.pointsAbbrev}",
                        icon = Icons.Default.CameraAlt,
                    )
                    if (allCaptured && solo.timeBonus > 0) {
                        HorizontalDivider(color = ColorOutlineVariant)
                        ScoreRow(
                            label = S.current.soloTimeBonus(solo.timeBonus),
                            value = "+${solo.timeBonus} ${S.current.pointsAbbrev}",
                            icon = Icons.Default.Timer,
                            valueColor = Color(0xFFFBBF24),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Category details
            solo.categories.forEach { category ->
                val isCaptured = category.id in solo.capturedCategories
                val speed = if (isCaptured) solo.getCaptureSpeed(category.id) else null
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (isCaptured) {
                            Icon(Icons.Default.Check, null, tint = ColorPrimary, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Default.Close, null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        }
                        Text(category.name, style = MaterialTheme.typography.bodyMedium, color = ColorOnSurface)
                    }
                    if (speed != null) {
                        Text("${speed}s", style = MaterialTheme.typography.labelMedium, color = ColorOnSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Submission status
            if (submitted) {
                Text(
                    "Score submitted!",
                    style = MaterialTheme.typography.labelMedium,
                    color = ColorPrimary,
                )
            } else if (submitError) {
                TextButton(onClick = {
                    submitError = false
                    scope.launch {
                        try {
                            GameRepository.submitSoloScore(
                                playerName = solo.playerName,
                                score = solo.totalScore,
                                categoriesCount = solo.capturedCategories.size,
                                timeBonus = solo.timeBonus,
                                durationSeconds = solo.totalDurationSeconds,
                            )
                            submitted = true
                        } catch (e: Exception) {
                            submitError = true
                        }
                    }
                }) {
                    Text(S.current.retry, color = ColorPrimary)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
        if (allCaptured) {
            ConfettiEffect(trigger = showConfetti, modifier = Modifier.fillMaxSize())
        }
        } // end Box
    }
}

@Composable
private fun ScoreRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    valueColor: Color = ColorOnSurface,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, null, tint = ColorPrimary, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = ColorOnSurface)
        }
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
