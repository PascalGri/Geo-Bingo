package pg.geobingo.one.ui.screens

import kotlinx.datetime.toLocalDateTime
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Style
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pg.geobingo.one.data.Player
import pg.geobingo.one.game.GameHistoryEntry
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.HistoryPlayer
import pg.geobingo.one.game.Screen
import pg.geobingo.one.network.parseHexColor
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(gameState: GameState) {
    SystemBackHandler { gameState.session.currentScreen = Screen.HOME }

    // Fade-in animation for content
    val contentAlpha = remember { Animatable(0f) }
    val contentOffset = remember { Animatable(40f) }
    LaunchedEffect(Unit) {
        launch { contentOffset.animateTo(0f, tween(400)) }
        contentAlpha.animateTo(1f, tween(400))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = "Spielverlauf",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = GradientPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { gameState.session.currentScreen = Screen.HOME }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = ColorPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        containerColor = ColorBackground,
    ) { padding ->
        if (gameState.ui.gameHistory.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).graphicsLayer { translationY = contentOffset.value; alpha = contentAlpha.value },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Default.History, null, modifier = Modifier.size(48.dp), tint = ColorOnSurfaceVariant)
                    Text(
                        "Noch keine Runden gespielt",
                        style = MaterialTheme.typography.bodyLarge,
                        color = ColorOnSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "Gespielte Runden erscheinen hier.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.screenHorizontal).graphicsLayer { translationY = contentOffset.value; alpha = contentAlpha.value },
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                itemsIndexed(gameState.ui.gameHistory, key = { _, entry -> entry.gameCode }) { index, entry ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                gameState.ui.gameHistory = gameState.ui.gameHistory.filterIndexed { i, _ -> i != index }
                                true
                            } else false
                        },
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color by animateColorAsState(
                                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) ColorError.copy(alpha = 0.3f) else Color.Transparent,
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = ColorError)
                            }
                        },
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true,
                    ) {
                        HistoryEntryCard(entry = entry, isLatest = index == 0)
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(entry: GameHistoryEntry, isLatest: Boolean) {
    // Load avatars from local cache
    var avatarBytes by remember { mutableStateOf(mapOf<String, ByteArray>()) }
    LaunchedEffect(entry) {
        val loaded = mutableMapOf<String, ByteArray>()
        entry.players.forEach { hp ->
            try {
                val bytes = LocalPhotoStore.loadAvatar(hp.id)
                if (bytes != null) loaded[hp.id] = bytes
            } catch (_: Exception) {}
        }
        if (loaded.isNotEmpty()) avatarBytes = loaded
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isLatest) ColorPrimary.copy(alpha = 0.4f) else ColorOutlineVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    if (entry.date.isNotBlank()) {
                        val dateText = try {
                            val instant = kotlinx.datetime.Instant.parse(entry.date)
                            val local = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                            val day = local.dayOfMonth.toString().padStart(2, '0')
                            val month = local.monthNumber.toString().padStart(2, '0')
                            val hour = local.hour.toString().padStart(2, '0')
                            val minute = local.minute.toString().padStart(2, '0')
                            "$day.$month.${local.year}  $hour:$minute"
                        } catch (_: Exception) { "" }
                        if (dateText.isNotEmpty()) {
                            Text(
                                dateText,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorOnSurface,
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            entry.gameCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorOnSurfaceVariant,
                            fontSize = 10.sp,
                        )
                        if (entry.jokerMode) {
                            Icon(Icons.Default.Style, null, modifier = Modifier.size(14.dp), tint = ColorOnSurfaceVariant)
                        }
                        Text(
                            "${entry.totalCategories} Kategorien",
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorOnSurfaceVariant,
                            fontSize = 10.sp,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        entry.playerName,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnSurfaceVariant,
                    )
                    Text(
                        "${entry.score} Pkt.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorPrimary,
                    )
                }
            }

            HorizontalDivider(color = ColorOutlineVariant)

            // Rankings with avatars
            entry.players.forEachIndexed { i, hp ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val rankColor = when (i) {
                        0 -> Color(0xFFFBBF24) // Gold
                        1 -> Color(0xFF94A3B8) // Silver
                        2 -> Color(0xFFCD7F32) // Bronze
                        else -> ColorOnSurfaceVariant
                    }
                    Text(
                        "${i + 1}.",
                        fontSize = 13.sp,
                        fontWeight = if (i < 3) FontWeight.Bold else FontWeight.Normal,
                        color = rankColor,
                    )
                    PlayerAvatarView(
                        player = Player(id = hp.id, name = hp.name, color = parseHexColor(hp.colorHex)),
                        size = 24.dp,
                        fontSize = 10.sp,
                        photoBytes = avatarBytes[hp.id],
                    )
                    Text(
                        hp.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (hp.name == entry.playerName) FontWeight.Bold else FontWeight.Normal,
                        color = if (hp.name == entry.playerName) ColorPrimary else ColorOnSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${hp.score} Pkt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnSurfaceVariant,
                    )
                }
            }
        }
    }
}
