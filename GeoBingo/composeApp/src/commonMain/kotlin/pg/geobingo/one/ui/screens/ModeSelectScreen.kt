package pg.geobingo.one.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import pg.geobingo.one.data.CATEGORY_TEMPLATES_SHUFFLED
import pg.geobingo.one.game.GameMode
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.game.state.SoloMode
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.i18n.S
import pg.geobingo.one.util.Analytics
import pg.geobingo.one.ui.theme.*

// ── Mode-card rainbow ───────────────────────────────────────────────────
// The mode list reads as a single top-to-bottom rainbow: each card gets the
// next hue. These override the per-mode brand gradients (GradientAiJudge etc.)
// ONLY on this selection screen — the in-game/transition screens keep their
// own mode colors. Order matches the card order in ModeSelectScreen.
private val RainbowSolo = listOf(Color(0xFFF87171), Color(0xFFEF4444))      // red
private val RainbowEndless = listOf(Color(0xFFFB923C), Color(0xFFF97316))   // orange
private val RainbowDaily = listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))     // amber
private val RainbowWeirdSolo = listOf(Color(0xFF34D399), Color(0xFF10B981)) // green
private val RainbowRoulette = listOf(Color(0xFF22D3EE), Color(0xFF06B6D4))  // cyan
private val RainbowAiJudge = listOf(Color(0xFF38BDF8), Color(0xFF3B82F6))   // blue
private val RainbowQuickStart = listOf(Color(0xFF818CF8), Color(0xFF6366F1)) // indigo
private val RainbowClassic = listOf(Color(0xFFA78BFA), Color(0xFF8B5CF6))   // violet
private val RainbowBlindBingo = listOf(Color(0xFFC084FC), Color(0xFFA855F7)) // purple
private val RainbowWeirdMp = listOf(Color(0xFFF472B6), Color(0xFFEC4899))   // pink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSelectScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val anim = rememberStaggeredAnimation(count = 12)
    fun Modifier.staggered(i: Int) = this.then(anim.modifier(i))
    var quickStartExpanded by remember { mutableStateOf(false) }
    var aiJudgeExpanded by remember { mutableStateOf(false) }
    var aiJudgeOutdoor by remember { mutableStateOf(true) }
    var quickStartOutdoor by remember { mutableStateOf(true) }
    var soloExpanded by remember { mutableStateOf(false) }
    var soloOutdoor by remember { mutableStateOf(true) }
    var soloCategoryCount by remember { mutableStateOf(5) }
    var aiConsentDialogReason by remember {
        mutableStateOf<pg.geobingo.one.ui.components.AiConsentReason?>(null)
    }

    // Build-17 model: AI consent is now resolved at app launch via the
    // hard-gate screen (Apple 5.1.1(i)/5.1.2(i) Nov-2025). No per-round
    // dialog here — we just check `AiConsent.ratingAccepted` and gate
    // entry into the AI modes. If the user declined the rating consent
    // earlier, we surface a proper modal explaining why and offering a
    // direct route to Settings (replacing the previous flat snackbar).
    fun gateRating(action: () -> Unit) {
        if (pg.geobingo.one.platform.AiConsent.ratingAccepted) {
            action()
        } else {
            aiConsentDialogReason = pg.geobingo.one.ui.components.AiConsentReason.RATING
        }
    }

    SystemBackHandler { nav.goBack() }

    Scaffold(
        topBar = {
            ModeSelectTopBar(
                gameState = gameState,
                onBack = { nav.goBack() },
                onNavigate = { nav.navigateTo(it) },
            )
        },
        containerColor = ColorBackground,
    ) { padding ->
        ModeSelectContent(
            padding = padding,
            staggered = { i -> Modifier.staggered(i) },
            soloExpanded = soloExpanded,
            soloOutdoor = soloOutdoor,
            soloCategoryCount = soloCategoryCount,
            onToggleSoloExpand = { soloExpanded = !soloExpanded },
            onSelectSoloOutdoor = { soloOutdoor = it },
            onSelectSoloCategoryCount = { soloCategoryCount = it },
            onConfirmSolo = {
                gateRating {
                    Analytics.track(Analytics.MODE_SELECTED, mapOf("mode" to "SOLO", "categories" to soloCategoryCount.toString()))
                    val duration = if (soloCategoryCount == 10) 600 else 300
                    // Clear any leftover state from a previous round (captures, ratings,
                    // and especially startTimeMillis — a stale value made the timer
                    // expire instantly and skip straight to the results screen).
                    gameState.solo.reset()
                    gameState.solo.mode = SoloMode.STANDARD
                    gameState.solo.isOutdoor = soloOutdoor
                    gameState.solo.categoryCount = soloCategoryCount
                    gameState.solo.categories = pg.geobingo.one.data.soloCategories(soloOutdoor, soloCategoryCount)
                    gameState.solo.totalDurationSeconds = duration
                    gameState.solo.timeRemainingSeconds = duration
                    gameState.solo.playerName = pg.geobingo.one.platform.AppSettings.getString("last_player_name", "Player")
                    nav.navigateTo(Screen.SOLO_START_TRANSITION)
                }
            },
            onEndlessClick = {
                gateRating {
                    Analytics.track(Analytics.MODE_SELECTED, mapOf("mode" to "SOLO_ENDLESS"))
                    // Route through the shared 3-2-1 countdown so the player is ready
                    // before the survival timer starts. Endless is still fully
                    // self-contained (own screen, local best streak); the transition
                    // just reads solo.mode to know it should hand off to SOLO_ENDLESS.
                    gameState.solo.reset()
                    gameState.solo.mode = SoloMode.ENDLESS
                    nav.navigateTo(Screen.SOLO_START_TRANSITION)
                }
            },
            onDailyRunClick = {
                gateRating {
                    Analytics.track(Analytics.MODE_SELECTED, mapOf("mode" to "SOLO_DAILY_RUN"))
                    gameState.solo.reset()
                    gameState.solo.mode = SoloMode.DAILY_RUN
                    gameState.solo.isOutdoor = true
                    gameState.solo.categoryCount = 5
                    gameState.solo.categories = pg.geobingo.one.data.dailyRunCategories(5)
                    gameState.solo.totalDurationSeconds = 300
                    gameState.solo.timeRemainingSeconds = 300
                    gameState.solo.playerName = pg.geobingo.one.platform.AppSettings.getString("last_player_name", "Player")
                    nav.navigateTo(Screen.SOLO_START_TRANSITION)
                }
            },
            onWeirdCoreSoloClick = {
                gateRating {
                    Analytics.track(Analytics.MODE_SELECTED, mapOf("mode" to "SOLO_WEIRD_CORE"))
                    gameState.solo.reset()
                    gameState.solo.mode = SoloMode.WEIRD_CORE
                    gameState.solo.isOutdoor = true
                    gameState.solo.categoryCount = 5
                    gameState.solo.categories = pg.geobingo.one.data.weirdCoreCategories(true, 5)
                    gameState.solo.totalDurationSeconds = 300
                    gameState.solo.timeRemainingSeconds = 300
                    gameState.solo.playerName = pg.geobingo.one.platform.AppSettings.getString("last_player_name", "Player")
                    nav.navigateTo(Screen.SOLO_START_TRANSITION)
                }
            },
            onRouletteClick = {
                gateRating {
                    Analytics.track(Analytics.MODE_SELECTED, mapOf("mode" to "SOLO_ROULETTE"))
                    // Roulette sets up the round itself after the reel lands.
                    nav.navigateTo(Screen.SOLO_ROULETTE)
                }
            },
            aiJudgeExpanded = aiJudgeExpanded,
            aiJudgeOutdoor = aiJudgeOutdoor,
            onToggleAiJudgeExpand = { aiJudgeExpanded = !aiJudgeExpanded },
            onSelectAiJudgeOutdoor = { aiJudgeOutdoor = it },
            onConfirmAiJudge = {
                gateRating {
                    Analytics.track(Analytics.MODE_SELECTED, mapOf("mode" to "AI_JUDGE"))
                    gameState.session.gameMode = GameMode.AI_JUDGE
                    gameState.session.playOutdoor = aiJudgeOutdoor
                    nav.navigateTo(Screen.CREATE_GAME)
                }
            },
            quickStartExpanded = quickStartExpanded,
            quickStartOutdoor = quickStartOutdoor,
            onToggleQuickStartExpand = { quickStartExpanded = !quickStartExpanded },
            onSelectQuickStartOutdoor = { quickStartOutdoor = it },
            onConfirmQuickStart = {
                Analytics.track(Analytics.MODE_SELECTED, mapOf("mode" to "QUICK_START"))
                gameState.session.gameMode = GameMode.QUICK_START
                gameState.session.quickStartOutdoor = quickStartOutdoor
                gameState.session.quickStartDurationMinutes = 15
                gameState.gameplay.gameDurationMinutes = 15
                nav.navigateTo(Screen.CREATE_GAME)
            },
            onClassicClick = {
                Analytics.track(Analytics.MODE_SELECTED, mapOf("mode" to "CLASSIC"))
                gameState.session.gameMode = GameMode.CLASSIC
                nav.navigateTo(Screen.CREATE_GAME)
            },
            onBlindBingoClick = {
                Analytics.track(Analytics.MODE_SELECTED, mapOf("mode" to "BLIND_BINGO"))
                gameState.session.gameMode = GameMode.BLIND_BINGO
                nav.navigateTo(Screen.CREATE_GAME)
            },
            onWeirdCoreClick = {
                Analytics.track(Analytics.MODE_SELECTED, mapOf("mode" to "WEIRD_CORE"))
                gameState.session.gameMode = GameMode.WEIRD_CORE
                nav.navigateTo(Screen.CREATE_GAME)
            },
        )
    }

    pg.geobingo.one.ui.components.AiConsentRequiredDialog(
        reason = aiConsentDialogReason,
        onDismiss = { aiConsentDialogReason = null },
        onOpenSettings = {
            aiConsentDialogReason = null
            nav.navigateTo(
                Screen.SETTINGS,
                pg.geobingo.one.navigation.NavArgs.Settings(
                    pg.geobingo.one.navigation.NavArgs.SettingsAnchor.AI_PRIVACY,
                ),
            )
        },
    )
}

