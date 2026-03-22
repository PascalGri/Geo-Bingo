package pg.geobingo.one.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pg.geobingo.one.game.*
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.ui.theme.Spacing
import pg.geobingo.one.ui.theme.rememberStaggeredAnimation

@Composable
fun HomeScreen(gameState: GameState) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(gameState.ui.pendingToast) {
        val msg = gameState.ui.pendingToast ?: return@LaunchedEffect
        gameState.ui.pendingToast = null
        snackbarHostState.showSnackbar(msg)
    }

    // Staggered entrance animations
    val anim = rememberStaggeredAnimation(count = 9)
    // Bottom buttons slide-up (more pronounced)
    val btnOffsets = (0..1).map { remember { Animatable(80f) } }
    val btnAlphas = (0..1).map { remember { Animatable(0f) } }
    LaunchedEffect(Unit) {
        for (i in btnOffsets.indices) {
            launch {
                delay(300L + i * 80L)
                launch { btnOffsets[i].animateTo(0f, tween(450)) }
                btnAlphas[i].animateTo(1f, tween(450))
            }
        }
    }

    fun Modifier.staggered(index: Int): Modifier = this.then(anim.modifier(index))

    // Dialog state for showing round winner
    var selectedHistoryEntry by remember { mutableStateOf<GameHistoryEntry?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ColorBackground,
    ) { _ ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground),
    ) {
        // Top glow - subtle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(ColorPrimary.copy(alpha = 0.08f), Color.Transparent),
                        radius = 800f,
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(80.dp))

            // Title
            AnimatedGradientText(
                text = "KatchIt!",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 52.sp,
                    letterSpacing = (-1).sp,
                ),
                gradientColors = GradientPrimary,
                durationMillis = 2500,
                modifier = Modifier.staggered(0),
            )

            Spacer(Modifier.height(6.dp))

            AnimatedGradientText(
                text = "Foto-Schnitzeljagd mit Freunden",
                style = MaterialTheme.typography.bodyLarge,
                gradientColors = GradientCool,
                durationMillis = 3000,
                modifier = Modifier.staggered(1),
            )

            Spacer(Modifier.height(40.dp))

            // How it works - vertical steps
            Column(
                modifier = Modifier
                    .staggered(2)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ColorSurface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HomeStep(
                    icon = Icons.Default.GridView,
                    number = "1",
                    text = "Kategorien w\u00E4hlen und Freunde einladen",
                )
                HomeStep(
                    icon = Icons.Default.CameraAlt,
                    number = "2",
                    text = "Raus in die Stadt und Motive fotografieren",
                )
                HomeStep(
                    icon = Icons.Default.HowToVote,
                    number = "3",
                    text = "Abstimmen, wer die besten Fotos hat",
                )
            }

            Spacer(Modifier.height(20.dp))

            // Datenschutz-Hinweis
            Row(
                modifier = Modifier
                    .staggered(3)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ColorSurface)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    Icons.Default.GppMaybe,
                    contentDescription = null,
                    tint = ColorOnSurfaceVariant,
                    modifier = Modifier.size(16.dp).padding(top = 1.dp),
                )
                Text(
                    text = "Fotografiere keine Personen ohne deren Zustimmung. " +
                           "Das Recht am eigenen Bild (\u00A7 22 KUG) sch\u00FCtzt jede Person. " +
                           "Die Verantwortung liegt beim jeweiligen Nutzer.",
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant,
                    lineHeight = 15.sp,
                )
            }

            Spacer(Modifier.height(24.dp))

            // Game History Section - always visible
            if (gameState.ui.gameHistory.isNotEmpty()) {
                Column(
                    modifier = Modifier.staggered(4).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = ColorOnSurfaceVariant,
                            )
                            Text(
                                "Letzte Spiele",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorOnSurface,
                            )
                        }
                        if (gameState.ui.gameHistory.size > 3) {
                            TextButton(onClick = { gameState.session.currentScreen = Screen.HISTORY }) {
                                Text(
                                    "Alle anzeigen",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ColorPrimary,
                                )
                            }
                        }
                    }

                    gameState.ui.gameHistory.take(3).forEach { entry ->
                        HomeHistoryCard(
                            entry = entry,
                            onClick = { selectedHistoryEntry = entry },
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            GradientButton(
                text = "Runde erstellen",
                onClick = { gameState.session.currentScreen = Screen.CREATE_GAME },
                modifier = Modifier.fillMaxWidth().graphicsLayer {
                    translationY = btnOffsets[0].value
                    alpha = btnAlphas[0].value
                },
                gradientColors = GradientPrimary,
                leadingIcon = {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp), tint = Color.White)
                },
            )

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = { gameState.session.currentScreen = Screen.JOIN_GAME },
                modifier = Modifier.fillMaxWidth().height(56.dp).graphicsLayer {
                    translationY = btnOffsets[1].value
                    alpha = btnAlphas[1].value
                },
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, ColorOutline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorOnSurface),
            ) {
                Icon(Icons.Default.Login, null, modifier = Modifier.size(18.dp), tint = ColorOnSurface)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Runde beitreten",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = ColorOnSurface,
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.staggered(6),
            ) {
                TextButton(onClick = { gameState.session.currentScreen = Screen.HOW_TO_PLAY }) {
                    Icon(
                        Icons.Default.HelpOutline,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = ColorOnSurfaceVariant,
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "Wie funktioniert's?",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnSurfaceVariant,
                    )
                }
                TextButton(onClick = { gameState.session.currentScreen = Screen.SETTINGS }) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = ColorOnSurfaceVariant,
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "Einstellungen",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnSurfaceVariant,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.staggered(7),
            ) {
                val uriHandler = LocalUriHandler.current
                TextButton(onClick = { uriHandler.openUri("https://katchit.app/impressum.html") }) {
                    Text(
                        "Impressum",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorOutline,
                    )
                }
                TextButton(onClick = { uriHandler.openUri("https://katchit.app/datenschutz.html") }) {
                    Text(
                        "Datenschutz",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorOutline,
                    )
                }
            }

            Text(
                "KatchIt! v1.0",
                style = MaterialTheme.typography.bodySmall,
                color = ColorOutline,
            )
            Spacer(Modifier.height(28.dp))
        }
    }

    // Winner dialog
    selectedHistoryEntry?.let { entry ->
        RoundWinnerDialog(entry = entry, onDismiss = { selectedHistoryEntry = null })
    }
    } // end Scaffold
}

