package pg.geobingo.one.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import pg.geobingo.one.data.*
import pg.geobingo.one.game.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGameScreen(gameState: GameState) {
    var playerNameInput by remember { mutableStateOf("") }
    var customNameInput by remember { mutableStateOf("") }
    var customCategories by remember { mutableStateOf(listOf<Category>()) }
    var customCategoryCounter by remember { mutableStateOf(0) }
    var selectedPresetIds by remember { mutableStateOf(setOf<String>()) }
    var durationMinutes by remember { mutableStateOf(15f) }

    val totalCategories = customCategories.size + selectedPresetIds.size
    val canAddMore = totalCategories < 10
    val canStart = gameState.players.size >= 2 && totalCategories in 2..10

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Neues Spiel") },
                navigationIcon = {
                    TextButton(onClick = { gameState.currentScreen = Screen.HOME }) {
                        Text("←")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Button(
                        onClick = {
                            val presets = PRESET_CATEGORIES.filter { it.id in selectedPresetIds }
                            gameState.selectedCategories = customCategories + presets
                            gameState.gameDurationMinutes = durationMinutes.toInt()
                            gameState.startGame()
                        },
                        enabled = canStart,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = when {
                                gameState.players.size < 2 -> "Mind. 2 Spieler hinzufügen"
                                totalCategories < 2 -> "Mind. 2 Kategorien wählen"
                                else -> "Spiel starten ($totalCategories Kategorien)"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Players ---
            SectionCard(title = "Spieler  ${gameState.players.size}/8") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = playerNameInput,
                        onValueChange = { playerNameInput = it },
                        placeholder = { Text("Name eingeben", style = MaterialTheme.typography.bodyMedium) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    FilledTonalButton(
                        onClick = {
                            val name = playerNameInput.trim()
                            if (name.isNotEmpty() && gameState.players.size < 8) {
                                val colorIndex = gameState.players.size % PLAYER_COLORS.size
                                val newPlayer = Player(
                                    id = "player_${gameState.players.size}_${name.hashCode()}",
                                    name = name,
                                    color = PLAYER_COLORS[colorIndex]
                                )
                                gameState.players = gameState.players + newPlayer
                                playerNameInput = ""
                            }
                        },
                        enabled = playerNameInput.trim().isNotEmpty() && gameState.players.size < 8,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (gameState.players.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Füge mindestens 2 Spieler hinzu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(gameState.players) { player ->
                            PlayerChip(player = player, onRemove = {
                                gameState.players = gameState.players.filter { it.id != player.id }
                            })
                        }
                    }
                }
            }

            // --- Custom Categories ---
            SectionCard(title = "Eigene Kategorien  ${customCategories.size}") {
                Text(
                    "Erstelle eigene Kategorien für dein Bingo-Feld (${totalCategories}/10 gesamt)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customNameInput,
                        onValueChange = { if (it.length <= 30) customNameInput = it },
                        placeholder = { Text("z.B. Rotes Auto", style = MaterialTheme.typography.bodyMedium) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    FilledTonalButton(
                        onClick = {
                            val name = customNameInput.trim()
                            if (name.isNotEmpty() && canAddMore) {
                                customCategories = customCategories + Category(
                                    id = "custom_$customCategoryCounter",
                                    name = name,
                                    emoji = "custom"
                                )
                                customCategoryCounter++
                                customNameInput = ""
                            }
                        },
                        enabled = customNameInput.trim().isNotEmpty() && canAddMore,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Hinzufügen", modifier = Modifier.size(18.dp))
                    }
                }

                if (customCategories.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        customCategories.forEach { cat ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(cat.id),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        cat.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    IconButton(
                                        onClick = { customCategories = customCategories.filter { it.id != cat.id } },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Entfernen",
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // --- Preset Categories ---
            SectionCard(title = "Aus Vorlagen wählen  ${selectedPresetIds.size}") {
                Text(
                    "Wähle optional aus fertigen Kategorien",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(420.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    userScrollEnabled = false
                ) {
                    items(PRESET_CATEGORIES) { category ->
                        val isSelected = category.id in selectedPresetIds
                        val isDisabled = !isSelected && !canAddMore
                        CategorySelectCard(
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
                            }
                        )
                    }
                }
            }

            // --- Duration ---
            SectionCard(title = "Spielzeit  ${durationMinutes.toInt()} Min.") {
                Slider(
                    value = durationMinutes,
                    onValueChange = { durationMinutes = it },
                    valueRange = 5f..60f,
                    steps = 10,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("5 Min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("60 Min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun PlayerChip(player: Player, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = player.color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, player.color.copy(alpha = 0.30f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Box(
                modifier = Modifier.size(20.dp).clip(CircleShape).background(player.color),
                contentAlignment = Alignment.Center
            ) {
                Text(player.name.take(1).uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(6.dp))
            Text(player.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(2.dp))
            TextButton(
                onClick = onRemove,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(24.dp)
            ) {
                Text("✕", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CategorySelectCard(category: Category, isSelected: Boolean, isDisabled: Boolean, onClick: () -> Unit) {
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isDisabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        isDisabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        onClick = onClick,
        modifier = Modifier.aspectRatio(0.85f).fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp),
        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = getCategoryIcon(category.id),
                contentDescription = category.name,
                modifier = Modifier.size(26.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else contentColor
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
                lineHeight = 12.sp
            )
        }
    }
}