// ── Top Bar ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelectTopBar(
    gameState: GameState,
    onBack: () -> Unit,
    onNavigate: (Screen) -> Unit,
) {
    TopAppBar(
        title = {
            AnimatedGradientText(
                text = S.current.gameMode,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                gradientColors = GradientPrimary,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = S.current.back, tint = ColorPrimary)
            }
        },
        actions = { pg.geobingo.one.ui.components.TopBarStarsAndProfile(gameState = gameState, onNavigate = onNavigate) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
    )
}

// ── Main Content ────────────────────────────────────────────────────────

@Composable
private fun ModeSelectContent(
    padding: PaddingValues,
    staggered: (Int) -> Modifier,
    soloExpanded: Boolean,
    soloOutdoor: Boolean,
    soloCategoryCount: Int,
    onToggleSoloExpand: () -> Unit,
    onSelectSoloOutdoor: (Boolean) -> Unit,
    onSelectSoloCategoryCount: (Int) -> Unit,
    onConfirmSolo: () -> Unit,
    onEndlessClick: () -> Unit,
    onDailyRunClick: () -> Unit,
    onWeirdCoreSoloClick: () -> Unit,
    onRouletteClick: () -> Unit,
    aiJudgeExpanded: Boolean,
    aiJudgeOutdoor: Boolean,
    onToggleAiJudgeExpand: () -> Unit,
    onSelectAiJudgeOutdoor: (Boolean) -> Unit,
    onConfirmAiJudge: () -> Unit,
    quickStartExpanded: Boolean,
    quickStartOutdoor: Boolean,
    onToggleQuickStartExpand: () -> Unit,
    onSelectQuickStartOutdoor: (Boolean) -> Unit,
    onConfirmQuickStart: () -> Unit,
    onClassicClick: () -> Unit,
    onBlindBingoClick: () -> Unit,
    onWeirdCoreClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        Text(
            S.current.howDoYouWantToPlay,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = ColorOnSurface,
            modifier = staggered(0),
        )

        Spacer(Modifier.height(4.dp))

        // ── Solo (all AI-rated; Standard + Daily are ranked) ─────────
        SoloSectionHeader()

        SoloChallengeCard(
            expanded = soloExpanded,
            outdoor = soloOutdoor,
            categoryCount = soloCategoryCount,
            onToggleExpand = onToggleSoloExpand,
            onSelectOutdoor = onSelectSoloOutdoor,
            onSelectCategoryCount = onSelectSoloCategoryCount,
            onConfirm = onConfirmSolo,
            modifier = staggered(1),
        )

        ModeCard(
            title = S.current.modeEndless,
            subtitle = S.current.modeEndlessSubtitle,
            description = S.current.modeEndlessDesc,
            icon = Icons.Default.LocalFireDepartment,
            gradientColors = RainbowEndless,
            modifier = staggered(2),
            titleBadge = { AnimatedAiBadge() },
            playersIcon = Icons.Default.Person,
            playersLabel = "1",
            onClick = onEndlessClick,
        )

        ModeCard(
            title = S.current.modeDailyRun,
            subtitle = S.current.modeDailyRunSubtitle,
            description = S.current.modeDailyRunDesc,
            icon = Icons.Default.Today,
            gradientColors = RainbowDaily,
            modifier = staggered(3),
            titleBadge = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AnimatedAiBadge()
                    RankedBadge()
                }
            },
            playersIcon = Icons.Default.Person,
            playersLabel = "1",
            onClick = onDailyRunClick,
        )

        ModeCard(
            title = S.current.modeWeirdCore,
            subtitle = S.current.modeWeirdCoreSubtitle,
            description = S.current.modeWeirdCoreSoloDesc,
            icon = Icons.Default.Psychology,
            gradientColors = RainbowWeirdSolo,
            modifier = staggered(4),
            titleBadge = { AnimatedAiBadge() },
            playersIcon = Icons.Default.Person,
            playersLabel = "1",
            onClick = onWeirdCoreSoloClick,
        )

        ModeCard(
            title = S.current.modeRoulette,
            subtitle = S.current.modeRouletteSubtitle,
            description = S.current.modeRouletteDesc,
            icon = Icons.Default.Casino,
            gradientColors = RainbowRoulette,
            modifier = staggered(5),
            titleBadge = { AnimatedAiBadge() },
            playersIcon = Icons.Default.Person,
            playersLabel = "1",
            onClick = onRouletteClick,
        )

        // ── Multiplayer ──────────────────────────────────────────────
        MultiplayerSectionHeader()

        AiJudgeCard(
            expanded = aiJudgeExpanded,
            outdoor = aiJudgeOutdoor,
            onToggleExpand = onToggleAiJudgeExpand,
            onSelectOutdoor = onSelectAiJudgeOutdoor,
            onConfirm = onConfirmAiJudge,
            gradientColors = RainbowAiJudge,
            modifier = staggered(6),
        )

        QuickStartCard(
            expanded = quickStartExpanded,
            outdoor = quickStartOutdoor,
            onToggleExpand = onToggleQuickStartExpand,
            onSelectOutdoor = onSelectQuickStartOutdoor,
            onConfirm = onConfirmQuickStart,
            gradientColors = RainbowQuickStart,
            modifier = staggered(7),
        )

        ModeCard(
            title = S.current.modeClassic,
            subtitle = S.current.modeClassicSubtitle,
            description = S.current.modeClassicDesc,
            icon = Icons.Default.GridView,
            gradientColors = RainbowClassic,
            modifier = staggered(8),
            playersIcon = Icons.Default.Groups,
            playersLabel = "2+",
            onClick = onClassicClick,
        )

        ModeCard(
            title = S.current.modeBlindBingo,
            subtitle = S.current.modeBlindBingoSubtitle,
            description = S.current.modeBlindBingoDesc,
            icon = Icons.Default.VisibilityOff,
            gradientColors = RainbowBlindBingo,
            modifier = staggered(9),
            playersIcon = Icons.Default.Groups,
            playersLabel = "2+",
            onClick = onBlindBingoClick,
        )

        ModeCard(
            title = S.current.modeWeirdCore,
            subtitle = S.current.modeWeirdCoreSubtitle,
            description = S.current.modeWeirdCoreDesc,
            icon = Icons.Default.QuestionMark,
            gradientColors = RainbowWeirdMp,
            modifier = staggered(10),
            playersIcon = Icons.Default.Groups,
            playersLabel = "2+",
            onClick = onWeirdCoreClick,
        )

        Spacer(Modifier.height(24.dp))
    }
}

