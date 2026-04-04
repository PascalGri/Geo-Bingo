package pg.geobingo.one.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.game.Screen
import pg.geobingo.one.i18n.S
import pg.geobingo.one.ui.theme.*

enum class NavTab(val icon: ImageVector, val targetScreen: Screen) {
    HOME(Icons.Default.Home, Screen.HOME),
    FRIENDS(Icons.Default.People, Screen.FRIENDS),
    SHOP(Icons.Default.Storefront, Screen.COSMETIC_SHOP),
    PROFILE(Icons.Default.Person, Screen.ACCOUNT),
}

/** Screens on which the bottom nav bar should be visible. */
fun Screen.showsBottomNav(): Boolean = this in setOf(
    Screen.HOME, Screen.FRIENDS, Screen.COSMETIC_SHOP, Screen.SHOP,
    Screen.SETTINGS, Screen.STATS, Screen.ACHIEVEMENTS, Screen.HISTORY,
    Screen.ACCOUNT, Screen.PROFILE, Screen.ACTIVITY_FEED,
    Screen.SOLO_LEADERBOARD, Screen.MP_LEADERBOARD,
)

/** Resolve which NavTab is active for the current screen. */
fun Screen.activeTab(): NavTab? = when (this) {
    Screen.HOME, Screen.HISTORY,
    Screen.SOLO_LEADERBOARD, Screen.MP_LEADERBOARD -> NavTab.HOME
    Screen.FRIENDS, Screen.ACTIVITY_FEED -> NavTab.FRIENDS
    Screen.COSMETIC_SHOP, Screen.SHOP -> NavTab.SHOP
    Screen.ACCOUNT, Screen.PROFILE, Screen.SETTINGS,
    Screen.STATS, Screen.ACHIEVEMENTS -> NavTab.PROFILE
    else -> null
}

@Composable
fun BottomNavBar(
    currentScreen: Screen,
    onTabSelected: (Screen) -> Unit,
) {
    val activeTab = currentScreen.activeTab()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorSurface)
            .navigationBarsPadding(),
    ) {
        // Top border line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(ColorOutline.copy(alpha = 0.3f))
                .align(Alignment.TopCenter),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavTab.entries.forEach { tab ->
                val selected = tab == activeTab
                NavBarItem(
                    tab = tab,
                    selected = selected,
                    label = when (tab) {
                        NavTab.HOME -> "Home"
                        NavTab.FRIENDS -> S.current.friends
                        NavTab.SHOP -> "Shop"
                        NavTab.PROFILE -> "Profil"
                    },
                    onClick = { onTabSelected(tab.targetScreen) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    tab: NavTab,
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconColor by animateColorAsState(
        targetValue = if (selected) ColorPrimary else ColorOnSurfaceVariant.copy(alpha = 0.6f),
        animationSpec = tween(200),
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) ColorPrimary else ColorOnSurfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(200),
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    )

    // Animated shimmer for Shop tab
    val isShop = tab == NavTab.SHOP
    val shimmer = rememberInfiniteTransition(label = "shopShimmer")
    val shimmerOffset by shimmer.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOff",
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Glow background for selected
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ColorPrimary.copy(alpha = 0.12f)),
                )
            }
            Icon(
                tab.icon,
                contentDescription = label,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                tint = if (isShop && !selected) {
                    // Animated gradient-like color for shop icon when not selected
                    Color(
                        red = 0.85f + 0.15f * kotlin.math.sin(shimmerOffset * 6.28f).toFloat(),
                        green = 0.55f + 0.15f * kotlin.math.cos(shimmerOffset * 6.28f).toFloat(),
                        blue = 0.94f,
                        alpha = 0.8f,
                    )
                } else iconColor,
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = labelColor,
        )
    }
}
