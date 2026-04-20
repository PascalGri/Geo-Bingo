package pg.geobingo.one.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pg.geobingo.one.game.state.CosmeticsManager
import pg.geobingo.one.network.PlayerCosmetics
import pg.geobingo.one.network.PlayerCosmeticsRepository
import pg.geobingo.one.ui.theme.*

/**
 * Variant for the banner — controls overall sizing and amount of detail.
 */
enum class PlayerBannerSize {
    /** ~52dp tall — used in lobby rows, leaderboard rows, friends list. */
    Compact,
    /** ~80dp tall — used on the profile screen and the cosmetic-shop preview card. */
    Hero,
}

/**
 * Rocket-League-style player banner: gradient background, framed avatar,
 * name with effect, title badge underneath. Used everywhere a player needs
 * to be displayed prominently.
 */
@Composable
fun PlayerBanner(
    name: String,
    cosmetics: PlayerCosmetics,
    modifier: Modifier = Modifier,
    avatarBytes: ByteArray? = null,
    avatarColor: Color = ColorPrimary,
    size: PlayerBannerSize = PlayerBannerSize.Compact,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val background = CosmeticsManager.bannerBackgroundById(cosmetics.bannerBackgroundId)
    val frame = CosmeticsManager.frameById(cosmetics.frameId)
    val title = CosmeticsManager.titleById(cosmetics.titleId)
    val nameEffect = CosmeticsManager.nameEffectById(cosmetics.nameEffectId)

    val avatarSize: Dp = if (size == PlayerBannerSize.Hero) 64.dp else 44.dp
    val height: Dp = if (size == PlayerBannerSize.Hero) 88.dp else 60.dp
    val cornerRadius = 14.dp

    // Drifting background gradient — every banner with 2+ colours subtly animates.
    val reduceMotion = LocalReduceMotion.current
    val canAnimate = !reduceMotion && background.gradientColors.size >= 2
    val transition = rememberInfiniteTransition(label = "bannerBg")
    val gradientShift by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (canAnimate) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bgShift",
    )

    val gradient: Brush = run {
        val shift = gradientShift * 200f
        if (background.gradientColors.size >= 2) {
            Brush.linearGradient(
                colors = background.gradientColors,
                start = Offset(shift, 0f),
                end = Offset(800f - shift, 400f),
            )
        } else {
            Brush.linearGradient(
                colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
                start = Offset(0f, 0f),
                end = Offset(800f, 400f),
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush = gradient)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(cornerRadius),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FramedAvatar(frameId = frame.id, size = avatarSize) {
                // `showFrame = false` — FramedAvatar already draws the
                // current frame. Without this the inner avatar re-drew its
                // OWN (remember-cached = stale) frame, so swapping frames
                // left the previous colour as a visible inner ring.
                PlayerAvatarViewRaw(
                    name = name,
                    color = avatarColor,
                    size = avatarSize - (frame.borderWidth.dp * 2),
                    photoBytes = avatarBytes,
                    showFrame = false,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CosmeticPlayerName(
                        name = name,
                        nameEffectId = nameEffect.id,
                        style = if (size == PlayerBannerSize.Hero) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        fontWeight = FontWeight.Bold,
                        fallbackColor = Color.White,
                    )
                    if (title.id != "title_none") {
                        Spacer(Modifier.width(8.dp))
                        PlayerTitleBadge(titleId = title.id)
                    }
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                } else if (title.id != "title_none" && size == PlayerBannerSize.Hero) {
                    Text(
                        text = title.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

/**
 * Composable hook: prefetches cosmetics for the given user-IDs and exposes the
 * resulting map. Updates reactively as the fetch completes.
 */
@Composable
fun rememberPlayerCosmeticsMap(userIds: List<String>): State<Map<String, PlayerCosmetics>> {
    return produceState(initialValue = emptyMap(), key1 = userIds) {
        if (userIds.isNotEmpty()) {
            value = PlayerCosmeticsRepository.getMany(userIds)
        }
    }
}

/**
 * Returns the local user's currently equipped cosmetics — wrapped as a
 * [PlayerCosmetics] so call sites can treat self + other players uniformly.
 *
 * Recomposes whenever [CosmeticsManager.equippedRevision] bumps (e.g. after equipping a new item).
 */
@Composable
fun rememberLocalUserCosmetics(): PlayerCosmetics {
    val rev by CosmeticsManager.equippedRevision.collectAsState()
    return remember(rev) {
        PlayerCosmetics(
            frameId = CosmeticsManager.getEquippedFrameId(),
            nameEffectId = CosmeticsManager.getEquippedNameEffectId(),
            titleId = CosmeticsManager.getEquippedTitleId(),
            bannerBackgroundId = CosmeticsManager.getEquippedBannerBackgroundId(),
        )
    }
}