// ── Multiplayer Section Header ──────────────────────────────────────────

@Composable
private fun MultiplayerSectionHeader() {
    HorizontalDivider(
        color = ColorOutlineVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
    Text(
        "Multiplayer",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = ColorOnSurfaceVariant,
    )
    Spacer(Modifier.height(4.dp))
}

// ── Solo Section Header (with leaderboard-scope hint) ───────────────────

@Composable
private fun SoloSectionHeader() {
    Text(
        "Solo",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = ColorOnSurfaceVariant,
    )
    Spacer(Modifier.height(4.dp))
    Row(
        // Top-align so that if the hint wraps (large font scale / very narrow
        // screen) the trophy icon stays beside the first line instead of floating
        // in the vertical centre of a two-line block.
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            Icons.Default.EmojiEvents,
            contentDescription = null,
            modifier = Modifier.padding(top = 1.dp).size(13.dp),
            tint = Color(0xFFF59E0B),
        )
        Text(
            S.current.soloRankedHint,
            style = MaterialTheme.typography.labelSmall,
            color = ColorOnSurfaceVariant.copy(alpha = 0.75f),
        )
    }
}

// ── Ranked Badge (shown on solo modes that count for the leaderboard) ──

@Composable
private fun RankedBadge() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFFBBF24).copy(alpha = 0.18f), Color(0xFFF59E0B).copy(alpha = 0.18f))
                )
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            Icons.Default.EmojiEvents,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = Color(0xFFF59E0B),
        )
        Text(
            S.current.soloRankedBadge,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFFF59E0B),
            maxLines = 1,
            softWrap = false,
        )
    }
}

