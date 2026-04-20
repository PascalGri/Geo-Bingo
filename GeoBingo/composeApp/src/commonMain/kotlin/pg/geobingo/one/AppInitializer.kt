package pg.geobingo.one

import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.state.CosmeticsManager
import pg.geobingo.one.i18n.Language
import pg.geobingo.one.i18n.S
import pg.geobingo.one.network.AccountManager
import pg.geobingo.one.network.FriendsManager
import pg.geobingo.one.platform.AdManager
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.BillingManager
import pg.geobingo.one.platform.ConsentManager
import pg.geobingo.one.platform.SettingsKeys
import pg.geobingo.one.platform.SoundEffect
import pg.geobingo.one.platform.SoundPlayer
import pg.geobingo.one.util.Analytics
import pg.geobingo.one.util.AppLogger
import katchit.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Encapsulates one-time app initialization logic extracted from App.kt.
 * Keeps App composable focused on UI composition.
 */
object AppInitializer {

    /**
     * Performs all one-time app startup tasks:
     * language, consent, billing, analytics, push, cloud sync, daily bonus, sound preload.
     */
    @OptIn(ExperimentalResourceApi::class)
    suspend fun initialize(gameState: GameState) {
        // Language
        val savedLang = AppSettings.getString(SettingsKeys.LANGUAGE, "de")
        val lang = Language.entries.find { it.code == savedLang } ?: Language.DE
        S.switchLanguage(lang)

        // Consent + Ads
        if (AdManager.isAdSupported) {
            ConsentManager.requestConsent { AdManager.preloadAds() }
        }

        // Billing — initialize() reads cached entitlements via Transaction.currentEntitlements
        // (no network / no Apple-ID prompt). We intentionally do NOT call restorePurchases()
        // here: it triggers AppStore.sync(), which on iOS can pop the Apple-ID password
        // dialog at app launch — users mistake it for an unwanted Apple login. The user
        // can still hit "Restore Purchases" in the shop / settings when they actually need it.
        if (BillingManager.isBillingSupported) {
            BillingManager.initialize()
        }

        // Analytics + push
        Analytics.track(Analytics.APP_OPENED)
        pg.geobingo.one.network.PushService.registerToken()

        // Auth initialization + cloud sync (handles web OAuth redirects)
        AccountManager.handleAppStartup()

        // Cosmetics: pull owned + equipped state from cloud, push any local-only purchases
        if (AccountManager.isLoggedIn) {
            CosmeticsManager.syncFromCloud()
        }

        // Daily login bonus + daily challenge reset
        gameState.stars.resetDailyChallengeIfNewDay()
        val bonusGranted = gameState.stars.checkDailyLoginBonus()
        if (bonusGranted) {
            gameState.ui.showDailyBonusBanner = true
            gameState.ui.pendingReward = pg.geobingo.one.game.state.RewardEvent(
                label = pg.geobingo.one.i18n.S.current.rewardDailyBonus,
                stars = 5,
            )
        }

        // Preload sounds
        preloadSounds()
    }

    /**
     * Updates online presence (last_seen). Called from a periodic loop.
     */
    suspend fun updatePresence() {
        if (AccountManager.isLoggedIn) FriendsManager.updateLastSeen()
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun preloadSounds() {
        try {
            val soundData = mutableMapOf<String, ByteArray>()
            SoundEffect.entries.map { it.fileName }.distinct().forEach { file ->
                try {
                    soundData[file] = Res.readBytes("files/$file")
                } catch (e: Exception) {
                    AppLogger.w("AppInitializer", "Sound load failed: $file", e)
                }
            }
            SoundPlayer.preload(soundData)
        } catch (e: Exception) {
            AppLogger.w("AppInitializer", "Sound preload failed", e)
        }
    }
}
