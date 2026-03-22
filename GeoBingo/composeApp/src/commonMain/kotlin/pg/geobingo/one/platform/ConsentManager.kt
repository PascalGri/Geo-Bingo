package pg.geobingo.one.platform

/**
 * Wraps the platform-specific GDPR consent flow (Google UMP on Android/iOS).
 * On Desktop and Web this is a No-Op.
 *
 * Call [requestConsent] once at app start, before loading any ads.
 * The [onReady] callback fires when consent is either already determined
 * or when the user has completed the consent dialog.
 */
expect object ConsentManager {
    /**
     * Request or show consent form if required.
     * [onReady] is always called, even if no form was shown.
     */
    fun requestConsent(onReady: () -> Unit)

    /**
     * Show the consent form again (for Settings screen "Werbeeinstellungen ändern").
     */
    fun showPrivacyOptionsForm(onDismiss: () -> Unit)

    /**
     * True if the user's consent allows loading personalized ads.
     * False = show non-personalized ads only.
     * Null = consent not yet determined.
     */
    val canShowPersonalizedAds: Boolean
}