// ── Player-count Badge (capacity indicator shown on every mode card) ────

@Composable
private fun PlayerCountBadge(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ColorSurfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = ColorOnSurfaceVariant,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = ColorOnSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
    }
}

// ── Shared: Gradient Icon Box ───────────────────────────────────────────

@Composable
private fun GradientIconBox(
    icon: ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = gradientColors,
                    start = Offset(0f, 0f),
                    end = Offset(200f, 200f),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = Color.White,
        )
    }
}

// ── Shared: Outdoor / Indoor Toggle ─────────────────────────────────────

@Composable
private fun OutdoorIndoorToggle(
    outdoor: Boolean,
    gradientColors: List<Color>,
    onSelectOutdoor: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val btnModifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
        // Outdoor
        ToggleOptionButton(
            selected = outdoor,
            icon = Icons.Default.WbSunny,
            label = S.current.outdoor,
            gradientColors = gradientColors,
            onClick = { onSelectOutdoor(true) },
            modifier = btnModifier,
        )
        // Indoor
        ToggleOptionButton(
            selected = !outdoor,
            icon = Icons.Default.House,
            label = S.current.indoor,
            gradientColors = gradientColors,
            onClick = { onSelectOutdoor(false) },
            modifier = btnModifier,
        )
    }
}

// ── Shared: Toggle Option Button ────────────────────────────────────────

