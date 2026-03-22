package pg.geobingo.one.platform

import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

private const val REWARDED_AD_UNIT_ID = "ca-app-pub-4871207394525716/9291372203"
private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-4871207394525716/9320827219"

actual object AdManager {
    actual val isAdSupported: Boolean = true

    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    actual fun preloadAds() {
        mainHandler.post {
            loadRewardedAd()
            loadInterstitialAd()
        }
    }

    private fun loadRewardedAd() {
        val ctx = appContext
        RewardedAd.load(ctx, REWARDED_AD_UNIT_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { rewardedAd = null }
            })
    }

    private fun loadInterstitialAd() {
        val ctx = appContext
        InterstitialAd.load(ctx, INTERSTITIAL_AD_UNIT_ID, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { interstitialAd = null }
            })
    }

    actual fun showRewardedAd(onReward: () -> Unit, onDismiss: () -> Unit) {
        val activity = currentActivity ?: run { onDismiss(); return }
        val ad = rewardedAd ?: run { onDismiss(); return }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewardedAd()
                onDismiss()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                onDismiss()
            }
        }
        ad.show(activity) { onReward() }
    }

    actual fun showInterstitialAd(onDismiss: () -> Unit) {
        val activity = currentActivity ?: run { onDismiss(); return }
        val ad = interstitialAd ?: run { onDismiss(); return }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitialAd()
                onDismiss()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                onDismiss()
            }
        }
        ad.show(activity)
    }
}
