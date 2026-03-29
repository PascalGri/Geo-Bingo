package pg.geobingo.one.game.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.toLocalDateTime
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.SettingsKeys

class StarsState {
    var starCount by mutableStateOf(AppSettings.getInt(SettingsKeys.STAR_COUNT, 0))
        private set
    var adsWatchedToday by mutableStateOf(AppSettings.getInt(SettingsKeys.ADS_WATCHED_TODAY, 0))
        private set
    var lastAdDate by mutableStateOf(AppSettings.getString(SettingsKeys.LAST_AD_DATE, ""))
        private set
    var lastLoginDate by mutableStateOf(AppSettings.getString(SettingsKeys.LAST_LOGIN_DATE, ""))
        private set
    var lastDailyDate by mutableStateOf(AppSettings.getString(SettingsKeys.LAST_DAILY_DATE, ""))
        private set
    var skipCardsCount by mutableStateOf(AppSettings.getInt(SettingsKeys.SKIP_CARDS_COUNT, 0))
        private set
    var noAdsPurchased by mutableStateOf(AppSettings.getBoolean(SettingsKeys.NO_ADS_PURCHASED, false))
        private set
    var dailyChallengeCompleted by mutableStateOf(AppSettings.getBoolean(SettingsKeys.DAILY_CHALLENGE_COMPLETED, false))
        private set

    val canWatchAd: Boolean
        get() {
            resetAdsIfNewDay()
            return adsWatchedToday < 5
        }

    val adsRemainingToday: Int
        get() {
            resetAdsIfNewDay()
            return (5 - adsWatchedToday).coerceAtLeast(0)
        }

    fun add(amount: Int) {
        starCount += amount
        AppSettings.setInt(SettingsKeys.STAR_COUNT, starCount)
    }

    fun spend(amount: Int): Boolean {
        if (starCount < amount) return false
        starCount -= amount
        AppSettings.setInt(SettingsKeys.STAR_COUNT, starCount)
        return true
    }

    fun recordAdWatched() {
        resetAdsIfNewDay()
        adsWatchedToday++
        lastAdDate = todayString()
        AppSettings.setInt(SettingsKeys.ADS_WATCHED_TODAY, adsWatchedToday)
        AppSettings.setString(SettingsKeys.LAST_AD_DATE, lastAdDate)
    }

    fun checkDailyLoginBonus(): Boolean {
        val today = todayString()
        if (lastLoginDate == today) return false
        lastLoginDate = today
        AppSettings.setString(SettingsKeys.LAST_LOGIN_DATE, today)
        add(5)
        return true
    }

    fun completeDailyChallenge(reward: Int) {
        if (dailyChallengeCompleted) return
        dailyChallengeCompleted = true
        AppSettings.setBoolean(SettingsKeys.DAILY_CHALLENGE_COMPLETED, true)
        add(reward)
    }

    fun resetDailyChallengeIfNewDay() {
        val today = todayString()
        if (lastDailyDate != today) {
            lastDailyDate = today
            dailyChallengeCompleted = false
            AppSettings.setString(SettingsKeys.LAST_DAILY_DATE, today)
            AppSettings.setBoolean(SettingsKeys.DAILY_CHALLENGE_COMPLETED, false)
        }
    }

    fun useSkipCard(): Boolean {
        if (skipCardsCount <= 0) return false
        skipCardsCount--
        AppSettings.setInt(SettingsKeys.SKIP_CARDS_COUNT, skipCardsCount)
        return true
    }

    fun addSkipCards(count: Int) {
        skipCardsCount += count
        AppSettings.setInt(SettingsKeys.SKIP_CARDS_COUNT, skipCardsCount)
    }

    fun updateNoAdsPurchased(purchased: Boolean) {
        noAdsPurchased = purchased
        AppSettings.setBoolean(SettingsKeys.NO_ADS_PURCHASED, purchased)
    }

    fun reload() {
        starCount = AppSettings.getInt(SettingsKeys.STAR_COUNT, 0)
        skipCardsCount = AppSettings.getInt(SettingsKeys.SKIP_CARDS_COUNT, 0)
        noAdsPurchased = AppSettings.getBoolean(SettingsKeys.NO_ADS_PURCHASED, false)
    }

    private fun resetAdsIfNewDay() {
        val today = todayString()
        if (lastAdDate != today) {
            adsWatchedToday = 0
            lastAdDate = today
            AppSettings.setInt(SettingsKeys.ADS_WATCHED_TODAY, 0)
            AppSettings.setString(SettingsKeys.LAST_AD_DATE, today)
        }
    }

    private fun todayString(): String {
        val now = kotlinx.datetime.Clock.System.now()
        val utc = now.toLocalDate(kotlinx.datetime.TimeZone.UTC)
        return utc.toString() // yyyy-MM-dd
    }
}

private fun kotlinx.datetime.Instant.toLocalDate(tz: kotlinx.datetime.TimeZone): kotlinx.datetime.LocalDate =
    toLocalDateTime(tz).date