@Composable
private fun ToggleOptionButton(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                if (selected) Brush.linearGradient(gradientColors)
                else Brush.linearGradient(listOf(ColorSurfaceVariant, ColorSurfaceVariant))
            )
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else ColorOutline,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = if (selected) Color.White else ColorOnSurfaceVariant)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) Color.White else ColorOnSurfaceVariant,
            )
        }
    }
}

// ── Shared: Gradient Confirm Button ─────────────────────────────────────

@Composable
private fun GradientConfirmButton(
    text: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(gradientColors))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

// ── Shared: Expandable Card Options Section ─────────────────────────────

@Composable
private fun ExpandableCardOptionsSection(
    outdoor: Boolean,
    gradientColors: List<Color>,
    locationLabel: String,
    onSelectOutdoor: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    extraContent: (@Composable () -> Unit)? = null,
) {
    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = ColorOutlineVariant)
    Spacer(Modifier.height(14.dp))

    Text(
        locationLabel,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = ColorOnSurfaceVariant,
    )
    Spacer(Modifier.height(10.dp))

    OutdoorIndoorToggle(
        outdoor = outdoor,
        gradientColors = gradientColors,
        onSelectOutdoor = onSelectOutdoor,
    )

    extraContent?.invoke()

    Spacer(Modifier.height(12.dp))

    GradientConfirmButton(
        text = S.current.letsGo,
        gradientColors = gradientColors,
        onClick = onConfirm,
    )
}

