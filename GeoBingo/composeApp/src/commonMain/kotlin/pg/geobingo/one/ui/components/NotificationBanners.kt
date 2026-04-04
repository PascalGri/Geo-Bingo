package pg.geobingo.one.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.i18n.S
import pg.geobingo.one.navigation.NavArgs
import pg.geobingo.one.navigation.NavigationManager
import pg.geobingo.one.network.AccountManager
import pg.geobingo.one.network.FriendshipDto
import pg.geobingo.one.network.FriendsManager
import pg.geobingo.one.network.GameInviteDto
import pg.geobingo.one.network.NotificationRealtimeManager
import pg.geobingo.one.network.UserProfile
import pg.geobingo.one.util.AppLogger

/**
 * Manages polling and display of game invite / friend request banners.
 */
@Composable
fun NotificationBanners(
    gameState: GameState,
    nav: NavigationManager,
) {
    var pendingInviteBanner by remember { mutableStateOf<Pair<GameInviteDto, UserProfile>?>(null) }
    var pendingFriendBanner by remember { mutableStateOf<Pair<FriendshipDto, UserProfile>?>(null) }
    val bannerScope = rememberCoroutineScope()

    // Try realtime subscription, fall back to polling if it fails
    LaunchedEffect(Unit) {
        val userId = AccountManager.currentUserId
        var realtimeActive = false

        if (userId != null) {
            val rtManager = NotificationRealtimeManager(userId)
            realtimeActive = rtManager.subscribe(this)

            if (realtimeActive) {
                // Collect realtime invite changes
                launch {
                    rtManager.inviteChanges.collect {
                        if (it.status == "pending" && pendingInviteBanner == null) {
                            val invites = FriendsManager.getPendingInvites()
                            if (invites.isNotEmpty()) pendingInviteBanner = invites.first()
                        }
                    }
                }
                // Collect realtime friendship changes
                launch {
                    rtManager.friendshipChanges.collect {
                        if (it.status == "pending" && pendingFriendBanner == null && pendingInviteBanner == null) {
                            val requests = FriendsManager.getPendingRequests()
                            if (requests.isNotEmpty()) pendingFriendBanner = requests.first()
                        }
                    }
                }
            }
        }

        // Fallback polling (30s when realtime active, 10s without)
        val interval = if (realtimeActive) 30_000L else 10_000L
        while (true) {
            delay(interval)
            if (AccountManager.isLoggedIn) {
                try {
                    if (pendingInviteBanner == null) {
                        val invites = FriendsManager.getPendingInvites()
                        if (invites.isNotEmpty()) pendingInviteBanner = invites.first()
                    }
                    if (pendingFriendBanner == null && pendingInviteBanner == null) {
                        val requests = FriendsManager.getPendingRequests()
                        if (requests.isNotEmpty()) pendingFriendBanner = requests.first()
                    }
                } catch (e: Exception) {
                    AppLogger.w("NotificationBanners", "Invite/friend poll failed", e)
                }
            }
        }
    }

    GameInviteBanner(
        invite = pendingInviteBanner,
        onAccept = { invite ->
            bannerScope.launch {
                FriendsManager.respondToInvite(invite.id, true)
                pendingInviteBanner = null
                nav.navigateTo(Screen.JOIN_GAME, NavArgs.JoinGame(inviteCode = invite.game_code))
            }
        },
        onDismiss = { invite ->
            bannerScope.launch {
                FriendsManager.respondToInvite(invite.id, false)
                pendingInviteBanner = null
            }
        },
    )

    FriendRequestBanner(
        request = pendingFriendBanner,
        isVisible = pendingInviteBanner == null,
        onAccept = { friendship ->
            bannerScope.launch {
                FriendsManager.acceptFriendRequest(friendship.id)
                pendingFriendBanner = null
            }
        },
        onNavigateToFriends = { friendship ->
            bannerScope.launch {
                FriendsManager.acceptFriendRequest(friendship.id)
                pendingFriendBanner = null
                nav.navigateTo(Screen.FRIENDS)
            }
        },
        onDismiss = { pendingFriendBanner = null },
    )
}

@Composable
private fun GameInviteBanner(
    invite: Pair<GameInviteDto, UserProfile>?,
    onAccept: (GameInviteDto) -> Unit,
    onDismiss: (GameInviteDto) -> Unit,
) {
    AnimatedVisibility(
        visible = invite != null,
        enter = slideInVertically { -it } + expandVertically(),
        exit = slideOutVertically { -it } + shrinkVertically(),
    ) {
        invite?.let { (inv, profile) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFFEC4899))))
                    .clickable { onAccept(inv) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.SportsEsports, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${S.current.gameInviteFrom} ${profile.display_name.ifBlank { "Player" }}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            "${S.current.join} - Code: ${inv.game_code}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                    IconButton(
                        onClick = { onDismiss(inv) },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendRequestBanner(
    request: Pair<FriendshipDto, UserProfile>?,
    isVisible: Boolean,
    onAccept: (FriendshipDto) -> Unit,
    onNavigateToFriends: (FriendshipDto) -> Unit,
    onDismiss: () -> Unit,
) {
    AnimatedVisibility(
        visible = request != null && isVisible,
        enter = slideInVertically { -it } + expandVertically(),
        exit = slideOutVertically { -it } + shrinkVertically(),
    ) {
        request?.let { (friendship, profile) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF22D3EE))))
                    .clickable { onNavigateToFriends(friendship) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.People, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            S.current.friendRequestReceived,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            profile.display_name.ifBlank { "Player" },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { onAccept(friendship) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(S.current.accept, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
