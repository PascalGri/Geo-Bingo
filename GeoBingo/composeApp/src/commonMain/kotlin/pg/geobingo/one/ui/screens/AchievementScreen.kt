package pg.geobingo.one.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.state.Achievement
import pg.geobingo.one.game.state.AchievementManager
import pg.geobingo.one.i18n.S
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    SystemBackHandler { nav.goBack() }

    val allAchievements = remember { AchievementManager.ALL_ACHIEVEMENTS }
    val unlockedCount = remember { AchievementManager.getUnlockedCount() }
    val total = allAchievements.size

    val anim = rememberStaggeredAnimation(count = total + 2)
    fun Modifier.staggered(index: Int): Modifier = this.then(anim.modifier(index))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = S.current.achievementsTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = GradientPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.goHome() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = S.current.back,
                            tint = ColorPrimary,
                        )
                    }
                },
                actions = {
                    pg.geobingo.one.ui.components.TopBarStarsAndProfile(
                        gameState = gameState,
                        onNavigate = { nav.navigateTo(it) },
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        containerColor = ColorBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Progress header ──────────────────────────────────────────────
            GradientBorderCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .staggered(0),
                borderColors = GradientPrimary,
                backgroundColor = ColorSurface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = S.current.achievementsUnlocked(unlockedCount, total),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorOnSurface,
                        )
                        Text(
                            text = "$unlockedCount/$total",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorPrimary,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { if (total > 0) unlockedCount.toFloat() / total else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = ColorPrimary,
                        trackColor = ColorSurfaceVariant,
                    )
                }
            }

            // ── Achievement grid (2 columns) ─────────────────────────────────
            val rows = allAchievements.chunked(2)
            rows.forEachIndexed { rowIndex, rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowItems.forEachIndexed { colIndex, achievement ->
                        val globalIndex = rowIndex * 2 + colIndex + 1
                        val unlocked = remember { AchievementManager.isUnlocked(achievement.id) }
                        AchievementCard(
                            achievement = achievement,
                            unlocked = unlocked,
                            modifier = Modifier
                                .weight(1f)
                                .staggered(globalIndex),
                        )
                    }
                    // Fill remainder if odd item in last row
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AchievementCard(
    achievement: Achievement,
    unlocked: Boolean,
    modifier: Modifier = Modifier,
) {
    val s = S.current
    val name = achievementName(achievement.id, s)
    val description = achievementDescription(achievement.id, s)

    val borderColors = if (unlocked) GradientPrimary else listOf(ColorOutline.copy(alpha = 0.35f), ColorOutline.copy(alpha = 0.2f))
    val iconTint = if (unlocked) ColorPrimary else ColorOnSurfaceVariant.copy(alpha = 0.35f)

    GradientBorderCard(
        modifier = modifier,
        borderColors = borderColors,
        backgroundColor = ColorSurface,
        durationMillis = if (unlocked) 4000 else 20000,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = achievement.icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = iconTint,
            )
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (unlocked) ColorOnSurface else ColorOnSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 16.sp,
            )
            Text(
                text = if (unlocked) description else s.achievementsLocked,
                style = MaterialTheme.typography.bodySmall,
                color = if (unlocked) ColorOnSurfaceVariant else ColorOnSurfaceVariant.copy(alpha = 0.3f),
                textAlign = TextAlign.Center,
                maxLines = 3,
                lineHeight = 14.sp,
                fontSize = 11.sp,
            )
        }
    }
}

private fun achievementName(id: String, s: pg.geobingo.one.i18n.StringRes): String = when (id) {
    "perfect_game"   -> s.achievementPerfectGame
    "speed_demon"    -> s.achievementSpeedDemon
    "ten_games"      -> s.achievementTenGames
    "fifty_games"    -> s.achievementFiftyGames
    "score_300"      -> s.achievementScore300
    "score_500"      -> s.achievementScore500
    "outdoor_10"     -> s.achievementOutdoor10
    "indoor_10"      -> s.achievementIndoor10
    "star_100"       -> s.achievementStar100
    "star_500"       -> s.achievementStar500
    "all_captured_10"-> s.achievementAllCaptured10
    "marathon"       -> s.achievementMarathon
    "perfect_3"      -> s.achievementPerfect3
    "fast_finish"    -> s.achievementFastFinish
    else             -> id
}

private fun achievementDescription(id: String, s: pg.geobingo.one.i18n.StringRes): String = when (id) {
    "perfect_game"   -> s.achievementPerfectGameDesc
    "speed_demon"    -> s.achievementSpeedDemonDesc
    "ten_games"      -> s.achievementTenGamesDesc
    "fifty_games"    -> s.achievementFiftyGamesDesc
    "score_300"      -> s.achievementScore300Desc
    "score_500"      -> s.achievementScore500Desc
    "outdoor_10"     -> s.achievementOutdoor10Desc
    "indoor_10"      -> s.achievementIndoor10Desc
    "star_100"       -> s.achievementStar100Desc
    "star_500"       -> s.achievementStar500Desc
    "all_captured_10"-> s.achievementAllCaptured10Desc
    "marathon"       -> s.achievementMarathonDesc
    "perfect_3"      -> s.achievementPerfect3Desc
    "fast_finish"    -> s.achievementFastFinishDesc
    else             -> ""
}
