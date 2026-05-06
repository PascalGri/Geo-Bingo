package pg.geobingo.one.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pg.geobingo.one.i18n.S
import pg.geobingo.one.ui.theme.ColorOnSurface
import pg.geobingo.one.ui.theme.ColorOnSurfaceVariant
import pg.geobingo.one.ui.theme.ColorPrimary
import pg.geobingo.one.ui.theme.ColorSurface

/**
 * Which AI consent the user is missing — drives the dialog title + body.
 * Mirrors the two granular flags in [pg.geobingo.one.platform.AiConsent].
 */
enum class AiConsentReason { MODERATION, RATING }

/**
 * Modal explaining why a feature is blocked because the user has not
 * granted the corresponding AI consent. Replaces the generic "feature
 * disabled" snackbars (which couldn't carry a tappable Settings link
 * elegantly).
 *
 * Usage: pass a non-null [reason] to show the dialog, null to hide.
 * [onOpenSettings] should navigate to the Settings screen with
 * `NavArgs.Settings(SettingsAnchor.AI_PRIVACY)` so the user lands
 * directly on the toggle they need.
 */
@Composable
fun AiConsentRequiredDialog(
    reason: AiConsentReason?,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    if (reason == null) return
    val (icon, title, body) = when (reason) {
        AiConsentReason.MODERATION -> Triple(
            Icons.Default.PrivacyTip,
            S.current.aiRequiredTitleModeration,
            S.current.aiRequiredBodyModeration,
        )
        AiConsentReason.RATING -> Triple(
            Icons.Default.AutoAwesome,
            S.current.aiRequiredTitleRating,
            S.current.aiRequiredBodyRating,
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorSurface,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Icon(
                icon,
                contentDescription = null,
                tint = ColorPrimary,
                modifier = Modifier.size(32.dp),
            )
        },
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ColorOnSurface,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorOnSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text(
                    S.current.aiRequiredOpenSettings,
                    color = ColorPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    S.current.aiRequiredCancel,
                    color = ColorOnSurfaceVariant,
                )
            }
        },
    )
}
