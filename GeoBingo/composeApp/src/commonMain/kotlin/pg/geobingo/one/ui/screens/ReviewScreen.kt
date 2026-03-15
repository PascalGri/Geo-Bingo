package pg.geobingo.one.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pg.geobingo.one.data.Category
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.network.CaptureDto
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.platform.toImageBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(gameState: GameState) {
    val scope = rememberCoroutineScope()
    val gameId = gameState.gameId ?: return
    val categories = gameState.selectedCategories
    val totalPlayers = gameState.players.size
    val myPlayerId = gameState.myPlayerId ?: return

    val categoryIndex = gameState.reviewCategoryIndex

    // Load all captures once
    LaunchedEffect(gameId) {
        try {
            gameState.allCaptures = GameRepository.getCaptures(gameId)
        } catch (_: Exception) {}
    }

    // Poll for category index changes and "results" status
    LaunchedEffect(gameId) {
        while (true) {
            try {
                val game = GameRepository.getGameById(gameId)
                val newIndex = game?.review_category_index ?: 0
                if (newIndex != gameState.reviewCategoryIndex) {
                    gameState.reviewCategoryIndex = newIndex
                    gameState.hasSubmittedCurrentCategory = false
                    gameState.categoryVotes = emptyMap()
                }
                if (game?.status == "results") {
                    gameState.allVotes = GameRepository.getVotes(gameId)
                    gameState.currentScreen = Screen.RESULTS
                    break
                }
            } catch (_: Exception) {}
            delay(2500)
        }
    }

    if (categoryIndex >= categories.size) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentCategory = categories[categoryIndex]
    val capturesForCategory = gameState.allCaptures.filter { it.category_id == currentCategory.id }
    val othersCaptures = capturesForCategory.filter { it.player_id != myPlayerId }

    if (gameState.hasSubmittedCurrentCategory) {
        WaitingForOthersScreen(
            categoryName = currentCategory.name,
            categoryIndex = categoryIndex,
            totalCategories = categories.size
        )
    } else {
        VotingScreen(
            gameState = gameState,
            currentCategory = currentCategory,
            categoryIndex = categoryIndex,
            totalCategories = categories.size,
            othersCaptures = othersCaptures,
            totalPlayers = totalPlayers,
            onSubmit = { votes ->
                scope.launch {
                    try {
                        GameRepository.submitCategoryVotes(
                            gameId = gameId,
                            voterId = myPlayerId,
                            categoryId = currentCategory.id,
                            votes = votes.toList()
                        )
                        gameState.hasSubmittedCurrentCategory = true
                        gameState.categoryVotes = emptyMap()

                        // Check if all players submitted → advance
                        val submissionCount = GameRepository.getVoteSubmissionCount(gameId, currentCategory.id)
                        if (submissionCount >= totalPlayers) {
                            val nextIndex = categoryIndex + 1
                            if (nextIndex >= categories.size) {
                                GameRepository.setGameStatus(gameId, "results")
                            } else {
                                GameRepository.setReviewCategoryIndex(gameId, nextIndex)
                                gameState.reviewCategoryIndex = nextIndex
                                gameState.hasSubmittedCurrentCategory = false
                                gameState.categoryVotes = emptyMap()
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VotingScreen(
    gameState: GameState,
    currentCategory: Category,
    categoryIndex: Int,
    totalCategories: Int,
    othersCaptures: List<CaptureDto>,
    totalPlayers: Int,
    onSubmit: (Map<String, Boolean>) -> Unit
) {
    val scope = rememberCoroutineScope()
    val gameId = gameState.gameId ?: return

    // votes: targetPlayerId -> approved
    var votes by remember(categoryIndex) { mutableStateOf(mapOf<String, Boolean>()) }
    var photoCache by remember { mutableStateOf(mapOf<String, ImageBitmap?>()) }

    // Load photos for each capture
    LaunchedEffect(othersCaptures) {
        othersCaptures.forEach { capture ->
            if (capture.player_id !in photoCache) {
                scope.launch {
                    val bytes = GameRepository.downloadPhoto(gameId, capture.player_id, capture.category_id)
                    photoCache = photoCache + (capture.player_id to bytes?.toImageBitmap())
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Abstimmung", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Kategorie ${categoryIndex + 1} / $totalCategories", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Button(
                        onClick = { onSubmit(votes) },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(27.dp)
                    ) {
                        Text(
                            "Abstimmung abschicken",
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
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(currentCategory.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Haben andere Spieler das wirklich gefunden?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                }
            }

            if (othersCaptures.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("\uD83D\uDE05", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Niemand hat diese Kategorie fotografiert", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            } else {
                othersCaptures.forEach { capture ->
                    val player = gameState.players.find { it.id == capture.player_id }
                    val photo = photoCache[capture.player_id]
                    val isApproved = votes[capture.player_id]

                    PlayerPhotoVoteCard(
                        playerName = player?.name ?: "Spieler",
                        playerColor = player?.color ?: Color.Gray,
                        photo = photo,
                        isApproved = isApproved,
                        onVote = { approved ->
                            votes = votes + (capture.player_id to approved)
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PlayerPhotoVoteCard(
    playerName: String,
    playerColor: Color,
    photo: ImageBitmap?,
    isApproved: Boolean?,
    onVote: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (isApproved) {
                true -> MaterialTheme.colorScheme.primaryContainer
                false -> MaterialTheme.colorScheme.errorContainer
                null -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Player name row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(playerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(playerName.take(1).uppercase(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Text(playerName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }

            // Photo
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (photo != null) {
                    Image(bitmap = photo, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.height(8.dp))
                        Text("Foto l\u00e4dt...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Vote buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Reject
                OutlinedButton(
                    onClick = { onVote(false) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isApproved == false) MaterialTheme.colorScheme.error.copy(alpha = 0.1f) else Color.Transparent
                    ),
                    border = BorderStroke(if (isApproved == false) 2.dp else 1.dp, if (isApproved == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Ablehnen", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Nein", color = MaterialTheme.colorScheme.error)
                }
                // Approve
                Button(
                    onClick = { onVote(true) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isApproved == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isApproved == true) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Best\u00e4tigen", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ja")
                }
            }
        }
    }
}

@Composable
private fun WaitingForOthersScreen(
    categoryName: String,
    categoryIndex: Int,
    totalCategories: Int
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Text("\u2705 Abgestimmt!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Warte auf andere Spieler...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Kategorie ${categoryIndex + 1} / $totalCategories: $categoryName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
