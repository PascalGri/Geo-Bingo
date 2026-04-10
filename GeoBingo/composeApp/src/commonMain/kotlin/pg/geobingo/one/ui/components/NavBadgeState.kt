package pg.geobingo.one.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import pg.geobingo.one.network.DirectMessageManager
import pg.geobingo.one.network.FriendsManager

/**
 * Polls pending friend requests + unread DMs and exposes a single
 * combined count for the bottom-nav Friends tab badge.
 *
 * Polling interval: 30s. Cheap because both endpoints have a short server-side
 * cache and the queries are tiny (count-only). The badge auto-refreshes when
 * the user leaves the FriendsScreen / DirectMessageScreen.
 */
@Composable
fun rememberFriendsBadgeCount(): State<Int> {
    val count = remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val pending = FriendsManager.getPendingRequests().size
                val unread = DirectMessageManager.getUnreadCount()
                count.value = pending + unread
            } catch (_: Throwable) {
                // ignore — keep last value
            }
            delay(30_000L)
        }
    }
    return count
}
