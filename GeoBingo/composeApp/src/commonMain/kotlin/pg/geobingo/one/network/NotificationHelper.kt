package pg.geobingo.one.network

import pg.geobingo.one.i18n.S
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.util.AppLogger

/**
 * Centralized push notification triggers for various app events.
 * Each function is safe to call (catches exceptions internally).
 */
object NotificationHelper {
    private const val TAG = "NotificationHelper"

    /** Notify all friends that a game has finished and show the winner. */
    suspend fun notifyFriendsGameFinished(winnerName: String, gameCode: String) {
        val myId = AccountManager.currentUserId ?: return
        try {
            val friends = FriendsManager.getFriends()
            val myName = AppSettings.getString("last_player_name", "Player")
            friends.forEach { friend ->
                val friendId = friend.userId
                PushService.sendPushToUser(
                    friendId,
                    "Runde beendet!",
                    "$winnerName hat eine Runde gewonnen! Fordere sie/ihn heraus.",
                    mapOf("game_code" to gameCode),
                )
            }
        } catch (e: Exception) {
            AppLogger.d(TAG, "notifyFriendsGameFinished failed", e)
        }
    }

    /** Notify friends that the user started a solo challenge (social engagement). */
    suspend fun notifySoloHighScore(score: Int) {
        val myId = AccountManager.currentUserId ?: return
        val myName = AppSettings.getString("last_player_name", "Player")
        if (score < 200) return // Only brag about good scores
        try {
            val friends = FriendsManager.getFriends()
            friends.take(5).forEach { friend ->
                val friendId = friend.userId
                PushService.sendPushToUser(
                    friendId,
                    "Neuer Solo-Rekord!",
                    "$myName hat $score Punkte im Solo-Modus erreicht!",
                )
            }
        } catch (e: Exception) {
            AppLogger.d(TAG, "notifySoloHighScore failed", e)
        }
    }

    /** Notify a user that their friend request was accepted. */
    suspend fun notifyFriendRequestAccepted(toUserId: String) {
        val myName = AppSettings.getString("last_player_name", "Player")
        try {
            PushService.sendPushToUser(
                toUserId,
                "Freundschaftsanfrage angenommen!",
                "$myName hat deine Anfrage angenommen. Startet eine Runde!",
            )
        } catch (e: Exception) {
            AppLogger.d(TAG, "notifyFriendRequestAccepted failed", e)
        }
    }

    /** Notify lobby players that the game is about to start. */
    suspend fun notifyGameStarting(playerUserIds: List<String>, gameCode: String) {
        try {
            playerUserIds.forEach { userId ->
                PushService.sendPushToUser(
                    userId,
                    "Spiel startet!",
                    "Deine Runde ($gameCode) beginnt jetzt!",
                    mapOf("game_code" to gameCode),
                )
            }
        } catch (e: Exception) {
            AppLogger.d(TAG, "notifyGameStarting failed", e)
        }
    }

    /** Notify user of daily challenge reset (called from edge function, not client). */
    suspend fun notifyDailyChallengeAvailable(toUserId: String) {
        try {
            PushService.sendPushToUser(
                toUserId,
                "Taegliche Challenge bereit!",
                "Schliesse deine taegliche Challenge ab und verdiene Stars!",
                mapOf("type" to "daily_challenge"),
            )
        } catch (e: Exception) {
            AppLogger.d(TAG, "notifyDailyChallengeAvailable failed", e)
        }
    }

    /** Notify user of achievement unlock. */
    suspend fun notifyAchievementUnlocked(achievementName: String) {
        // This is a local notification concept - on real device would use local notification API
        // For now it's handled by the UI toast system in the app
    }
}
