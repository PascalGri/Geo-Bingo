package pg.geobingo.one.platform

/**
 * Platform-agnostic wrapper for Google AdMob.
 * On Desktop and Web [isAdSupported] is false and all methods are No-Ops.
 *
 * Ad Unit IDs (Test-IDs sind vorbelegt — vor Release durch echte IDs ersetzen):
 *   Android Rewarded:     ca-app-pub-3940256099942544/5224354917
 *   Android Interstitial: ca-app-pub-3940256099942544/1033173712
 *   iOS Rewarded:         ca-app-pub-3940256099942544/1712485313
 *   iOS Interstitial:     ca-app-pub-3940256099942544/4411468910
 */
expect object AdManager {
    /** True only on iOS and Android where AdMob is available. */
    val isAdSupported: Boolean

    /**
     * Preload ads in the background. Call once after consent is given.
     */
    fun preloadAds()

    /**
     * Show a rewarded ad. [onReward] fires when the user earned the reward.
     * [onDismiss] always fires when the ad is closed (with or without reward).
     * On unsupported platforms [onDismiss] is called immediately.
     */
    fun showRewardedAd(onReward: () -> Unit, onDismiss: () -> Unit)

    /**
     * Show a full-screen interstitial ad.
     * [onDismiss] fires when the ad is closed.
     * On unsupported platforms [onDismiss] is called immediately.
     */
    fun showInterstitialAd(onDismiss: () -> Unit)
}
