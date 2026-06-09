package pg.geobingo.one.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.game.Screen
import pg.geobingo.one.i18n.S
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.SettingsKeys
import pg.geobingo.one.ui.theme.*

enum class NavTab(val icon: ImageVector, val targetScreen: Screen) {
    HOME(Icons.Default.Home, Screen.HOME),
    FRIENDS(Icons.Default.People, Screen.FRIENDS),
    SHOP(Icons.Default.Storefront, Screen.SHOP),
    SETTINGS(Icons.Default.Settings, Screen.SETTINGS),
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
    Screen.SETTINGS, Screen.ACCOUNT, Screen.PROFILE,
    Screen.STATS, Screen.ACHIEVEMENTS -> NavTab.SETTINGS
    else -> null
}

private fun NavTab.label(): String = when (this) {
    NavTab.HOME -> S.current.navHome
    NavTab.FRIENDS -> S.current.friends
    NavTab.SHOP -> S.current.shop
    NavTab.SETTINGS -> S.current.settingsTitle
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavBar(
    currentScreen: Screen,
    friendsBadgeCount: Int = 0,
    onTabSelected: (NavTab) -> Unit,
    onActiveTabReselected: (NavTab) -> Unit = {},
    onPlayCreate: () -> Unit = {},
    onPlayJoin: () -> Unit = {},
) {
    val activeTab = currentScreen.activeTab()
    val haptic = LocalHapticFeedback.current
    var showPlaySheet by remember { mutableStateOf(false) }

    // How far the raised central "Spielen" FAB pokes above the bar surface.
    val fabOverhang = 20.dp

    Box(modifier = Modifier.fillMaxWidth()) {
        // ── The bar itself, pinned to the bottom. The top padding leaves a
        //    transparent strip so the centred Play FAB can float above it. ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(top = fabOverhang)
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
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavTab.entries.forEachIndexed { index, tab ->
                    // Reserve the centre slot for the floating Play FAB.
                    if (index == 2) Spacer(Modifier.weight(1f))
                    val selected = tab == activeTab
                    NavBarItem(
                        tab = tab,
                        selected = selected,
                        label = tab.label(),
                        badgeCount = if (tab == NavTab.FRIENDS) friendsBadgeCount else 0,
                        onClick = {
                            if (AppSettings.getBoolean(SettingsKeys.HAPTIC_ENABLED, true)) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            if (selected) {
                                onActiveTabReselected(tab)
                            } else {
                                onTabSelected(tab)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // ── Raised central "Spielen" FAB → opens the create/join sheet. ──
        PlayFab(
            modifier = Modifier.align(Alignment.TopCenter),
            onClick = {
                if (AppSettings.getBoolean(SettingsKeys.HAPTIC_ENABLED, true)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                showPlaySheet = true
            },
        )
    }

    if (showPlaySheet) {
        PlaySheet(
            onCreate = {
                showPlaySheet = false
                onPlayCreate()
            },
            onJoin = {
                showPlaySheet = false
                onPlayJoin()
            },
            onDismiss = { showPlaySheet = false },
        )
    }
}

/** Raised, gradient-filled central FAB that launches the create/join sheet. */
@Composable
private fun PlayFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(10.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(Brush.linearGradient(GradientPrimary)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = S.current.play,
                tint = Color.White,
                modifier = Modifier.size(30.dp),
            )
        }
        Text(
            S.current.play,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = ColorPrimary,
        )
    }
}

/** Bottom sheet with the two play entry points (create vs. join a round). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaySheet(
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ColorSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 32.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                S.current.play,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ColorOnSurface,
            )
            Spacer(Modifier.height(4.dp))
            GradientButton(
                text = S.current.createRound,
                onClick = onCreate,
                modifier = Modifier.fillMaxWidth(),
                gradientColors = GradientPrimary,
                height = 58.dp,
                fontSize = 17.sp,
                leadingIcon = {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(22.dp), tint = Color.White)
                },
            )
            OutlinedButton(
                onClick = onJoin,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(29.dp),
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
    }
}

@Composable
private fun NavBarItem(
    tab: NavTab,
    selected: Boolean,
    label: String,
    badgeCount: Int,
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

    // Subtle shimmer pulse for the Shop tab when not selected
    val isShop = tab == NavTab.SHOP
    val shimmer = rememberInfiniteTransition(label = "shopShimmer")
    val shimmerPulse by shimmer.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shopPulse",
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
                    // Subtle warm-to-primary pulse instead of the previous loud color cycling
                    androidx.compose.ui.graphics.lerp(
                        ColorOnSurfaceVariant.copy(alpha = 0.65f),
                        ColorPrimary.copy(alpha = 0.7f),
                        shimmerPulse,
                    )
                } else iconColor,
            )

            // Unread / pending badge (top-right of icon)
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-4).dp)
                        .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                        .clip(CircleShape)
                        .background(ColorError)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
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
