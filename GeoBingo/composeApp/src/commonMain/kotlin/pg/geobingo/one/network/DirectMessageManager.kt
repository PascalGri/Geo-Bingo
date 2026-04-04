package pg.geobingo.one.network

import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import pg.geobingo.one.util.AppLogger

object DirectMessageManager {
    private const val TAG = "DirectMessageManager"

    @Serializable
    data class DirectMessageDto(
        val id: String = "",
        val from_user_id: String = "",
        val to_user_id: String = "",
        val message: String = "",
        val read: Boolean = false,
        val created_at: String = "",
    )

    @Serializable
    private data class DmInsertDto(
        val from_user_id: String,
        val to_user_id: String,
        val message: String,
    )

    suspend fun sendMessage(toUserId: String, message: String) {
        val fromUserId = AccountManager.currentUserId ?: return
        val sanitized = message.trim().take(500)
        if (sanitized.isBlank()) return
        try {
            supabase.from("direct_messages").insert(
                DmInsertDto(from_user_id = fromUserId, to_user_id = toUserId, message = sanitized)
            )
        } catch (e: Exception) {
            AppLogger.w(TAG, "sendMessage failed", e)
            throw e
        }
    }

    suspend fun getMessages(friendUserId: String, limit: Int = 50): List<DirectMessageDto> {
        val myId = AccountManager.currentUserId ?: return emptyList()
        return try {
            supabase.from("direct_messages")
                .select {
                    filter {
                        or {
                            and {
                                eq("from_user_id", myId)
                                eq("to_user_id", friendUserId)
                            }
                            and {
                                eq("from_user_id", friendUserId)
                                eq("to_user_id", myId)
                            }
                        }
                    }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                    limit(limit.toLong())
                }
                .decodeList()
        } catch (e: Exception) {
            AppLogger.w(TAG, "getMessages failed", e)
            emptyList()
        }
    }

    suspend fun markAsRead(friendUserId: String) {
        val myId = AccountManager.currentUserId ?: return
        try {
            supabase.from("direct_messages")
                .update({ set("read", true) }) {
                    filter {
                        eq("to_user_id", myId)
                        eq("from_user_id", friendUserId)
                        eq("read", false)
                    }
                }
        } catch (e: Exception) {
            AppLogger.w(TAG, "markAsRead failed", e)
        }
    }
}
