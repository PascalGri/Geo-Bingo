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
import pg.geobingo.one.network.GameRealtimeManager
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.PlayerDto
import pg.geobingo.one.network.parseHexColor
import pg.geobingo.one.network.toPlayer
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.ui.theme.PlayerAvatarViewRaw

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(gameState: GameState) {
    val scope = rememberCoroutineScope()
    var isStarting by remember { mutableStateOf(false) }
    val gameId = gameState.gameId ?: return
    val realtime = remember(gameId) { GameRealtimeManager(gameId) }

    // Lobby auto-close timeout (host only): 5 min without a second player joining
    var lobbyTimeoutSeconds by remember { mutableStateOf(300) }
    LaunchedEffect(gameId) {
        if (!gameState.isHost) return@LaunchedEffect
        while (lobbyTimeoutSeconds > 0) {
            delay(1000)
            if (gameState.lobbyPlayers.size >= 2) return@LaunchedEffect // second player joined → cancel
            lobbyTimeoutSeconds--
        }
        try { GameRepository.setGameStatus(gameId, "closed") } catch (_: Exception) {}
        gameState.resetGame()
    }

    // Initial player load
    LaunchedEffect(gameId) {
        try { gameState.lobbyPlayers = GameRepository.getPlayers(gameId) } catch (_: Exception) {}
    }

    // Realtime: new player joined
    LaunchedEffect(gameId) {
        realtime.playerInserts.collect {
            try { gameState.lobbyPlayers = GameRepository.getPlayers(gameId) } catch (_: Exception) {}
        }
    }

    // Realtime: game status changed (guests only)
    LaunchedEffect(gameId) {
        if (!gameState.isHost) {
            realtime.gameUpdates.collect { game ->
                when (game.status) {
                    "running" -> {
                        val playerDtos = GameRepository.getPlayers(gameId)
                        gameState.players = playerDtos.map { it.toPlayer() }
                        gameState.captures = playerDtos.associate { it.id to emptySet() }
                        gameState.photos = playerDtos.associate { it.id to emptyMap() }
                        gameState.timeRemainingSeconds = gameState.gameDurationMinutes * 60
                        gameState.isGameRunning = true
                        gameState.currentPlayerIndex = playerDtos.indexOfFirst { it.id == gameState.myPlayerId }
                            .takeIf { it >= 0 } ?: 0
                        gameState.currentScreen = Screen.GAME
                    }
                    "closed" -> {
                        gameState.pendingToast = "Der Host hat die Lobby geschlossen."
                        gameState.resetGame()
                    }
                }
            }
        }
    }

    // Subscribe Realtime channel + fallback poll every 15s
    LaunchedEffect(gameId) {
        try { realtime.subscribe() } catch (_: Exception) {}
        // Fallback polling in case Realtime misses an event
        while (true) {
            delay(3_000)
            try {
                gameState.lobbyPlayers = GameRepository.getPlayers(gameId)
                if (!gameState.isHost) {
                    val game = GameRepository.getGameById(gameId)
                    when (game?.status) {
                        "running" -> {
                            val playerDtos = GameRepository.getPlayers(gameId)
                            gameState.players = playerDtos.map { it.toPlayer() }
                            gameState.captures = playerDtos.associate { it.id to emptySet() }
                            gameState.photos = playerDtos.associate { it.id to emptyMap() }
                            gameState.timeRemainingSeconds = gameState.gameDurationMinutes * 60
                            gameState.isGameRunning = true
                            gameState.currentPlayerIndex = playerDtos.indexOfFirst { it.id == gameState.myPlayerId }
                                .takeIf { it >= 0 } ?: 0
                            gameState.currentScreen = Screen.GAME
                        }
                        "closed" -> {
                            gameState.pendingToast = "Der Host hat die Lobby geschlossen."
                            gameState.resetGame()
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    DisposableEffect(gameId) {
        onDispose { scope.launch { try { realtime.unsubscribe() } catch (_: Exception) {} } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Wartezimmer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorOnSurface,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { gameState.resetGame() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Verlassen", tint = ColorPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        bottomBar = {
            if (gameState.isHost) {
                Surface(shadowElevation = 8.dp, color = ColorSurface) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        GradientButton(
                            text = if (gameState.lobbyPlayers.size < 2)
                                "Mind. 2 Spieler nötig"
                            else
                                "Spiel starten (${gameState.lobbyPlayers.size} Spieler)",
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
                            modifier = Modifier.fillMaxWidth(),
                            gradientColors = GradientPrimary,
                            leadingIcon = if (isStarting) ({
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            }) else ({
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }),
                        )
                    }
                }
            }
        },
        containerColor = ColorBackground,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(Modifier.height(16.dp))

                // Game code card with animated gradient border
                GradientBorderCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                    borderColors = GradientPrimary,
                    backgroundColor = ColorPrimaryContainer,
                    borderWidth = 1.5.dp,
                    durationMillis = 3000,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Rundencode",
                            style = MaterialTheme.typography.labelMedium,
                            color = ColorOnPrimaryContainer.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.height(12.dp))
                        AnimatedGradientText(
                            text = gameState.gameCode ?: "------",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 8.sp,
                            ),
                            gradientColors = GradientPrimary,
                            durationMillis = 2000,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Teile diesen Code mit deinen Freunden",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorOnPrimaryContainer.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    AnimatedGradientText(
                        text = "Spieler (${gameState.lobbyPlayers.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = GradientCool,
                        durationMillis = 3500,
                    )
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = ColorPrimary,
                    )
                }
            }

            items(gameState.lobbyPlayers) { player ->
                LobbyPlayerRow(player = player, isMe = player.id == gameState.myPlayerId)
            }

            item {
                Spacer(Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ColorSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ColorOutlineVariant),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = ColorSecondary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            buildString {
                                append("${gameState.selectedCategories.size} Kategorien · ${gameState.gameDurationMinutes} Min.")
                                if (gameState.jokerMode) append(" · 🃏 Joker-Modus")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorOnSurfaceVariant,
                        )
                    }
                }
            }

            if (!gameState.isHost) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Warte auf den Host...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorOnSurfaceVariant,
                        )
                    }
                }
            }

            // Timeout warning for host (last 60 seconds)
            if (gameState.isHost && gameState.lobbyPlayers.size < 2 && lobbyTimeoutSeconds <= 60) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ColorError.copy(alpha = 0.1f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ColorError.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(0.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = ColorError,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Lobby schließt in ${gameState.formatTime(lobbyTimeoutSeconds)}, falls kein Spieler beitritt",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorError,
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun LobbyPlayerRow(player: PlayerDto, isMe: Boolean) {
    val color = parseHexColor(player.color)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ColorOutlineVariant),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerAvatarViewRaw(
                name = player.name,
                color = color,
                avatar = player.avatar,
                size = 40.dp,
                fontSize = 16.sp,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                player.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                color = ColorOnSurface,
            )
            if (isMe) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ColorPrimaryContainer)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        "Du",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
