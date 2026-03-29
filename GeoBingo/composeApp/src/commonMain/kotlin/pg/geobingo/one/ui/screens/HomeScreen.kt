package pg.geobingo.one.ui.screens

import kotlinx.datetime.toLocalDateTime
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import pg.geobingo.one.ui.components.EarnStarsDialog
import pg.geobingo.one.ui.components.StarsChip
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.ui.theme.Spacing
import pg.geobingo.one.ui.theme.rememberStaggeredAnimation

@Composable
fun HomeScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(gameState.ui.pendingToast) {
        val msg = gameState.ui.pendingToast ?: return@LaunchedEffect
        gameState.ui.pendingToast = null
        snackbarHostState.showSnackbar(msg)
    }

    // ── Rejoin check ─────────────────────────────────────────────────
    var showRejoinDialog by remember { mutableStateOf(false) }
    var rejoinLoading by remember { mutableStateOf(false) }
    val rejoinCode = remember { ActiveSession.getGameCode() }
    val rejoinName = remember { ActiveSession.getPlayerName() }

    LaunchedEffect(Unit) {
        if (ActiveSession.exists()) {
            showRejoinDialog = true
        }
    }

    if (showRejoinDialog) {
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
                    onClick = {
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
                    onClick = {
                        ActiveSession.clear()
                        showRejoinDialog = false
                    },
                    enabled = !rejoinLoading,
                ) {
                    Text(S.current.rejoinDismiss, color = ColorOnSurfaceVariant)
                }
            },
        )
    }

    val anim = rememberStaggeredAnimation(count = 5)
    val btnOffsets = (0..1).map { remember { Animatable(80f) } }
    val btnAlphas = (0..1).map { remember { Animatable(0f) } }
    LaunchedEffect(Unit) {
        for (i in btnOffsets.indices) {
            launch {
                delay(180L + i * 100L)
                launch { btnOffsets[i].animateTo(0f, tween(500)) }
                btnAlphas[i].animateTo(1f, tween(500))
            }
        }
    }

    fun Modifier.staggered(index: Int): Modifier = this.then(anim.modifier(index))

    var selectedHistoryEntry by remember { mutableStateOf<GameHistoryEntry?>(null) }
    var showEarnStarsDialog by remember { mutableStateOf(false) }
    var pendingFriendRequestCount by remember { mutableStateOf(0) }

    // Poll pending friend requests count
    LaunchedEffect(Unit) {
        if (!AccountManager.isLoggedIn) return@LaunchedEffect
        while (true) {
            try {
                pendingFriendRequestCount = FriendsManager.getPendingRequests().size
            } catch (_: Exception) {}
            delay(15_000L)
        }
    }
    // Reset daily challenge if a new UTC day has started (handles app staying open past midnight)
    LaunchedEffect(Unit) {
        while (true) {
            gameState.stars.resetDailyChallengeIfNewDay()
            delay(60_000L) // check every 60 seconds
        }
    }
    val dailyChallenge = remember { DailyChallengeManager.getTodayChallenge() }

    if (showEarnStarsDialog) {
        EarnStarsDialog(
            starsState = gameState.stars,
            onWatchAd = {
                AdManager.showRewardedAd(
                    onReward = {
                        gameState.stars.add(10)
                        gameState.stars.recordAdWatched()
                        gameState.ui.pendingToast = S.current.starsEarned
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
        bottomBar = {
            // ── CTA BUTTONS (fixed at bottom) ────────────────────────────
            Surface(color = Color.Transparent) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = Spacing.screenHorizontal)
                        .padding(bottom = 24.dp, top = 8.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GradientButton(
                        text = S.current.createRound,
                        onClick = { nav.navigateTo(Screen.SELECT_MODE) },
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = S.current.createRound }.graphicsLayer {
                            translationY = btnOffsets[0].value
                            alpha = btnAlphas[0].value
                        },
                        gradientColors = GradientPrimary,
                        height = 62.dp,
                        fontSize = 17.sp,
                        leadingIcon = {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(22.dp), tint = Color.White)
                        },
                    )

                    OutlinedButton(
                        onClick = { nav.navigateTo(Screen.JOIN_GAME) },
                        modifier = Modifier.fillMaxWidth().height(62.dp).graphicsLayer {
                            translationY = btnOffsets[1].value
                            alpha = btnAlphas[1].value
                        },
                        shape = RoundedCornerShape(31.dp),
                        border = BorderStroke(1.5.dp, ColorPrimary.copy(alpha = 0.55f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorOnSurface),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Login,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = ColorPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            S.current.joinRound,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            color = ColorOnSurface,
                        )
                    }

                    // ── FOOTER ────────────────────────────────────────────────
                    val uriHandler = LocalUriHandler.current
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = { nav.navigateTo(Screen.FRIENDS) }) {
                                Box {
                                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(14.dp), tint = ColorOnSurfaceVariant)
                                    if (pendingFriendRequestCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .align(Alignment.TopEnd)
                                                .offset(x = 4.dp, y = (-4).dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(Color(0xFFEF4444)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                "$pendingFriendRequestCount",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.width(4.dp))
                                Text(S.current.friends, style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant)
                            }
                            TextButton(onClick = { nav.navigateTo(Screen.SETTINGS) }) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp), tint = ColorOnSurfaceVariant)
                                Spacer(Modifier.width(4.dp))
                                Text(S.current.settings, style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant)
                            }
                            TextButton(onClick = { nav.navigateTo(Screen.STATS) }) {
                                Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(14.dp), tint = ColorOnSurfaceVariant)
                                Spacer(Modifier.width(4.dp))
                                Text(S.current.statsTitle, style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant)
                            }
                            TextButton(onClick = { nav.navigateTo(Screen.MP_LEADERBOARD) }) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(14.dp), tint = ColorOnSurfaceVariant)
                                Spacer(Modifier.width(4.dp))
                                Text(S.current.multiplayerLeaderboard, style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant)
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = { uriHandler.openUri("https://katchit.app/impressum.html") }) {
                                Text(S.current.impressum, style = MaterialTheme.typography.labelSmall, color = ColorOutline)
                            }
                            TextButton(onClick = { uriHandler.openUri("https://katchit.app/datenschutz.html") }) {
                                Text(S.current.privacy, style = MaterialTheme.typography.labelSmall, color = ColorOutline)
                            }
                        }
                    }
                }
            }
        },
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
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                // ── HERO ──────────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp, bottom = 24.dp)
                        .padding(horizontal = 24.dp)
                        .staggered(0),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    AnimatedHeroTitle()
                    Spacer(Modifier.height(10.dp))
                    HeroTagline()
                }

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

                // ── DAILY CHALLENGE ──────────────────────────────────────────
                run {
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
                                        if (mode == "QUICK_START") {
                                            gameState.session.gameMode = pg.geobingo.one.game.GameMode.QUICK_START
                                            gameState.session.quickStartOutdoor = true
                                            gameState.session.quickStartDurationMinutes = 15
                                            gameState.gameplay.gameDurationMinutes = 15
                                            nav.navigateTo(Screen.CREATE_GAME)
                                        } else {
                                            val gameMode = when (mode) {
                                                "CLASSIC" -> pg.geobingo.one.game.GameMode.CLASSIC
                                                "BLIND_BINGO" -> pg.geobingo.one.game.GameMode.BLIND_BINGO
                                                "WEIRD_CORE" -> pg.geobingo.one.game.GameMode.WEIRD_CORE
                                                else -> pg.geobingo.one.game.GameMode.CLASSIC
                                            }
                                            gameState.session.gameMode = gameMode
                                            nav.navigateTo(Screen.CREATE_GAME)
                                        }
                                    }
                                    else -> nav.navigateTo(Screen.SELECT_MODE)
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

                // ── WEEKLY CHALLENGE ─────────────────────────────────────────
                run {
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
                            .then(if (!isWeeklyDone) Modifier.clickable { nav.navigateTo(Screen.SELECT_MODE) } else Modifier),
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

                Spacer(Modifier.height(8.dp))

                // ── SOLO LEADERBOARD (Top 5) ─────────────────────────────────
                SoloLeaderboardPreview(
                    modifier = Modifier
                        .padding(horizontal = Spacing.screenHorizontal)
                        .fillMaxWidth(),
                    onViewAll = { nav.navigateTo(Screen.SOLO_LEADERBOARD) },
                )

                Spacer(Modifier.height(4.dp))

                // ── HOW TO PLAY ──────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .padding(horizontal = Spacing.screenHorizontal)
                        .fillMaxWidth()
                        .clickable { nav.navigateTo(Screen.HOW_TO_PLAY) }
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        S.current.howToPlay,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = ColorOnSurfaceVariant.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = ColorOnSurfaceVariant.copy(alpha = 0.5f),
                    )
                }

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
    val playerName = remember { AppSettings.getString("last_player_name", "") }

    LaunchedEffect(Unit) {
        try {
            val rawOut = GameRepository.getSoloLeaderboard(30, isOutdoor = true)
            outdoorScores = rawOut.distinctBy { it.player_name }.take(5)
            val rawIn = GameRepository.getSoloLeaderboard(30, isOutdoor = false)
            indoorScores = rawIn.distinctBy { it.player_name }.take(5)
        } catch (_: Exception) { }
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
