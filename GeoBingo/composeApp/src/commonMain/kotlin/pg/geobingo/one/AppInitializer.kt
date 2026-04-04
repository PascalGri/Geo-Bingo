package pg.geobingo.one

import pg.geobingo.one.game.GameState
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

        // Billing
        if (BillingManager.isBillingSupported) {
            BillingManager.initialize()
            BillingManager.restorePurchases(
                onRestored = { products ->
                    if ("pg.geobingo.one.no_ads" in products) {
                        gameState.stars.updateNoAdsPurchased(true)
                    }
                },
                onError = {},
            )
        }

        // Analytics + push
        Analytics.track(Analytics.APP_OPENED)
        pg.geobingo.one.network.PushService.registerToken()

        // Cloud sync
        val userId = AccountManager.currentUserId
        if (userId != null) {
            AccountManager.syncLocalToCloud(userId)
        }

        // Daily login bonus + daily challenge reset
        gameState.stars.resetDailyChallengeIfNewDay()
        val bonusGranted = gameState.stars.checkDailyLoginBonus()
        if (bonusGranted) {
            gameState.ui.pendingToast = "${S.current.dailyLoginBonus}: +5 ${S.current.stars}"
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
