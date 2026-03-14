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
import pg.geobingo.one.data.*
import pg.geobingo.one.game.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGameScreen(gameState: GameState) {
    var playerNameInput by remember { mutableStateOf("") }
    var selectedCategoryIds by remember { mutableStateOf(setOf<String>()) }
    var durationMinutes by remember { mutableStateOf(15f) }

    val canStart = gameState.players.size >= 2 && selectedCategoryIds.size >= 9

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
                            gameState.selectedCategories = PRESET_CATEGORIES.filter { it.id in selectedCategoryIds }
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
                            text = if (canStart) "Spiel starten" else "Mind. 2 Spieler & 9 Kategorien",
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

            // --- Categories ---
            SectionCard(title = "Kategorien  ${selectedCategoryIds.size}/24") {
                Text(
                    "Wähle mindestens 9 Kategorien für dein Bingo-Feld",
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
                        val isSelected = category.id in selectedCategoryIds
                        CategorySelectCard(
                            category = category,
                            isSelected = isSelected,
                            onClick = {
                                selectedCategoryIds = if (isSelected) {
                                    selectedCategoryIds - category.id
                                } else {
                                    selectedCategoryIds + category.id
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
private fun CategorySelectCard(category: Category, isSelected: Boolean, onClick: () -> Unit) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

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
            Text(text = category.emoji, fontSize = 24.sp)
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
