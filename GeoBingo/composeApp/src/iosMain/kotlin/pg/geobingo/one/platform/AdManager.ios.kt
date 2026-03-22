package pg.geobingo.one.platform

// iOS AdMob ist über Swift/ObjC implementiert.
// Der Kotlin-Code delegiert via AdManagerBridge an Swift.
// Siehe iosApp/iosApp/AdManagerBridge.swift für die Swift-Seite.

actual object AdManager {
    actual val isAdSupported: Boolean = true

    actual fun preloadAds() {
        AdManagerBridge.preloadAds()
    }

    actual fun showRewardedAd(onReward: () -> Unit, onDismiss: () -> Unit) {
        AdManagerBridge.showRewardedAd(
            onReward = onReward,
            onDismiss = onDismiss
        )
    }

    actual fun showInterstitialAd(onDismiss: () -> Unit) {
        AdManagerBridge.showInterstitialAd(onDismiss = onDismiss)
    }
}

// Bridge-Objekt — Swift befüllt die callbacks
object AdManagerBridge {
    var preloadCallback: (() -> Unit)? = null
    var rewardCallback: (() -> Unit)? = null
    var rewardDismissCallback: (() -> Unit)? = null
    var interstitialDismissCallback: (() -> Unit)? = null

    // Diese werden von Swift aufgerufen:
    var shouldPreload = false
    var shouldShowRewarded = false
    var shouldShowInterstitial = false

    fun preloadAds() {
        shouldPreload = true
    }

    fun showRewardedAd(onReward: () -> Unit, onDismiss: () -> Unit) {
        rewardCallback = onReward
        rewardDismissCallback = onDismiss
        shouldShowRewarded = true
    }

    fun showInterstitialAd(onDismiss: () -> Unit) {
        interstitialDismissCallback = onDismiss
        shouldShowInterstitial = true
    }
}
