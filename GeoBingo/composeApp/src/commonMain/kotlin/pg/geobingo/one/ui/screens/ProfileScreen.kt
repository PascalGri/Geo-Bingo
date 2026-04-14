package pg.geobingo.one.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.i18n.S
import pg.geobingo.one.network.AccountManager
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.platform.SettingsKeys
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.components.PlayerBanner
import pg.geobingo.one.ui.components.PlayerBannerSize
import pg.geobingo.one.ui.components.rememberLocalUserCosmetics
import pg.geobingo.one.ui.theme.*

private val ProfileGradient = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val profileVersion = AccountManager.profileVersion
    val isLoggedIn = AccountManager.isLoggedIn
    val playerName = remember(profileVersion) { AppSettings.getString("last_player_name", "Player") }
    val avatarBytes = remember(profileVersion) { LocalPhotoStore.loadAvatar("profile") }

    val gamesPlayed = AppSettings.getInt(SettingsKeys.GAMES_PLAYED, 0)
    val gamesWon = AppSettings.getInt(SettingsKeys.GAMES_WON, 0)
    val winRate = if (gamesPlayed > 0) (gamesWon * 100 / gamesPlayed) else 0
    val longestStreak = AppSettings.getInt(SettingsKeys.LONGEST_WIN_STREAK, 0)
    val currentStreak = AppSettings.getInt(SettingsKeys.CURRENT_WIN_STREAK, 0)
    val totalStarsEarned = AppSettings.getInt(SettingsKeys.TOTAL_STARS_EARNED, 0)
    val totalStarsCount = AppSettings.getInt(SettingsKeys.TOTAL_STARS_COUNT, 0)
    val avgRating = if (totalStarsCount > 0) totalStarsEarned.toFloat() / totalStarsCount else 0f

    SystemBackHandler { nav.goBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = S.current.profile,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = ProfileGradient,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.goHome() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = S.current.back, tint = ColorPrimary)
                    }
                },
                actions = { pg.geobingo.one.ui.components.TopBarStarsAndProfile(gameState = gameState, onNavigate = { nav.navigateTo(it) }) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        containerColor = ColorBackground,
    ) { padding ->
        val localCosmetics = rememberLocalUserCosmetics()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Rocket-League-style player banner ─────────────────────
            PlayerBanner(
                name = playerName,
                cosmetics = localCosmetics,
                avatarBytes = avatarBytes,
                avatarColor = ProfileGradient.first(),
                size = PlayerBannerSize.Hero,
                subtitle = if (isLoggedIn) AccountManager.currentUser?.email else null,
            )

            Spacer(Modifier.height(20.dp))

            // Stats grid — only shown for signed-in users; guests see the
            // sign-in CTA at the bottom instead.
            if (isLoggedIn) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileStatCard(Icons.Default.SportsEsports, "$gamesPlayed", S.current.gamesPlayed, ProfileGradient, Modifier.weight(1f))
                    ProfileStatCard(Icons.Default.EmojiEvents, "$gamesWon", S.current.wins, ProfileGradient, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileStatCard(Icons.Default.Percent, "$winRate%", S.current.winRate, ProfileGradient, Modifier.weight(1f))
                    ProfileStatCard(Icons.Default.LocalFireDepartment, "$longestStreak", S.current.winStreak, ProfileGradient, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileStatCard(Icons.Default.Star, "${(avgRating * 10).toInt() / 10.0}", S.current.averageRating, ProfileGradient, Modifier.weight(1f))
                    ProfileStatCard(Icons.Default.Bolt, "$currentStreak", S.current.currentStreak, ProfileGradient, Modifier.weight(1f))
                }
            } else {
                // Guest: show sign-in explanation instead of empty/zero stats
                Spacer(Modifier.height(8.dp))
                Text(
                    S.current.signInRequiredDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorOnSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }

            Spacer(Modifier.height(24.dp))

            // Quick links (history/activity also gate themselves — keep visible so
            // logged-in users can navigate; guests will see the sign-in prompt)
            OutlinedButton(
                onClick = { nav.navigateTo(Screen.HISTORY) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ProfileGradient.first().copy(alpha = 0.5f)),
            ) {
                Icon(Icons.Default.History, null, tint = ProfileGradient.first(), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(S.current.history, color = ProfileGradient.first())
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = { nav.navigateTo(Screen.ACTIVITY_FEED) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ProfileGradient.first().copy(alpha = 0.5f)),
            ) {
                Icon(Icons.Default.RssFeed, null, tint = ProfileGradient.first(), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(S.current.activityFeed, color = ProfileGradient.first())
            }

            if (!isLoggedIn) {
                Spacer(Modifier.height(16.dp))
                GradientButton(
                    text = S.current.signIn,
                    onClick = { nav.navigateTo(Screen.ACCOUNT) },
                    gradientColors = ProfileGradient,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    GradientBorderCard(
        modifier = modifier,
        cornerRadius = 14.dp,
        borderColors = gradientColors,
        backgroundColor = ColorSurface,
        borderWidth = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, null, tint = gradientColors.first(), modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ColorOnSurface)
            Text(label, style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant)
        }
    }
}
