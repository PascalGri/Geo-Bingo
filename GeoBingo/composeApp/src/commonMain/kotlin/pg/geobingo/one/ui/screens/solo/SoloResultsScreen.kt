package pg.geobingo.one.ui.screens.solo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.game.state.AchievementManager
import pg.geobingo.one.game.state.Achievement
import pg.geobingo.one.game.state.SoloStatsManager
import pg.geobingo.one.i18n.S
import pg.geobingo.one.network.AccountManager
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.platform.AdManager
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.SettingsKeys
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.platform.SoundEffect
import pg.geobingo.one.platform.SoundPlayer
import pg.geobingo.one.platform.play
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
    val AIGradient = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
    val SoloGradient = listOf(Color(0xFF22D3EE), Color(0xFF6366F1))
    val GoldGradient = listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))

    // Track newly unlocked achievements
    var newAchievements by remember { mutableStateOf<List<Achievement>>(emptyList()) }
    var isNewPersonalBest by remember { mutableStateOf(false) }

    // Interstitial ad state (survives navigation away and back)

    // Record stats, check achievements, submit score
    LaunchedEffect(Unit) {
        Analytics.track(Analytics.SOLO_GAME_COMPLETED, mapOf(
            "score" to solo.totalScore.toString(),
            "captured" to solo.capturedCategories.size.toString(),
            "timeBonus" to solo.timeBonus.toString(),
            "starScore" to solo.starScore.toString(),
            "perfectGame" to solo.isPerfectGame.toString(),
            "categoryCount" to solo.categoryCount.toString(),
        ))

        // Record stats and check achievements
        val statsBefore = SoloStatsManager.getStats()
        isNewPersonalBest = solo.totalScore > statsBefore.bestScore
        SoloStatsManager.recordGame(solo)
        val statsAfter = SoloStatsManager.getStats()
        newAchievements = AchievementManager.checkAfterGame(solo, statsAfter)

        // Update persistent general stats
        val gamesPlayed = AppSettings.getInt(SettingsKeys.GAMES_PLAYED, 0) + 1
        AppSettings.setInt(SettingsKeys.GAMES_PLAYED, gamesPlayed)

        // Submit to server (rate-limited)
        if (!pg.geobingo.one.util.RateLimiter.allow(pg.geobingo.one.util.RateLimiter.KEY_SOLO_SUBMIT, pg.geobingo.one.util.RateLimiter.SOLO_SUBMIT_COOLDOWN_MS)) return@LaunchedEffect
        try {
            GameRepository.submitSoloScore(
                playerName = solo.playerName,
                score = solo.totalScore,
                categoriesCount = solo.categoryCount,
                timeBonus = solo.timeBonus,
                durationSeconds = solo.totalDurationSeconds,
                isOutdoor = solo.isOutdoor,
                userId = AccountManager.currentUserId,
            )
            submitted = true
            // Notify friends about high score
            if (isNewPersonalBest) {
                pg.geobingo.one.network.NotificationHelper.notifySoloHighScore(solo.totalScore)
            }
        } catch (e: Exception) {
            AppLogger.w("SoloResults", "Score submission failed", e)
            submitError = true
        }
    }

    // Daily challenge check for solo mode
    LaunchedEffect(Unit) {
        if (!gameState.stars.dailyChallengeCompleted) {
            val challenge = pg.geobingo.one.game.state.DailyChallengeManager.getTodayChallenge()
            val done = when (challenge.type) {
                pg.geobingo.one.game.state.ChallengeType.WIN_ROUND -> solo.capturedCategories.isNotEmpty()
                pg.geobingo.one.game.state.ChallengeType.CAPTURE_CATEGORIES -> solo.capturedCategories.size >= 3
                pg.geobingo.one.game.state.ChallengeType.PLAY_MODE -> false
            }
            if (done) {
                gameState.stars.completeDailyChallenge(challenge.reward)
                gameState.ui.pendingToast = "${S.current.dailyChallengeCompleted} +${challenge.reward} ${S.current.stars}"
            }
        }
        // Weekly challenge progress
        if (!gameState.stars.weeklyChallengeCompleted) {
            val weekly = pg.geobingo.one.game.state.WeeklyChallengeManager.getThisWeekChallenge()
            when (weekly.type) {
                pg.geobingo.one.game.state.WeeklyChallengeType.WIN_ROUNDS -> if (solo.capturedCategories.isNotEmpty()) gameState.stars.incrementWeeklyProgress()
                pg.geobingo.one.game.state.WeeklyChallengeType.PLAY_ROUNDS -> gameState.stars.incrementWeeklyProgress()
                pg.geobingo.one.game.state.WeeklyChallengeType.CAPTURE_TOTAL -> gameState.stars.incrementWeeklyProgress(solo.capturedCategories.size)
                pg.geobingo.one.game.state.WeeklyChallengeType.PLAY_ALL_MODES -> {}
                pg.geobingo.one.game.state.WeeklyChallengeType.WIN_STREAK -> if (solo.capturedCategories.isNotEmpty()) gameState.stars.incrementWeeklyProgress() else {
                    pg.geobingo.one.game.state.WeeklyChallengeManager.setProgress(0)
                }
            }
            if (gameState.stars.weeklyChallengeProgress >= weekly.target) {
                gameState.stars.completeWeeklyChallenge(weekly.reward)
                gameState.ui.pendingToast = "${S.current.weeklyChallengeCompleted} +${weekly.reward} ${S.current.stars}"
            }
        }
    }

    // Interstitial once per round (not skippable, except with No-Ads purchase)
    LaunchedEffect(Unit) {
        if (AdManager.isAdSupported && !gameState.stars.noAdsPurchased && !gameState.ui.interstitialShown) {
            kotlinx.coroutines.delay(1200)
            gameState.ui.interstitialShown = true
            AdManager.showInterstitialAd {}
        }
    }

    SystemBackHandler {
        solo.reset()
        gameState.ui.interstitialShown = false
        nav.resetTo(Screen.HOME)
    }

    val allCaptured = solo.capturedCategories.size == solo.categories.size && solo.categories.isNotEmpty()
    var showConfetti by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (allCaptured) {
            kotlinx.coroutines.delay(600)
            showConfetti = true
            if (gameState.ui.soundEnabled) SoundPlayer.play(SoundEffect.Confetti)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = AIGradient.first(), modifier = Modifier.size(18.dp))
                        AnimatedGradientText(
                            text = "KatchIt! AI",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            gradientColors = AIGradient,
                        )
                    }
                },
                actions = { pg.geobingo.one.ui.components.TopBarStarsAndProfile(gameState = gameState, onNavigate = { nav.navigateTo(it) }) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        bottomBar = {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                OutlinedButton(
                    onClick = { nav.navigateTo(Screen.SOLO_LEADERBOARD) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, SoloGradient.first()),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SoloGradient.first()),
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
                        gameState.ui.interstitialShown = false
                        nav.resetTo(Screen.HOME)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    gradientColors = SoloGradient,
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
            Spacer(Modifier.height(24.dp))

            // Trophy with pulse animation
            val trophyScale = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                trophyScale.animateTo(1.15f, tween(400))
                trophyScale.animateTo(1f, tween(200))
            }
            Icon(
                imageVector = if (solo.isPerfectGame) Icons.Default.Verified else Icons.Default.EmojiEvents,
                contentDescription = S.current.soloChallengeComplete,
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer { scaleX = trophyScale.value; scaleY = trophyScale.value },
                tint = if (solo.isPerfectGame) Color(0xFFFBBF24) else Color(0xFFFBBF24),
            )

            Spacer(Modifier.height(12.dp))

            // Perfect Game banner
            if (solo.isPerfectGame) {
                AnimatedGradientText(
                    text = S.current.soloPerfectGame,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    gradientColors = GoldGradient,
                )
                Spacer(Modifier.height(4.dp))
            }

            // New personal best banner
            if (isNewPersonalBest && solo.totalScore > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Color(0xFF22C55E), modifier = Modifier.size(16.dp))
                    Text(
                        S.current.soloNewPersonalBest,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF22C55E),
                    )
                }
                Spacer(Modifier.height(4.dp))
            }

            // AI judged badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = AIGradient.first(), modifier = Modifier.size(14.dp))
                AnimatedGradientText(
                    text = "AI bewertet",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    gradientColors = AIGradient,
                )
            }

            Spacer(Modifier.height(8.dp))

            // Animated score reveal
            val scoreAnim = remember { Animatable(0f) }
            LaunchedEffect(Unit) { scoreAnim.animateTo(1f, tween(800)) }
            AnimatedGradientText(
                text = "${solo.totalScore}",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                gradientColors = SoloGradient,
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
                borderColors = if (solo.isPerfectGame) GoldGradient else SoloGradient,
                backgroundColor = ColorSurface,
                borderWidth = 1.5.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ScoreRow(
                        label = "${solo.starSum} / ${solo.categories.size * 5} ${S.current.stars}",
                        value = "${solo.starScore} ${S.current.pointsAbbrev}",
                        icon = Icons.Default.Star,
                        valueColor = Color(0xFFFBBF24),
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
                    if (solo.isPerfectGame) {
                        HorizontalDivider(color = ColorOutlineVariant)
                        ScoreRow(
                            label = S.current.soloPerfectBonus,
                            value = "+${solo.perfectBonus} ${S.current.pointsAbbrev}",
                            icon = Icons.Default.Verified,
                            valueColor = Color(0xFFFBBF24),
                        )
                    }
                }
            }

            // Newly unlocked achievements
            if (newAchievements.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                newAchievements.forEach { achievement ->
                    GradientBorderCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        cornerRadius = 12.dp,
                        borderColors = GoldGradient,
                        backgroundColor = ColorSurface,
                        borderWidth = 1.5.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(achievement.icon, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(28.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    S.current.soloAchievementUnlocked,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFBBF24),
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    achievement.nameKey,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorOnSurface,
                                )
                                Text(
                                    achievement.descriptionKey,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ColorOnSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Category details with AI ratings
            solo.categories.forEach { category ->
                val isCaptured = category.id in solo.capturedCategories
                val speed = if (isCaptured) solo.getCaptureSpeed(category.id) else null
                val rating = solo.categoryRatings[category.id]
                val reason = solo.categoryReasons[category.id]
                var expanded by remember { mutableStateOf(false) }

                val ratingColor = when {
                    rating == null -> ColorOnSurfaceVariant
                    rating >= 4 -> Color(0xFF22C55E)
                    rating >= 3 -> Color(0xFFFBBF24)
                    else -> Color(0xFFEF4444)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (expanded) ColorSurface else Color.Transparent)
                        .clickable { if (reason != null && reason.isNotBlank()) expanded = !expanded }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            if (isCaptured) {
                                Icon(Icons.Default.Check, null, tint = ColorPrimary, modifier = Modifier.size(18.dp))
                            } else {
                                Icon(Icons.Default.Close, null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            }
                            Text(category.name, style = MaterialTheme.typography.bodyMedium, color = ColorOnSurface)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (speed != null) {
                                Text("${speed}s", style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant)
                            }
                            if (rating != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                                ) {
                                    repeat(5) { i ->
                                        Icon(
                                            if (i < rating) Icons.Default.Star else Icons.Default.StarBorder,
                                            null,
                                            tint = if (i < rating) ratingColor else ratingColor.copy(alpha = 0.3f),
                                            modifier = Modifier.size(12.dp),
                                        )
                                    }
                                }
                            }
                            if (reason != null && reason.isNotBlank()) {
                                Icon(
                                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    null,
                                    tint = ColorOnSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                    // Expandable AI reason
                    if (expanded && reason != null && reason.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.padding(start = 26.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(12.dp).padding(top = 2.dp))
                            Text(
                                reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorOnSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Solo stats summary card
            val stats = remember { SoloStatsManager.getStats() }
            if (stats.gamesPlayed > 1) {
                GradientBorderCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 12.dp,
                    borderColors = listOf(ColorOutlineVariant, ColorOutlineVariant),
                    backgroundColor = ColorSurface,
                    borderWidth = 1.dp,
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Solo Stats",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorOnSurface,
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatItem("Spiele", "${stats.gamesPlayed}")
                            StatItem("Rekord", "${stats.bestScore}")
                            StatItem("Schnitt", "${stats.averageScore}")
                            StatItem("Perfekt", "${stats.perfectGames}")
                        }
                        if (stats.fastestComplete > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatItem("Schnellste", "${stats.fastestComplete}s")
                                StatItem("Sterne", "${stats.totalStars}")
                                val unlocked = AchievementManager.getUnlockedCount()
                                val total = AchievementManager.ALL_ACHIEVEMENTS.size
                                StatItem("Achievements", "$unlocked/$total")
                            }
                        }
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
                                categoriesCount = solo.categoryCount,
                                timeBonus = solo.timeBonus,
                                durationSeconds = solo.totalDurationSeconds,
                                userId = AccountManager.currentUserId,
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
        if (allCaptured || solo.isPerfectGame) {
            ConfettiEffect(trigger = showConfetti, modifier = Modifier.fillMaxSize())
        }
        } // end Box
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ColorOnSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant)
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
