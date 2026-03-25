package pg.geobingo.one.ui.screens.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pg.geobingo.one.data.*
import pg.geobingo.one.game.GameConstants
import pg.geobingo.one.game.*
import pg.geobingo.one.util.AppLogger
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.generateCode
import pg.geobingo.one.network.toCategory
import pg.geobingo.one.network.toHex
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.platform.rememberPhotoCapturer
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.components.SelfiePicker
import pg.geobingo.one.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGameScreen(gameState: GameState) {
    val gameMode = gameState.session.gameMode

    var hostNameInput by remember { mutableStateOf("") }
    var selectedAvatarBytes by remember { mutableStateOf<ByteArray?>(null) }
    val photoCapturer = rememberPhotoCapturer { bytes ->
        if (bytes != null) selectedAvatarBytes = bytes
    }
    var customNameInput by remember { mutableStateOf("") }
    var customCategories by remember { mutableStateOf(listOf<Category>()) }
    var customCategoryCounter by remember { mutableStateOf(0) }
    var selectedPresetIds by remember { mutableStateOf(setOf<String>()) }

    // For WEIRD_CORE: use different preset pool; for others: standard pool
    val presetPool = remember(gameMode) {
        when (gameMode) {
            GameMode.WEIRD_CORE -> WEIRD_CORE_CATEGORIES
            else -> PRESET_CATEGORIES
        }
    }
    var visiblePresets by remember(gameMode) {
        mutableStateOf(presetPool.take(VISIBLE_PRESET_COUNT))
    }

    var durationMinutes by remember { mutableStateOf(GameConstants.DEFAULT_GAME_DURATION_MINUTES.toFloat()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val shuffleAlpha = remember { Animatable(1f) }

    val totalCategories = customCategories.size + selectedPresetIds.size
    val canStart = hostNameInput.trim().isNotEmpty() && (gameMode == GameMode.QUICK_START || totalCategories >= 2)
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        val msg = errorMessage ?: return@LaunchedEffect
        errorMessage = null
        snackbarHostState.showSnackbar(msg)
    }

    val anim = rememberStaggeredAnimation(count = 6)
    val bottomBarOffset = remember { Animatable(80f) }
    val bottomBarAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch {
            kotlinx.coroutines.delay(200L)
            launch { bottomBarOffset.animateTo(0f, tween(400)) }
            bottomBarAlpha.animateTo(1f, tween(400))
        }
    }

    fun Modifier.staggered(index: Int): Modifier = this.then(anim.modifier(index))

    SystemBackHandler { gameState.session.currentScreen = Screen.SELECT_MODE }

    val topBarTitle = when (gameMode) {
        GameMode.CLASSIC -> "Klassisch"
        GameMode.BLIND_BINGO -> "Blind Bingo"
        GameMode.WEIRD_CORE -> "Weird Core"
        GameMode.QUICK_START -> "Quick Start"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = topBarTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = when (gameMode) {
                            GameMode.CLASSIC -> GradientPrimary
                            GameMode.BLIND_BINGO -> GradientCool
                            GameMode.WEIRD_CORE -> GradientWeird
                            GameMode.QUICK_START -> GradientQuickStart
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { gameState.session.currentScreen = Screen.SELECT_MODE }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = ColorPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = ColorSurface,
                modifier = Modifier.graphicsLayer {
                    translationY = bottomBarOffset.value
                    alpha = bottomBarAlpha.value
                },
            ) {
                Column(modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = 12.dp)) {
                    GradientButton(
                        text = when {
                            hostNameInput.trim().isEmpty() -> "Name eingeben"
                            gameMode == GameMode.QUICK_START -> "Runde erstellen  ·  5 Kategorien"
                            totalCategories < 2 -> "Mind. 2 Kategorien wählen"
                            else -> "Runde erstellen  ·  $totalCategories Kategorien"
                        },
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                try {
                                    val allCategories = if (gameMode == GameMode.QUICK_START) {
                                        quickStartCategories(gameState.session.quickStartOutdoor)
                                    } else {
                                        val presets = presetPool.filter { it.id in selectedPresetIds }
                                        customCategories + presets
                                    }
                                    val effectiveDuration = if (gameMode == GameMode.QUICK_START) GameConstants.DEFAULT_GAME_DURATION_MINUTES else durationMinutes.toInt()
                                    val code = generateCode()

                                    // Step 1: Create game (rollback point)
                                    val game = GameRepository.createGame(code, effectiveDuration * 60, false, gameMode.name)
                                    try {
                                        // Step 2: Add host player
                                        val hostColor = PLAYER_COLORS[0].toHex()
                                        val hostDto = GameRepository.addPlayer(game.id, hostNameInput.trim(), hostColor)

                                        // Step 3: Upload avatar (optional, non-critical)
                                        val avatarBytes = selectedAvatarBytes
                                        if (avatarBytes != null) {
                                            try {
                                                GameRepository.uploadAvatarPhoto(hostDto.id, avatarBytes)
                                                GameRepository.setPlayerAvatar(hostDto.id, "selfie")
                                            } catch (e: Exception) { AppLogger.w("Create", "Avatar upload failed", e) }
                                            try { LocalPhotoStore.saveAvatar(hostDto.id, avatarBytes) } catch (e: Exception) { AppLogger.d("Create", "Avatar local save failed", e) }
                                        }

                                        // Step 4: Add categories
                                        val categoryDtos = GameRepository.addCategories(game.id, allCategories)

                                        // All critical steps succeeded — update state
                                        if (avatarBytes != null) {
                                            gameState.photo.setAvatar(hostDto.id, avatarBytes)
                                        }
                                        gameState.session.gameId = game.id
                                        gameState.session.gameCode = game.code
                                        gameState.session.isHost = true
                                        gameState.session.myPlayerId = hostDto.id
                                        gameState.gameplay.gameDurationMinutes = effectiveDuration
                                        gameState.joker.jokerMode = false
                                        gameState.gameplay.selectedCategories = categoryDtos.map { it.toCategory() }
                                        gameState.gameplay.lobbyPlayers = listOf(hostDto)
                                        gameState.session.currentScreen = Screen.LOBBY
                                    } catch (e: Exception) {
                                        // Cleanup: mark orphaned game as closed
                                        AppLogger.e("Create", "Game setup failed, cleaning up game ${game.id}", e)
                                        try { GameRepository.setGameStatus(game.id, "closed") } catch (cleanupErr: Exception) {
                                            AppLogger.w("Create", "Cleanup of orphaned game failed", cleanupErr)
                                        }
                                        throw e
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Fehler: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = canStart && !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        gradientColors = when (gameMode) {
                            GameMode.CLASSIC -> GradientPrimary
                            GameMode.BLIND_BINGO -> GradientCool
                            GameMode.WEIRD_CORE -> GradientWeird
                            GameMode.QUICK_START -> GradientQuickStart
                        },
                        leadingIcon = if (isLoading) {
                            { CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) }
                        } else null,
                    )
                }
            }
        },
        containerColor = ColorBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── Modus-Banner für Blind Bingo, Weird Core & Quick Start ────
            if (gameMode != GameMode.CLASSIC) {
                ModeBanner(gameMode = gameMode, modifier = Modifier.staggered(0))
            }

            // ── 1. Name & Avatar ──────────────────────────────────────────
            DarkSectionCard(
                title = "Name & Avatar",
                modifier = Modifier.staggered(if (gameMode == GameMode.CLASSIC) 0 else 1),
                gradientColors = when (gameMode) {
                    GameMode.CLASSIC -> GradientPrimary
                    GameMode.BLIND_BINGO -> GradientCool
                    GameMode.WEIRD_CORE -> GradientWeird
                    GameMode.QUICK_START -> GradientQuickStart
                },
            ) {
                OutlinedTextField(
                    value = hostNameInput,
                    onValueChange = { if (it.length <= 20) hostNameInput = it },
                    placeholder = { Text("z.B. Pascal", color = ColorOnSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorPrimary,
                        unfocusedBorderColor = ColorOutline,
                        focusedTextColor = ColorOnSurface,
                        unfocusedTextColor = ColorOnSurface,
                        cursorColor = ColorPrimary,
                        focusedContainerColor = ColorSurfaceVariant,
                        unfocusedContainerColor = ColorSurfaceVariant,
                    ),
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = ColorPrimary) },
                )
                Spacer(Modifier.height(12.dp))
                SelfiePicker(
                    avatarBytes = selectedAvatarBytes,
                    onTakePhoto = { photoCapturer.launch() },
                    onClear = { selectedAvatarBytes = null },
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Andere Spieler treten über einen Code bei.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorOnSurfaceVariant,
                )
            }

            // ── 2. Kategorien ─────────────────────────────────────────────
            if (gameMode != GameMode.QUICK_START) {
            val catSectionIndex = if (gameMode == GameMode.CLASSIC) 1 else 2
            DarkSectionCard(
                title = "Kategorien  ·  $totalCategories ausgewählt",
                modifier = Modifier.staggered(catSectionIndex),
                gradientColors = when (gameMode) {
                    GameMode.CLASSIC -> GradientPrimary
                    GameMode.BLIND_BINGO -> GradientCool
                    GameMode.WEIRD_CORE -> GradientWeird
                    GameMode.QUICK_START -> GradientQuickStart
                },
            ) {
                // Custom category input – hidden for Weird Core (categories are fixed/curated)
                if (gameMode != GameMode.WEIRD_CORE) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = customNameInput,
                            onValueChange = { if (it.length <= 30) customNameInput = it },
                            placeholder = { Text("Eigene Kategorie...", color = ColorOnSurfaceVariant) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ColorPrimary,
                                unfocusedBorderColor = ColorOutline,
                                focusedTextColor = ColorOnSurface,
                                unfocusedTextColor = ColorOnSurface,
                                cursorColor = ColorPrimary,
                                focusedContainerColor = ColorSurfaceVariant,
                                unfocusedContainerColor = ColorSurfaceVariant,
                            ),
                        )
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (customNameInput.trim().isNotEmpty())
                                        Brush.linearGradient(GradientPrimary)
                                    else
                                        Brush.linearGradient(listOf(ColorSurfaceVariant, ColorSurfaceVariant))
                                )
                                .clickable {
                                    val name = customNameInput.trim()
                                    if (name.isNotEmpty()) {
                                        customCategories = customCategories + Category(
                                            id = "custom_$customCategoryCounter",
                                            name = name,
                                            emoji = "custom",
                                        )
                                        customCategoryCounter++
                                        customNameInput = ""
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp), tint = Color.White)
                        }
                    }

                    if (customCategories.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            customCategories.forEach { cat ->
                                key(cat.id) {
                                    AnimatedVisibility(
                                        visible = true,
                                        enter = expandVertically(tween(300)) + fadeIn(tween(300)),
                                        exit = shrinkVertically(tween(300)) + fadeOut(tween(300)),
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = ColorPrimaryContainer,
                                            border = BorderStroke(1.dp, ColorPrimary.copy(alpha = 0.3f)),
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Icon(
                                                    imageVector = getCategoryIcon(cat.id),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp).rotate(getCategoryIconRotation(cat.id)),
                                                    tint = ColorPrimary,
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    cat.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.weight(1f),
                                                    color = ColorOnPrimaryContainer,
                                                )
                                                IconButton(
                                                    onClick = { customCategories = customCategories.filter { it.id != cat.id } },
                                                    modifier = Modifier.size(28.dp),
                                                ) {
                                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp), tint = ColorOnSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = ColorOutlineVariant)
                        Text("Vorlagen", style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = ColorOutlineVariant)
                    }
                    Spacer(Modifier.height(12.dp))
                } else {
                    // Weird Core: small note
                    Text(
                        "Wähle die Kategorien aus, mit denen ihr spielen wollt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // Preset / Weird Core category chips
                val shuffleGradient = when (gameMode) {
                    GameMode.WEIRD_CORE -> GradientWeird
                    GameMode.BLIND_BINGO -> GradientCool
                    else -> GradientPrimary
                }

                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = shuffleAlpha.value },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    visiblePresets.forEach { category ->
                        val isSelected = category.id in selectedPresetIds
                        DarkCategorySelectCard(
                            category = category,
                            isSelected = isSelected,
                            onClick = {
                                selectedPresetIds = if (isSelected)
                                    selectedPresetIds - category.id
                                else
                                    selectedPresetIds + category.id
                            },
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(shuffleGradient))
                        .clickable {
                            scope.launch {
                                shuffleAlpha.animateTo(0f, tween(150))
                                val selectedOnes = presetPool.filter { it.id in selectedPresetIds }
                                val unselectedPool = presetPool.filter { it.id !in selectedPresetIds }.shuffled()
                                val fillCount = (VISIBLE_PRESET_COUNT - selectedOnes.size).coerceAtLeast(0)
                                visiblePresets = (selectedOnes + unselectedPool.take(fillCount)).shuffled()
                                shuffleAlpha.animateTo(1f, tween(250))
                            }
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Default.Shuffle, null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Text(
                            "Andere Vorschläge",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                }
            }

            } // end if (gameMode != GameMode.QUICK_START)

            // ── Speed Bonus Hinweis ───────────────────────────────────────
            run {
                val speedIndex = if (gameMode == GameMode.CLASSIC) 2 else 3
                val speedGradient = when (gameMode) {
                    GameMode.BLIND_BINGO -> GradientCool
                    GameMode.WEIRD_CORE -> GradientWeird
                    GameMode.QUICK_START -> GradientQuickStart
                    else -> GradientPrimary
                }
                SpeedBonusCard(
                    gradientColors = speedGradient,
                    modifier = Modifier.staggered(speedIndex),
                )
            }

            // ── 3. Spielzeit ──────────────────────────────────────────────
            if (gameMode != GameMode.QUICK_START) {
                val timeIndex = when (gameMode) {
                    GameMode.CLASSIC -> 3
                    else -> 4
                }
                DurationSection(
                    durationMinutes = durationMinutes,
                    onDurationChange = { durationMinutes = it },
                    gradientColors = when (gameMode) {
                        GameMode.CLASSIC -> GradientPrimary
                        GameMode.BLIND_BINGO -> GradientCool
                        GameMode.WEIRD_CORE -> GradientWeird
                        GameMode.QUICK_START -> GradientQuickStart
                    },
                    modifier = Modifier.staggered(timeIndex),
                )
            } // end if (gameMode != GameMode.QUICK_START)

            Spacer(Modifier.height(4.dp))
        }
    }
}

