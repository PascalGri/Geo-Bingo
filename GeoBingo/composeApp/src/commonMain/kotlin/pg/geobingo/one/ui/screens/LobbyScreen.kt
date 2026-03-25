package pg.geobingo.one.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pg.geobingo.one.game.GameMode
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.PlayerDto
import pg.geobingo.one.network.parseHexColor
import pg.geobingo.one.network.toPlayer
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.ui.theme.PlayerAvatarViewRaw
import pg.geobingo.one.ui.theme.Spacing
import pg.geobingo.one.ui.theme.rememberStaggeredAnimation
import pg.geobingo.one.ui.theme.rememberFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(gameState: GameState) {
    val scope = rememberCoroutineScope()
    var isStarting by remember { mutableStateOf(false) }
    val gameId = gameState.session.gameId ?: return
    val realtime = remember(gameId) { gameState.ensureRealtime(gameId) }
    val feedback = rememberFeedback(gameState)

    // Lobby auto-close timeout (host only): 5 min without a second player joining
    var lobbyTimeoutSeconds by remember { mutableStateOf(300) }
    LaunchedEffect(gameId) {
        if (!gameState.session.isHost) return@LaunchedEffect
        while (lobbyTimeoutSeconds > 0) {
            delay(1000)
            if (gameState.gameplay.lobbyPlayers.size >= 2) return@LaunchedEffect // second player joined → cancel
            lobbyTimeoutSeconds--
        }
        try { GameRepository.setGameStatus(gameId, "closed") } catch (e: Exception) { e.printStackTrace() }
        gameState.resetGame()
    }

    // Initial player load
    LaunchedEffect(gameId) {
        try { gameState.gameplay.lobbyPlayers = GameRepository.getPlayers(gameId) } catch (e: Exception) { e.printStackTrace() }
    }

    // 1. Subscribe first
    LaunchedEffect(gameId) {
        realtime.subscribe()
    }

    // 2. Realtime: player joined
    LaunchedEffect(gameId) {
        realtime.playerInserts.collect {
            try { gameState.gameplay.lobbyPlayers = GameRepository.getPlayers(gameId) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // 3. Realtime: game status changes (guests transition automatically)
    LaunchedEffect(gameId) {
        if (!gameState.session.isHost) {
            realtime.gameUpdates.collect { game ->
                when (game.status) {
                    "running" -> {
                        val playerDtos = GameRepository.getPlayers(gameId)
                        gameState.gameplay.players = playerDtos.map { it.toPlayer() }
                        gameState.gameplay.captures = playerDtos.associate { it.id to emptySet() }
                        gameState.gameplay.timeRemainingSeconds = gameState.gameplay.gameDurationMinutes * 60
                        gameState.gameplay.isGameRunning = true
                        gameState.gameplay.currentPlayerIndex = playerDtos.indexOfFirst { it.id == gameState.session.myPlayerId }
                            .takeIf { it >= 0 } ?: 0
                        feedback.gameStart()
                        gameState.session.currentScreen = Screen.GAME
                    }
                    "closed" -> {
                        gameState.ui.pendingToast = "Der Host hat die Lobby geschlossen."
                        gameState.resetGame()
                    }
                }
            }
        }
    }

    // 4. Fallback Polling (with backoff on errors)
    LaunchedEffect(gameId) {
        var interval = 3_000L
        while (true) {
            delay(interval)
            try {
                gameState.gameplay.lobbyPlayers = GameRepository.getPlayers(gameId)
                if (!gameState.session.isHost) {
                    val game = GameRepository.getGameById(gameId)
                    if (game?.status == "running" && gameState.session.currentScreen == Screen.LOBBY) {
                        val playerDtos = GameRepository.getPlayers(gameId)
                        gameState.gameplay.players = playerDtos.map { it.toPlayer() }
                        gameState.gameplay.captures = playerDtos.associate { it.id to emptySet() }
                        gameState.gameplay.timeRemainingSeconds = gameState.gameplay.gameDurationMinutes * 60
                        gameState.gameplay.isGameRunning = true
                        gameState.gameplay.currentPlayerIndex = playerDtos.indexOfFirst { it.id == gameState.session.myPlayerId }
                            .takeIf { it >= 0 } ?: 0
                        feedback.gameStart()
                        gameState.session.currentScreen = Screen.GAME
                    }
                }
                interval = 3_000L
            } catch (e: Exception) {
                e.printStackTrace()
                interval = (interval * 1.5).toLong().coerceAtMost(15_000L)
            }
        }
    }

    val gameMode = gameState.session.gameMode
    val modeGradient = when (gameMode) {
        GameMode.CLASSIC    -> GradientPrimary
        GameMode.BLIND_BINGO -> GradientCool
        GameMode.WEIRD_CORE -> GradientWeird
        GameMode.QUICK_START -> GradientQuickStart
    }
    val modeLabel = when (gameMode) {
        GameMode.CLASSIC    -> "Klassisch"
        GameMode.BLIND_BINGO -> "Blind Bingo"
        GameMode.WEIRD_CORE -> "Weird Core"
        GameMode.QUICK_START -> "Quick Start"
    }
    val modeIcon = when (gameMode) {
        GameMode.CLASSIC    -> Icons.Default.GridOn
        GameMode.BLIND_BINGO -> Icons.Default.VisibilityOff
        GameMode.WEIRD_CORE -> Icons.Default.QuestionMark
        GameMode.QUICK_START -> Icons.Default.Bolt
    }

    val anim = rememberStaggeredAnimation(count = 3)
    val btnOffset = remember { Animatable(80f) }
    val btnAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch {
            delay(200L)
            launch { btnOffset.animateTo(0f, tween(450)) }
            btnAlpha.animateTo(1f, tween(450))
        }
    }

    fun Modifier.staggered(index: Int): Modifier = this.then(anim.modifier(index))

    SystemBackHandler { gameState.resetGame() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = "Wartezimmer",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = modeGradient,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { gameState.resetGame() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Verlassen", tint = ColorPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        bottomBar = {
            if (gameState.session.isHost) {
                Surface(shadowElevation = 8.dp, color = ColorSurface, modifier = Modifier.graphicsLayer { translationY = btnOffset.value; alpha = btnAlpha.value }) {
                    Column(modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = 12.dp)) {
                        // Lobby timeout warning (visible when < 60s remaining and still waiting for players)
                        if (lobbyTimeoutSeconds in 1..59 && gameState.gameplay.lobbyPlayers.size < 2) {
                            val timeoutMin = lobbyTimeoutSeconds / 60
                            val timeoutSec = lobbyTimeoutSeconds % 60
                            val timeStr = if (timeoutMin > 0) "${timeoutMin}:${timeoutSec.toString().padStart(2, '0')}"
                                else "${timeoutSec}s"
                            val pulseAlpha = if (lobbyTimeoutSeconds < 30) {
                                if (lobbyTimeoutSeconds % 2 == 0) 1f else 0.6f
                            } else 1f
                            Text(
                                "Lobby schließt in $timeStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorError,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .graphicsLayer { alpha = pulseAlpha },
                                textAlign = TextAlign.Center,
                            )
                        }
                        GradientButton(
                            text = if (gameState.gameplay.lobbyPlayers.size < 2)
                                "Mind. 2 Spieler nötig"
                            else
                                "Spiel starten (${gameState.gameplay.lobbyPlayers.size} Spieler)",
                            gradientColors = modeGradient,
                            onClick = {
                                scope.launch {
                                    isStarting = true
                                    try {
                                        GameRepository.startGame(gameId)
                                        val playerDtos = GameRepository.getPlayers(gameId)
                                        gameState.gameplay.players = playerDtos.map { it.toPlayer() }
                                        gameState.gameplay.captures = playerDtos.associate { it.id to emptySet() }
                                        gameState.gameplay.timeRemainingSeconds = gameState.gameplay.gameDurationMinutes * 60
                                        gameState.gameplay.isGameRunning = true
                                        gameState.gameplay.currentPlayerIndex = playerDtos.indexOfFirst { it.id == gameState.session.myPlayerId }
                                            .takeIf { it >= 0 } ?: 0
                                        feedback.gameStart()
                                        gameState.session.currentScreen = Screen.GAME
                                    } catch (e: Exception) {
                                        isStarting = false
                                    }
                                }
                            },
                            enabled = gameState.gameplay.lobbyPlayers.size >= 2 && !isStarting,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isStarting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp,
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            } else {
                // Guest hint: "Warte auf den Host..."
                Surface(shadowElevation = 4.dp, color = ColorSurface) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(Spacing.screenHorizontal, 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = modeGradient.first(),
                            )
                            Text(
                                "Warte auf den Host...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ColorOnSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                            )
                        }
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
                .padding(horizontal = Spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                GradientBorderCard(
                    modifier = Modifier.fillMaxWidth().staggered(0),
                    cornerRadius = 20.dp,
                    borderColors = modeGradient,
                    backgroundColor = ColorSurface,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Modus-Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(modeGradient.first().copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                        ) {
                            Icon(
                                modeIcon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = modeGradient.first(),
                            )
                            Text(
                                modeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = modeGradient.first(),
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Rundencode",
                            style = MaterialTheme.typography.labelMedium,
                            color = ColorOnSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        AnimatedGradientText(
                            text = gameState.session.gameCode ?: "------",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 8.sp,
                            ),
                            gradientColors = modeGradient,
                            durationMillis = 2000,
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().staggered(1),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    AnimatedGradientText(
                        text = "Spieler (${gameState.gameplay.lobbyPlayers.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = modeGradient,
                    )
                }
            }

            val hostPlayerId = gameState.gameplay.lobbyPlayers.firstOrNull()?.id
            items(gameState.gameplay.lobbyPlayers) { player ->
                LobbyPlayerRow(player = player, isMe = player.id == gameState.session.myPlayerId, isHost = player.id == hostPlayerId, photoBytes = gameState.photo.playerAvatarBytes[player.id])
            }
        }
    }
}

@Composable
private fun LobbyPlayerRow(player: PlayerDto, isMe: Boolean, isHost: Boolean, photoBytes: ByteArray?) {
    val color = parseHexColor(player.color)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ColorOutlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerAvatarViewRaw(
                name = player.name,
                color = color,
                avatar = player.avatar,
                size = 40.dp,
                fontSize = 16.sp,
                photoBytes = photoBytes,
            )
            Spacer(Modifier.width(12.dp))
            if (isHost) {
                CrownIcon(modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
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
                    Text("Du", style = MaterialTheme.typography.labelSmall, color = ColorPrimary)
                }
            }
        }
    }
}

@Composable
private fun CrownIcon(modifier: Modifier = Modifier, color: Color = Color(0xFFFBBF24)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            // Crown shape matching Material Symbols "Crown"
            // Bottom band
            moveTo(w * 0.12f, h * 0.88f)
            lineTo(w * 0.88f, h * 0.88f)
            lineTo(w * 0.88f, h * 0.72f)
            lineTo(w * 0.12f, h * 0.72f)
            close()
            // Crown body with 3 pointed peaks
            moveTo(w * 0.12f, h * 0.72f)
            lineTo(w * 0.04f, h * 0.18f)  // left tip
            lineTo(w * 0.32f, h * 0.46f)  // left valley
            lineTo(w * 0.50f, h * 0.12f)  // center peak
            lineTo(w * 0.68f, h * 0.46f)  // right valley
            lineTo(w * 0.96f, h * 0.18f)  // right tip
            lineTo(w * 0.88f, h * 0.72f)  // back to base right
            close()
        }
        drawPath(path, color, style = Fill)
        // Small circles on the 3 tips
        drawCircle(color, radius = w * 0.045f, center = androidx.compose.ui.geometry.Offset(w * 0.04f, h * 0.15f))
        drawCircle(color, radius = w * 0.045f, center = androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.09f))
        drawCircle(color, radius = w * 0.045f, center = androidx.compose.ui.geometry.Offset(w * 0.96f, h * 0.15f))
    }
}
