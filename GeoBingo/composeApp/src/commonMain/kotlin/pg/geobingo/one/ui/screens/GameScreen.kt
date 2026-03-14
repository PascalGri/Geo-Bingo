package pg.geobingo.one.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import kotlinx.coroutines.delay
import pg.geobingo.one.data.Category
import pg.geobingo.one.data.Player
import pg.geobingo.one.game.*

@Composable
fun GameScreen(gameState: GameState) {
    // Timer tick
    LaunchedEffect(Unit) {
        while (gameState.isGameRunning && gameState.timeRemainingSeconds > 0) {
            delay(1000L)
            if (gameState.isGameRunning) {
                gameState.timeRemainingSeconds--
                if (gameState.timeRemainingSeconds <= 0) {
                    gameState.endGame()
                }
            }
        }
    }

    val isLow = gameState.timeRemainingSeconds in 1..60
    val timerColor by animateColorAsState(
        targetValue = if (isLow) Color(0xFFD32F2F) else Color.White,
        animationSpec = tween(500)
    )
    val timerBg by animateColorAsState(
        targetValue = if (isLow) Color(0xFFFFEBEE) else Color(0xFF1B5E20),
        animationSpec = tween(500)
    )

    val currentPlayer = gameState.currentPlayer

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8F5E9))
    ) {
        // Top bar with timer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1B5E20), Color(0xFF2E7D32))
                    )
                )
                .padding(top = 44.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                // Timer
                Box(
                    modifier = Modifier
                        .background(timerBg, RoundedCornerShape(16.dp))
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⏱ ${gameState.formatTime(gameState.timeRemainingSeconds)}",
                        color = timerColor,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Player tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gameState.players.forEachIndexed { index, player ->
                        val isActive = index == gameState.currentPlayerIndex
                        val captured = gameState.captures[player.id]?.size ?: 0
                        PlayerTab(
                            player = player,
                            isActive = isActive,
                            captureCount = captured,
                            onClick = { gameState.currentPlayerIndex = index }
                        )
                    }
                }
            }
        }

        // Info bar
        if (currentPlayer != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(currentPlayer.color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            currentPlayer.name.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            currentPlayer.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                        val count = gameState.captures[currentPlayer.id]?.size ?: 0
                        Text(
                            "$count/${gameState.selectedCategories.size} gefunden",
                            fontSize = 11.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }

                Button(
                    onClick = { gameState.endGame() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Spiel beenden", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = Color(0xFFE0E0E0))
        }

        // Bingo grid
        if (currentPlayer != null) {
            val cols = if (gameState.selectedCategories.size <= 9) 3 else 4
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols),
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(gameState.selectedCategories) { category ->
                    val captured = gameState.isCaptured(currentPlayer.id, category.id)
                    BingoCategoryCard(
                        category = category,
                        isCaptured = captured,
                        playerColor = currentPlayer.color,
                        onClick = {
                            gameState.toggleCapture(currentPlayer.id, category.id)
                        }
                    )
                }
            }

            // Navigation between players
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (gameState.currentPlayerIndex > 0) {
                    OutlinedButton(
                        onClick = { gameState.currentPlayerIndex-- },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32)),
                        border = BorderStroke(1.dp, Color(0xFF2E7D32))
                    ) {
                        Text("◀ Vorheriger")
                    }
                }
                if (gameState.currentPlayerIndex < gameState.players.size - 1) {
                    Button(
                        onClick = { gameState.currentPlayerIndex++ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("Nächster ▶")
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerTab(player: Player, isActive: Boolean, captureCount: Int, onClick: () -> Unit) {
    val bg = if (isActive) Color.White else Color.White.copy(alpha = 0.2f)
    val textColor = if (isActive) player.color else Color.White
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(player.color),
                contentAlignment = Alignment.Center
            ) {
                Text(player.name.take(1).uppercase(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(5.dp))
            Text(player.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textColor)
            if (captureCount > 0) {
                Spacer(Modifier.width(4.dp))
                Text("($captureCount)", fontSize = 11.sp, color = textColor.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun BingoCategoryCard(
    category: Category,
    isCaptured: Boolean,
    playerColor: Color,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (isCaptured) playerColor else Color.White,
        animationSpec = tween(300)
    )
    val textColor = if (isCaptured) Color.White else Color(0xFF212121)
    val subColor = if (isCaptured) Color.White.copy(alpha = 0.8f) else Color(0xFF757575)

    Card(
        onClick = onClick,
        modifier = Modifier
            .aspectRatio(0.9f)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCaptured) 6.dp else 2.dp),
        border = if (isCaptured) BorderStroke(2.dp, playerColor.copy(alpha = 0.5f)) else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isCaptured) {
                    Text("✓", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                    Spacer(Modifier.height(2.dp))
                }
                Text(text = category.emoji, fontSize = 28.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = category.name,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 13.sp
                )
                if (isCaptured) {
                    Spacer(Modifier.height(2.dp))
                    Text("📍 Gefunden!", fontSize = 8.sp, color = subColor)
                }
            }
        }
    }
}