// ── Quick Start Card ────────────────────────────────────────────────────

@Composable
private fun QuickStartCard(
    expanded: Boolean,
    outdoor: Boolean,
    onToggleExpand: () -> Unit,
    onSelectOutdoor: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    gradientColors: List<Color> = GradientQuickStart,
    modifier: Modifier = Modifier,
) {
    val accentColor = gradientColors.first()
    val scope = rememberCoroutineScope()
    val pressScale = remember { Animatable(1f) }

    GradientBorderCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = pressScale.value; scaleY = pressScale.value }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        scope.launch { pressScale.animateTo(0.97f, tween(80)) }
                        tryAwaitRelease()
                        scope.launch { pressScale.animateTo(1f, tween(120)) }
                    },
                    onTap = { onToggleExpand() },
                )
            },
        cornerRadius = 18.dp,
        borderColors = gradientColors,
        backgroundColor = ColorSurface,
        borderWidth = 1.5.dp,
        glassmorphism = false,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                GradientIconBox(icon = Icons.Default.Bolt, gradientColors = gradientColors)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        S.current.modeQuickStart,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorOnSurface,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            S.current.modeQuickStartSubtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        PlayerCountBadge(icon = Icons.Default.Groups, label = "2+")
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        S.current.modeQuickStartDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnSurfaceVariant,
                        lineHeight = 17.sp,
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).padding(top = 2.dp),
                    tint = ColorOnSurfaceVariant,
                )
            }

            if (expanded) {
                ExpandableCardOptionsSection(
                    outdoor = outdoor,
                    gradientColors = gradientColors,
                    locationLabel = S.current.whereDoYouPlay,
                    onSelectOutdoor = onSelectOutdoor,
                    onConfirm = onConfirm,
                )
            }
        }
    }
}