// ── Modus-Banner für Blind Bingo und Weird Core ───────────────────────────────

@Composable
private fun ModeBanner(gameMode: GameMode, modifier: Modifier = Modifier) {
    val (icon, title, text, gradientColors) = when (gameMode) {
        GameMode.BLIND_BINGO -> ModeBannerData(
            icon = Icons.Default.VisibilityOff,
            title = "Blind Bingo aktiv",
            text = "Zu Beginn ist nur die erste Kategorie sichtbar. Im Laufe des Spiels werden nach und nach neue Kategorien enthüllt.",
            colors = GradientCool,
        )
        GameMode.WEIRD_CORE -> ModeBannerData(
            icon = Icons.Default.QuestionMark,
            title = "Weird Core aktiv",
            text = "Nur absurde Kategorien. Kein Standardfoto – gefragt sind unerwartete Momente, NPC-Beobachtungen und Dinge, die eigentlich nicht existieren sollten.",
            colors = GradientWeird,
        )
        GameMode.QUICK_START -> ModeBannerData(
            icon = Icons.Default.Bolt,
            title = "Quick Start aktiv",
            text = "15 Minuten, 5 vorgefertigte Kategorien – einfach Name eingeben und direkt losspielen.",
            colors = GradientQuickStart,
        )
        else -> return
    }

    GradientBorderCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        borderColors = gradientColors,
        backgroundColor = Color(0xFF0C0B15),
        borderWidth = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp).padding(top = 2.dp),
                tint = gradientColors.first(),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = gradientColors.first(),
                )
                Text(
                    text,
                    style = MaterialTheme.typography.bodySmall,
                    color = gradientColors.first().copy(alpha = 0.8f),
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

private data class ModeBannerData(
    val icon: ImageVector,
    val title: String,
    val text: String,
    val colors: List<Color>,
)

@Composable
internal fun DarkSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = GradientPrimary,
    content: @Composable ColumnScope.() -> Unit,
) {
    GradientBorderCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        borderColors = gradientColors,
        backgroundColor = ColorSurface,
        borderWidth = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            AnimatedGradientText(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                gradientColors = gradientColors,
                durationMillis = 3000,
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
