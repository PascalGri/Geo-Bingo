package pg.geobingo.one.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pg.geobingo.one.data.*
import pg.geobingo.one.game.*
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.generateCode
import pg.geobingo.one.network.toCategory
import pg.geobingo.one.network.toHex
import pg.geobingo.one.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGameScreen(gameState: GameState) {
    var hostNameInput by remember { mutableStateOf("") }
    var customNameInput by remember { mutableStateOf("") }
    var customCategories by remember { mutableStateOf(listOf<Category>()) }
    var customCategoryCounter by remember { mutableStateOf(0) }
    var selectedPresetIds by remember { mutableStateOf(setOf<String>()) }
    var durationMinutes by remember { mutableStateOf(15f) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val totalCategories = customCategories.size + selectedPresetIds.size
    val canAddMore = totalCategories < 10
    val canStart = hostNameInput.trim().isNotEmpty() && totalCategories in 2..10

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
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Zurück",
                            tint = ColorPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorSurface,
                ),
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = ColorSurface,
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    if (errorMessage != null) {
                        Text(
                            errorMessage!!,
                            color = ColorError,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    GradientButton(
                        text = when {
                            hostNameInput.trim().isEmpty() -> "Name eingeben"
                            totalCategories < 2 -> "Mind. 2 Kategorien wählen"
                            else -> "Runde erstellen ($totalCategories Kategorien)"
                        },
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                try {
                                    val presets = PRESET_CATEGORIES.filter { it.id in selectedPresetIds }
                                    val allCategories = customCategories + presets
                                    val code = generateCode()
                                    val game = GameRepository.createGame(code, durationMinutes.toInt() * 60)
                                    val colorIndex = 0
                                    val hostColor = PLAYER_COLORS[colorIndex].toHex()
                                    val hostDto = GameRepository.addPlayer(game.id, hostNameInput.trim(), hostColor)
                                    val categoryDtos = GameRepository.addCategories(game.id, allCategories)
                                    gameState.gameId = game.id
                                    gameState.gameCode = game.code
                                    gameState.isHost = true
                                    gameState.myPlayerId = hostDto.id
                                    gameState.gameDurationMinutes = durationMinutes.toInt()
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
                        leadingIcon = if (isLoading) ({
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        }) else null,
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
            // --- Host Name ---
            DarkSectionCard(title = "Dein Name") {
                OutlinedTextField(
                    value = hostNameInput,
                    onValueChange = { if (it.length <= 20) hostNameInput = it },
                    placeholder = {
                        Text("z.B. Pascal", style = MaterialTheme.typography.bodyMedium, color = ColorOnSurfaceVariant)
                    },
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
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = ColorPrimary)
                    },
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Andere Spieler treten über einen Code bei.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorOnSurfaceVariant,
                )
            }

            // --- Custom Categories ---
            DarkSectionCard(title = "Eigene Kategorien  ${customCategories.size}") {
                Text(
                    "Erstelle eigene Kategorien für dein Bingo-Feld (${totalCategories}/10 gesamt)",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorOnSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = customNameInput,
                        onValueChange = { if (it.length <= 30) customNameInput = it },
                        placeholder = {
                            Text("z.B. Rotes Auto", style = MaterialTheme.typography.bodyMedium, color = ColorOnSurfaceVariant)
                        },
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
                                if (customNameInput.trim().isNotEmpty() && canAddMore)
                                    Brush.linearGradient(GradientPrimary)
                                else
                                    Brush.linearGradient(listOf(ColorSurfaceVariant, ColorSurfaceVariant))
                            )
                            .clickable {
                                val name = customNameInput.trim()
                                if (name.isNotEmpty() && canAddMore) {
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
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Hinzufügen",
                            modifier = Modifier.size(20.dp),
                            tint = Color.White,
                        )
                    }
                }

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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(cat.id),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = ColorPrimary,
                                    )
                                    Spacer(Modifier.width(10.dp))
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
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Entfernen",
                                            modifier = Modifier.size(14.dp),
                                            tint = ColorOnSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (!canAddMore) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Maximum von 10 Kategorien erreicht",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorError,
                    )
                }
            }

            // --- Preset Categories ---
            DarkSectionCard(title = "Aus Vorlagen wählen  ${selectedPresetIds.size}") {
                Text(
                    "Wähle optional aus fertigen Kategorien",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorOnSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(420.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    userScrollEnabled = false,
                ) {
                    items(PRESET_CATEGORIES) { category ->
                        val isSelected = category.id in selectedPresetIds
                        val isDisabled = !isSelected && !canAddMore
                        DarkCategorySelectCard(
                            category = category,
                            isSelected = isSelected,
                            isDisabled = isDisabled,
                            onClick = {
                                if (!isDisabled || isSelected) {
                                    selectedPresetIds = if (isSelected) {
                                        selectedPresetIds - category.id
                                    } else {
                                        selectedPresetIds + category.id
                                    }
                                }
                            },
                        )
                    }
                }
            }

            // --- Duration ---
            DarkSectionCard(title = "Spielzeit  ${durationMinutes.toInt()} Min.") {
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

            Spacer(Modifier.height(4.dp))
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

@Composable
private fun DarkCategorySelectCard(
    category: Category,
    isSelected: Boolean,
    isDisabled: Boolean,
    onClick: () -> Unit,
) {
    val bg = when {
        isSelected -> ColorPrimaryContainer
        isDisabled -> ColorSurfaceVariant.copy(alpha = 0.4f)
        else -> ColorSurfaceVariant
    }
    val contentColor = when {
        isSelected -> ColorOnPrimaryContainer
        isDisabled -> ColorOnSurfaceVariant.copy(alpha = 0.4f)
        else -> ColorOnSurfaceVariant
    }

    Card(
        onClick = onClick,
        modifier = Modifier.aspectRatio(0.85f).fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
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
                modifier = Modifier.size(26.dp),
                tint = if (isSelected) ColorPrimary else contentColor,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 12.sp,
            )
        }
    }
}