// ── Mode Card (non-expandable) ──────────────────────────────────────────

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    description: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    playersIcon: ImageVector,
    playersLabel: String,
    modifier: Modifier = Modifier,
    titleBadge: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val accentColor = gradientColors.first()
    val scope = rememberCoroutineScope()
    val pressScale = remember { Animatable(1f) }

    GradientBorderCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = pressScale.value; scaleY = pressScale.value }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        scope.launch { pressScale.animateTo(0.97f, tween(80)) }
                        tryAwaitRelease()
                        scope.launch { pressScale.animateTo(1f, tween(120)) }
                    },
                    onTap = { onClick() },
                )
            },
        cornerRadius = 18.dp,
        borderColors = gradientColors,
        backgroundColor = ColorSurface,
        borderWidth = 1.5.dp,
        glassmorphism = false,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            GradientIconBox(icon = icon, gradientColors = gradientColors)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // FlowRow so that on narrow phones the badge(s) wrap to the next line
                // as intact chips instead of the "Ranked/Bestenliste" badge text
                // breaking mid-word. Daily Run carries two badges (AI + Ranked),
                // which is what overflowed the single Row before.
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorOnSurface,
                        maxLines = 1,
                    )
                    titleBadge?.invoke()
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    PlayerCountBadge(icon = playersIcon, label = playersLabel)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorOnSurfaceVariant,
                    lineHeight = 17.sp,
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp).padding(top = 2.dp),
                tint = ColorOnSurfaceVariant,
            )
        }
    }
}

// ── Animated AI Badge ────────────────────────────────────────────────────

@Composable
private fun AnimatedAiBadge() {
    val reduceMotion = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "aiBadge")
    val sparkleRotation by transition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sparkleRotation",
    )
    val sparkleScale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sparkleScale",
    )
    val gradientOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "badgeGradient",
    )

    val bgBrush = Brush.linearGradient(
        colors = GradientAiJudge.map { it.copy(alpha = 0.15f) },
        start = Offset(gradientOffset, 0f),
        end = Offset(gradientOffset + 100f, 40f),
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgBrush)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier
                .size(12.dp)
                .graphicsLayer {
                    if (!reduceMotion) {
                        rotationZ = sparkleRotation
                        scaleX = sparkleScale
                        scaleY = sparkleScale
                    }
                },
            tint = GradientAiJudge.first(),
        )
        AnimatedGradientText(
            text = "AI",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            gradientColors = GradientAiJudge,
            durationMillis = 3000,
        )
    }
}

// ── AI Judge Card ────────────────────────────────────────────────────────

@Composable
private fun AiJudgeCard(
    expanded: Boolean,
    outdoor: Boolean,
    onToggleExpand: () -> Unit,
    onSelectOutdoor: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    gradientColors: List<Color> = GradientAiJudge,
    modifier: Modifier = Modifier,
) {
    val accentColor = gradientColors.first()
    val scope = rememberCoroutineScope()
    val pressScale = remember { Animatable(1f) }

    GradientBorderCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = pressScale.value; scaleY = pressScale.value }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        scope.launch { pressScale.animateTo(0.97f, tween(80)) }
                        tryAwaitRelease()
                        scope.launch { pressScale.animateTo(1f, tween(120)) }
                    },
                    onTap = { onToggleExpand() },
                )
            },
        cornerRadius = 18.dp,
        borderColors = gradientColors,
        backgroundColor = ColorSurface,
        borderWidth = 1.5.dp,
        glassmorphism = false,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                GradientIconBox(icon = Icons.Default.AutoAwesome, gradientColors = gradientColors)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            S.current.modeAiJudge,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorOnSurface,
                        )
                        AnimatedAiBadge()
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            S.current.modeAiJudgeSubtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        PlayerCountBadge(icon = Icons.Default.Groups, label = "2+")
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        S.current.modeAiJudgeDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnSurfaceVariant,
                        lineHeight = 17.sp,
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).padding(top = 2.dp),
                    tint = ColorOnSurfaceVariant,
                )
            }

            if (expanded) {
                ExpandableCardOptionsSection(
                    outdoor = outdoor,
                    gradientColors = gradientColors,
                    locationLabel = S.current.whereDoYouPlay,
                    onSelectOutdoor = onSelectOutdoor,
                    onConfirm = onConfirm,
                )
            }
        }
    }
}

