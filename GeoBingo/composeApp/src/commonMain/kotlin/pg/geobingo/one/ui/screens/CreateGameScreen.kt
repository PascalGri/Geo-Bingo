package pg.geobingo.one.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
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

    val totalCategories = customCategories.size + selectedPresetIds.size
    val canStart = hostNameInput.trim().isNotEmpty() && totalCategories >= 2

    SystemBackHandler { gameState.currentScreen = Screen.HOME }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Neues Spiel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorOnSurface,
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
            Surface(shadowElevation = 8.dp, color = ColorSurface) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    if (errorMessage != null) {
                        Text(
                            errorMessage!!,
                            color = ColorError,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── 1. Name & Avatar ─────────────────────────────────────────────
            DarkSectionCard(title = "Name & Avatar") {
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
            DarkSectionCard(title = "Kategorien  ·  $totalCategories ausgewählt") {

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

                // Custom category chips
                if (customCategories.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        customCategories.forEach { cat ->
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

                // Preset grid (3 columns)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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

                // Shuffle button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(GradientPrimary))
                        .clickable {
                            // Keep already-selected ones visible, fill rest with new suggestions
                            val selectedOnes = PRESET_CATEGORIES.filter { it.id in selectedPresetIds }
                            val unselectedPool = PRESET_CATEGORIES.filter { it.id !in selectedPresetIds }.shuffled()
                            val fillCount = (VISIBLE_PRESET_COUNT - selectedOnes.size).coerceAtLeast(0)
                            visiblePresets = (selectedOnes + unselectedPool.take(fillCount)).shuffled()
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
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1A1A2E),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.35f)),
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
            DarkSectionCard(title = "Spielzeit  ·  ${durationMinutes.toInt()} Min.") {
                Slider(
                    value = durationMinutes,
                    onValueChange = { durationMinutes = it },
                    valueRange = 5f..60f,
                    steps = 10,
                    colors = SliderDefaults.colors(
                        thumbColor = ColorPrimary,
                        activeTrackColor = ColorPrimary,
                        inactiveTrackColor = ColorSurfaceVariant,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("5 Min", style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant)
                    Text("60 Min", style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant)
                }
            }

            // ── 4. Joker-Modus ───────────────────────────────────────────────
            DarkSectionCard(title = "Optionen") {
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
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(if (imageBitmap != null) Color.Transparent else ColorSurfaceVariant)
                .clickable { onTakePhoto() },
            contentAlignment = Alignment.Center,
        ) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                // Dark overlay with camera icon for retake
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(22.dp), tint = Color.White)
                }
            } else {
                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(28.dp), tint = ColorOnSurfaceVariant)
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
private fun DarkSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        border = BorderStroke(1.dp, ColorOutlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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

    Card(
        modifier = modifier
            .aspectRatio(0.85f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showInfo = true },
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ColorPrimaryContainer else ColorSurfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (isSelected) BorderStroke(1.5.dp, ColorPrimary.copy(alpha = 0.7f)) else null,
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
                tint = if (isSelected) ColorPrimary else ColorOnSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) ColorOnPrimaryContainer else ColorOnSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 12.sp,
            )
        }
    }
}
