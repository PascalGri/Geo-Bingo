package pg.geobingo.one.ui.screens

import kotlinx.datetime.toLocalDateTime
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Login
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

    val anim = rememberStaggeredAnimation(count = 8)
    val btnOffsets = (0..1).map { remember { Animatable(80f) } }
    val btnAlphas = (0..1).map { remember { Animatable(0f) } }
    LaunchedEffect(Unit) {
        for (i in btnOffsets.indices) {
            launch {
                delay(180L + i * 100L)
                launch { btnOffsets[i].animateTo(0f, tween(500)) }
                btnAlphas[i].animateTo(1f, tween(500))
            }
        }
    }

    fun Modifier.staggered(index: Int): Modifier = this.then(anim.modifier(index))

    var selectedHistoryEntry by remember { mutableStateOf<GameHistoryEntry?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ColorBackground,
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize().background(ColorBackground)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                // ── HERO ──────────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggered(0),
                ) {
                    AnimatedGradientBox(
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        gradientColors = GradientPrimary,
                        durationMillis = 5000,
                    ) {}
                    // Dark overlay for readability
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.10f),
                                        Color.Black.copy(alpha = 0.50f),
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color.White.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = Color.White,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "KatchIt!",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 54.sp,
                                letterSpacing = (-1.5).sp,
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Fotografiere. Vergleiche. Gewinne.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                letterSpacing = 0.3.sp,
                            ),
                            color = Color.White.copy(alpha = 0.88f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                // ── FEATURE PILLS ─────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .staggered(1)
                        .padding(top = 16.dp, bottom = 4.dp)
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    FeaturePill(icon = Icons.Default.People, label = "Mit Freunden")
                    FeaturePill(icon = Icons.Default.LocationOn, label = "Stadtabenteuer")
                    FeaturePill(icon = Icons.Default.HowToVote, label = "Live-Voting")
                }

                Spacer(Modifier.height(20.dp))

                // ── CTA BUTTONS ───────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .padding(horizontal = Spacing.screenHorizontal)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GradientButton(
                        text = "Runde erstellen",
                        onClick = { gameState.session.currentScreen = Screen.CREATE_GAME },
                        modifier = Modifier.fillMaxWidth().graphicsLayer {
                            translationY = btnOffsets[0].value
                            alpha = btnAlphas[0].value
                        },
                        gradientColors = GradientPrimary,
                        height = 62.dp,
                        fontSize = 17.sp,
                        leadingIcon = {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(22.dp), tint = Color.White)
                        },
                    )

                    OutlinedButton(
                        onClick = { gameState.session.currentScreen = Screen.JOIN_GAME },
                        modifier = Modifier.fillMaxWidth().height(62.dp).graphicsLayer {
                            translationY = btnOffsets[1].value
                            alpha = btnAlphas[1].value
                        },
                        shape = RoundedCornerShape(31.dp),
                        border = BorderStroke(1.5.dp, ColorPrimary.copy(alpha = 0.55f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorOnSurface),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Login,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = ColorPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Runde beitreten",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            color = ColorOnSurface,
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // ── HOW IT WORKS ──────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .staggered(2)
                        .padding(horizontal = Spacing.screenHorizontal)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "So einfach geht's",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ColorOnSurface,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                    BoldStep(
                        number = "1",
                        icon = Icons.Default.GridView,
                        title = "Kategorien wählen",
                        desc = "Erstelle eine Runde und lade Freunde ein",
                        gradientColors = GradientWarm,
                    )
                    BoldStep(
                        number = "2",
                        icon = Icons.Default.CameraAlt,
                        title = "Raus in die Stadt",
                        desc = "Fotografiere Motive so kreativ wie möglich",
                        gradientColors = GradientPrimary,
                    )
                    BoldStep(
                        number = "3",
                        icon = Icons.Default.HowToVote,
                        title = "Abstimmen & gewinnen",
                        desc = "Wähle die besten Fotos und küre den Sieger",
                        gradientColors = GradientCool,
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ── PRIVACY HINT ──────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .staggered(3)
                        .padding(horizontal = Spacing.screenHorizontal)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        Icons.Default.GppMaybe,
                        contentDescription = null,
                        tint = ColorOnSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(13.dp).padding(top = 1.dp),
                    )
                    Text(
                        text = "Fotografiere keine Personen ohne deren Zustimmung (\u00A7 22 KUG).",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorOnSurfaceVariant.copy(alpha = 0.55f),
                        lineHeight = 14.sp,
                    )
                }

                Spacer(Modifier.height(28.dp))

                // ── HISTORY ───────────────────────────────────────────────────
                if (gameState.ui.gameHistory.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .staggered(4)
                            .padding(horizontal = Spacing.screenHorizontal)
                            .fillMaxWidth(),
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
                            HomeHistoryCard(entry = entry, onClick = { selectedHistoryEntry = entry })
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // ── FOOTER LINKS ──────────────────────────────────────────────
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.staggered(5),
                ) {
                    TextButton(onClick = { gameState.session.currentScreen = Screen.HOW_TO_PLAY }) {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = ColorOnSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
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
                            modifier = Modifier.size(14.dp),
                            tint = ColorOnSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Einstellungen",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorOnSurfaceVariant,
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.staggered(6),
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

        selectedHistoryEntry?.let { entry ->
            RoundWinnerDialog(entry = entry, onDismiss = { selectedHistoryEntry = null })
        }
    }
}

@Composable
private fun FeaturePill(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ColorSurface)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = ColorPrimary,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = ColorOnSurface,
        )
    }
}

@Composable
private fun BoldStep(
    number: String,
    icon: ImageVector,
    title: String,
    desc: String,
    gradientColors: List<Color>,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ColorSurface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AnimatedGradientBox(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(15.dp)),
            gradientColors = gradientColors,
            durationMillis = 4000,
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp), tint = Color.White)
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                "Schritt $number",
                style = MaterialTheme.typography.labelSmall,
                color = ColorOnSurfaceVariant,
            )
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = ColorOnSurface,
            )
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = ColorOnSurfaceVariant,
                lineHeight = 16.sp,
            )
        }
    }
}

private fun formatHistoryDate(isoDate: String): String {
    if (isoDate.isBlank()) return ""
    return try {
        val instant = kotlinx.datetime.Instant.parse(isoDate)
        val local = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        val day = local.dayOfMonth.toString().padStart(2, '0')
        val month = local.monthNumber.toString().padStart(2, '0')
        val hour = local.hour.toString().padStart(2, '0')
        val minute = local.minute.toString().padStart(2, '0')
        "$day.$month.${local.year}  $hour:$minute"
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