// ── Solo Challenge Card ──────────────────────────────────────────────────

private val SoloGradientColors = RainbowSolo

@Composable
private fun SoloChallengeCard(
    expanded: Boolean,
    outdoor: Boolean,
    categoryCount: Int,
    onToggleExpand: () -> Unit,
    onSelectOutdoor: (Boolean) -> Unit,
    onSelectCategoryCount: (Int) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradientColors = SoloGradientColors
    val accentColor = gradientColors.first()
    val scope = rememberCoroutineScope()
    val pressScale = remember { Animatable(1f) }

    GradientBorderCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = pressScale.value; scaleY = pressScale.value }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        scope.launch { pressScale.animateTo(0.97f, tween(80)) }
                        tryAwaitRelease()
                        scope.launch { pressScale.animateTo(1f, tween(120)) }
                    },
                    onTap = { onToggleExpand() },
                )
            },
        cornerRadius = 18.dp,
        borderColors = gradientColors,
        backgroundColor = ColorSurface,
        borderWidth = 1.5.dp,
        glassmorphism = false,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                GradientIconBox(icon = Icons.Default.Person, gradientColors = gradientColors)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            S.current.soloMode,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorOnSurface,
                            maxLines = 1,
                        )
                        AnimatedAiBadge()
                        RankedBadge()
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            S.current.soloModeSubtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        PlayerCountBadge(icon = Icons.Default.Person, label = "1")
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        S.current.soloModeDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnSurfaceVariant,
                        lineHeight = 17.sp,
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).padding(top = 2.dp),
                    tint = ColorOnSurfaceVariant,
                )
            }

            if (expanded) {
                ExpandableCardOptionsSection(
                    outdoor = outdoor,
                    gradientColors = gradientColors,
                    locationLabel = S.current.whereDoYouPlaySolo,
                    onSelectOutdoor = onSelectOutdoor,
                    onConfirm = onConfirm,
                    extraContent = {
                        Spacer(Modifier.height(14.dp))
                        SoloCategoryCountToggle(
                            categoryCount = categoryCount,
                            gradientColors = gradientColors,
                            onSelectCategoryCount = onSelectCategoryCount,
                        )
                    },
                )
            }
        }
    }
}

// ── Solo Category Count Toggle ──────────────────────────────────────────

@Composable
private fun SoloCategoryCountToggle(
    categoryCount: Int,
    gradientColors: List<Color>,
    onSelectCategoryCount: (Int) -> Unit,
) {
    Text(
        S.current.soloCategoryCountLabel,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = ColorOnSurfaceVariant,
    )
    Spacer(Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val catBtnModifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
        listOf(5 to "5 ${S.current.categories}", 10 to "10 ${S.current.categories}").forEach { (count, label) ->
            val selected = categoryCount == count
            ToggleOptionButton(
                selected = selected,
                icon = if (count == 5) Icons.Default.GridView else Icons.Default.GridOn,
                label = label,
                gradientColors = gradientColors,
                onClick = { onSelectCategoryCount(count) },
                modifier = catBtnModifier,
            )
        }
    }

    Spacer(Modifier.height(6.dp))
    Text(
        if (categoryCount == 10) "10 Min. | ${S.current.outdoor}/${S.current.indoor}"
        else "5 Min. | ${S.current.outdoor}/${S.current.indoor}",
        style = MaterialTheme.typography.labelSmall,
        color = ColorOnSurfaceVariant.copy(alpha = 0.7f),
    )
}
