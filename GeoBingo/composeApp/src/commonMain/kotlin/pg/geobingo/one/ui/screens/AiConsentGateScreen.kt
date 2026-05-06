package pg.geobingo.one.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.i18n.S
import pg.geobingo.one.platform.AiConsent
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.SettingsKeys
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.theme.*

/**
 * Build-17 hard-gate AI consent screen — required for Apple guidelines
 * 5.1.1(i)/5.1.2(i) (Nov-2025 update).
 *
 * Pattern adapted from GymFusion (Apple Forums thread 820209) — the only
 * publicly documented success case for these guidelines:
 * - Modal-style hard gate, no skip / dismiss / system-back exit
 * - Shown at every app launch until the user taps Confirm
 * - Two granular toggles (moderation vs. rating) — user picks what they
 *   consent to; the app remains usable without either
 * - Three acknowledgment checkboxes that must all be ticked before the
 *   Confirm button enables — proves the disclosure was read
 * - Locale-aware Privacy Policy link inline
 *
 * Exit: tapping Confirm writes both toggle values to AppSettings, marks
 * the gate resolved, and navigates to ONBOARDING (first launch) or HOME
 * (returning user). Decline path is just "leave both toggles off and tap
 * Confirm" — there is no separate Decline button per GymFusion's pattern.
 */
@Composable
fun AiConsentGateScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val uriHandler = LocalUriHandler.current

    // Initial values: prefer the new granular flags, but if a v1.x install
    // had only the legacy single-flag accepted, surface that as a default
    // so the user doesn't have to re-toggle from scratch (still requires a
    // Confirm tap to pass the gate, satisfying Apple's "permission before
    // sharing" wording).
    val legacyAccepted = remember {
        AppSettings.getBoolean(SettingsKeys.AI_CONSENT_ACCEPTED, false)
    }
    var moderationOn by remember {
        mutableStateOf(AiConsent.moderationAccepted || legacyAccepted)
    }
    var ratingOn by remember {
        mutableStateOf(AiConsent.ratingAccepted || legacyAccepted)
    }

    var ackTransfer by remember { mutableStateOf(false) }
    var ackProcessor by remember { mutableStateOf(false) }
    var ackRevoke by remember { mutableStateOf(false) }

    val confirmEnabled = ackTransfer && ackProcessor && ackRevoke

    fun confirm() {
        AiConsent.setModeration(moderationOn)
        AiConsent.setRating(ratingOn)
        AiConsent.markGateResolved()
        val onboardingDone = AppSettings.getBoolean(SettingsKeys.ONBOARDING_COMPLETED, false)
        nav.resetTo(if (onboardingDone) Screen.HOME else Screen.ONBOARDING)
    }

    // Block Android system-back so the user can't dismiss the gate without
    // an explicit Confirm — Apple's hard-gate expectation (and harmless on
    // iOS where there is no system back gesture for top-level screens).
    SystemBackHandler { /* swallowed */ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 32.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            HeaderBlock()

            Text(
                text = S.current.aiGateIntro,
                style = MaterialTheme.typography.bodyMedium,
                color = ColorOnSurfaceVariant,
                lineHeight = 22.sp,
            )

            ConsentToggleRow(
                icon = Icons.Default.Shield,
                gradient = listOf(Color(0xFF22D3EE), Color(0xFF6366F1)),
                label = S.current.aiGateModerationLabel,
                description = S.current.aiGateModerationDesc,
                checked = moderationOn,
                onChange = { moderationOn = it },
            )

            ConsentToggleRow(
                icon = Icons.Default.AutoAwesome,
                gradient = GradientAiJudge,
                label = S.current.aiGateRatingLabel,
                description = S.current.aiGateRatingDesc,
                checked = ratingOn,
                onChange = { ratingOn = it },
            )

            AcknowledgementCheckbox(
                checked = ackTransfer,
                onChange = { ackTransfer = it },
                text = S.current.aiGateAcknowledgeTransfer,
            )
            AcknowledgementCheckbox(
                checked = ackProcessor,
                onChange = { ackProcessor = it },
                text = S.current.aiGateAcknowledgeProcessor,
            )
            AcknowledgementCheckbox(
                checked = ackRevoke,
                onChange = { ackRevoke = it },
                text = S.current.aiGateAcknowledgeRevoke,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { uriHandler.openUri(S.current.aiConsentPrivacyUrl) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = ColorPrimary,
                )
                Text(
                    text = S.current.aiGateOpenPrivacyPolicy,
                    style = MaterialTheme.typography.labelLarge,
                    color = ColorPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorBackground)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            GradientButton(
                text = S.current.aiGateConfirm,
                onClick = { confirm() },
                modifier = Modifier.fillMaxWidth(),
                enabled = confirmEnabled,
                gradientColors = GradientPrimary,
                height = 56.dp,
                fontSize = 17.sp,
            )
        }
    }
}

@Composable
private fun HeaderBlock() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(GradientAiJudge)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.PrivacyTip,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp),
            )
        }
        Text(
            text = S.current.aiGateTitle,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = ColorOnSurface,
        )
    }
}

@Composable
private fun ConsentToggleRow(
    icon: ImageVector,
    gradient: List<Color>,
    label: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    GradientBorderCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        borderColors = gradient,
        backgroundColor = ColorSurface,
        borderWidth = 1.5.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ColorOnSurface,
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorOnSurfaceVariant,
                    lineHeight = 18.sp,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = gradient.first(),
                    uncheckedThumbColor = ColorOnSurfaceVariant,
                    uncheckedTrackColor = ColorSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun AcknowledgementCheckbox(
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    text: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onChange(!checked) }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (checked) ColorPrimary else Color.Transparent)
                .border(
                    width = if (checked) 0.dp else 1.5.dp,
                    color = if (checked) Color.Transparent else ColorOutline,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = ColorOnSurface,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f),
        )
    }
}
