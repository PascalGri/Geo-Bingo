package pg.geobingo.one.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.data.Category
import pg.geobingo.one.data.Player
import pg.geobingo.one.game.*

@Composable
fun ReviewScreen(gameState: GameState) {
    val reviewPlayer = gameState.reviewPlayer
    val isLastPlayer = gameState.reviewPlayerIndex >= gameState.players.size - 1

    // Local vote toggles for current player's captures
    var approvedIds by remember(gameState.reviewPlayerIndex) { mutableStateOf(setOf<String>()) }

    if (reviewPlayer == null) {
        gameState.currentScreen = Screen.RESULTS
        return
    }

    val captures = gameState.getPlayerCaptures(reviewPlayer.id)

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
                    Brush.verticalGradient(colors = listOf(Color(0xFF1B5E20), Color(0xFF2E7D32)))
                )
                .padding(top = 44.dp, bottom = 16.dp, start = 20.dp, end = 20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Abstimmung",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Spieler ${gameState.reviewPlayerIndex + 1} / ${gameState.players.size}",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(Modifier.height(12.dp))

                // Progress dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    gameState.players.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .size(if (index == gameState.reviewPlayerIndex) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index <= gameState.reviewPlayerIndex) Color.White
                                    else Color.White.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
            }
        }

        // Player info card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(reviewPlayer.color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        reviewPlayer.name.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        reviewPlayer.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                    Text(
                        "${captures.size} Kategorien gefunden",
                        fontSize = 13.sp,
                        color = Color(0xFF757575)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "${approvedIds.size}/${captures.size}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2E7D32)
                )
            }
        }

        // Instructions
        Text(
            "Bestätigt jede Kategorie, die ${reviewPlayer.name} wirklich gefunden hat:",
            fontSize = 13.sp,
            color = Color(0xFF555555),
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))

        // Captures list
        if (captures.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("😅", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${reviewPlayer.name} hat nichts gefunden",
                        fontSize = 15.sp,
                        color = Color(0xFF757575),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                captures.forEach { category ->
                    val approved = category.id in approvedIds
                    VoteCaptureRow(
                        category = category,
                        player = reviewPlayer,
                        isApproved = approved,
                        onToggle = {
                            approvedIds = if (approved) {
                                approvedIds - category.id
                            } else {
                                approvedIds + category.id
                            }
                        }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // Bottom button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    gameState.submitVotes(reviewPlayer.id, approvedIds)
                    if (isLastPlayer) {
                        gameState.currentScreen = Screen.RESULTS
                    } else {
                        gameState.reviewPlayerIndex++
                        approvedIds = emptySet()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLastPlayer) Color(0xFFFFB300) else Color(0xFF2E7D32),
                    contentColor = if (isLastPlayer) Color(0xFF1A3000) else Color.White
                ),
                shape = RoundedCornerShape(27.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = if (isLastPlayer) "🏆  Ergebnisse anzeigen" else "Weiter → ${
                        gameState.players.getOrNull(gameState.reviewPlayerIndex + 1)?.name ?: ""
                    }",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun VoteCaptureRow(
    category: Category,
    player: Player,
    isApproved: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isApproved) Color(0xFFE8F5E9) else Color.White
        ),
        border = if (isApproved) BorderStroke(1.5.dp, Color(0xFF4CAF50)) else BorderStroke(1.dp, Color(0xFFE0E0E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(player.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(category.emoji, fontSize = 26.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    category.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📍", fontSize = 11.sp)
                    Spacer(Modifier.width(2.dp))
                    Text("GPS gespeichert", fontSize = 11.sp, color = Color(0xFF9E9E9E))
                    Spacer(Modifier.width(6.dp))
                    Text("📸", fontSize = 11.sp)
                    Spacer(Modifier.width(2.dp))
                    Text("Foto aufgenommen", fontSize = 11.sp, color = Color(0xFF9E9E9E))
                }
            }

            // Approve toggle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isApproved) Color(0xFF4CAF50) else Color(0xFFF5F5F5))
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isApproved) "✓" else "?",
                    fontSize = 20.sp,
                    color = if (isApproved) Color.White else Color(0xFF9E9E9E),
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
