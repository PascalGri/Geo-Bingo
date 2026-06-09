package pg.geobingo.one.ui.screens

import kotlinx.datetime.toLocalDateTime
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.*
import pg.geobingo.one.game.state.ChallengeType
import pg.geobingo.one.game.state.DailyChallenge
import pg.geobingo.one.game.state.DailyChallengeManager
import pg.geobingo.one.game.state.WeeklyChallengeManager
import pg.geobingo.one.game.state.WeeklyChallengeType
import pg.geobingo.one.i18n.S
import pg.geobingo.one.network.AccountManager
import pg.geobingo.one.network.FriendsManager
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.SoloScoreDto
import pg.geobingo.one.platform.AdManager
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.ui.components.CollectScrollToTop
import pg.geobingo.one.ui.components.EarnStarsDialog
import pg.geobingo.one.ui.components.ScrollToTopTags
import pg.geobingo.one.ui.components.StarsChip
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.ui.theme.Spacing
import pg.geobingo.one.ui.theme.rememberStaggeredAnimation

@Composable
fun HomeScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val homeScrollState = rememberScrollState()
    CollectScrollToTop(ScrollToTopTags.HOME, homeScrollState)
    LaunchedEffect(gameState.ui.pendingToast) {
        val msg = gameState.ui.pendingToast ?: return@LaunchedEffect
        gameState.ui.pendingToast = null
        snackbarHostState.showSnackbar(msg)
    }

    // ── Rejoin check ─────────────────────────────────────────────────
    // Multiplayer rejoin (existing) + solo rejoin (NEW): either a persisted
    // lobby/game exists server-side, or a solo snapshot is still in its
    // time window. Solo takes precedence only when no multiplayer session.
    var showRejoinDialog by remember { mutableStateOf(false) }
    var rejoinLoading by remember { mutableStateOf(false) }
    val rejoinCode = remember { ActiveSession.getGameCode() }
    val rejoinName = remember { ActiveSession.getPlayerName() }
    val soloMeta = remember { ActiveSession.getSoloSessionMeta() }
    val soloSessionAvailable = soloMeta != null

    var showSoloRejoinDialog by remember { mutableStateOf(false) }
    var soloRejoinLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        when {
            ActiveSession.exists() -> showRejoinDialog = true
            soloSessionAvailable -> showSoloRejoinDialog = true
        }
    }

    if (showRejoinDialog) {
        RejoinGameDialog(
            rejoinCode = rejoinCode,
            rejoinName = rejoinName,
            rejoinLoading = rejoinLoading,
            onRejoin = {
                if (!rejoinLoading) {
                    rejoinLoading = true
                    scope.launch {
                        val target = ActiveSession.rejoin(gameState)
                        if (target != null) {
                            nav.resetTo(target)
                        } else {
                            showRejoinDialog = false
                            rejoinLoading = false
                            gameState.ui.pendingToast = S.current.error
                        }
                    }
                }
            },
            onDismiss = {
                ActiveSession.clear()
                showRejoinDialog = false
            },
        )
    }

    if (showSoloRejoinDialog && soloMeta != null) {
        RejoinSoloDialog(
            meta = soloMeta,
            loading = soloRejoinLoading,
            onRejoin = {
                if (!soloRejoinLoading) {
                    soloRejoinLoading = true
                    val target = ActiveSession.rejoinSolo(gameState)
                    if (target != null) {
                        nav.resetTo(target)
                    } else {
                        showSoloRejoinDialog = false
                        soloRejoinLoading = false
                        gameState.ui.pendingToast = S.current.error
                    }
                }
            },
            onDismiss = {
                ActiveSession.clearSolo()
                showSoloRejoinDialog = false
            },
        )
    }

    val anim = rememberStaggeredAnimation(count = 5)

    fun Modifier.staggered(index: Int): Modifier = this.then(anim.modifier(index))

    var selectedHistoryEntry by remember { mutableStateOf<GameHistoryEntry?>(null) }
    var showEarnStarsDialog by remember { mutableStateOf(false) }

    // Reset daily challenge if a new UTC day has started (handles app staying open past midnight)
    LaunchedEffect(Unit) {
        while (true) {
            gameState.stars.resetDailyChallengeIfNewDay()
            delay(60_000L) // check every 60 seconds
        }
    }
    val dailyChallenge = remember { DailyChallengeManager.getTodayChallenge() }

    // Launch today's Daily Run from the home banner. The mode is AI-rated, so
    // honor the rating-consent hard-gate (Apple 5.1.1(i)): if consent isn't
    // granted yet, route through Mode-Select where the consent dialog fires.
    val launchDailyRun: () -> Unit = {
        if (!pg.geobingo.one.platform.AiConsent.ratingAccepted) {
            nav.navigateTo(Screen.SELECT_MODE)
        } else {
            pg.geobingo.one.util.Analytics.track(
                pg.geobingo.one.util.Analytics.MODE_SELECTED,
                mapOf("mode" to "SOLO_DAILY_RUN"),
            )
            gameState.solo.reset()
            gameState.solo.mode = pg.geobingo.one.game.state.SoloMode.DAILY_RUN
            gameState.solo.isOutdoor = true
            gameState.solo.categoryCount = 5
            gameState.solo.categories = pg.geobingo.one.data.dailyRunCategories(5)
            gameState.solo.totalDurationSeconds = 300
            gameState.solo.timeRemainingSeconds = 300
            gameState.solo.playerName = AppSettings.getString("last_player_name", "Player")
            nav.navigateTo(Screen.SOLO_START_TRANSITION)
        }
    }

    if (showEarnStarsDialog) {
        EarnStarsDialog(
            starsState = gameState.stars,
            onWatchAd = {
                AdManager.showRewardedAd(
                    onReward = {
                        gameState.stars.add(10)
                        gameState.stars.recordAdWatched()
                        gameState.ui.pendingReward = pg.geobingo.one.game.state.RewardEvent(
                            label = S.current.rewardVideoWatched,
                            stars = 10,
                        )
                    },
                    onDismiss = { showEarnStarsDialog = false },
                )
            },
            onDismiss = { showEarnStarsDialog = false },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ColorBackground,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // ── FULL-SCREEN BACKGROUND GRADIENT ──────────────────────────
            // Dark base
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1C0030),
                                Color(0xFF0D0818),
                                ColorBackground,
                            )
                        )
                    )
            )
            // Diagonal accent: rose bottom-left to purple top-right
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFF43F5E).copy(alpha = 0.13f),
                                Color.Transparent,
                                Color(0xFFA855F7).copy(alpha = 0.09f),
                            ),
                            start = Offset(0f, 600f),
                            end = Offset(500f, 0f),
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(homeScrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                // ── HERO ──────────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp, bottom = 12.dp)
                        .padding(horizontal = 24.dp)
                        .staggered(0),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    AnimatedHeroTitle()
                    Spacer(Modifier.height(10.dp))
                    HeroTagline()
                    Spacer(Modifier.height(14.dp))
                    // ── HOW TO PLAY (pinned under hero heading) ──────────
                    HowToPlayPill(onClick = { nav.navigateTo(Screen.HOW_TO_PLAY) })
                }

                // ── DAILY RUN BANNER (links to today's ranked daily mode) ─────
                DailyRunBanner(
                    onPlay = launchDailyRun,
                    modifier = Modifier
                        .padding(horizontal = Spacing.screenHorizontal)
                        .fillMaxWidth()
                        .staggered(1),
                )

                Spacer(Modifier.height(8.dp))

                // ── DAILY BONUS BANNER ────────────────────────────────────────
                DailyBonusBanner(
                    visible = gameState.ui.showDailyBonusBanner,
                    onDismiss = { gameState.ui.showDailyBonusBanner = false },
                )

                // ── STARS + SHOP (minimal) ────────────────────────────────────
                Row(
                    modifier = Modifier
                        .padding(horizontal = Spacing.screenHorizontal)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    pg.geobingo.one.ui.components.TopBarStarsAndProfile(
                        gameState = gameState,
                        onNavigate = { nav.navigateTo(it) },
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ── CHALLENGES (compact, side-by-side) ────────────────────────
                Row(
                    modifier = Modifier
                        .padding(horizontal = Spacing.screenHorizontal)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DailyChallengeCardCompact(
                        gameState = gameState,
                        dailyChallenge = dailyChallenge,
                        onNavigate = { nav.navigateTo(it) },
                        modifier = Modifier.weight(1f),
                    )
                    WeeklyChallengeCardCompact(
                        gameState = gameState,
                        onNavigate = { nav.navigateTo(it) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ── SOLO LEADERBOARD (Top 5) ─────────────────────────────────
                SoloLeaderboardPreview(
                    modifier = Modifier
                        .padding(horizontal = Spacing.screenHorizontal)
                        .fillMaxWidth(),
                    onViewAll = { nav.navigateTo(Screen.SOLO_LEADERBOARD) },
                )

                Spacer(Modifier.height(12.dp))

                // ── DISCLAIMER ───────────────────────────────────────────────
                Text(
                    S.current.photoConsentDisclaimer,
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .padding(horizontal = Spacing.screenHorizontal + 8.dp)
                        .fillMaxWidth(),
                )

                Spacer(Modifier.height(16.dp))

                // ── HISTORY ───────────────────────────────────────────────────
                if (gameState.ui.gameHistory.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .staggered(2)
                            .padding(horizontal = Spacing.screenHorizontal)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = ColorOnSurfaceVariant,
                                )
                                Text(
                                    S.current.recentGames,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorOnSurface,
                                )
                            }
                            if (gameState.ui.gameHistory.size > 3) {
                                TextButton(onClick = { nav.navigateTo(Screen.HISTORY) }) {
                                    Text(
                                        S.current.showAll,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ColorPrimary,
                                    )
                                }
                            }
                        }
                        gameState.ui.gameHistory.take(3).forEach { entry ->
                            HomeHistoryCard(entry = entry, onClick = { selectedHistoryEntry = entry })
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        selectedHistoryEntry?.let { entry ->
            RoundWinnerDialog(entry = entry, onDismiss = { selectedHistoryEntry = null })
        }
    }
}

// ── REJOIN GAME DIALOG ───────────────────────────────────────────────────────

@Composable
private fun RejoinGameDialog(
    rejoinCode: String,
    rejoinName: String,
    rejoinLoading: Boolean,
    onRejoin: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { },
        containerColor = ColorSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Refresh, null, tint = ColorPrimary, modifier = Modifier.size(22.dp))
                Text(S.current.rejoinTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ColorOnSurface)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(S.current.rejoinBody, style = MaterialTheme.typography.bodyMedium, color = ColorOnSurfaceVariant)
                if (rejoinCode.isNotBlank()) {
                    Text("${S.current.roundCode}: $rejoinCode", style = MaterialTheme.typography.labelMedium, color = ColorOnSurface, fontWeight = FontWeight.SemiBold)
                }
                if (rejoinName.isNotBlank()) {
                    Text(rejoinName, style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onRejoin,
                enabled = !rejoinLoading,
            ) {
                if (rejoinLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ColorPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(S.current.rejoining, color = ColorPrimary)
                } else {
                    Text(S.current.rejoinButton, color = ColorPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !rejoinLoading,
            ) {
                Text(S.current.rejoinDismiss, color = ColorOnSurfaceVariant)
            }
        },
    )
}

@Composable
private fun RejoinSoloDialog(
    meta: ActiveSession.SoloSessionMeta,
    loading: Boolean,
    onRejoin: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { },
        containerColor = ColorSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(22.dp))
                Text(S.current.rejoinSoloTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ColorOnSurface)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(S.current.rejoinSoloBody, style = MaterialTheme.typography.bodyMedium, color = ColorOnSurfaceVariant)
                Text(
                    S.current.rejoinSoloProgress(meta.capturedCount, meta.totalCategories),
                    style = MaterialTheme.typography.labelMedium,
                    color = ColorOnSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    S.current.rejoinSoloRemaining(meta.remainingSeconds / 60, meta.remainingSeconds % 60),
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onRejoin, enabled = !loading) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF8B5CF6))
                    Spacer(Modifier.width(8.dp))
                    Text(S.current.rejoining, color = Color(0xFF8B5CF6))
                } else {
                    Text(S.current.rejoinButton, color = Color(0xFF8B5CF6), fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) {
                Text(S.current.rejoinDismiss, color = ColorOnSurfaceVariant)
            }
        },
    )
}

