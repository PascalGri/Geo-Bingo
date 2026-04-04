package pg.geobingo.one.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.i18n.S
import pg.geobingo.one.network.AccountManager
import pg.geobingo.one.network.DirectMessageManager
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.util.AppLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectMessageScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val friendId = gameState.ui.selectedDmFriendId ?: run {
        nav.goBack()
        return
    }
    val friendName = gameState.ui.selectedDmFriendName
    val myId = AccountManager.currentUserId ?: ""

    var messages by remember { mutableStateOf<List<DirectMessageManager.DirectMessageDto>>(emptyList()) }
    var messageInput by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Load messages on entry and mark as read
    LaunchedEffect(friendId) {
        try {
            messages = DirectMessageManager.getMessages(friendId)
            DirectMessageManager.markAsRead(friendId)
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        } catch (e: Exception) {
            AppLogger.w("DM", "Initial load failed", e)
        }
    }

    // Poll for new messages every 3 seconds
    LaunchedEffect(friendId) {
        while (true) {
            delay(3_000L)
            try {
                val updated = DirectMessageManager.getMessages(friendId)
                if (updated.size != messages.size) {
                    messages = updated
                    DirectMessageManager.markAsRead(friendId)
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }
            } catch (e: Exception) {
                AppLogger.w("DM", "Poll failed", e)
            }
        }
    }

    SystemBackHandler { nav.goBack() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = friendName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = ColorOnSurface,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.goBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = S.current.back,
                            tint = ColorPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        bottomBar = {
            Surface(
                color = ColorSurface,
                tonalElevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = {
                            Text(S.current.typeMessageHint, color = ColorOnSurfaceVariant)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorPrimary,
                            unfocusedBorderColor = ColorOutline,
                            focusedTextColor = ColorOnSurface,
                            unfocusedTextColor = ColorOnSurface,
                            cursorColor = ColorPrimary,
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val text = messageInput.trim()
                            if (text.isBlank() || sending) return@IconButton
                            messageInput = ""
                            sending = true
                            scope.launch {
                                try {
                                    DirectMessageManager.sendMessage(friendId, text)
                                    messages = DirectMessageManager.getMessages(friendId)
                                    if (messages.isNotEmpty()) {
                                        listState.animateScrollToItem(messages.size - 1)
                                    }
                                } catch (e: Exception) {
                                    AppLogger.w("DM", "Send failed", e)
                                    snackbarHostState.showSnackbar("${S.current.error}: ${e.message}")
                                } finally {
                                    sending = false
                                }
                            }
                        },
                        enabled = messageInput.isNotBlank() && !sending,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = S.current.send,
                            tint = if (messageInput.isNotBlank() && !sending) ColorPrimary else ColorOnSurfaceVariant,
                        )
                    }
                }
            }
        },
        containerColor = ColorBackground,
    ) { padding ->
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    S.current.noMessagesYet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorOnSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(messages, key = { it.id.ifEmpty { it.created_at + it.from_user_id } }) { msg ->
                    MessageBubble(
                        message = msg,
                        isFromMe = msg.from_user_id == myId,
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: DirectMessageManager.DirectMessageDto,
    isFromMe: Boolean,
) {
    val bubbleColor = if (isFromMe) ColorPrimary else ColorSurfaceVariant
    val textColor = if (isFromMe) androidx.compose.ui.graphics.Color.White else ColorOnSurface
    val alignment = if (isFromMe) Arrangement.End else Arrangement.Start

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = alignment,
    ) {
        Column(
            horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isFromMe) 12.dp else 2.dp,
                            bottomEnd = if (isFromMe) 2.dp else 12.dp,
                        )
                    )
                    .background(bubbleColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatDmTimestamp(message.created_at),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = ColorOnSurfaceVariant,
            )
        }
    }
}

private fun formatDmTimestamp(createdAt: String): String {
    if (createdAt.isBlank()) return ""
    // createdAt format: "2024-01-15T14:30:00.000Z" or similar
    return try {
        // Extract just the time portion HH:MM
        val timePart = createdAt.substringAfter("T").substringBefore(".")
        if (timePart.length >= 5) timePart.substring(0, 5) else createdAt
    } catch (e: Exception) {
        createdAt
    }
}
