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
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.*
import pg.geobingo.one.i18n.S
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.ui.theme.Spacing
import pg.geobingo.one.ui.theme.rememberStaggeredAnimation

@Composable
fun HomeScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(gameState.ui.pendingToast) {
        val msg = gameState.ui.pendingToast ?: return@LaunchedEffect
        gameState.ui.pendingToast = null
        snackbarHostState.showSnackbar(msg)
    }

    val anim = rememberStaggeredAnimation(count = 5)
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
                    // Dark base
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF1C0030),
                                        Color(0xFF0D0818),
                                        ColorBackground,
                                    )
                                )
                            )
                    )
                    // Diagonal accent: rose bottom-left to purple top-right
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFF43F5E).copy(alpha = 0.13f),
                                        Color.Transparent,
                                        Color(0xFFA855F7).copy(alpha = 0.09f),
                                    ),
                                    start = Offset(0f, 300f),
                                    end = Offset(500f, 0f),
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
                        AnimatedHeroTitle()
                        Spacer(Modifier.height(10.dp))
                        HeroTagline()
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── CTA BUTTONS ───────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .padding(horizontal = Spacing.screenHorizontal)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GradientButton(
                        text = S.current.createRound,
                        onClick = { nav.navigateTo(Screen.SELECT_MODE) },
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
                        onClick = { nav.navigateTo(Screen.JOIN_GAME) },
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
                            S.current.joinRound,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            color = ColorOnSurface,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── HOW TO PLAY ──────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .padding(horizontal = Spacing.screenHorizontal)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ColorSurfaceVariant.copy(alpha = 0.4f))
                        .clickable { nav.navigateTo(Screen.HOW_TO_PLAY) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = ColorPrimary.copy(alpha = 0.7f),
                    )
                    Text(
                        S.current.howToPlay,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = ColorOnSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = ColorOnSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── DISCLAIMER ───────────────────────────────────────────────
                Text(
                    S.current.photoConsentDisclaimer,
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .padding(horizontal = Spacing.screenHorizontal + 8.dp)
                        .fillMaxWidth(),
                )

                Spacer(Modifier.height(16.dp))

                // ── HISTORY ───────────────────────────────────────────────────
                if (gameState.ui.gameHistory.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .staggered(2)
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
                                    S.current.recentGames,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorOnSurface,
                                )
                            }
                            if (gameState.ui.gameHistory.size > 3) {
                                TextButton(onClick = { nav.navigateTo(Screen.HISTORY) }) {
                                    Text(
                                        S.current.showAll,
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

                // Push footer to bottom when content is short
                Spacer(Modifier.weight(1f))

                // ── FOOTER ────────────────────────────────────────────────────
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.staggered(3),
                ) {
                    TextButton(onClick = { nav.navigateTo(Screen.SETTINGS) }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = ColorOnSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            S.current.settings,
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorOnSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { nav.navigateTo(Screen.STATS) }) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = ColorOnSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            S.current.statsTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorOnSurfaceVariant,
                        )
                    }
                    val uriHandler = LocalUriHandler.current
                    TextButton(onClick = { uriHandler.openUri("https://katchit.app/impressum.html") }) {
                        Text(
                            S.current.impressum,
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorOutline,
                        )
                    }
                    TextButton(onClick = { uriHandler.openUri("https://katchit.app/datenschutz.html") }) {
                        Text(
                            S.current.privacy,
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorOutline,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        selectedHistoryEntry?.let { entry ->
            RoundWinnerDialog(entry = entry, onDismiss = { selectedHistoryEntry = null })
        }
    }
}

// ── HERO TITLE ────────────────────────────────────────────────────────────────

@Composable
private fun AnimatedHeroTitle() {
    val titleStyle = MaterialTheme.typography.displaySmall.copy(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 58.sp,
        letterSpacing = (-2).sp,
    )
    Box(contentAlignment = Alignment.Center) {
        // Glow layer: blurred copy of the text behind
        AnimatedGradientText(
            text = "KatchIt!",
            style = titleStyle.copy(
                shadow = Shadow(
                    color = Color(0xFFD946EF).copy(alpha = 0.7f),
                    blurRadius = 40f,
                ),
            ),
            gradientColors = GradientPrimary,
            durationMillis = 3000,
            modifier = Modifier.blur(24.dp),
        )
        // Second glow: tighter, more saturated
        AnimatedGradientText(
            text = "KatchIt!",
            style = titleStyle.copy(
                shadow = Shadow(
                    color = Color(0xFFF43F5E).copy(alpha = 0.5f),
                    blurRadius = 20f,
                ),
            ),
            gradientColors = GradientPrimary,
            durationMillis = 3000,
            modifier = Modifier.blur(10.dp),
        )
        // Sharp foreground text
        AnimatedGradientText(
            text = "KatchIt!",
            style = titleStyle,
            gradientColors = GradientPrimary,
            durationMillis = 3000,
        )
    }
}

// ── HERO TAGLINE (Fotografiere · Bewerte · Gewinne) ───────────────────────────

@Composable
private fun HeroTagline() {
    val words = listOf(S.current.heroTagCapture, S.current.heroTagRate, S.current.heroTagWin)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        words.forEachIndexed { index, word ->
            Text(
                text = word,
                style = MaterialTheme.typography.bodyMedium.copy(
                    letterSpacing = 0.2.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = Color.White.copy(alpha = 0.90f),
            )
            if (index < words.lastIndex) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.45f)),
                )
            }
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
                        "${winner.score} ${S.current.pointsAbbrev}",
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
                    "${S.current.roundCode} ${entry.gameCode}",
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
                                "${winner.score} ${S.current.points}",
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
                            "${hp.score} ${S.current.pointsAbbrev}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorOnSurfaceVariant,
                        )
                    }
                }

                Text(
                    "${entry.totalCategories} ${S.current.categories}  |  ${entry.players.size} ${S.current.players}" +
                            if (entry.jokerMode) "  |  Joker" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(S.current.close, color = ColorPrimary)
            }
        },
    )
}
