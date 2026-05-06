package pg.geobingo.one.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Reactive central holder for the two granular AI-consent flags introduced
 * in Build 17 to satisfy Apple guidelines 5.1.1(i) / 5.1.2(i) (Nov-2025
 * update). Replaces the legacy single-flag `AI_CONSENT_ACCEPTED` model.
 *
 * - [moderationAccepted]: photos may be sent to Cloudflare Workers AI for
 *   safety moderation (avatar uploads, multiplayer photo uploads).
 * - [ratingAccepted]: photos may be sent to Google Gemini (primary) and
 *   Cloudflare Workers AI (fallback) for AI photo rating in Solo and
 *   Multiplayer "AI Judge" modes.
 *
 * The hard-gate screen is shown on every app launch until BOTH flags have
 * been resolved by an explicit Confirm tap (which writes whatever the user
 * toggled). A user can resolve the gate with both toggles off — the app is
 * still usable, just with avatar/multiplayer-photo and AI modes disabled.
 *
 * Reset on sign-out so each account makes its own privacy decision (Apple
 * 5.1.1(i): consent is account-bound, not device-bound).
 */
object AiConsent {
    private var _moderationAccepted by mutableStateOf(
        AppSettings.getBoolean(SettingsKeys.AI_CONSENT_MODERATION, false)
    )
    private var _ratingAccepted by mutableStateOf(
        AppSettings.getBoolean(SettingsKeys.AI_CONSENT_RATING, false)
    )
    private var _gateResolved by mutableStateOf(
        AppSettings.getBoolean(GATE_RESOLVED_KEY, false)
    )

    val moderationAccepted: Boolean get() = _moderationAccepted
    val ratingAccepted: Boolean get() = _ratingAccepted

    /**
     * True once the user has explicitly tapped Confirm on the hard-gate
     * screen — independent of which toggles they enabled. Until this is
     * true, the gate must be shown on every app launch (Apple-Forums
     * thread 820209 GymFusion learning: a `hasSeenConsent` flag would let
     * reviewer fresh installs skip the gate, which produces re-rejection).
     */
    val gateResolved: Boolean get() = _gateResolved

    fun setModeration(value: Boolean) {
        _moderationAccepted = value
        AppSettings.setBoolean(SettingsKeys.AI_CONSENT_MODERATION, value)
    }

    fun setRating(value: Boolean) {
        _ratingAccepted = value
        AppSettings.setBoolean(SettingsKeys.AI_CONSENT_RATING, value)
    }

    fun markGateResolved() {
        _gateResolved = true
        AppSettings.setBoolean(GATE_RESOLVED_KEY, true)
    }

    /**
     * Reset on sign-out — the next user must make their own decision.
     */
    fun resetForSignOut() {
        setModeration(false)
        setRating(false)
        _gateResolved = false
        AppSettings.setBoolean(GATE_RESOLVED_KEY, false)
    }

    private const val GATE_RESOLVED_KEY = "ai_consent_gate_resolved_v2"
}
