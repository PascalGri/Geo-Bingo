package pg.geobingo.one.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.i18n.S
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.SettingsKeys
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    SystemBackHandler { nav.goBack() }

    val gamesPlayed = remember { AppSettings.getInt(SettingsKeys.GAMES_PLAYED) }
    val gamesWon = remember { AppSettings.getInt(SettingsKeys.GAMES_WON) }
    val winRate = remember {
        if (gamesPlayed > 0) (gamesWon.toFloat() / gamesPlayed * 100).toInt() else 0
    }
    val totalStarsEarned = remember { AppSettings.getInt(SettingsKeys.TOTAL_STARS_EARNED) }
    val totalStarsCount = remember { AppSettings.getInt(SettingsKeys.TOTAL_STARS_COUNT) }
    val averageRating = remember {
        if (totalStarsCount > 0) totalStarsEarned.toFloat() / totalStarsCount else 0f
    }
    val longestStreak = remember { AppSettings.getInt(SettingsKeys.LONGEST_WIN_STREAK) }

    val anim = rememberStaggeredAnimation(count = 7)
    fun Modifier.staggered(index: Int): Modifier = this.then(anim.modifier(index))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = S.current.statsTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = GradientPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = S.current.back, tint = ColorPrimary)
                    }
                },
                actions = { pg.geobingo.one.ui.components.TopBarStarsAndProfile(gameState = gameState, onNavigate = { nav.navigateTo(it) }) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        containerColor = ColorBackground,
    ) { padding ->
        if (gamesPlayed == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .staggered(0),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.Default.BarChart,
                    title = S.current.noStatsYet,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenHorizontal, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Row 1: Games Played / Games Won
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(
                        icon = Icons.Default.SportsEsports,
                        value = gamesPlayed.toString(),
                        label = S.current.gamesPlayed,
                        modifier = Modifier.weight(1f).staggered(0),
                    )
                    StatCard(
                        icon = Icons.Default.EmojiEvents,
                        value = gamesWon.toString(),
                        label = S.current.gamesWon,
                        modifier = Modifier.weight(1f).staggered(1),
                    )
                }

                // Row 2: Win Rate / Average Rating
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(
                        icon = Icons.Default.Percent,
                        value = "$winRate%",
                        label = S.current.winRate,
                        modifier = Modifier.weight(1f).staggered(2),
                    )
                    StatCard(
                        icon = Icons.Default.Star,
                        value = if (totalStarsCount > 0) {
                            val rounded = (averageRating * 10).toInt()
                            "${rounded / 10}.${rounded % 10}"
                        } else "--",
                        label = S.current.avgRating,
                        modifier = Modifier.weight(1f).staggered(3),
                    )
                }

                // Row 3: Longest Win Streak (full width)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(
                        icon = Icons.Default.LocalFireDepartment,
                        value = longestStreak.toString(),
                        label = S.current.longestStreak,
                        modifier = Modifier.weight(1f).staggered(4),
                    )
                    // Empty spacer to keep 2-column grid consistent
                    Spacer(modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    GradientBorderCard(
        modifier = modifier,
        borderColors = GradientPrimary,
        backgroundColor = ColorSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = ColorPrimary,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = ColorOnSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = ColorOnSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 16.sp,
            )
        }
    }
}
