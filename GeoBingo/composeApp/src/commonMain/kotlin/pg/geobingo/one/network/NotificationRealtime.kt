package pg.geobingo.one.network

import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import pg.geobingo.one.util.AppLogger

/**
 * Realtime subscriptions for social notifications (friend requests + game invites).
 * Replaces polling for immediate push-style updates.
 * Falls back gracefully if subscription fails.
 */
class NotificationRealtimeManager(private val userId: String) {

    private val channel = supabase.channel("notifications-$userId")

    private val _friendshipChanges = MutableSharedFlow<FriendshipDto>(extraBufferCapacity = 8)
    val friendshipChanges: Flow<FriendshipDto> = _friendshipChanges.asSharedFlow()

    private val _inviteChanges = MutableSharedFlow<GameInviteDto>(extraBufferCapacity = 8)
    val inviteChanges: Flow<GameInviteDto> = _inviteChanges.asSharedFlow()

    private var collectJob: Job? = null

    /**
     * Subscribe to realtime changes on friendships and game_invites tables.
     * Returns true if subscription succeeded.
     */
    suspend fun subscribe(scope: CoroutineScope): Boolean {
        return try {
            val friendshipFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "friendships"
                filter("friend_id", FilterOperator.EQ, userId)
            }.let { insertFlow ->
                insertFlow
            }

            val inviteFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "game_invites"
                filter("to_user_id", FilterOperator.EQ, userId)
            }

            channel.subscribe(blockUntilSubscribed = true)

            collectJob = scope.launch {
                launch {
                    friendshipFlow.collect { action ->
                        try {
                            _friendshipChanges.emit(action.decodeRecord())
                        } catch (e: Exception) {
                            AppLogger.d(TAG, "Failed to decode friendship change", e)
                        }
                    }
                }
                launch {
                    inviteFlow.collect { action ->
                        try {
                            _inviteChanges.emit(action.decodeRecord())
                        } catch (e: Exception) {
                            AppLogger.d(TAG, "Failed to decode invite change", e)
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            AppLogger.w(TAG, "Notification realtime subscription failed, will use polling fallback", e)
            false
        }
    }

    suspend fun unsubscribe() {
        collectJob?.cancel()
        collectJob = null
        try {
            channel.unsubscribe()
        } catch (e: Exception) {
            AppLogger.d(TAG, "Notification realtime unsubscribe failed", e)
        }
    }

    companion object {
        private const val TAG = "NotificationRealtime"
    }
}
