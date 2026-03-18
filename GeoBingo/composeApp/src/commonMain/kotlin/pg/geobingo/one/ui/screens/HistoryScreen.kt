package pg.geobingo.one.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.data.Player
import pg.geobingo.one.game.GameHistoryEntry
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.HistoryPlayer
import pg.geobingo.one.game.Screen
import pg.geobingo.one.network.parseHexColor
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(gameState: GameState) {
    SystemBackHandler { gameState.currentScreen = Screen.HOME }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Spielverlauf",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorOnSurface,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { gameState.currentScreen = Screen.HOME }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück", tint = ColorPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        containerColor = ColorBackground,
    ) { padding ->
        if (gameState.gameHistory.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("📋", fontSize = 48.sp)
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
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                itemsIndexed(gameState.gameHistory) { index, entry ->
                    HistoryEntryCard(entry = entry, isLatest = index == 0)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AnimatedGradientText(
                            text = "Code: ${entry.gameCode}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            gradientColors = GradientPrimary,
                            durationMillis = 3000,
                        )
                        if (entry.jokerMode) {
                            Text(
                                "🃏",
                                fontSize = 14.sp,
                            )
                        }
                    }
                    Text(
                        "${entry.totalCategories} Kategorien",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnSurfaceVariant,
                    )
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
                    val medal = when (i) {
                        0 -> "🥇"
                        1 -> "🥈"
                        2 -> "🥉"
                        else -> "${i + 1}."
                    }
                    Text(medal, fontSize = if (i < 3) 16.sp else 12.sp)
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
