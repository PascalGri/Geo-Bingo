package pg.geobingo.one.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.i18n.S
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.SettingsKeys
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.components.MiniBarChart
import pg.geobingo.one.ui.components.PieChart
import pg.geobingo.one.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val snackbarHostState = remember { SnackbarHostState() }
    var showAuthDialog by remember { mutableStateOf(false) }
    SystemBackHandler { nav.goBack() }

    // Core stats
    val gamesPlayed = remember { AppSettings.getInt(SettingsKeys.GAMES_PLAYED) }
    val gamesWon = remember { AppSettings.getInt(SettingsKeys.GAMES_WON) }
    val winRate = remember {
        if (gamesPlayed > 0) (gamesWon.toFloat() / gamesPlayed * 100).toInt() else 0
    }
    val totalStarsEarned = remember { AppSettings.getInt(SettingsKeys.TOTAL_STARS_EARNED) }
    val totalStarsCount = remember { AppSettings.getInt(SettingsKeys.TOTAL_STARS_COUNT) }
    val averageRating = remember {
        if (totalStarsCount > 0) totalStarsEarned.toFloat() / totalStarsCount else 0f
    }
    val longestStreak = remember { AppSettings.getInt(SettingsKeys.LONGEST_WIN_STREAK) }
    val currentStreak = remember { AppSettings.getInt(SettingsKeys.CURRENT_WIN_STREAK) }

    // Detailed stats
    val totalCaptures = remember { AppSettings.getInt(SettingsKeys.TOTAL_CAPTURES) }
    val totalSpeedBonuses = remember { AppSettings.getInt(SettingsKeys.TOTAL_SPEED_BONUSES) }
    val bestScore = remember { AppSettings.getInt(SettingsKeys.BEST_GAME_SCORE) }
    val totalTimeSec = remember { AppSettings.getInt(SettingsKeys.TOTAL_GAME_TIME_SECONDS) }
    val totalCategoriesPlayed = remember { AppSettings.getInt(SettingsKeys.TOTAL_CATEGORIES_PLAYED) }

    // Derived stats
    val capturesPerGame = remember {
        if (gamesPlayed > 0) totalCaptures.toFloat() / gamesPlayed else 0f
    }
    val avgTimePerCapture = remember {
        if (totalCaptures > 0) totalTimeSec.toFloat() / totalCaptures else 0f
    }

    // Favorite mode
    val modeClassic = remember { AppSettings.getInt(SettingsKeys.MODE_CLASSIC_COUNT) }
    val modeBlind = remember { AppSettings.getInt(SettingsKeys.MODE_BLIND_COUNT) }
    val modeWeird = remember { AppSettings.getInt(SettingsKeys.MODE_WEIRD_COUNT) }
    val modeQuick = remember { AppSettings.getInt(SettingsKeys.MODE_QUICK_COUNT) }
    val modeAiJudge = remember { AppSettings.getInt(SettingsKeys.MODE_AI_JUDGE_COUNT) }
    val favoriteMode = remember {
        val modes = listOf("Classic" to modeClassic, "Blind Bingo" to modeBlind, "Weird Core" to modeWeird, "Quick Start" to modeQuick, "AI Judge" to modeAiJudge)
        modes.maxByOrNull { it.second }?.takeIf { it.second > 0 }?.first ?: "--"
    }

    // Format total time
    val totalTimeFormatted = remember {
        val hours = totalTimeSec / 3600
        val minutes = (totalTimeSec % 3600) / 60
        if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    val avgTimeCaptureFormatted = remember {
        if (totalCaptures > 0) {
            val sec = avgTimePerCapture.toInt()
            val min = sec / 60
            val s = sec % 60
            if (min > 0) "${min}m ${s}s" else "${s}s"
        } else "--"
    }

    val anim = rememberStaggeredAnimation(count = 18)
    fun Modifier.staggered(index: Int): Modifier = this.then(anim.modifier(index))

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = S.current.statsTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = GradientPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.goHome() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = S.current.back, tint = ColorPrimary)
                    }
                },
                actions = { pg.geobingo.one.ui.components.TopBarStarsAndProfile(gameState = gameState, onNavigate = { nav.navigateTo(it) }) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        containerColor = ColorBackground,
    ) { padding ->
        if (!pg.geobingo.one.network.AccountManager.isLoggedIn) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                SignInRequiredState(
                    icon = Icons.Default.BarChart,
                    title = S.current.signInRequired,
                    description = S.current.signInRequiredDesc,
                    signInLabel = S.current.signIn,
                    onSignIn = { showAuthDialog = true },
                )
            }
        } else if (gamesPlayed == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .staggered(0),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.Default.BarChart,
                    title = S.current.noStatsYet,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenHorizontal, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // ── Section: Overview ────────────────────────────────
                SectionHeader(text = S.current.statsOverview, modifier = Modifier.staggered(0))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatCard(
                        icon = Icons.Default.SportsEsports,
                        value = gamesPlayed.toString(),
                        label = S.current.gamesPlayed,
                        modifier = Modifier.weight(1f).staggered(1),
                    )
                    StatCard(
                        icon = Icons.Default.EmojiEvents,
                        value = gamesWon.toString(),
                        label = S.current.gamesWon,
                        modifier = Modifier.weight(1f).staggered(2),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatCard(
                        icon = Icons.Default.Percent,
                        value = "$winRate%",
                        label = S.current.winRate,
                        modifier = Modifier.weight(1f).staggered(3),
                    )
                    StatCard(
                        icon = Icons.Default.CameraAlt,
                        value = totalCaptures.toString(),
                        label = S.current.statsTotalCaptures,
                        modifier = Modifier.weight(1f).staggered(4),
                    )
                }

                // ── Section: Performance ─────────────────────────────
                SectionHeader(text = S.current.statsPerformance, modifier = Modifier.staggered(5))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatCard(
                        icon = Icons.Default.Star,
                        value = if (totalStarsCount > 0) {
                            val rounded = (averageRating * 10).toInt()
                            "${rounded / 10}.${rounded % 10}"
                        } else "--",
                        label = S.current.avgRating,
                        modifier = Modifier.weight(1f).staggered(6),
                    )
                    StatCard(
                        icon = Icons.Default.WorkspacePremium,
                        value = bestScore.takeIf { it > 0 }?.toString() ?: "--",
                        label = S.current.statsBestScore,
                        modifier = Modifier.weight(1f).staggered(7),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatCard(
                        icon = Icons.Default.Bolt,
                        value = totalSpeedBonuses.toString(),
                        label = S.current.statsSpeedBonuses,
                        modifier = Modifier.weight(1f).staggered(8),
                    )
                    StatCard(
                        icon = Icons.Default.PhotoCamera,
                        value = if (gamesPlayed > 0) {
                            val rounded = (capturesPerGame * 10).toInt()
                            "${rounded / 10}.${rounded % 10}"
                        } else "--",
                        label = S.current.statsCapturesPerGame,
                        modifier = Modifier.weight(1f).staggered(9),
                    )
                }

                // ── Section: Activity ────────────────────────────────
                SectionHeader(text = S.current.statsActivity, modifier = Modifier.staggered(10))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatCard(
                        icon = Icons.Default.LocalFireDepartment,
                        value = longestStreak.toString(),
                        label = S.current.longestStreak,
                        accentColor = if (currentStreak > 0 && currentStreak == longestStreak) Color(0xFFEF4444) else null,
                        modifier = Modifier.weight(1f).staggered(11),
                    )
                    StatCard(
                        icon = Icons.Default.Whatshot,
                        value = currentStreak.toString(),
                        label = S.current.statsCurrentStreak,
                        accentColor = if (currentStreak >= 3) Color(0xFFEF4444) else null,
                        modifier = Modifier.weight(1f).staggered(12),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatCard(
                        icon = Icons.Default.Timer,
                        value = totalTimeFormatted,
                        label = S.current.statsTotalTime,
                        modifier = Modifier.weight(1f).staggered(13),
                    )
                    StatCard(
                        icon = Icons.Default.Speed,
                        value = avgTimeCaptureFormatted,
                        label = S.current.statsAvgTimePerCapture,
                        modifier = Modifier.weight(1f).staggered(14),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatCard(
                        icon = Icons.Default.Style,
                        value = favoriteMode,
                        label = S.current.statsFavoriteMode,
                        smallValue = true,
                        modifier = Modifier.weight(1f).staggered(15),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }

                // ── Section: Mode Distribution ───────────────────────
                val modeSegments = listOf(
                    S.current.modeClassic to modeClassic.toFloat(),
                    S.current.modeBlindBingo to modeBlind.toFloat(),
                    S.current.modeWeirdCore to modeWeird.toFloat(),
                    S.current.modeQuickStart to modeQuick.toFloat(),
                    S.current.modeAiJudge to modeAiJudge.toFloat(),
                ).filter { it.second > 0f }

                if (modeSegments.isNotEmpty()) {
                    SectionHeader(
                        text = S.current.statsModeDistribution,
                        modifier = Modifier.staggered(16),
                    )
                    GradientBorderCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggered(16),
                        borderColors = GradientPrimary,
                        backgroundColor = ColorSurface,
                    ) {
                        PieChart(
                            segments = modeSegments,
                            colors = listOf(
                                Color(0xFF6366F1), // Indigo — Classic
                                Color(0xFF22D3EE), // Cyan — Blind Bingo
                                Color(0xFF22C55E), // Green — Weird Core
                                Color(0xFFF59E0B), // Amber — Quick Start
                                Color(0xFFEC4899), // Pink — AI Judge
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                        )
                    }
                }

                // ── Section: Performance Overview ────────────────────
                val speedBonusRate = if (gamesPlayed > 0) totalSpeedBonuses.toFloat() / gamesPlayed * 100f else 0f
                val avgRatingScaled = averageRating * 20f // 0–5 stars → 0–100 scale

                val perfData = buildList {
                    if (gamesPlayed > 0) add(S.current.winRate to winRate.toFloat())
                    if (totalStarsCount > 0) add(S.current.avgRating to avgRatingScaled)
                    if (gamesPlayed > 0) add(S.current.statsSpeedBonuses to speedBonusRate)
                    if (gamesPlayed > 0) add(S.current.statsCapturesPerGame to capturesPerGame * 10f)
                }

                if (perfData.isNotEmpty()) {
                    SectionHeader(
                        text = S.current.statsPerformanceOverview,
                        modifier = Modifier.staggered(17),
                    )
                    GradientBorderCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggered(17),
                        borderColors = GradientCool,
                        backgroundColor = ColorSurface,
                    ) {
                        MiniBarChart(
                            data = perfData,
                            barColor = Color(0xFF6366F1),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    SignInDialogHost(
        visible = showAuthDialog,
        onDismiss = { showAuthDialog = false },
        gameState = gameState,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = ColorOnSurfaceVariant,
        modifier = modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    smallValue: Boolean = false,
) {
    GradientBorderCard(
        modifier = modifier,
        borderColors = if (accentColor != null) listOf(accentColor, accentColor.copy(alpha = 0.5f)) else GradientPrimary,
        backgroundColor = ColorSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = accentColor ?: ColorPrimary,
            )
            Text(
                text = value,
                style = if (smallValue) MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        else MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = ColorOnSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = ColorOnSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 14.sp,
            )
        }
    }
}
