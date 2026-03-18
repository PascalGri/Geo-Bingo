package pg.geobingo.one.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pg.geobingo.one.data.*
import pg.geobingo.one.game.*
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.generateCode
import pg.geobingo.one.network.toCategory
import pg.geobingo.one.network.toHex
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.platform.rememberPhotoCapturer
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.platform.toImageBitmap
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.ui.theme.Spacing
import pg.geobingo.one.ui.theme.rememberStaggeredAnimation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGameScreen(gameState: GameState) {
    var hostNameInput by remember { mutableStateOf("") }
    var selectedAvatarBytes by remember { mutableStateOf<ByteArray?>(null) }
    var jokerMode by remember { mutableStateOf(false) }

    val photoCapturer = rememberPhotoCapturer { bytes ->
        if (bytes != null) selectedAvatarBytes = bytes
    }
    var customNameInput by remember { mutableStateOf("") }
    var customCategories by remember { mutableStateOf(listOf<Category>()) }
    var customCategoryCounter by remember { mutableStateOf(0) }
    var selectedPresetIds by remember { mutableStateOf(setOf<String>()) }
    var visiblePresets by remember { mutableStateOf(PRESET_CATEGORIES.take(VISIBLE_PRESET_COUNT)) }
    var durationMinutes by remember { mutableStateOf(15f) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Shuffle fade animation for preset grid
    val shuffleAlpha = remember { Animatable(1f) }

    val totalCategories = customCategories.size + selectedPresetIds.size
    val canStart = hostNameInput.trim().isNotEmpty() && totalCategories >= 2
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        val msg = errorMessage ?: return@LaunchedEffect
        errorMessage = null
        snackbarHostState.showSnackbar(msg)
    }

    val anim = rememberStaggeredAnimation(count = 5)
    val bottomBarOffset = remember { Animatable(80f) }
    val bottomBarAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch {
            delay(200L)
            launch { bottomBarOffset.animateTo(0f, tween(400)) }
            bottomBarAlpha.animateTo(1f, tween(400))
        }
    }

    fun Modifier.staggered(index: Int): Modifier = this.then(anim.modifier(index))

    SystemBackHandler { gameState.currentScreen = Screen.HOME }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = "Neues Spiel",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = GradientPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { gameState.currentScreen = Screen.HOME }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück", tint = ColorPrimary)
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
                            totalCategories < 2 -> "Mind. 2 Kategorien wählen"
                            else -> "Runde erstellen  ·  $totalCategories Kategorien"
                        },
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                try {
                                    val presets = PRESET_CATEGORIES.filter { it.id in selectedPresetIds }
                                    val allCategories = customCategories + presets
                                    val code = generateCode()
                                    val game = GameRepository.createGame(code, durationMinutes.toInt() * 60, jokerMode)
                                    val hostColor = PLAYER_COLORS[0].toHex()
                                    val hostDto = GameRepository.addPlayer(game.id, hostNameInput.trim(), hostColor)
                                    val avatarBytes = selectedAvatarBytes
                                    if (avatarBytes != null) {
                                        try {
                                            GameRepository.uploadAvatarPhoto(hostDto.id, avatarBytes)
                                            GameRepository.setPlayerAvatar(hostDto.id, "selfie")
                                        } catch (e: Exception) { e.printStackTrace() }
                                        try { LocalPhotoStore.saveAvatar(hostDto.id, avatarBytes) } catch (_: Exception) {}
                                    }
                                    val categoryDtos = GameRepository.addCategories(game.id, allCategories)
                                    if (avatarBytes != null) {
                                        gameState.playerAvatarBytes = gameState.playerAvatarBytes + (hostDto.id to avatarBytes)
                                    }
                                    gameState.gameId = game.id
                                    gameState.gameCode = game.code
                                    gameState.isHost = true
                                    gameState.myPlayerId = hostDto.id
                                    gameState.gameDurationMinutes = durationMinutes.toInt()
                                    gameState.jokerMode = jokerMode
                                    gameState.selectedCategories = categoryDtos.map { it.toCategory() }
                                    gameState.lobbyPlayers = listOf(hostDto)
                                    gameState.currentScreen = Screen.LOBBY
                                } catch (e: Exception) {
                                    errorMessage = "Fehler: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = canStart && !isLoading,
                        modifier = Modifier.fillMaxWidth(),
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

            // ── 1. Name & Avatar ─────────────────────────────────────────────
            DarkSectionCard(title = "Name & Avatar", modifier = Modifier.staggered(0)) {
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

            // ── 2. Kategorien (eigene + Vorlagen zusammen) ───────────────────
            DarkSectionCard(title = "Kategorien  ·  $totalCategories ausgewählt", modifier = Modifier.staggered(1)) {

                // Custom input row
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

                // Custom category chips (animated)
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

                // Divider with label
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = ColorOutlineVariant)
                    Text(
                        "Vorlagen",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorOnSurfaceVariant,
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = ColorOutlineVariant)
                }
                Spacer(Modifier.height(12.dp))

                // Preset grid (3 columns) with shuffle fade
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.graphicsLayer { alpha = shuffleAlpha.value },
                ) {
                    visiblePresets.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            rowItems.forEach { category ->
                                val isSelected = category.id in selectedPresetIds
                                DarkCategorySelectCard(
                                    category = category,
                                    isSelected = isSelected,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedPresetIds = if (isSelected)
                                            selectedPresetIds - category.id
                                        else
                                            selectedPresetIds + category.id
                                    },
                                )
                            }
                            repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Shuffle button with fade animation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(GradientPrimary))
                        .clickable {
                            scope.launch {
                                // Fade out
                                shuffleAlpha.animateTo(0f, tween(150))
                                // Swap presets
                                val selectedOnes = PRESET_CATEGORIES.filter { it.id in selectedPresetIds }
                                val unselectedPool = PRESET_CATEGORIES.filter { it.id !in selectedPresetIds }.shuffled()
                                val fillCount = (VISIBLE_PRESET_COUNT - selectedOnes.size).coerceAtLeast(0)
                                visiblePresets = (selectedOnes + unselectedPool.take(fillCount)).shuffled()
                                // Fade in
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

            // ── Speed Bonus Hinweis ───────────────────────────────────────────
            GradientBorderCard(
                modifier = Modifier.fillMaxWidth().staggered(2),
                cornerRadius = 12.dp,
                borderColors = GradientGold,
                backgroundColor = Color(0xFF1A1A2E),
                borderWidth = 1.dp,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(Icons.Default.Bolt, null, modifier = Modifier.size(18.dp), tint = Color(0xFFFBBF24))
                    Text(
                        "Wer eine Kategorie als Erster fotografiert, bekommt +1 Schnelligkeitsbonus.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFBBF24).copy(alpha = 0.85f),
                        lineHeight = 17.sp,
                    )
                }
            }

            // ── 3. Spielzeit ─────────────────────────────────────────────────
            DarkSectionCard(title = "Spielzeit", modifier = Modifier.staggered(3)) {
                var isDragging by remember { mutableStateOf(false) }
                val bubbleScale = remember { Animatable(0f) }

                LaunchedEffect(isDragging) {
                    if (isDragging) {
                        bubbleScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                    } else {
                        delay(300)
                        bubbleScale.animateTo(0f, tween(200))
                    }
                }

                // Slider with value bubble
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val fraction = ((durationMinutes - 5f) / (60f - 5f)).coerceIn(0f, 1f)
                    val thumbRadius = 10.dp
                    val trackPadding = thumbRadius
                    val availableWidth = maxWidth - trackPadding * 2

                    // Value bubble above thumb
                    if (bubbleScale.value > 0f) {
                        Box(
                            modifier = Modifier
                                .offset(x = trackPadding + availableWidth * fraction - 20.dp)
                                .scale(bubbleScale.value)
                                .graphicsLayer { transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f) },
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ColorPrimary,
                                shadowElevation = 4.dp,
                            ) {
                                Text(
                                    "${durationMinutes.toInt()} Min",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                        }
                    }

                    Slider(
                        value = durationMinutes,
                        onValueChange = {
                            durationMinutes = it
                            isDragging = true
                        },
                        onValueChangeFinished = { isDragging = false },
                        valueRange = 5f..60f,
                        steps = 10,
                        colors = SliderDefaults.colors(
                            thumbColor = ColorPrimary,
                            activeTrackColor = ColorPrimary,
                            inactiveTrackColor = ColorSurfaceVariant,
                        ),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("5 Min", style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant)
                    Text("${durationMinutes.toInt()} Min", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = ColorPrimary)
                    Text("60 Min", style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant)
                }
            }

            // ── 4. Joker-Modus ───────────────────────────────────────────────
            DarkSectionCard(title = "Optionen", modifier = Modifier.staggered(4)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Style, null, modifier = Modifier.size(18.dp), tint = ColorPrimary)
                            Text(
                                "Joker-Modus",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorOnSurface,
                            )
                        }
                        Text(
                            "Jeder Spieler darf einmal ein Wildcard-Foto machen – mit eigenem Thema.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorOnSurfaceVariant,
                            lineHeight = 16.sp,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = jokerMode,
                        onCheckedChange = { jokerMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ColorPrimary,
                            uncheckedThumbColor = ColorOnSurfaceVariant,
                            uncheckedTrackColor = ColorSurfaceVariant,
                        ),
                    )
                }
                if (jokerMode) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ColorPrimaryContainer,
                        border = BorderStroke(1.dp, ColorPrimary.copy(alpha = 0.3f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Default.Lightbulb, null, modifier = Modifier.size(16.dp), tint = ColorPrimary)
                            Text(
                                "Spieler tippen auf den Joker-Button, geben ein Thema ein und machen ein Foto. Das Joker-Bild wird ganz normal abgestimmt.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorOnPrimaryContainer,
                                lineHeight = 16.sp,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
fun SelfiePicker(
    avatarBytes: ByteArray?,
    onTakePhoto: () -> Unit,
    onClear: () -> Unit,
) {
    val imageBitmap = remember(avatarBytes) { avatarBytes?.toImageBitmap() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (imageBitmap != null) {
            GradientBorderCard(
                modifier = Modifier.size(68.dp),
                cornerRadius = 34.dp,
                borderColors = GradientPrimary,
                backgroundColor = Color.Transparent,
                borderWidth = 2.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .clickable { onTakePhoto() },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(22.dp), tint = Color.White)
                    }
                }
            }
        } else {
            AnimatedGradientBox(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .clickable { onTakePhoto() },
                gradientColors = listOf(ColorSurfaceVariant, ColorOutline, ColorSurfaceVariant),
                durationMillis = 3000,
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(28.dp), tint = ColorOnSurfaceVariant)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (imageBitmap != null) "Selfie aufgenommen" else "Selfie aufnehmen",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = ColorOnSurface,
            )
            Text(
                if (imageBitmap != null) "Tippen zum Neuaufnehmen" else "Optional · wird als Profilbild angezeigt",
                style = MaterialTheme.typography.bodySmall,
                color = ColorOnSurfaceVariant,
            )
            if (imageBitmap != null) {
                TextButton(
                    onClick = onClear,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(24.dp),
                ) {
                    Text(
                        "Entfernen",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorError,
                    )
                }
            }
        }
    }
}

