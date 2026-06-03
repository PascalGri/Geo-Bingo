package pg.geobingo.one.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import pg.geobingo.one.ui.theme.LocalReduceMotion

// ─────────────────────────────────────────────────────────────────────────────
//  Ultimate-tier holographic treatment (shared across all cosmetic renderers)
// ─────────────────────────────────────────────────────────────────────────────
//
// Items at or above CosmeticsManager.ULTIMATE_THRESHOLD render with a moving
// iridescent "foil" sheen plus a soft glow on EVERY screen (shop, banners,
// avatars, titles, bingo board). All of it is gated on LocalReduceMotion so
// Web/wasmJs and older devices fall back to a clean static look — and the
// helpers create no infinite transition at all when motion is reduced, so
// there is zero per-frame recomposition in that mode.

/**
 * Translucent iridescent sheen colours. Endpoints are fully transparent so the
 * band has soft edges and each item's own palette shows through underneath.
 */
internal val HoloSheenColors = listOf(
    Color(0x00FFFFFF),
    Color(0x40A5F3FC), // cyan-200
    Color(0x59F0ABFC), // fuchsia-300
    Color(0x40FDE68A), // amber-200
    Color(0x00FFFFFF),
)

/** Iridescent palette for Ultimate borders / glows. */
internal val HoloEdgeColors = listOf(
    Color(0xFF22D3EE), // cyan
    Color(0xFFA855F7), // violet
    Color(0xFFEC4899), // pink
    Color(0xFFFBBF24), // gold
    Color(0xFF22D3EE), // back to cyan (continuous sweep)
)

/** Soft halo colour painted behind Ultimate avatars. */
internal val HoloGlow = Color(0xFFB388FF)

/**
 * 0→1 shimmer phase driving the Ultimate effects. Reverses so the sheen sweeps
 * back and forth. Returns a constant 0f under reduce-motion — and, crucially,
 * creates no [rememberInfiniteTransition] in that case, so it never schedules a
 * frame callback. Only call this when an item is actually Ultimate so ordinary
 * cosmetics pay nothing.
 */
@Composable
internal fun rememberHoloPhase(durationMillis: Int = 3200): Float {
    if (LocalReduceMotion.current) return 0f
    val transition = rememberInfiniteTransition(label = "holo")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "holoPhase",
    )
    return phase
}

/**
 * Diagonal holographic sheen brush positioned by [phase] (0..1). [span] is the
 * approximate pixel size of the surface; the band travels a little past both
 * edges so the highlight fully enters and exits.
 */
internal fun holoSheenBrush(phase: Float, span: Float = 600f): Brush {
    val shift = phase * (span * 1.6f) - (span * 0.3f)
    return Brush.linearGradient(
        colors = HoloSheenColors,
        start = Offset(shift, 0f),
        end = Offset(shift + span * 0.5f, span),
    )
}
