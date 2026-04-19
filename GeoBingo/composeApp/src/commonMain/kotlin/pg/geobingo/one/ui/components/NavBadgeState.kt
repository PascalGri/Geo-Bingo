package pg.geobingo.one.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import pg.geobingo.one.network.FriendsManager

/**
 * Polls pending friend requests and exposes the count for the bottom-nav
 * Friends tab badge. Polling interval: 30s.
 */
@Composable
fun rememberFriendsBadgeCount(): State<Int> {
    val count = remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            try {
                count.value = FriendsManager.getPendingRequests().size
            } catch (_: Throwable) {
                // ignore — keep last value
            }
            delay(30_000L)
        }
    }
    return count
}
