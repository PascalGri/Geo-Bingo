package pg.geobingo.one.platform

actual object ConsentManager {
    actual fun requestConsent(onReady: () -> Unit) = onReady()
    actual fun showPrivacyOptionsForm(onDismiss: () -> Unit) = onDismiss()
    actual val canShowPersonalizedAds: Boolean = false
}