// ── HOME BOTTOM BAR ──────────────────────────────────────────────────────────

// ── DAILY CHALLENGE CARD ─────────────────────────────────────────────────────

/**
 * Pinned "How to Play" pill shown directly under the hero heading. Compact,
 * gradient-bordered, tappable.
 */
@Composable
private fun HowToPlayPill(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ColorSurface.copy(alpha = 0.7f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(GradientPrimary),
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.HelpOutline,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = GradientPrimary.first(),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            S.current.howToPlay,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = ColorOnSurface,
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = ColorOnSurfaceVariant,
        )
    }
}

// ── DAILY RUN BANNER ─────────────────────────────────────────────────────────

/**
 * Prominent home banner that launches today's Daily Run — the ranked, global,
 * resets-every-day solo mode. Static gradient (no animation) to stay cheap on
 * web/wasm and older devices.
 */
@Composable
private fun DailyRunBanner(onPlay: () -> Unit, modifier: Modifier = Modifier) {
    val gradient = listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(gradient))
            .clickable(onClick = onPlay)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Today, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    S.current.modeDailyRun,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(Icons.Default.EmojiEvents, null, tint = Color.White, modifier = Modifier.size(10.dp))
                    Text(
                        S.current.soloRankedBadge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        fontSize = 9.sp,
                    )
                }
            }
            Text(
                S.current.modeDailyRunSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.92f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

/**
 * Compact daily-challenge card designed to sit side-by-side with the weekly
 * challenge. Uses vertical layout to fit half the screen width.
 */
@Composable
private fun DailyChallengeCardCompact(
    gameState: GameState,
    dailyChallenge: DailyChallenge,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDailyDone = gameState.stars.dailyChallengeCompleted
    val challengeText = when (dailyChallenge.type) {
        ChallengeType.WIN_ROUND -> S.current.challengeWinRound
        ChallengeType.PLAY_MODE -> {
            val modeName = when (dailyChallenge.targetMode) {
                "CLASSIC" -> S.current.modeClassic
                "BLIND_BINGO" -> S.current.modeBlindBingo
                "WEIRD_CORE" -> S.current.modeWeirdCore
                "QUICK_START" -> S.current.modeQuickStart
                else -> dailyChallenge.targetMode ?: ""
            }
            "${S.current.challengePlayMode} $modeName"
        }
        ChallengeType.CAPTURE_CATEGORIES -> S.current.challengeCaptureCategories
    }
    GradientBorderCard(
        modifier = modifier
            .fillMaxWidth()
            .then(if (!isDailyDone) Modifier.clickable {
                when (dailyChallenge.type) {
                    ChallengeType.PLAY_MODE -> {
                        val mode = dailyChallenge.targetMode
                        // AI Judge requires user to have granted rating consent at
                        // the hard-gate (Apple 5.1.1(i)) — without it we'd skip
                        // the per-mode consent on Mode-Select. Force the user
                        // through Mode-Select so the consent / settings prompt
                        // can fire. (Same for any other future AI mode.)
                        if (mode == "AI_JUDGE" && !pg.geobingo.one.platform.AiConsent.ratingAccepted) {
                            onNavigate(Screen.SELECT_MODE)
                            return@clickable
                        }
                        if (mode == "QUICK_START") {
                            gameState.session.gameMode = pg.geobingo.one.game.GameMode.QUICK_START
                            gameState.session.quickStartOutdoor = true
                            gameState.session.quickStartDurationMinutes = 15
                            gameState.gameplay.gameDurationMinutes = 15
                            onNavigate(Screen.CREATE_GAME)
                        } else {
                            val gameMode = when (mode) {
                                "CLASSIC" -> pg.geobingo.one.game.GameMode.CLASSIC
                                "BLIND_BINGO" -> pg.geobingo.one.game.GameMode.BLIND_BINGO
                                "WEIRD_CORE" -> pg.geobingo.one.game.GameMode.WEIRD_CORE
                                "AI_JUDGE" -> pg.geobingo.one.game.GameMode.AI_JUDGE
                                else -> pg.geobingo.one.game.GameMode.CLASSIC
                            }
                            gameState.session.gameMode = gameMode
                            onNavigate(Screen.CREATE_GAME)
                        }
                    }
                    else -> onNavigate(Screen.SELECT_MODE)
                }
            } else Modifier),
        cornerRadius = 12.dp,
        borderColors = if (isDailyDone) listOf(Color(0xFF22C55E), Color(0xFF16A34A)) else GradientGold,
        backgroundColor = ColorSurface,
        borderWidth = 1.dp,
    ) {
        Column(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    if (isDailyDone) Icons.Default.Check else Icons.Default.Today,
                    null,
                    tint = if (isDailyDone) Color(0xFF22C55E) else Color(0xFFFBBF24),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    S.current.dailyChallenge,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDailyDone) Color(0xFF22C55E) else Color(0xFFFBBF24),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                )
                Spacer(Modifier.weight(1f))
                if (!isDailyDone) {
                    Text(
                        "+${dailyChallenge.reward}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFBBF24),
                    )
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(11.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (isDailyDone) S.current.dailyChallengeCompleted else challengeText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDailyDone) Color(0xFF22C55E) else ColorOnSurface,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WeeklyChallengeCardCompact(
    gameState: GameState,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val weeklyChallenge = remember { WeeklyChallengeManager.getThisWeekChallenge() }
    val isWeeklyDone = gameState.stars.weeklyChallengeCompleted
    val weeklyProgress = gameState.stars.weeklyChallengeProgress.coerceAtMost(weeklyChallenge.target)
    val progressFraction = weeklyProgress.toFloat() / weeklyChallenge.target.toFloat()
    val weeklyText = when (weeklyChallenge.type) {
        WeeklyChallengeType.WIN_ROUNDS -> S.current.challengeWinRounds
        WeeklyChallengeType.PLAY_ROUNDS -> S.current.challengePlayRounds
        WeeklyChallengeType.CAPTURE_TOTAL -> S.current.challengeCaptureTotal
        WeeklyChallengeType.PLAY_ALL_MODES -> S.current.challengePlayAllModes
        WeeklyChallengeType.WIN_STREAK -> S.current.challengeWinStreak
    }
    val weeklyGradient = listOf(Color(0xFF8B5CF6), Color(0xFFA855F7))
    GradientBorderCard(
        modifier = modifier
            .fillMaxWidth()
            .then(if (!isWeeklyDone) Modifier.clickable { onNavigate(Screen.SELECT_MODE) } else Modifier),
        cornerRadius = 12.dp,
        borderColors = if (isWeeklyDone) listOf(Color(0xFF22C55E), Color(0xFF16A34A)) else weeklyGradient,
        backgroundColor = ColorSurface,
        borderWidth = 1.dp,
    ) {
        Column(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    if (isWeeklyDone) Icons.Default.Check else Icons.Default.DateRange,
                    null,
                    tint = if (isWeeklyDone) Color(0xFF22C55E) else weeklyGradient.first(),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    S.current.weeklyChallenge,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isWeeklyDone) Color(0xFF22C55E) else weeklyGradient.first(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                )
                Spacer(Modifier.weight(1f))
                if (!isWeeklyDone) {
                    Text(
                        "+${weeklyChallenge.reward}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = weeklyGradient.first(),
                    )
                    Icon(Icons.Default.Star, null, tint = weeklyGradient.first(), modifier = Modifier.size(11.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (isWeeklyDone) S.current.weeklyChallengeCompleted else "$weeklyText ($weeklyProgress/${weeklyChallenge.target})",
                style = MaterialTheme.typography.bodySmall,
                color = if (isWeeklyDone) Color(0xFF22C55E) else ColorOnSurface,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            if (!isWeeklyDone) {
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ColorSurfaceVariant),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Brush.horizontalGradient(weeklyGradient)),
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyChallengeCard(
    gameState: GameState,
    dailyChallenge: DailyChallenge,
    onNavigate: (Screen) -> Unit,
) {
    val isDailyDone = gameState.stars.dailyChallengeCompleted
    val challengeText = when (dailyChallenge.type) {
        ChallengeType.WIN_ROUND -> S.current.challengeWinRound
        ChallengeType.PLAY_MODE -> {
            val modeName = when (dailyChallenge.targetMode) {
                "CLASSIC" -> S.current.modeClassic
                "BLIND_BINGO" -> S.current.modeBlindBingo
                "WEIRD_CORE" -> S.current.modeWeirdCore
                "QUICK_START" -> S.current.modeQuickStart
                else -> dailyChallenge.targetMode ?: ""
            }
            "${S.current.challengePlayMode} $modeName"
        }
        ChallengeType.CAPTURE_CATEGORIES -> S.current.challengeCaptureCategories
    }
    GradientBorderCard(
        modifier = Modifier
            .padding(horizontal = Spacing.screenHorizontal)
            .fillMaxWidth()
            .then(if (!isDailyDone) Modifier.clickable {
                when (dailyChallenge.type) {
                    ChallengeType.PLAY_MODE -> {
                        val mode = dailyChallenge.targetMode
                        if (mode == "AI_JUDGE" && !pg.geobingo.one.platform.AiConsent.ratingAccepted) {
                            onNavigate(Screen.SELECT_MODE)
                            return@clickable
                        }
                        if (mode == "QUICK_START") {
                            gameState.session.gameMode = pg.geobingo.one.game.GameMode.QUICK_START
                            gameState.session.quickStartOutdoor = true
                            gameState.session.quickStartDurationMinutes = 15
                            gameState.gameplay.gameDurationMinutes = 15
                            onNavigate(Screen.CREATE_GAME)
                        } else {
                            val gameMode = when (mode) {
                                "CLASSIC" -> pg.geobingo.one.game.GameMode.CLASSIC
                                "BLIND_BINGO" -> pg.geobingo.one.game.GameMode.BLIND_BINGO
                                "WEIRD_CORE" -> pg.geobingo.one.game.GameMode.WEIRD_CORE
                                "AI_JUDGE" -> pg.geobingo.one.game.GameMode.AI_JUDGE
                                else -> pg.geobingo.one.game.GameMode.CLASSIC
                            }
                            gameState.session.gameMode = gameMode
                            onNavigate(Screen.CREATE_GAME)
                        }
                    }
                    else -> onNavigate(Screen.SELECT_MODE)
                }
            } else Modifier),
        cornerRadius = 14.dp,
        borderColors = if (isDailyDone) listOf(Color(0xFF22C55E), Color(0xFF16A34A)) else GradientGold,
        backgroundColor = ColorSurface,
        borderWidth = 1.5.dp,
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(
                        if (isDailyDone) listOf(Color(0xFF22C55E), Color(0xFF16A34A)) else GradientGold
                    )),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isDailyDone) Icons.Default.Check else Icons.Default.Today,
                    null, tint = Color.White, modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    S.current.dailyChallenge,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDailyDone) Color(0xFF22C55E) else Color(0xFFFBBF24),
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (isDailyDone) S.current.dailyChallengeCompleted else challengeText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDailyDone) Color(0xFF22C55E) else ColorOnSurface,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (isDailyDone) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF22C55E), modifier = Modifier.size(24.dp))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("+${dailyChallenge.reward}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                }
                Icon(Icons.Default.ChevronRight, null, tint = ColorOnSurfaceVariant, modifier = Modifier.size(16.dp))
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}

// ── WEEKLY CHALLENGE CARD ────────────────────────────────────────────────────

@Composable
private fun WeeklyChallengeCard(
    gameState: GameState,
    onNavigate: (Screen) -> Unit,
) {
    val weeklyChallenge = remember { WeeklyChallengeManager.getThisWeekChallenge() }
    val isWeeklyDone = gameState.stars.weeklyChallengeCompleted
    val weeklyProgress = gameState.stars.weeklyChallengeProgress.coerceAtMost(weeklyChallenge.target)
    val progressFraction = weeklyProgress.toFloat() / weeklyChallenge.target.toFloat()
    val weeklyText = when (weeklyChallenge.type) {
        WeeklyChallengeType.WIN_ROUNDS -> "${S.current.challengeWinRounds} (${S.current.challengeProgress(weeklyProgress, weeklyChallenge.target)})"
        WeeklyChallengeType.PLAY_ROUNDS -> "${S.current.challengePlayRounds} (${S.current.challengeProgress(weeklyProgress, weeklyChallenge.target)})"
        WeeklyChallengeType.CAPTURE_TOTAL -> "${S.current.challengeCaptureTotal} (${S.current.challengeProgress(weeklyProgress, weeklyChallenge.target)})"
        WeeklyChallengeType.PLAY_ALL_MODES -> "${S.current.challengePlayAllModes} (${S.current.challengeProgress(weeklyProgress, weeklyChallenge.target)})"
        WeeklyChallengeType.WIN_STREAK -> "${S.current.challengeWinStreak} (${S.current.challengeProgress(weeklyProgress, weeklyChallenge.target)})"
    }
    val weeklyGradient = listOf(Color(0xFF8B5CF6), Color(0xFFA855F7))

    GradientBorderCard(
        modifier = Modifier
            .padding(horizontal = Spacing.screenHorizontal)
            .fillMaxWidth()
            .then(if (!isWeeklyDone) Modifier.clickable { onNavigate(Screen.SELECT_MODE) } else Modifier),
        cornerRadius = 14.dp,
        borderColors = if (isWeeklyDone) listOf(Color(0xFF22C55E), Color(0xFF16A34A)) else weeklyGradient,
        backgroundColor = ColorSurface,
        borderWidth = 1.5.dp,
    ) {
        Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(
                            if (isWeeklyDone) listOf(Color(0xFF22C55E), Color(0xFF16A34A)) else weeklyGradient
                        )),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isWeeklyDone) Icons.Default.Check else Icons.Default.DateRange,
                        null, tint = Color.White, modifier = Modifier.size(20.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        S.current.weeklyChallenge,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isWeeklyDone) Color(0xFF22C55E) else weeklyGradient.first(),
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (isWeeklyDone) S.current.weeklyChallengeCompleted else weeklyText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isWeeklyDone) Color(0xFF22C55E) else ColorOnSurface,
                        fontWeight = FontWeight.Medium,
                    )
                }
                if (isWeeklyDone) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF22C55E), modifier = Modifier.size(24.dp))
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("+${weeklyChallenge.reward}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = weeklyGradient.first())
                        Icon(Icons.Default.Star, null, tint = weeklyGradient.first(), modifier = Modifier.size(14.dp))
                    }
                }
            }
            // Progress bar
            if (!isWeeklyDone) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(ColorSurfaceVariant),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Brush.horizontalGradient(weeklyGradient)),
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}

