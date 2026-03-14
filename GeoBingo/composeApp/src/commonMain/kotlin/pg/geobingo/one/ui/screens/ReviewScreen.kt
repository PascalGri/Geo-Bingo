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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.data.Category
import pg.geobingo.one.data.Player
import pg.geobingo.one.game.*
import pg.geobingo.one.platform.toImageBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(gameState: GameState) {
    val reviewPlayer = gameState.reviewPlayer
    val isLastPlayer = gameState.reviewPlayerIndex >= gameState.players.size - 1

    var approvedIds by remember(gameState.reviewPlayerIndex) { mutableStateOf(setOf<String>()) }

    if (reviewPlayer == null) {
        gameState.currentScreen = Screen.RESULTS
        return
    }

    val captures = gameState.getPlayerCaptures(reviewPlayer.id)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Abstimmung",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Spieler ${gameState.reviewPlayerIndex + 1} / ${gameState.players.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Progress dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        gameState.players.forEachIndexed { index, _ ->
                            Box(
                                modifier = Modifier
                                    .size(if (index == gameState.reviewPlayerIndex) 8.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index <= gameState.reviewPlayerIndex)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
                            gameState.submitVotes(reviewPlayer.id, approvedIds)
                            if (isLastPlayer) {
                                gameState.currentScreen = Screen.RESULTS
                            } else {
                                gameState.reviewPlayerIndex++
                                approvedIds = emptySet()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = if (isLastPlayer) "Ergebnisse anzeigen"
                            else "Weiter → ${gameState.players.getOrNull(gameState.reviewPlayerIndex + 1)?.name ?: ""}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Player info
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(46.dp).clip(CircleShape).background(reviewPlayer.color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            reviewPlayer.name.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            reviewPlayer.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${captures.size} Kategorien gefunden",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "${approvedIds.size}/${captures.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                "Bestätigt die Kategorien, die ${reviewPlayer.name} wirklich gefunden hat:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                textAlign = TextAlign.Center
            )

            if (captures.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("😅", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${reviewPlayer.name} hat nichts gefunden",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    captures.forEach { category ->
                        val approved = category.id in approvedIds
                        val photoBytes = gameState.getPhoto(reviewPlayer.id, category.id)
                        val thumbnail: ImageBitmap? = remember(photoBytes) { photoBytes?.toImageBitmap() }
                        VoteCaptureRow(
                            category = category,
                            player = reviewPlayer,
                            isApproved = approved,
                            thumbnail = thumbnail,
                            onToggle = {
                                approvedIds = if (approved) approvedIds - category.id else approvedIds + category.id
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun VoteCaptureRow(
    category: Category,
    player: Player,
    isApproved: Boolean,
    thumbnail: ImageBitmap?,
    onToggle: () -> Unit
) {
    val containerColor = if (isApproved) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val borderColor = if (isApproved) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (isApproved) 1.5.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo or emoji
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(player.color.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(category.emoji, fontSize = 26.sp)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (thumbnail != null) "📸 Foto vorhanden" else "Kein Foto",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (thumbnail != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Approve toggle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isApproved) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isApproved) "✓" else "✕",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isApproved) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
