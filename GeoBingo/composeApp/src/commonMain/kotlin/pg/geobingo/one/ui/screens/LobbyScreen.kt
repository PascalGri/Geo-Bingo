package pg.geobingo.one.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pg.geobingo.one.game.GameConstants
import pg.geobingo.one.game.GameMode
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.i18n.S
import pg.geobingo.one.util.AppLogger
import pg.geobingo.one.network.AccountManager
import pg.geobingo.one.network.FriendsManager
import pg.geobingo.one.network.FriendInfo
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.PlayerDto
import pg.geobingo.one.network.toCategory
import pg.geobingo.one.network.parseHexColor
import pg.geobingo.one.network.toPlayer
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.platform.rememberShareManager
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.ui.theme.PlayerAvatarViewRaw
import pg.geobingo.one.ui.theme.Spacing
import pg.geobingo.one.ui.theme.rememberStaggeredAnimation
import pg.geobingo.one.ui.theme.rememberFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val scope = rememberCoroutineScope()
    var isStarting by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val gameId = gameState.session.gameId ?: return
    val syncScope = rememberCoroutineScope()
    val sync = remember(gameId) { gameState.ensureSyncManager(gameId, syncScope) }
    val realtime = gameState.realtime
    val feedback = rememberFeedback(gameState)
    val clipboardManager = LocalClipboardManager.current
    val shareManager = rememberShareManager()

    // Cleanup realtime when leaving screen
    DisposableEffect(gameId) {
        onDispose { /* sync will be cleaned up on game reset */ }
    }

    // Lobby auto-close timeout (host only): 5 min without a second player joining
    var lobbyTimeoutSeconds by remember { mutableStateOf(GameConstants.LOBBY_TIMEOUT_SECONDS) }
    LaunchedEffect(gameId) {
        if (!gameState.session.isHost) return@LaunchedEffect
        while (lobbyTimeoutSeconds > 0) {
            delay(1000)
            if (gameState.gameplay.lobbyPlayers.size >= 2) return@LaunchedEffect // second player joined → cancel
            lobbyTimeoutSeconds--
        }
        // Re-check player count from server before closing to avoid race condition
        val serverPlayers = try { GameRepository.getPlayers(gameId) } catch (_: Exception) { emptyList() }
        if (serverPlayers.size >= 2) return@LaunchedEffect
        try { GameRepository.setGameStatus(gameId, "closed") } catch (e: Exception) { AppLogger.w("Lobby", "Operation failed", e) }
        gameState.resetGame(); nav.resetTo(Screen.HOME)
    }

    // Initial player load
    LaunchedEffect(gameId) {
        try { gameState.gameplay.lobbyPlayers = GameRepository.getPlayers(gameId) } catch (e: Exception) { AppLogger.w("Lobby", "Operation failed", e) }
    }

    // Sync: player joined (realtime + polling via SyncManager)
    LaunchedEffect(gameId) {
        sync.playerJoined.collect {
            try { gameState.gameplay.lobbyPlayers = GameRepository.getPlayers(gameId) } catch (e: Exception) { AppLogger.w("Lobby", "Player reload failed", e) }
        }
    }

    // Team mode state
    var showCreateTeamDialog by remember { mutableStateOf(false) }
    var newTeamNameInput by remember { mutableStateOf("") }

    // Friend invite state
    var showInviteFriendsDialog by remember { mutableStateOf(false) }
    var onlineFriends by remember { mutableStateOf<List<FriendInfo>>(emptyList()) }
    var friendsLoading by remember { mutableStateOf(false) }

    // Count distinct teams that have players
    val activeTeamCount = if (gameState.gameplay.teamModeEnabled) {
        gameState.gameplay.teamAssignments.values.toSet().size
    } else 0

    val canStartTeamMode = !gameState.gameplay.teamModeEnabled || activeTeamCount >= 2

    // Sync: game status changes (handles both realtime + polling)
    LaunchedEffect(gameId) {
        sync.gameUpdates.collect { game ->
            // Refresh lobby players from polling
            try { gameState.gameplay.lobbyPlayers = GameRepository.getPlayers(gameId) } catch (e: Exception) { AppLogger.w("Lobby", "Player reload failed", e) }

            if (!gameState.session.isHost) {
                when (game.status) {
                    "running" -> {
                        if (gameState.session.currentScreen == Screen.LOBBY) {
                            // Re-validate game mode and duration from server (defensive)
                            gameState.session.gameMode = try { GameMode.valueOf(game.game_mode) } catch (_: Exception) { gameState.session.gameMode }
                            gameState.gameplay.gameDurationMinutes = game.duration_s / 60
                            gameState.joker.jokerMode = game.joker_mode
                            // Refresh categories from server in case host changed them
                            try {
                                val cats = GameRepository.getCategories(gameId)
                                if (cats.isNotEmpty()) gameState.gameplay.selectedCategories = cats.map { it.toCategory() }
                            } catch (e: Exception) { AppLogger.w("Lobby", "Category refresh failed", e) }
                            val playerDtos = GameRepository.getPlayers(gameId)
                            gameState.gameplay.players = playerDtos.map { it.toPlayer() }
                            gameState.gameplay.captures = playerDtos.associate { it.id to emptySet() }
                            gameState.gameplay.timeRemainingSeconds = gameState.gameplay.gameDurationMinutes * 60
                            gameState.gameplay.isGameRunning = true
                            gameState.gameplay.currentPlayerIndex = playerDtos.indexOfFirst { it.id == gameState.session.myPlayerId }
                                .takeIf { it >= 0 } ?: 0
                            // Load team assignments from server (guest)
                            try {
                                val teams = GameRepository.getTeamAssignments(gameId)
                                if (teams.isNotEmpty()) {
                                    gameState.gameplay.teamModeEnabled = true
                                    gameState.gameplay.teamAssignments = teams
                                }
                            } catch (e: Exception) { AppLogger.w("Lobby", "Team load failed", e) }
                            feedback.gameStart()
                            nav.replaceCurrent(Screen.GAME_START_TRANSITION)
                        }
                    }
                    "closed" -> {
                        gameState.ui.pendingToast = S.current.hostClosedLobby
                        gameState.resetGame(); nav.resetTo(Screen.HOME)
                    }
                }
            }
        }
    }

    // Consume pending toasts (e.g. quick reactions, code copied)
    LaunchedEffect(gameState.ui.pendingToast) {
        val msg = gameState.ui.pendingToast ?: return@LaunchedEffect
        gameState.ui.pendingToast = null
        snackbarHostState.showSnackbar(msg)
    }

    val gameMode = gameState.session.gameMode
    val modeGradient = when (gameMode) {
        GameMode.CLASSIC    -> GradientPrimary
        GameMode.BLIND_BINGO -> GradientCool
        GameMode.WEIRD_CORE -> GradientWeird
        GameMode.QUICK_START -> GradientQuickStart
    }
    val modeLabel = when (gameMode) {
        GameMode.CLASSIC    -> S.current.modeClassic
        GameMode.BLIND_BINGO -> S.current.modeBlindBingo
        GameMode.WEIRD_CORE -> S.current.modeWeirdCore
        GameMode.QUICK_START -> S.current.modeQuickStart
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

    SystemBackHandler { gameState.resetGame(); nav.resetTo(Screen.HOME) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = S.current.waitingRoom,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = modeGradient,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { gameState.resetGame(); nav.resetTo(Screen.HOME) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = S.current.leave, tint = ColorPrimary)
                    }
                },
                actions = { pg.geobingo.one.ui.components.TopBarStarsAndProfile(gameState = gameState, onNavigate = { nav.navigateTo(it) }) },
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
                                S.current.lobbyClosesIn(timeStr),
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
                            text = when {
                                gameState.gameplay.lobbyPlayers.size < 2 -> S.current.minPlayersNeeded(2)
                                gameState.gameplay.teamModeEnabled && activeTeamCount < 2 -> S.current.minTwoTeamsNeeded
                                else -> S.current.startGame(gameState.gameplay.lobbyPlayers.size)
                            },
                            gradientColors = modeGradient,
                            onClick = {
                                scope.launch {
                                    isStarting = true
                                    try {
                                        // Save team assignments before starting
                                        if (gameState.gameplay.teamModeEnabled && gameState.gameplay.teamAssignments.isNotEmpty()) {
                                            try { GameRepository.saveTeamAssignments(gameId, gameState.gameplay.teamAssignments) } catch (e: Exception) { AppLogger.w("Lobby", "Team save failed", e) }
                                        }
                                        GameRepository.startGame(gameId)
                                        val playerDtos = GameRepository.getPlayers(gameId)
                                        gameState.gameplay.players = playerDtos.map { it.toPlayer() }
                                        gameState.gameplay.captures = playerDtos.associate { it.id to emptySet() }
                                        gameState.gameplay.timeRemainingSeconds = gameState.gameplay.gameDurationMinutes * 60
                                        gameState.gameplay.isGameRunning = true
                                        gameState.gameplay.currentPlayerIndex = playerDtos.indexOfFirst { it.id == gameState.session.myPlayerId }
                                            .takeIf { it >= 0 } ?: 0
                                        feedback.gameStart()
                                        nav.replaceCurrent(Screen.GAME_START_TRANSITION)
                                    } catch (e: Exception) {
                                        isStarting = false
                                    }
                                }
                            },
                            enabled = gameState.gameplay.lobbyPlayers.size >= 2 && canStartTeamMode && !isStarting,
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
                                S.current.waitingForHost,
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
        // Create Team Dialog (must be outside LazyColumn)
        if (gameState.gameplay.teamModeEnabled && showCreateTeamDialog) {
            AlertDialog(
                onDismissRequest = { showCreateTeamDialog = false; newTeamNameInput = "" },
                containerColor = ColorSurface,
                title = {
                    Text(S.current.createTeam, fontWeight = FontWeight.Bold, color = ColorOnSurface)
                },
                text = {
                    OutlinedTextField(
                        value = newTeamNameInput,
                        onValueChange = { if (it.length <= 20) newTeamNameInput = it },
                        placeholder = { Text(S.current.teamNamePlaceholder, color = ColorOnSurfaceVariant) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = modeGradient.first(),
                            unfocusedBorderColor = ColorOutline,
                            focusedTextColor = ColorOnSurface,
                            unfocusedTextColor = ColorOnSurface,
                            cursorColor = modeGradient.first(),
                        ),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val name = newTeamNameInput.trim()
                            if (name.isNotEmpty()) {
                                val teamNum = gameState.gameplay.nextTeamNumber
                                gameState.gameplay.nextTeamNumber = teamNum + 1
                                gameState.gameplay.teamNames = gameState.gameplay.teamNames + (teamNum to name)
                                val myId = gameState.session.myPlayerId
                                if (myId != null) {
                                    gameState.gameplay.teamAssignments = gameState.gameplay.teamAssignments + (myId to teamNum)
                                }
                                newTeamNameInput = ""
                                showCreateTeamDialog = false
                            }
                        },
                        enabled = newTeamNameInput.trim().isNotEmpty(),
                    ) {
                        Text(S.current.confirm, color = modeGradient.first())
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateTeamDialog = false; newTeamNameInput = "" }) {
                        Text(S.current.cancel, color = ColorOnSurfaceVariant)
                    }
                },
            )
        }

        // Invite Friends Dialog
        if (showInviteFriendsDialog) {
            AlertDialog(
                onDismissRequest = { showInviteFriendsDialog = false },
                containerColor = ColorSurface,
                title = { Text(S.current.inviteToGame, fontWeight = FontWeight.Bold, color = ColorOnSurface) },
                text = {
                    if (friendsLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = modeGradient.first(), modifier = Modifier.size(24.dp))
                        }
                    } else if (onlineFriends.isEmpty()) {
                        Text(S.current.friendsEmpty, color = ColorOnSurfaceVariant)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            onlineFriends.forEach { friend ->
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            val code = gameState.session.gameCode ?: return@clickable
                                            val gid = gameState.session.gameId ?: return@clickable
                                            scope.launch {
                                                FriendsManager.sendGameInvite(friend.userId, code, gid)
                                                gameState.ui.pendingToast = "${S.current.inviteToGame}: ${friend.displayName}"
                                                showInviteFriendsDialog = false
                                            }
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box {
                                        PlayerAvatarViewRaw(
                                            name = friend.displayName,
                                            color = modeGradient.first(),
                                            size = 36.dp,
                                            fontSize = 14.sp,
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(if (friend.isOnline) Color(0xFF22C55E) else ColorOutlineVariant)
                                                .align(Alignment.BottomEnd),
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(friend.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = ColorOnSurface)
                                    Spacer(Modifier.weight(1f))
                                    if (friend.isOnline) {
                                        Text(S.current.friendsOnline, style = MaterialTheme.typography.labelSmall, color = Color(0xFF22C55E))
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showInviteFriendsDialog = false }) {
                        Text(S.current.cancel, color = ColorOnSurfaceVariant)
                    }
                },
            )
        }

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
                            S.current.roundCode,
                            style = MaterialTheme.typography.labelMedium,
                            color = ColorOnSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        val code = gameState.session.gameCode ?: "------"
                        AnimatedGradientText(
                            text = code,
                            style = AppTextStyles.gameCode,
                            gradientColors = modeGradient,
                            durationMillis = 2000,
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString(code))
                                gameState.ui.pendingToast = S.current.codeCopied
                            },
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            S.current.tapToCopy,
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorOnSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(code))
                                    gameState.ui.pendingToast = S.current.codeCopied
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = modeGradient.first().copy(alpha = 0.12f),
                                ),
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = S.current.codeCopied,
                                    modifier = Modifier.size(16.dp),
                                    tint = modeGradient.first(),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    S.current.roundCode,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = modeGradient.first(),
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    shareManager.shareText("${S.current.joinRound}: KatchIt!\nhttps://katchit.app/join/$code")
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = modeGradient.first().copy(alpha = 0.12f),
                                ),
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = S.current.shareCode,
                                    modifier = Modifier.size(16.dp),
                                    tint = modeGradient.first(),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    S.current.share,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = modeGradient.first(),
                                )
                            }
                        }
                        // Invite Friends button (only for logged-in users)
                        if (AccountManager.isLoggedIn) {
                            Spacer(Modifier.height(8.dp))
                            FilledTonalButton(
                                onClick = {
                                    friendsLoading = true
                                    showInviteFriendsDialog = true
                                    scope.launch {
                                        try {
                                            onlineFriends = FriendsManager.getFriends()
                                        } catch (_: Exception) {}
                                        friendsLoading = false
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = modeGradient.first().copy(alpha = 0.12f),
                                ),
                            ) {
                                Icon(
                                    Icons.Default.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = modeGradient.first(),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    S.current.inviteFriends,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = modeGradient.first(),
                                )
                            }
                        }
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
                        text = S.current.playersCount(gameState.gameplay.lobbyPlayers.size),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = modeGradient,
                    )
                }
            }

            val hostPlayerId = gameState.gameplay.lobbyPlayers.firstOrNull()?.id
            items(gameState.gameplay.lobbyPlayers) { player ->
                LobbyPlayerRow(player = player, isMe = player.id == gameState.session.myPlayerId, isHost = player.id == hostPlayerId, photoBytes = gameState.photo.playerAvatarBytes[player.id])
            }

            // ── Team Assignment Section ──────────────────────────────────
            if (gameState.gameplay.teamModeEnabled) {
                item {
                    Spacer(Modifier.height(8.dp))
                    AnimatedGradientText(
                        text = S.current.selectTeams,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = modeGradient,
                    )
                }

                // Show each team
                val teamNumbers = gameState.gameplay.teamNames.keys.sorted()
                val teamColors = listOf(
                    modeGradient.first(),
                    modeGradient.last(),
                    Color(0xFF22D3EE), // cyan
                    Color(0xFFFB923C), // orange
                    Color(0xFF84CC16), // lime
                    Color(0xFFFF6B6B), // coral
                )

                teamNumbers.forEachIndexed { idx, teamNum ->
                    item(key = "team_$teamNum") {
                        val teamColor = teamColors[idx % teamColors.size]
                        val teamName = gameState.gameplay.teamNames[teamNum] ?: S.current.teamName(teamNum)
                        val teamPlayers = gameState.gameplay.lobbyPlayers.filter {
                            gameState.gameplay.teamAssignments[it.id] == teamNum
                        }
                        val myId = gameState.session.myPlayerId
                        val isMyTeam = gameState.gameplay.teamAssignments[myId] == teamNum

                        GradientBorderCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 14.dp,
                            borderColors = listOf(teamColor, teamColor.copy(alpha = 0.5f)),
                            backgroundColor = ColorSurface,
                            borderWidth = if (isMyTeam) 2.dp else 1.dp,
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(teamColor),
                                        )
                                        Text(
                                            teamName,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = teamColor,
                                        )
                                        Text(
                                            "(${teamPlayers.size})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ColorOnSurfaceVariant,
                                        )
                                    }
                                    if (!isMyTeam && myId != null) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(teamColor.copy(alpha = 0.15f))
                                                .clickable {
                                                    gameState.gameplay.teamAssignments = gameState.gameplay.teamAssignments + (myId to teamNum)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                        ) {
                                            Text(
                                                S.current.joinTeam,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = teamColor,
                                            )
                                        }
                                    }
                                }
                                if (teamPlayers.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    teamPlayers.forEach { player ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            PlayerAvatarViewRaw(
                                                name = player.name,
                                                color = parseHexColor(player.color),
                                                avatar = player.avatar,
                                                size = 24.dp,
                                                fontSize = 10.sp,
                                                photoBytes = gameState.photo.playerAvatarBytes[player.id],
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                player.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = ColorOnSurface,
                                            )
                                            if (player.id == myId) {
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    "(${S.current.you})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = ColorPrimary,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Create Team button
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(modeGradient.first().copy(alpha = 0.1f))
                            .border(
                                width = 1.dp,
                                color = modeGradient.first().copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable { showCreateTeamDialog = true }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = modeGradient.first(),
                            )
                            Text(
                                S.current.createTeam,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = modeGradient.first(),
                            )
                        }
                    }
                }
            }

            // Quick reactions
            item {
                var lastReaction by remember { mutableStateOf<String?>(null) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    val reactionItems = listOf(
                        Icons.Default.ThumbUp to S.current.ready,
                        Icons.Default.Timer to S.current.hurryUp,
                    )
                    reactionItems.forEach { (icon, label) ->
                        OutlinedButton(
                            onClick = {
                                lastReaction = label
                                gameState.ui.pendingToast = label
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                modeGradient.first().copy(alpha = 0.4f),
                            ),
                            shape = RoundedCornerShape(20.dp),
                        ) {
                            Icon(
                                icon,
                                contentDescription = label,
                                modifier = Modifier.size(16.dp),
                                tint = modeGradient.first(),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                color = modeGradient.first(),
                            )
                        }
                    }
                }
            }

            // Empty lobby hint when only host is present
            if (gameState.gameplay.lobbyPlayers.size <= 1) {
                item {
                    val shareIconAlpha = remember { Animatable(0.4f) }
                    LaunchedEffect(Unit) {
                        shareIconAlpha.animateTo(
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800),
                                repeatMode = RepeatMode.Reverse,
                            ),
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .graphicsLayer { alpha = shareIconAlpha.value },
                            tint = modeGradient.first().copy(alpha = 0.6f),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            S.current.emptyLobbyHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorOnSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
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
                    Text(S.current.you, style = MaterialTheme.typography.labelSmall, color = ColorPrimary)
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
