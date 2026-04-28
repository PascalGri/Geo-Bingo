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
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.i18n.S
import pg.geobingo.one.util.Analytics
import pg.geobingo.one.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSelectScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val anim = rememberStaggeredAnimation(count = 6)
    fun Modifier.staggered(i: Int) = this.then(anim.modifier(i))
    var quickStartExpanded by remember { mutableStateOf(false) }
    var aiJudgeExpanded by remember { mutableStateOf(false) }
    var aiJudgeOutdoor by remember { mutableStateOf(true) }
    var quickStartOutdoor by remember { mutableStateOf(true) }
    var soloExpanded by remember { mutableStateOf(false) }
    var soloOutdoor by remember { mutableStateOf(true) }
    var soloCategoryCount by remember { mutableStateOf(5) }

    // Up-front consent for the AI-powered modes (Solo + Multiplayer AI Judge).
    // Both modes send photos to a third-party AI service (Cloudflare or
    // Google Gemini), so per Apple guideline 5.1.1(i) we must obtain
    // permission BEFORE any data leaves the device. Asking right when the
    // user confirms an AI-powered mode means the round can't even start
    // without consent.
    var aiConsentAccepted by remember {
        mutableStateOf(AppSettings.getBoolean(pg.geobingo.one.platform.SettingsKeys.AI_CONSENT_ACCEPTED, false))
    }
    var showAiConsentDialog by remember { mutableStateOf(false) }
    var pendingAiAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val gateAi: (() -> Unit) -> Unit = { action ->
        if (aiConsentAccepted) {
            action()
        } else {
            pendingAiAction = action
            showAiConsentDialog = true
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
                gateAi {
                    Analytics.track(Analytics.MODE_SELECTED, mapOf("mode" to "SOLO", "categories" to soloCategoryCount.toString()))
                    val duration = if (soloCategoryCount == 10) 600 else 300
                    // Clear any leftover state from a previous round (captures, ratings,
                    // and especially startTimeMillis — a stale value made the timer
                    // expire instantly and skip straight to the results screen).
                    gameState.solo.reset()
                    gameState.solo.isOutdoor = soloOutdoor
                    gameState.solo.categoryCount = soloCategoryCount
                    gameState.solo.categories = pg.geobingo.one.data.soloCategories(soloOutdoor, soloCategoryCount)
                    gameState.solo.totalDurationSeconds = duration
                    gameState.solo.timeRemainingSeconds = duration
                    gameState.solo.playerName = pg.geobingo.one.platform.AppSettings.getString("last_player_name", "Player")
                    nav.navigateTo(Screen.SOLO_START_TRANSITION)
                }
            },
            aiJudgeExpanded = aiJudgeExpanded,
            aiJudgeOutdoor = aiJudgeOutdoor,
            onToggleAiJudgeExpand = { aiJudgeExpanded = !aiJudgeExpanded },
            onSelectAiJudgeOutdoor = { aiJudgeOutdoor = it },
            onConfirmAiJudge = {
                gateAi {
                    Analytics.track(Analytics.MODE_SELECTED, mapOf("mode" to "AI_JUDGE"))
                    gameState.session.gameMode = GameMode.AI_JUDGE
                    gameState.session.aiJudgeOutdoor = aiJudgeOutdoor
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

    if (showAiConsentDialog) {
        AlertDialog(
            onDismissRequest = {
                // Tap-outside / back: treat as decline. Don't run the pending
                // action; user stays on the mode-select screen and can pick a
                // non-AI mode (Classic / Blind Bingo / Weird Core / Quick).
                showAiConsentDialog = false
                pendingAiAction = null
            },
            icon = {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(28.dp),
                )
            },
            title = { Text(S.current.aiConsentTitle, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    S.current.aiConsentMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorOnSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    AppSettings.setBoolean(pg.geobingo.one.platform.SettingsKeys.AI_CONSENT_ACCEPTED, true)
                    aiConsentAccepted = true
                    showAiConsentDialog = false
                    val action = pendingAiAction
                    pendingAiAction = null
                    action?.invoke()
                }) {
                    Text(S.current.aiConsentAccept)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAiConsentDialog = false
                    pendingAiAction = null
                }) {
                    Text(S.current.aiConsentDecline)
                }
            },
        )
    }
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

        // ── Solo Challenge (top) ─────────────────────────────────────
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

        // ── Multiplayer ──────────────────────────────────────────────
        MultiplayerSectionHeader()

        AiJudgeCard(
            expanded = aiJudgeExpanded,
            outdoor = aiJudgeOutdoor,
            onToggleExpand = onToggleAiJudgeExpand,
            onSelectOutdoor = onSelectAiJudgeOutdoor,
            onConfirm = onConfirmAiJudge,
            modifier = staggered(1),
        )

        QuickStartCard(
            expanded = quickStartExpanded,
            outdoor = quickStartOutdoor,
            onToggleExpand = onToggleQuickStartExpand,
            onSelectOutdoor = onSelectQuickStartOutdoor,
            onConfirm = onConfirmQuickStart,
            modifier = staggered(2),
        )

        ModeCard(
            mode = GameMode.CLASSIC,
            title = S.current.modeClassic,
            subtitle = S.current.modeClassicSubtitle,
            description = S.current.modeClassicDesc,
            icon = Icons.Default.GridView,
            gradientColors = GradientPrimary,
            modifier = staggered(3),
            onClick = onClassicClick,
        )

        ModeCard(
            mode = GameMode.BLIND_BINGO,
            title = S.current.modeBlindBingo,
            subtitle = S.current.modeBlindBingoSubtitle,
            description = S.current.modeBlindBingoDesc,
            icon = Icons.Default.VisibilityOff,
            gradientColors = GradientCool,
            modifier = staggered(4),
            onClick = onBlindBingoClick,
        )

        ModeCard(
            mode = GameMode.WEIRD_CORE,
            title = S.current.modeWeirdCore,
            subtitle = S.current.modeWeirdCoreSubtitle,
            description = S.current.modeWeirdCoreDesc,
            icon = Icons.Default.QuestionMark,
            gradientColors = GradientWeird,
            modifier = staggered(5),
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
    modifier: Modifier = Modifier,
) {
    val gradientColors = GradientQuickStart
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
                    Text(
                        S.current.modeQuickStartSubtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        fontWeight = FontWeight.Medium,
                    )
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
    mode: GameMode,
    title: String,
    subtitle: String,
    description: String,
    icon: ImageVector,
    gradientColors: List<Color>,
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorOnSurface,
                    )
                    titleBadge?.invoke()
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Medium,
                )
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
    modifier: Modifier = Modifier,
) {
    val gradientColors = GradientAiJudge
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
                    Text(
                        S.current.modeAiJudgeSubtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        fontWeight = FontWeight.Medium,
                    )
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

private val SoloGradientColors = listOf(Color(0xFF22D3EE), Color(0xFF6366F1))

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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            S.current.soloMode,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorOnSurface,
                        )
                        AnimatedAiBadge()
                    }
                    Text(
                        S.current.soloModeSubtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        fontWeight = FontWeight.Medium,
                    )
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
