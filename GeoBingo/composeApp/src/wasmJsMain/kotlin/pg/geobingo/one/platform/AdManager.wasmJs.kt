package pg.geobingo.one.platform

actual object AdManager {
    actual val isAdSupported: Boolean = false
    actual fun preloadAds() {}
    actual fun showRewardedAd(onReward: () -> Unit, onDismiss: () -> Unit) = onDismiss()
    actual fun showInterstitialAd(onDismiss: () -> Unit) = onDismiss()
}
