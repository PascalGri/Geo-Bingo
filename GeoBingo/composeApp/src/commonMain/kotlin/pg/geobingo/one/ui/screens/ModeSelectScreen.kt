package pg.geobingo.one.ui.screens

import androidx.compose.animation.core.Animatable
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
import pg.geobingo.one.game.GameMode
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.i18n.S
import pg.geobingo.one.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSelectScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val anim = rememberStaggeredAnimation(count = 5)
    fun Modifier.staggered(i: Int) = this.then(anim.modifier(i))
    var quickStartExpanded by remember { mutableStateOf(false) }
    var quickStartOutdoor by remember { mutableStateOf(true) }
    var quickStartDifficulty by remember { mutableStateOf("medium") }

    SystemBackHandler { nav.goBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = S.current.gameMode,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = GradientPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = S.current.back, tint = ColorPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        containerColor = ColorBackground,
    ) { padding ->
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
                modifier = Modifier.staggered(0),
            )

            Spacer(Modifier.height(4.dp))

            QuickStartCard(
                expanded = quickStartExpanded,
                outdoor = quickStartOutdoor,
                difficulty = quickStartDifficulty,
                onToggleExpand = { quickStartExpanded = !quickStartExpanded },
                onSelectOutdoor = { quickStartOutdoor = it },
                onSelectDifficulty = { quickStartDifficulty = it },
                onConfirm = {
                    gameState.session.gameMode = GameMode.QUICK_START
                    gameState.session.quickStartOutdoor = quickStartOutdoor
                    gameState.session.quickStartDurationMinutes = 15
                    gameState.session.quickStartDifficulty = quickStartDifficulty
                    gameState.gameplay.gameDurationMinutes = 15
                    nav.navigateTo(Screen.CREATE_GAME)
                },
                modifier = Modifier.staggered(1),
            )

            ModeCard(
                mode = GameMode.CLASSIC,
                title = S.current.modeClassic,
                subtitle = S.current.modeClassicSubtitle,
                description = S.current.modeClassicDesc,
                icon = Icons.Default.GridView,
                gradientColors = GradientPrimary,
                modifier = Modifier.staggered(2),
                onClick = {
                    gameState.session.gameMode = GameMode.CLASSIC
                    nav.navigateTo(Screen.CREATE_GAME)
                },
            )

            ModeCard(
                mode = GameMode.BLIND_BINGO,
                title = S.current.modeBlindBingo,
                subtitle = S.current.modeBlindBingoSubtitle,
                description = S.current.modeBlindBingoDesc,
                icon = Icons.Default.VisibilityOff,
                gradientColors = GradientCool,
                modifier = Modifier.staggered(3),
                onClick = {
                    gameState.session.gameMode = GameMode.BLIND_BINGO
                    nav.navigateTo(Screen.CREATE_GAME)
                },
            )

            ModeCard(
                mode = GameMode.WEIRD_CORE,
                title = S.current.modeWeirdCore,
                subtitle = S.current.modeWeirdCoreSubtitle,
                description = S.current.modeWeirdCoreDesc,
                icon = Icons.Default.QuestionMark,
                gradientColors = GradientWeird,
                modifier = Modifier.staggered(4),
                onClick = {
                    gameState.session.gameMode = GameMode.WEIRD_CORE
                    nav.navigateTo(Screen.CREATE_GAME)
                },
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickStartCard(
    expanded: Boolean,
    outdoor: Boolean,
    difficulty: String,
    onToggleExpand: () -> Unit,
    onSelectOutdoor: (Boolean) -> Unit,
    onSelectDifficulty: (String) -> Unit,
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
                Box(
                    modifier = Modifier
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
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = Color.White,
                    )
                }
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
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = ColorOutlineVariant)
                Spacer(Modifier.height(14.dp))

                Text(
                    S.current.whereDoYouPlay,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorOnSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val btnModifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    // Draußen
                    Box(
                        modifier = btnModifier
                            .background(
                                if (outdoor) Brush.linearGradient(gradientColors)
                                else Brush.linearGradient(listOf(ColorSurfaceVariant, ColorSurfaceVariant))
                            )
                            .border(
                                width = 1.dp,
                                color = if (outdoor) Color.Transparent else ColorOutline,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable { onSelectOutdoor(true) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.WbSunny, null, modifier = Modifier.size(16.dp), tint = if (outdoor) Color.White else ColorOnSurfaceVariant)
                            Text(
                                S.current.outdoor,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (outdoor) Color.White else ColorOnSurfaceVariant,
                            )
                        }
                    }
                    // Drinnen
                    Box(
                        modifier = btnModifier
                            .background(
                                if (!outdoor) Brush.linearGradient(gradientColors)
                                else Brush.linearGradient(listOf(ColorSurfaceVariant, ColorSurfaceVariant))
                            )
                            .border(
                                width = 1.dp,
                                color = if (!outdoor) Color.Transparent else ColorOutline,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable { onSelectOutdoor(false) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.House, null, modifier = Modifier.size(16.dp), tint = if (!outdoor) Color.White else ColorOnSurfaceVariant)
                            Text(
                                S.current.indoor,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (!outdoor) Color.White else ColorOnSurfaceVariant,
                            )
                        }
                    }
                }

                // ── Difficulty selection ────────────────────────────────────
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val diffBtnModifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    listOf(
                        "easy" to S.current.difficultyEasy,
                        "medium" to S.current.difficultyMedium,
                        "hard" to S.current.difficultyHard,
                    ).forEach { (diff, label) ->
                        val selected = difficulty == diff
                        Box(
                            modifier = diffBtnModifier
                                .background(
                                    if (selected) Brush.linearGradient(gradientColors)
                                    else Brush.linearGradient(listOf(ColorSurfaceVariant, ColorSurfaceVariant))
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selected) Color.Transparent else ColorOutline,
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .clickable { onSelectDifficulty(diff) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) Color.White else ColorOnSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(gradientColors))
                        .clickable { onConfirm() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        S.current.letsGo,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeCard(
    mode: GameMode,
    title: String,
    subtitle: String,
    description: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
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
            // Icon box
            Box(
                modifier = Modifier
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

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorOnSurface,
                )
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
