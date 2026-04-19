package pg.geobingo.one.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameMode
import pg.geobingo.one.navigation.NavArgs
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.i18n.S
import pg.geobingo.one.network.*
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.components.CollectScrollToTop
import pg.geobingo.one.ui.components.PlayerBanner
import pg.geobingo.one.ui.components.PlayerBannerSize
import pg.geobingo.one.ui.components.ScrollToTopTags
import pg.geobingo.one.ui.components.rememberPlayerCosmeticsMap
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.util.AppLogger

private val FriendsGradient = listOf(Color(0xFF6366F1), Color(0xFFEC4899))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val isLoggedIn = AccountManager.isLoggedIn

    var friends by remember { mutableStateOf<List<FriendInfo>>(emptyList()) }
    var pendingRequests by remember { mutableStateOf<List<Pair<FriendshipDto, UserProfile>>>(emptyList()) }
    var pendingInvites by remember { mutableStateOf<List<Pair<GameInviteDto, UserProfile>>>(emptyList()) }
    var myFriendCode by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf<FriendInfo?>(null) }
    var friendAvatars by remember { mutableStateOf<Map<String, ByteArray>>(emptyMap()) }
    val friendsListState = rememberLazyListState()
    CollectScrollToTop(ScrollToTopTags.FRIENDS, friendsListState)

    // Prefetch cosmetics for all friends (and pending request senders) in a single query
    val friendUserIds = remember(friends, pendingRequests) {
        (friends.map { it.userId } + pendingRequests.map { it.second.id }).distinct().filter { it.isNotBlank() }
    }
    val friendCosmetics by rememberPlayerCosmeticsMap(friendUserIds)

    // Load data
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) { loading = false; return@LaunchedEffect }
        try {
            myFriendCode = FriendsManager.ensureFriendCode()
            friends = FriendsManager.getFriends()
            pendingRequests = FriendsManager.getPendingRequests()
            pendingInvites = FriendsManager.getPendingInvites()
            // Load avatars for friends that have one
            val avatarMap = mutableMapOf<String, ByteArray>()
            friends.filter { it.avatarUrl.isNotBlank() }.forEach { friend ->
                try {
                    val bytes = FriendsManager.downloadFriendAvatar(friend.userId)
                    if (bytes != null) avatarMap[friend.userId] = bytes
                } catch (e: Exception) {
                    AppLogger.w("Friends", "Avatar download failed: ${friend.userId}", e)
                }
            }
            friendAvatars = avatarMap
        } catch (e: Exception) {
            AppLogger.w("Friends", "Load failed", e)
        }
        loading = false
    }

    // Refresh periodically for online status
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(30_000L)
            try {
                friends = FriendsManager.getFriends()
                pendingRequests = FriendsManager.getPendingRequests()
                pendingInvites = FriendsManager.getPendingInvites()
            } catch (e: Exception) {
                AppLogger.w("Friends", "Refresh failed", e)
            }
        }
    }

    SystemBackHandler { nav.goBack() }

    // Add Friend Dialog
    if (showAddDialog) {
        AddFriendDialog(
            onDismiss = { showAddDialog = false },
            onSubmit = { code ->
                scope.launch {
                    val result = FriendsManager.sendFriendRequest(code)
                    showAddDialog = false
                    result.fold(
                        onSuccess = { snackbarHostState.showSnackbar(S.current.friendRequestSent) },
                        onFailure = { e ->
                            val msg = when (e.message) {
                                "not_found" -> S.current.friendNotFound
                                "already_friends", "already_pending" -> S.current.friendAlreadyAdded
                                "self" -> S.current.friendAlreadyAdded
                                else -> "${S.current.error}: ${e.message}"
                            }
                            snackbarHostState.showSnackbar(msg)
                        },
                    )
                    // Refresh
                    try { pendingRequests = FriendsManager.getPendingRequests() } catch (e: Exception) {
                        AppLogger.w("Friends", "Refresh pending requests failed", e)
                    }
                }
            },
        )
    }

    // Remove Friend Dialog
    showRemoveDialog?.let { friend ->
        AlertDialog(
            onDismissRequest = { showRemoveDialog = null },
            containerColor = ColorSurface,
            title = { Text(S.current.removeFriend, fontWeight = FontWeight.Bold, color = ColorOnSurface) },
            text = { Text(S.current.removeFriendConfirm, color = ColorOnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        FriendsManager.removeFriendship(friend.friendshipId)
                        friends = friends.filter { it.friendshipId != friend.friendshipId }
                        showRemoveDialog = null
                        snackbarHostState.showSnackbar(S.current.friendRemoved)
                    }
                }) { Text(S.current.removeFriend, color = Color(0xFFEF4444)) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = null }) { Text(S.current.cancel, color = ColorOnSurfaceVariant) }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = S.current.friends,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = FriendsGradient,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.goHome() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = S.current.back, tint = ColorPrimary)
                    }
                },
                actions = {
                    if (isLoggedIn) {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = S.current.addFriend, tint = FriendsGradient.first())
                        }
                    }
                    pg.geobingo.one.ui.components.TopBarStarsAndProfile(gameState = gameState, onNavigate = { nav.navigateTo(it) })
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        containerColor = ColorBackground,
    ) { padding ->
        if (!isLoggedIn) {
            // Not logged in state
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Default.People, null, tint = ColorOnSurfaceVariant, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        S.current.loginRequiredForFriends,
                        style = MaterialTheme.typography.bodyLarge,
                        color = ColorOnSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    GradientButton(
                        text = S.current.signIn,
                        onClick = { nav.navigateTo(Screen.ACCOUNT) },
                        gradientColors = FriendsGradient,
                        modifier = Modifier.fillMaxWidth(0.6f),
                    )
                }
            }
        } else if (loading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FriendsGradient.first())
            }
        } else {
            LazyColumn(
                state = friendsListState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // My Friend Code
                item {
                    MyFriendCodeCard(
                        code = myFriendCode ?: "...",
                        onCopy = {
                            myFriendCode?.let {
                                clipboardManager.setText(AnnotatedString(it))
                                scope.launch { snackbarHostState.showSnackbar(S.current.friendCodeCopied) }
                            }
                        },
                    )
                }

                // Pending Game Invites
                if (pendingInvites.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        AnimatedGradientText(
                            text = S.current.gameInviteReceived,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            gradientColors = FriendsGradient,
                        )
                    }
                    items(pendingInvites, key = { it.first.id }) { (invite, profile) ->
                        GameInviteCard(
                            invite = invite,
                            fromName = profile.display_name.ifBlank { "Player" },
                            onAccept = {
                                scope.launch {
                                    FriendsManager.respondToInvite(invite.id, true)
                                    pendingInvites = pendingInvites.filter { it.first.id != invite.id }
                                    // Navigate to join game with code
                                    nav.navigateTo(Screen.JOIN_GAME, NavArgs.JoinGame(inviteCode = invite.game_code))
                                }
                            },
                            onDecline = {
                                scope.launch {
                                    FriendsManager.respondToInvite(invite.id, false)
                                    pendingInvites = pendingInvites.filter { it.first.id != invite.id }
                                }
                            },
                        )
                    }
                }

                // Pending Friend Requests
                if (pendingRequests.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        AnimatedGradientText(
                            text = "${S.current.friendRequests} (${pendingRequests.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            gradientColors = FriendsGradient,
                        )
                    }
                    items(pendingRequests, key = { it.first.id }) { (friendship, profile) ->
                        FriendRequestCard(
                            name = profile.display_name.ifBlank { "Player" },
                            onAccept = {
                                scope.launch {
                                    FriendsManager.acceptFriendRequest(friendship.id)
                                    // Notify the requester
                                    pg.geobingo.one.network.NotificationHelper.notifyFriendRequestAccepted(friendship.requested_by)
                                    pendingRequests = pendingRequests.filter { it.first.id != friendship.id }
                                    friends = FriendsManager.getFriends()
                                    snackbarHostState.showSnackbar(S.current.friendAdded)
                                }
                            },
                            onDecline = {
                                scope.launch {
                                    FriendsManager.removeFriendship(friendship.id)
                                    pendingRequests = pendingRequests.filter { it.first.id != friendship.id }
                                }
                            },
                        )
                    }
                }

                // Friends List
                item {
                    Spacer(Modifier.height(4.dp))
                    AnimatedGradientText(
                        text = "${S.current.friends} (${friends.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = FriendsGradient,
                    )
                }

                if (friends.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.People, null, tint = ColorOnSurfaceVariant, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(12.dp))
                                Text(S.current.friendsEmpty, style = MaterialTheme.typography.bodyLarge, color = ColorOnSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(friends, key = { it.userId }) { friend ->
                        FriendRow(
                            friend = friend,
                            avatarBytes = friendAvatars[friend.userId],
                            cosmetics = friendCosmetics[friend.userId] ?: pg.geobingo.one.network.PlayerCosmetics.NONE,
                            canInvite = gameState.session.gameCode != null && gameState.session.isHost,
                            onInvite = {
                                val code = gameState.session.gameCode ?: return@FriendRow
                                val gid = gameState.session.gameId ?: return@FriendRow
                                scope.launch {
                                    FriendsManager.sendGameInvite(friend.userId, code, gid)
                                    snackbarHostState.showSnackbar("${S.current.inviteToGame}: ${friend.displayName}")
                                }
                            },
                            onRemove = { showRemoveDialog = friend },
                        )
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

// ── Sub-components ──────────────────────────────────────────────────

@Composable
private fun MyFriendCodeCard(code: String, onCopy: () -> Unit) {
    GradientBorderCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        borderColors = FriendsGradient,
        backgroundColor = ColorSurface,
        borderWidth = 1.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(S.current.yourFriendCode, style = MaterialTheme.typography.labelMedium, color = ColorOnSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            AnimatedGradientText(
                text = code,
                style = AppTextStyles.gameCode.copy(fontSize = 28.sp),
                gradientColors = FriendsGradient,
                durationMillis = 2500,
            )
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(
                onClick = onCopy,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = FriendsGradient.first().copy(alpha = 0.12f)),
            ) {
                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp), tint = FriendsGradient.first())
                Spacer(Modifier.width(6.dp))
                Text(S.current.copyFriendCode, style = MaterialTheme.typography.labelMedium, color = FriendsGradient.first())
            }
        }
    }
}

