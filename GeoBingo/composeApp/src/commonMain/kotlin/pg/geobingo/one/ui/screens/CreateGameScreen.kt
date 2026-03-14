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
import androidx.compose.ui.graphics.Brush
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8F5E9))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1B5E20), Color(0xFF2E7D32))
                    )
                )
                .padding(top = 48.dp, bottom = 20.dp, start = 20.dp, end = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { gameState.currentScreen = Screen.HOME },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text("← Zurück", fontSize = 14.sp)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "Neues Spiel",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(80.dp))
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Players ---
            SectionCard(title = "👥 Spieler (${gameState.players.size}/8)") {
                // Input row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = playerNameInput,
                        onValueChange = { playerNameInput = it },
                        placeholder = { Text("Spielername", fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2E7D32),
                            unfocusedBorderColor = Color(0xFFBDBDBD)
                        )
                    )
                    Button(
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (gameState.players.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Füge mindestens 2 Spieler hinzu",
                            color = Color(0xFF9E9E9E),
                            fontSize = 13.sp
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
            SectionCard(title = "🗂️ Kategorien (${selectedCategoryIds.size}/24 gewählt, min. 9)") {
                Text(
                    "Wähle mindestens 9 Kategorien für dein Bingo-Feld",
                    color = Color(0xFF666666),
                    fontSize = 12.sp
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
            SectionCard(title = "⏱️ Spielzeit: ${durationMinutes.toInt()} Minuten") {
                Slider(
                    value = durationMinutes,
                    onValueChange = { durationMinutes = it },
                    valueRange = 5f..60f,
                    steps = 10,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF2E7D32),
                        activeTrackColor = Color(0xFF2E7D32),
                        inactiveTrackColor = Color(0xFFA5D6A7)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("5 Min", color = Color(0xFF9E9E9E), fontSize = 11.sp)
                    Text("60 Min", color = Color(0xFF9E9E9E), fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        // Start button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    gameState.selectedCategories = PRESET_CATEGORIES.filter { it.id in selectedCategoryIds }
                    gameState.gameDurationMinutes = durationMinutes.toInt()
                    gameState.startGame()
                },
                enabled = canStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB300),
                    contentColor = Color(0xFF1A3000),
                    disabledContainerColor = Color(0xFFE0E0E0),
                    disabledContentColor = Color(0xFF9E9E9E)
                ),
                shape = RoundedCornerShape(28.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = if (canStart) "🚀  SPIEL STARTEN" else "Mindestens 2 Spieler & 9 Kategorien",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun PlayerChip(player: Player, onRemove: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(player.color.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .border(1.dp, player.color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(player.color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                player.name.take(1).uppercase(),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(player.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF212121))
        Spacer(Modifier.width(4.dp))
        TextButton(
            onClick = onRemove,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(24.dp)
        ) {
            Text("✕", fontSize = 11.sp, color = Color(0xFF9E9E9E))
        }
    }
}

@Composable
private fun CategorySelectCard(category: Category, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) Color(0xFF2E7D32) else Color(0xFFF5F5F5)
    val textColor = if (isSelected) Color.White else Color(0xFF212121)
    val subColor = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF757575)

    Card(
        onClick = onClick,
        modifier = Modifier
            .aspectRatio(0.85f)
            .fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        border = if (isSelected) BorderStroke(2.dp, Color(0xFF1B5E20)) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isSelected) {
                Text("✓", fontSize = 10.sp, color = Color.White.copy(alpha = 0.9f))
            }
            Text(text = category.emoji, fontSize = 26.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                text = category.name,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 12.sp
            )
        }
    }
}