// ── DAILY BONUS BANNER ───────────────────────────────────────────────────────

@Composable
private fun DailyBonusBanner(visible: Boolean, onDismiss: () -> Unit) {
    // Auto-dismiss after 4 seconds
    LaunchedEffect(visible) {
        if (visible) {
            delay(4000L)
            onDismiss()
        }
    }

    val scale = remember { Animatable(0.8f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            launch { scale.animateTo(1f, tween(500, easing = androidx.compose.animation.core.EaseOutBack)) }
            alpha.animateTo(1f, tween(400))
        } else if (alpha.value > 0f) {
            launch { scale.animateTo(0.9f, tween(300)) }
            alpha.animateTo(0f, tween(300))
        }
    }

    if (visible || alpha.value > 0f) {
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                }
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFFFBBF24), Color(0xFFF59E0B), Color(0xFFEF4444))
                    )
                )
                .clickable { onDismiss() }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = Color.White,
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        S.current.dailyLoginBonus,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        "+5 ${S.current.stars}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

// ── HERO TITLE ────────────────────────────────────────────────────────────────

@Composable
private fun AnimatedHeroTitle() {
    AnimatedGradientText(
        text = "KatchIt!",
        style = AppTextStyles.heroTitle.copy(
            shadow = Shadow(
                color = Color(0xFFD946EF).copy(alpha = 0.6f),
                blurRadius = 50f,
            ),
        ),
        gradientColors = GradientPrimary,
        durationMillis = 3000,
    )
}

