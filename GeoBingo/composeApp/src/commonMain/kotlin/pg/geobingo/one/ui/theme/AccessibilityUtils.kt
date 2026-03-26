package pg.geobingo.one.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────
//  Responsive max-width wrapper
// ─────────────────────────────────────────────

/**
 * Wraps content with a maximum width constraint.
 * On wider screens (tablets, desktop), content stays centered and readable.
 */
@Composable
fun MaxWidthContainer(
    maxWidth: Dp = 480.dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier.widthIn(max = maxWidth).fillMaxWidth(),
            content = content,
        )
    }
}

// ─────────────────────────────────────────────
//  Semantic accessibility helpers
// ─────────────────────────────────────────────

/** Mark a composable as a semantic heading for screen readers. */
fun Modifier.semanticHeading(label: String? = null): Modifier = this.semantics {
    heading()
    if (label != null) contentDescription = label
}

/** Set content description for screen reader accessibility. */
fun Modifier.accessibilityLabel(label: String): Modifier = this.semantics {
    contentDescription = label
}

/** Ensure minimum touch target size (48dp per Material guidelines). */
fun Modifier.minTouchTarget(): Modifier = this.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
