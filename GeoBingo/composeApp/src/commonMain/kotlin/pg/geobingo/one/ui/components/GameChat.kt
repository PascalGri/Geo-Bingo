package pg.geobingo.one.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pg.geobingo.one.game.GameState
import pg.geobingo.one.i18n.S
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.ModerationManager
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.util.AppLogger

@Composable
fun GameChatOverlay(
    gameState: GameState,
    modifier: Modifier = Modifier,
) {
    val gameId = gameState.session.gameId ?: return
    val myPlayerId = gameState.session.myPlayerId ?: return
    val myName = gameState.gameplay.players.find { it.id == myPlayerId }?.name ?: "Player"
    val scope = rememberCoroutineScope()
    val syncManager = gameState.syncManager

    var expanded by remember { mutableStateOf(false) }
    var messages by remember { mutableStateOf<List<GameRepository.ChatMessageDto>>(emptyList()) }
    var messageInput by remember { mutableStateOf("") }
    var unreadCount by remember { mutableStateOf(0) }
    var blockedIdsVersion by remember { mutableStateOf(0) }
    var moderationTargetMsg by remember { mutableStateOf<GameRepository.ChatMessageDto?>(null) }
    var moderationToast by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    // Map player_id → user_id via the game's player list. Needed because chat
    // messages only carry player_id, but user-level blocking persists across
    // games and must key on user_id.
    val playerIdToUserId = remember(gameState.gameplay.players) {
        gameState.gameplay.players.mapNotNull { p -> p.userId?.let { p.id to it } }.toMap()
    }
    val blockedUserIds = remember(blockedIdsVersion) { ModerationManager.blockedUserIds() }
    val visibleMessages = remember(messages, blockedUserIds) {
        messages.filter { msg ->
            val uid = playerIdToUserId[msg.player_id]
            uid == null || uid !in blockedUserIds
        }
    }

    // Load initial messages
    LaunchedEffect(gameId) {
        try {
            messages = GameRepository.getChatMessages(gameId)
        } catch (e: Exception) {
            AppLogger.d("Chat", "Load failed", e)
        }
    }

    // Listen for new messages via realtime
    LaunchedEffect(syncManager) {
        syncManager?.chatMessageInserted?.collect { msg ->
            messages = messages + msg
            if (!expanded) unreadCount++
            // Auto-scroll to bottom
            if (expanded && messages.isNotEmpty()) {
                try { listState.animateScrollToItem(messages.size - 1) } catch (e: Exception) {
                    AppLogger.d("Chat", "Auto-scroll failed", e)
                }
            }
        }
    }

    // Reset unread when expanding
    LaunchedEffect(expanded) {
        if (expanded) {
            unreadCount = 0
            if (messages.isNotEmpty()) {
                try { listState.animateScrollToItem(messages.size - 1) } catch (e: Exception) {
                    AppLogger.d("Chat", "Scroll on expand failed", e)
                }
            }
        }
    }

    // ── Moderation dialog (report / block) ─────────────────────────────────
    moderationTargetMsg?.let { msg ->
        val reportedUserId = playerIdToUserId[msg.player_id]
        AlertDialog(
            onDismissRequest = { moderationTargetMsg = null },
            containerColor = ColorSurface,
            title = {
                Text(
                    S.current.moderationTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = ColorOnSurface,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        S.current.moderationSubtitle(msg.player_name),
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val target = moderationTargetMsg ?: return@clickable
                                moderationTargetMsg = null
                                scope.launch {
                                    ModerationManager.reportContent(
                                        contentType = "chat_message",
                                        contentId = target.id,
                                        contentSnapshot = target.message,
                                        reportedUserId = reportedUserId,
                                    )
                                    moderationToast = S.current.reportSubmitted
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Default.Flag, null, tint = ColorError, modifier = Modifier.size(18.dp))
                        Text(
                            S.current.reportMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorOnSurface,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = reportedUserId != null) {
                                val uid = reportedUserId ?: return@clickable
                                ModerationManager.blockUser(uid)
                                blockedIdsVersion++
                                moderationTargetMsg = null
                                moderationToast = S.current.userBlocked
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Default.Block, null, tint = ColorError, modifier = Modifier.size(18.dp))
                        Column {
                            Text(
                                S.current.blockUser,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (reportedUserId != null) ColorOnSurface else ColorOnSurfaceVariant,
                            )
                            if (reportedUserId == null) {
                                Text(
                                    S.current.blockUnavailableGuest,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ColorOnSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { moderationTargetMsg = null }) {
                    Text(S.current.cancel, color = ColorOnSurfaceVariant)
                }
            },
        )
    }

    // Toast-like ephemeral confirmation after report / block.
    moderationToast?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(2000L)
            moderationToast = null
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ColorSurface,
                shadowElevation = 6.dp,
            ) {
                Text(
                    msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorOnSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }

    Column(modifier = modifier) {
        // Chat toggle button
        Box(
            modifier = Modifier
                .align(Alignment.End)
                .padding(end = 8.dp, bottom = 4.dp)
                .clip(CircleShape)
                .background(ColorSurface)
                .clickable { expanded = !expanded }
                .padding(10.dp),
        ) {
            Icon(
                if (expanded) Icons.Default.ExpandMore else Icons.Default.ChatBubble,
                null,
                tint = ColorPrimary,
                modifier = Modifier.size(20.dp),
            )
            if (unreadCount > 0 && !expanded) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$unreadCount", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Chat panel
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp),
                color = ColorSurface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                shadowElevation = 8.dp,
            ) {
                Column {
                    // Messages list
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(visibleMessages, key = { it.id }) { msg ->
                            val isMe = msg.player_id == myPlayerId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    "${msg.player_name}: ",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMe) ColorPrimary else ColorOnSurfaceVariant,
                                )
                                Text(
                                    msg.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ColorOnSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                // Moderation menu on others' messages. Required by
                                // App-Store Guideline 1.2: report + block must exist
                                // for every piece of user-generated content.
                                if (!isMe) {
                                    IconButton(
                                        onClick = { moderationTargetMsg = msg },
                                        modifier = Modifier.size(22.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = S.current.moderationMenu,
                                            modifier = Modifier.size(14.dp),
                                            tint = ColorOnSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Input row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = messageInput,
                            onValueChange = { if (it.length <= 100) messageInput = it },
                            placeholder = { Text(S.current.typeMessage, style = MaterialTheme.typography.bodySmall, color = ColorOnSurfaceVariant) },
                            modifier = Modifier.weight(1f).height(44.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = ColorOnSurface),
                            shape = RoundedCornerShape(22.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ColorPrimary,
                                unfocusedBorderColor = ColorOutline,
                                focusedContainerColor = ColorSurfaceVariant,
                                unfocusedContainerColor = ColorSurfaceVariant,
                                cursorColor = ColorPrimary,
                            ),
                        )
                        IconButton(
                            onClick = {
                                val text = messageInput.trim()
                                if (text.isNotEmpty()) {
                                    messageInput = ""
                                    scope.launch {
                                        try {
                                            GameRepository.sendChatMessage(gameId, myPlayerId, myName, text)
                                        } catch (e: Exception) {
                                            AppLogger.w("Chat", "Send failed", e)
                                        }
                                    }
                                }
                            },
                            enabled = messageInput.trim().isNotEmpty(),
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = ColorPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