// ── HERO TAGLINE (Fotografiere · Bewerte · Gewinne) ───────────────────────────

@Composable
private fun HeroTagline() {
    val words = listOf(S.current.heroTagCapture, S.current.heroTagRate, S.current.heroTagWin)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        words.forEachIndexed { index, word ->
            Text(
                text = word,
                style = MaterialTheme.typography.bodyMedium.copy(
                    letterSpacing = 0.2.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = Color.White.copy(alpha = 0.90f),
            )
            if (index < words.lastIndex) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.45f)),
                )
            }
        }
    }
}

private fun formatHistoryDate(isoDate: String): String {
    if (isoDate.isBlank()) return ""
    return try {
        val instant = kotlinx.datetime.Instant.parse(isoDate)
        val local = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        val day = local.dayOfMonth.toString().padStart(2, '0')
        val month = local.monthNumber.toString().padStart(2, '0')
        val hour = local.hour.toString().padStart(2, '0')
        val minute = local.minute.toString().padStart(2, '0')
        "$day.$month.${local.year}  $hour:$minute"
    } catch (_: Exception) {
        ""
    }
}

@Composable
private fun HomeHistoryCard(entry: GameHistoryEntry, onClick: () -> Unit) {
    val winner = entry.players.firstOrNull()
    val dateText = formatHistoryDate(entry.date)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        border = BorderStroke(1.dp, ColorOutlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (dateText.isNotEmpty()) {
                    Text(
                        dateText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = ColorOnSurface,
                    )
                }
                Text(
                    entry.gameCode,
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant,
                    fontSize = 10.sp,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (winner != null) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFFFBBF24),
                    )
                    Text(
                        winner.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorOnSurface,
                    )
                    Text(
                        "${winner.score} ${S.current.pointsAbbrev}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorPrimary,
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = ColorOnSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RoundWinnerDialog(entry: GameHistoryEntry, onDismiss: () -> Unit) {
    val winner = entry.players.firstOrNull()
    val dateText = formatHistoryDate(entry.date)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorSurface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (dateText.isNotEmpty()) {
                    Text(
                        dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnSurfaceVariant,
                    )
                }
                Text(
                    "${S.current.roundCode} ${entry.gameCode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant,
                    fontSize = 10.sp,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (winner != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFBBF24).copy(alpha = 0.1f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = Color(0xFFFBBF24),
                        )
                        Column {
                            Text(
                                winner.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorOnSurface,
                            )
                            Text(
                                "${winner.score} ${S.current.points}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorPrimary,
                            )
                        }
                    }
                }

                HorizontalDivider(color = ColorOutlineVariant)

                entry.players.forEachIndexed { i, hp ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val rankColor = when (i) {
                            0 -> Color(0xFFFBBF24)
                            1 -> Color(0xFF94A3B8)
                            2 -> Color(0xFFCD7F32)
                            else -> ColorOnSurfaceVariant
                        }
                        Text(
                            "${i + 1}.",
                            fontSize = 13.sp,
                            fontWeight = if (i < 3) FontWeight.Bold else FontWeight.Normal,
                            color = rankColor,
                        )
                        Text(
                            hp.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (hp.name == entry.playerName) FontWeight.Bold else FontWeight.Normal,
                            color = if (hp.name == entry.playerName) ColorPrimary else ColorOnSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${hp.score} ${S.current.pointsAbbrev}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorOnSurfaceVariant,
                        )
                    }
                }

                Text(
                    "${entry.totalCategories} ${S.current.categories}  |  ${entry.players.size} ${S.current.players}" +
                            if (entry.jokerMode) "  |  Joker" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(S.current.close, color = ColorPrimary)
            }
        },
    )
}