@Composable
private fun HomeStep(icon: ImageVector, number: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedGradientBox(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp)),
            gradientColors = GradientPrimary,
            durationMillis = 3000,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = ColorOnSurface,
            )
        }
    }
}

private fun formatHistoryDate(isoDate: String): String {
    if (isoDate.isBlank()) return ""
    return try {
        // ISO format: 2026-03-21T14:30:00.123456789Z
        val datePart = isoDate.substringBefore("T")
        val timePart = isoDate.substringAfter("T").substringBefore(".")
            .substringBefore("Z").substringBefore("+")
        val parts = datePart.split("-")
        val timeParts = timePart.split(":")
        if (parts.size >= 3 && timeParts.size >= 2) {
            "${parts[2]}.${parts[1]}.${parts[0]}  ${timeParts[0]}:${timeParts[1]}"
        } else ""
    } catch (_: Exception) {
        ""
    }
}

@Composable
private fun HomeHistoryCard(entry: GameHistoryEntry, onClick: () -> Unit) {
    val winner = entry.players.firstOrNull()
    val dateText = formatHistoryDate(entry.date)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        border = BorderStroke(1.dp, ColorOutlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (dateText.isNotEmpty()) {
                    Text(
                        dateText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = ColorOnSurface,
                    )
                }
                Text(
                    entry.gameCode,
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant,
                    fontSize = 10.sp,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (winner != null) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFFFBBF24),
                    )
                    Text(
                        winner.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorOnSurface,
                    )
                    Text(
                        "${winner.score} Pkt.",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorPrimary,
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = ColorOnSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RoundWinnerDialog(entry: GameHistoryEntry, onDismiss: () -> Unit) {
    val winner = entry.players.firstOrNull()
    val dateText = formatHistoryDate(entry.date)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorSurface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (dateText.isNotEmpty()) {
                    Text(
                        dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnSurfaceVariant,
                    )
                }
                Text(
                    "Runde ${entry.gameCode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant,
                    fontSize = 10.sp,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Winner highlight
                if (winner != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFBBF24).copy(alpha = 0.1f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = Color(0xFFFBBF24),
                        )
                        Column {
                            Text(
                                winner.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorOnSurface,
                            )
                            Text(
                                "${winner.score} Punkte",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorPrimary,
                            )
                        }
                    }
                }

                HorizontalDivider(color = ColorOutlineVariant)

                // All rankings
                entry.players.forEachIndexed { i, hp ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val rankColor = when (i) {
                            0 -> Color(0xFFFBBF24)
                            1 -> Color(0xFF94A3B8)
                            2 -> Color(0xFFCD7F32)
                            else -> ColorOnSurfaceVariant
                        }
                        Text(
                            "${i + 1}.",
                            fontSize = 13.sp,
                            fontWeight = if (i < 3) FontWeight.Bold else FontWeight.Normal,
                            color = rankColor,
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

                // Game info
                Text(
                    "${entry.totalCategories} Kategorien  |  ${entry.players.size} Spieler" +
                            if (entry.jokerMode) "  |  Joker" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schliessen", color = ColorPrimary)
            }
        },
    )
}
