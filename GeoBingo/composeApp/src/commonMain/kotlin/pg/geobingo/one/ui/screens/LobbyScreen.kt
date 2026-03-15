package pg.geobingo.one.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.PlayerDto
import pg.geobingo.one.network.parseHexColor
import pg.geobingo.one.network.toPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(gameState: GameState) {
    val scope = rememberCoroutineScope()
    var isStarting by remember { mutableStateOf(false) }
    val gameId = gameState.gameId ?: return

    // Poll for new players and game status every 3 seconds
    LaunchedEffect(gameId) {
        while (true) {
            try {
                val players = GameRepository.getPlayers(gameId)
                gameState.lobbyPlayers = players

                if (!gameState.isHost) {
                    val game = GameRepository.getGameById(gameId)
                    if (game?.status == "running") {
                        val playerDtos = GameRepository.getPlayers(gameId)
                        gameState.players = playerDtos.map { it.toPlayer() }
                        gameState.captures = playerDtos.associate { it.id to emptySet() }
                        gameState.photos = playerDtos.associate { it.id to emptyMap() }
                        gameState.timeRemainingSeconds = gameState.gameDurationMinutes * 60
                        gameState.isGameRunning = true
                        gameState.currentPlayerIndex = playerDtos.indexOfFirst { it.id == gameState.myPlayerId }
                            .takeIf { it >= 0 } ?: 0
                        gameState.currentScreen = Screen.GAME
                        break
                    }
                }
            } catch (_: Exception) {}
            delay(3000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wartezimmer") },
                navigationIcon = {
                    IconButton(onClick = { gameState.resetGame() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Verlassen")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (gameState.isHost) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isStarting = true
                                    try {
                                        GameRepository.startGame(gameId)
                                        val playerDtos = GameRepository.getPlayers(gameId)
                                        gameState.players = playerDtos.map { it.toPlayer() }
                                        gameState.captures = playerDtos.associate { it.id to emptySet() }
                                        gameState.photos = playerDtos.associate { it.id to emptyMap() }
                                        gameState.timeRemainingSeconds = gameState.gameDurationMinutes * 60
                                        gameState.isGameRunning = true
                                        gameState.currentPlayerIndex = playerDtos.indexOfFirst { it.id == gameState.myPlayerId }
                                            .takeIf { it >= 0 } ?: 0
                                        gameState.currentScreen = Screen.GAME
                                    } catch (e: Exception) {
                                        isStarting = false
                                    }
                                }
                            },
                            enabled = gameState.lobbyPlayers.size >= 2 && !isStarting,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(27.dp)
                        ) {
                            if (isStarting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (gameState.lobbyPlayers.size < 2) "Mind. 2 Spieler nötig"
                                    else "Spiel starten (${gameState.lobbyPlayers.size} Spieler)",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))

                // Game code card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Rundencode",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            gameState.gameCode ?: "------",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 8.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Teile diesen Code mit deinen Freunden",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Spieler (${gameState.lobbyPlayers.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            items(gameState.lobbyPlayers) { player ->
                PlayerLobbyRow(player = player, isMe = player.id == gameState.myPlayerId)
            }

            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${gameState.selectedCategories.size} Kategorien · ${gameState.gameDurationMinutes} Min.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!gameState.isHost) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Warte auf den Host...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PlayerLobbyRow(player: PlayerDto, isMe: Boolean) {
    val color = parseHexColor(player.color)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    player.name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                player.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (isMe) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "Du",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
