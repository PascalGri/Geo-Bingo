package pg.geobingo.one.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pg.geobingo.one.i18n.S
import pg.geobingo.one.ui.theme.ColorOnSurfaceVariant
import pg.geobingo.one.ui.theme.ColorPrimary

/**
 * Reusable consent body for the AI photo-rating disclosure dialog. Renders
 * the long disclosure prose followed by a clickable "View Privacy Policy"
 * link that opens the locale-appropriate katchit.app/datenschutz.html or
 * /privacy.html in an external browser. Required by Apple guideline 5.1.1(i)
 * — the reviewer must be able to verify the third-party processor terms one
 * tap away from the consent prompt, in their own language.
 *
 * The body is height-capped + vertically scrollable so the long disclosure
 * (now including data-processor + no-training magic phrases) doesn't push
 * the dialog buttons off the bottom of small / iPhone-compat screens.
 */
@Composable
fun AiConsentDialogText() {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .heightIn(max = 360.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            S.current.aiConsentMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = ColorOnSurfaceVariant,
        )
        TextButton(
            onClick = { uriHandler.openUri(S.current.aiConsentPrivacyUrl) },
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = ColorPrimary,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                S.current.aiConsentPrivacyPolicy,
                style = MaterialTheme.typography.labelMedium,
                color = ColorPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