// ── Solo Leaderboard Preview ─────────────────────────────────────────────

private val SoloGradient = listOf(Color(0xFF22D3EE), Color(0xFF6366F1))

@Composable
private fun SoloLeaderboardPreview(
    modifier: Modifier = Modifier,
    onViewAll: () -> Unit,
) {
    var outdoorScores by remember { mutableStateOf<List<SoloScoreDto>>(emptyList()) }
    var indoorScores by remember { mutableStateOf<List<SoloScoreDto>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(0) } // 0=outdoor, 1=indoor
    var loading by remember { mutableStateOf(true) }
    val profileVersion = pg.geobingo.one.network.AccountManager.profileVersion
    val playerName = remember(profileVersion) { AppSettings.getString("last_player_name", "") }

    // On a cold start the Supabase client / network often isn't ready yet when
    // this first fires, so the fetch throws and (previously) the exception was
    // swallowed and the board silently stayed empty — "nur manchmal geladen".
    // Retry a few times with backoff, and re-run whenever the account profile
    // becomes ready (profileVersion changes) so a freshly-attached session also
    // triggers a reload.
    LaunchedEffect(profileVersion) {
        loading = true
        var attempt = 0
        while (true) {
            try {
                val rawOut = GameRepository.getSoloLeaderboard(30, isOutdoor = true)
                val rawIn = GameRepository.getSoloLeaderboard(30, isOutdoor = false)
                outdoorScores = rawOut.distinctBy { it.player_name }.take(5)
                indoorScores = rawIn.distinctBy { it.player_name }.take(5)
                break // fetched (even an empty board is a valid, cached result)
            } catch (_: Exception) {
                attempt++
                if (attempt >= 4) break
                delay(500L * attempt) // 0.5s → 1s → 1.5s
            }
        }
        loading = false
    }

    val topScores = if (selectedTab == 0) outdoorScores else indoorScores
    if (loading || (outdoorScores.isEmpty() && indoorScores.isEmpty())) return

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        border = BorderStroke(1.dp, ColorOutlineVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onViewAll() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                    AnimatedGradientText(
                        text = "Solo Bestenliste",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        gradientColors = SoloGradient,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        S.current.showAll,
                        style = MaterialTheme.typography.labelSmall,
                        color = SoloGradient.first(),
                        fontWeight = FontWeight.Medium,
                    )
                    Icon(Icons.Default.ChevronRight, null, tint = SoloGradient.first(), modifier = Modifier.size(14.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            // Outdoor/Indoor toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(0 to S.current.outdoor, 1 to S.current.indoor).forEach { (idx, label) ->
                    val selected = selectedTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) SoloGradient.first().copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { selectedTab = idx }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) SoloGradient.first() else ColorOnSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Top 5 rows
            if (topScores.isEmpty()) {
                Text(
                    S.current.soloNoScoresYet,
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            topScores.forEachIndexed { index, score ->
                val isMe = score.player_name == playerName
                val rankColor = when (index) {
                    0 -> Color(0xFFFBBF24)
                    1 -> Color(0xFF94A3B8)
                    2 -> Color(0xFFCD7F32)
                    else -> ColorOnSurfaceVariant
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isMe) Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SoloGradient.first().copy(alpha = 0.08f))
                                .padding(horizontal = 6.dp, vertical = 5.dp)
                            else Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Rank
                    Box(modifier = Modifier.width(22.dp)) {
                        if (index < 3) {
                            Icon(Icons.Default.EmojiEvents, null, tint = rankColor, modifier = Modifier.size(16.dp))
                        } else {
                            Text(
                                "#${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = rankColor,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))

                    // Name
                    Text(
                        score.player_name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isMe) FontWeight.Bold else FontWeight.Medium,
                        color = if (isMe) SoloGradient.first() else ColorOnSurface,
                        modifier = Modifier.weight(1f),
                    )

                    // Score
                    Text(
                        "${score.score} ${S.current.pointsAbbrev}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (index == 0) rankColor else ColorOnSurface,
                    )
                }
            }
        }
    }
}
