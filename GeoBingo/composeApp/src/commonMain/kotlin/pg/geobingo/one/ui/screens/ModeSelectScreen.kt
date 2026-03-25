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
import pg.geobingo.one.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSelectScreen(gameState: GameState) {
    val anim = rememberStaggeredAnimation(count = 5)
    fun Modifier.staggered(i: Int) = this.then(anim.modifier(i))
    var quickStartExpanded by remember { mutableStateOf(false) }
    var quickStartOutdoor by remember { mutableStateOf(true) }

    SystemBackHandler { gameState.session.currentScreen = Screen.HOME }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = "Spielmodus",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = GradientPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { gameState.session.currentScreen = Screen.HOME }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = ColorPrimary)
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
                "Wie wollt ihr spielen?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ColorOnSurface,
                modifier = Modifier.staggered(0),
            )

            Spacer(Modifier.height(4.dp))

            ModeCard(
                mode = GameMode.CLASSIC,
                title = "Klassisch",
                subtitle = "Kategorien wählen, Fotos schießen, abstimmen",
                description = "Wählt aus dutzenden Vorlagen oder erstellt eigene Kategorien. Wer zuerst fotografiert, bekommt Bonuspunkte.",
                icon = Icons.Default.GridView,
                gradientColors = GradientPrimary,
                modifier = Modifier.staggered(1),
                onClick = {
                    gameState.session.gameMode = GameMode.CLASSIC
                    gameState.session.currentScreen = Screen.CREATE_GAME
                },
            )

            ModeCard(
                mode = GameMode.BLIND_BINGO,
                title = "Blind Bingo",
                subtitle = "Kategorien werden nach und nach enthüllt",
                description = "Ihr seht zu Beginn nur die erste Kategorie. Alle paar Minuten kommt eine neue dazu — plant voraus!",
                icon = Icons.Default.VisibilityOff,
                gradientColors = GradientCool,
                modifier = Modifier.staggered(2),
                onClick = {
                    gameState.session.gameMode = GameMode.BLIND_BINGO
                    gameState.session.currentScreen = Screen.CREATE_GAME
                },
            )

            ModeCard(
                mode = GameMode.WEIRD_CORE,
                title = "Weird Core",
                subtitle = "Nur die absurdesten Kategorien",
                description = "Vergiss klassische Fotografie. Hier zählen absurde Beobachtungen, NPC-Momente und Dinge, die eigentlich nicht existieren sollten.",
                icon = Icons.Default.QuestionMark,
                gradientColors = GradientWeird,
                modifier = Modifier.staggered(3),
                onClick = {
                    gameState.session.gameMode = GameMode.WEIRD_CORE
                    gameState.session.currentScreen = Screen.CREATE_GAME
                },
            )

            QuickStartCard(
                expanded = quickStartExpanded,
                outdoor = quickStartOutdoor,
                onToggleExpand = { quickStartExpanded = !quickStartExpanded },
                onSelectOutdoor = { quickStartOutdoor = it },
                onConfirm = {
                    gameState.session.gameMode = GameMode.QUICK_START
                    gameState.session.quickStartOutdoor = quickStartOutdoor
                    gameState.session.currentScreen = Screen.CREATE_GAME
                },
                modifier = Modifier.staggered(4),
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

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
                        "Quick Start",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorOnSurface,
                    )
                    Text(
                        "15 Min · 5 Kategorien · sofort losspielen",
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Kein Setup, kein Warten. Nur Name eingeben, Umgebung wählen und die Runde startet direkt.",
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
                    "Wo spielt ihr?",
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
                                "Draußen",
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
                                "Drinnen",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (!outdoor) Color.White else ColorOnSurfaceVariant,
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
                        "Los geht's",
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