@Composable
private fun FriendRow(
    friend: FriendInfo,
    avatarBytes: ByteArray? = null,
    canInvite: Boolean,
    cosmetics: pg.geobingo.one.network.PlayerCosmetics = pg.geobingo.one.network.PlayerCosmetics.NONE,
    onInvite: () -> Unit,
    onRemove: () -> Unit,
) {
    val statusText = if (friend.isOnline) {
        "\u25CF ${S.current.friendsOnline}"
    } else {
        S.current.friendsOffline
    }
    PlayerBanner(
        name = friend.displayName,
        cosmetics = cosmetics,
        avatarBytes = avatarBytes,
        avatarColor = FriendsGradient.first(),
        size = PlayerBannerSize.Compact,
        subtitle = statusText,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (canInvite && friend.isOnline) {
                    FilledTonalButton(
                        onClick = onInvite,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.15f),
                        ),
                        modifier = Modifier.height(28.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Login,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White,
                        )
                    }
                    Spacer(Modifier.width(2.dp))
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = Color.White.copy(alpha = 0.7f))
                }
            }
        },
    )
}

@Composable
private fun FriendRequestCard(name: String, onAccept: () -> Unit, onDecline: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FriendsGradient.first().copy(alpha = 0.06f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, FriendsGradient.first().copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerAvatarViewRaw(name = name, color = FriendsGradient.first(), size = 36.dp, fontSize = 14.sp)
            Spacer(Modifier.width(12.dp))
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = ColorOnSurface, modifier = Modifier.weight(1f))
            FilledTonalButton(
                onClick = onAccept,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF22C55E).copy(alpha = 0.15f)),
                modifier = Modifier.height(32.dp),
            ) {
                Text(S.current.accept, style = MaterialTheme.typography.labelSmall, color = Color(0xFF22C55E), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onDecline, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = ColorOnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun GameInviteCard(invite: GameInviteDto, fromName: String, onAccept: () -> Unit, onDecline: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFBBF24).copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.SportsEsports, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${S.current.gameInviteFrom} $fromName", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = ColorOnSurface)
                Text("Code: ${invite.game_code}", style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant)
            }
            FilledTonalButton(
                onClick = onAccept,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF22C55E).copy(alpha = 0.15f)),
                modifier = Modifier.height(32.dp),
            ) {
                Text(S.current.join, style = MaterialTheme.typography.labelSmall, color = Color(0xFF22C55E), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onDecline, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = ColorOnSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFriendDialog(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var codeInput by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorSurface,
        title = { Text(S.current.addFriend, fontWeight = FontWeight.Bold, color = ColorOnSurface) },
        text = {
            Column {
                Text(S.current.addFriendDesc, style = MaterialTheme.typography.bodySmall, color = ColorOnSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { if (it.length <= 8) codeInput = it.uppercase() },
                    placeholder = { Text(S.current.friendCode, color = ColorOnSurfaceVariant) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FriendsGradient.first(),
                        unfocusedBorderColor = ColorOutline,
                        focusedTextColor = ColorOnSurface,
                        unfocusedTextColor = ColorOnSurface,
                        cursorColor = FriendsGradient.first(),
                    ),
                    textStyle = AppTextStyles.codeInput.copy(color = ColorOnSurface),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(codeInput) },
                enabled = codeInput.trim().length == 8,
            ) { Text(S.current.addFriend, color = FriendsGradient.first()) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(S.current.cancel, color = ColorOnSurfaceVariant) }
        },
    )
}