@Composable
private fun DarkSectionCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    GradientBorderCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        borderColors = GradientPrimary,
        backgroundColor = ColorSurface,
        borderWidth = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            AnimatedGradientText(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                gradientColors = GradientPrimary,
                durationMillis = 3000,
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DarkCategorySelectCard(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showInfo by remember { mutableStateOf(false) }

    // Scale bounce on selection change
    val scaleAnim = remember { Animatable(1f) }
    LaunchedEffect(isSelected) {
        scaleAnim.animateTo(1.12f, tween(100))
        scaleAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            containerColor = ColorSurface,
            icon = {
                Icon(
                    imageVector = getCategoryIcon(category.id),
                    contentDescription = null,
                    tint = if (isSelected) ColorPrimary else ColorOnSurfaceVariant,
                    modifier = Modifier.size(32.dp).rotate(getCategoryIconRotation(category.id)),
                )
            },
            title = {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorOnSurface,
                    textAlign = TextAlign.Center,
                )
            },
            text = if (category.description.isNotBlank()) {
                {
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorOnSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else null,
            confirmButton = {
                TextButton(onClick = { showInfo = false; onClick() }) {
                    Icon(
                        if (isSelected) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                        null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (isSelected) "Abwählen" else "Auswählen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text("Schließen")
                }
            },
        )
    }

    if (isSelected) {
        GradientBorderCard(
            modifier = modifier
                .scale(scaleAnim.value)
                .aspectRatio(0.85f)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showInfo = true },
                ),
            cornerRadius = 10.dp,
            borderColors = GradientPrimary,
            backgroundColor = ColorPrimaryContainer,
            borderWidth = 1.5.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = getCategoryIcon(category.id),
                    contentDescription = category.name,
                    modifier = Modifier.size(26.dp).rotate(getCategoryIconRotation(category.id)),
                    tint = ColorPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = ColorOnPrimaryContainer,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 12.sp,
                )
            }
        }
    } else {
        Card(
            modifier = modifier
                .scale(scaleAnim.value)
                .aspectRatio(0.85f)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showInfo = true },
                ),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = ColorSurfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = getCategoryIcon(category.id),
                    contentDescription = category.name,
                    modifier = Modifier.size(26.dp).rotate(getCategoryIconRotation(category.id)),
                    tint = ColorOnSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = ColorOnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 12.sp,
                )
            }
        }
    }
}
