package pg.geobingo.one.network

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import kotlinx.serialization.Serializable
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameConstants
import pg.geobingo.one.util.AppLogger
import kotlin.random.Random

@Serializable
data class FriendshipDto(
    val id: String = "",
    val user_id: String = "",
    val friend_id: String = "",
    val status: String = "pending",
    val requested_by: String = "",
    val created_at: String = "",
)

@Serializable
private data class FriendshipInsertDto(
    val user_id: String,
    val friend_id: String,
    val status: String = "pending",
    val requested_by: String,
)

@Serializable
data class GameInviteDto(
    val id: String = "",
    val from_user_id: String = "",
    val to_user_id: String = "",
    val game_code: String = "",
    val game_id: String = "",
    val status: String = "pending",
    val created_at: String = "",
)

@Serializable
private data class GameInviteInsertDto(
    val from_user_id: String,
    val to_user_id: String,
    val game_code: String,
    val game_id: String,
)

data class FriendInfo(
    val userId: String,
    val displayName: String,
    val avatarUrl: String,
    val isOnline: Boolean,
    val lastSeen: String?,
    val friendshipId: String,
)

object FriendsManager {
    private const val TAG = "FriendsManager"

    /** Generate a unique 8-char friend code for the current user. */
    suspend fun ensureFriendCode(): String? {
        val userId = AccountManager.currentUserId ?: return null
        // Check if already has a code
        try {
            val profile = supabase.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<UserProfile>()
            if (profile?.friend_code != null) return profile.friend_code
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to read profile", e)
        }
        // Generate and set a new code
        val code = generateFriendCode()
        try {
            supabase.from("profiles").update({ set("friend_code", code) }) {
                filter { eq("id", userId) }
            }
            return code
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to set friend code", e)
            return null
        }
    }

    /** Update last_seen timestamp for the current user. */
    suspend fun updateLastSeen() {
        val userId = AccountManager.currentUserId ?: return
        try {
            val now = kotlinx.datetime.Clock.System.now().toString()
            supabase.from("profiles").update({ set("last_seen", now) }) {
                filter { eq("id", userId) }
            }
        } catch (e: Exception) {
            AppLogger.d(TAG, "Failed to update last_seen", e)
        }
    }

    /** Send a friend request by friend code. */
    suspend fun sendFriendRequest(friendCode: String): Result<Unit> {
        val myId = AccountManager.currentUserId ?: return Result.failure(Exception("Not logged in"))
        return try {
            // Look up user by friend code
            val target = supabase.from("profiles")
                .select { filter { eq("friend_code", friendCode.uppercase().trim()) } }
                .decodeSingleOrNull<UserProfile>()
                ?: return Result.failure(Exception("not_found"))

            if (target.id == myId) return Result.failure(Exception("self"))

            // Check existing friendship
            val existing = getExistingFriendship(myId, target.id)
            if (existing != null) {
                return if (existing.status == "accepted") {
                    Result.failure(Exception("already_friends"))
                } else {
                    Result.failure(Exception("already_pending"))
                }
            }

            // Create friendship (store with sorted IDs for uniqueness)
            val (uid1, uid2) = if (myId < target.id) myId to target.id else target.id to myId
            supabase.from("friendships").insert(
                FriendshipInsertDto(user_id = uid1, friend_id = uid2, requested_by = myId)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to send friend request", e)
            Result.failure(e)
        }
    }

    /** Accept a pending friend request. */
    suspend fun acceptFriendRequest(friendshipId: String): Result<Unit> {
        return try {
            supabase.from("friendships").update({ set("status", "accepted") }) {
                filter { eq("id", friendshipId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to accept friend request", e)
            Result.failure(e)
        }
    }

    /** Decline/remove a friendship. */
    suspend fun removeFriendship(friendshipId: String): Result<Unit> {
        return try {
            supabase.from("friendships").delete {
                filter { eq("id", friendshipId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to remove friendship", e)
            Result.failure(e)
        }
    }

    /** Get all accepted friends with their profile info and online status. */
    suspend fun getFriends(): List<FriendInfo> {
        val myId = AccountManager.currentUserId ?: return emptyList()
        return try {
            val friendships = supabase.from("friendships")
                .select { filter { or { eq("user_id", myId); eq("friend_id", myId) } }; filter { eq("status", "accepted") } }
                .decodeList<FriendshipDto>()

            val friendIds = friendships.map { if (it.user_id == myId) it.friend_id else it.user_id }
            if (friendIds.isEmpty()) return emptyList()

            val profiles = friendIds.mapNotNull { fid ->
                try {
                    supabase.from("profiles")
                        .select { filter { eq("id", fid) } }
                        .decodeSingleOrNull<UserProfile>()
                } catch (_: Exception) { null }
            }

            val profileMap = profiles.associateBy { it.id }
            friendships.mapNotNull { fs ->
                val friendId = if (fs.user_id == myId) fs.friend_id else fs.user_id
                val profile = profileMap[friendId] ?: return@mapNotNull null
                FriendInfo(
                    userId = friendId,
                    displayName = profile.display_name.ifBlank { "Player" },
                    avatarUrl = profile.avatar_url,
                    isOnline = isRecentlyOnline(profile.last_seen),
                    lastSeen = profile.last_seen,
                    friendshipId = fs.id,
                )
            }.sortedByDescending { it.isOnline }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to get friends", e)
            emptyList()
        }
    }

    /** Get pending friend requests (incoming). */
    suspend fun getPendingRequests(): List<Pair<FriendshipDto, UserProfile>> {
        val myId = AccountManager.currentUserId ?: return emptyList()
        return try {
            // Incoming: where I'm involved but not the requester, and status is pending
            val friendships = supabase.from("friendships")
                .select { filter { or { eq("user_id", myId); eq("friend_id", myId) } }; filter { eq("status", "pending") } }
                .decodeList<FriendshipDto>()
                .filter { it.requested_by != myId }

            friendships.mapNotNull { fs ->
                val requesterId = fs.requested_by
                try {
                    val profile = supabase.from("profiles")
                        .select { filter { eq("id", requesterId) } }
                        .decodeSingleOrNull<UserProfile>()
                    if (profile != null) fs to profile else null
                } catch (_: Exception) { null }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to get pending requests", e)
            emptyList()
        }
    }

    /** Send a game invite to a friend. */
    suspend fun sendGameInvite(toUserId: String, gameCode: String, gameId: String): Result<Unit> {
        val myId = AccountManager.currentUserId ?: return Result.failure(Exception("Not logged in"))
        return try {
            supabase.from("game_invites").insert(
                GameInviteInsertDto(from_user_id = myId, to_user_id = toUserId, game_code = gameCode, game_id = gameId)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to send game invite", e)
            Result.failure(e)
        }
    }

    /** Get pending game invites for the current user. */
    suspend fun getPendingInvites(): List<Pair<GameInviteDto, UserProfile>> {
        val myId = AccountManager.currentUserId ?: return emptyList()
        return try {
            val invites = supabase.from("game_invites")
                .select { filter { eq("to_user_id", myId); eq("status", "pending") } }
                .decodeList<GameInviteDto>()

            invites.mapNotNull { invite ->
                try {
                    val profile = supabase.from("profiles")
                        .select { filter { eq("id", invite.from_user_id) } }
                        .decodeSingleOrNull<UserProfile>()
                    if (profile != null) invite to profile else null
                } catch (_: Exception) { null }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to get pending invites", e)
            emptyList()
        }
    }

    /** Mark an invite as accepted or declined. */
    suspend fun respondToInvite(inviteId: String, accept: Boolean) {
        try {
            supabase.from("game_invites").update({ set("status", if (accept) "accepted" else "declined") }) {
                filter { eq("id", inviteId) }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to respond to invite", e)
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private suspend fun getExistingFriendship(userId1: String, userId2: String): FriendshipDto? {
        val (uid1, uid2) = if (userId1 < userId2) userId1 to userId2 else userId2 to userId1
        return try {
            supabase.from("friendships")
                .select { filter { eq("user_id", uid1); eq("friend_id", uid2) } }
                .decodeSingleOrNull()
        } catch (_: Exception) { null }
    }

    private fun isRecentlyOnline(lastSeen: String?): Boolean {
        if (lastSeen == null) return false
        // Consider online if last_seen within 5 minutes
        // Simple heuristic: parse ISO timestamp and compare
        return try {
            val now = kotlinx.datetime.Clock.System.now()
            val seen = kotlinx.datetime.Instant.parse(lastSeen)
            (now - seen).inWholeMinutes < 5
        } catch (_: Exception) { false }
    }

    /** Download a friend's profile avatar by their user ID. */
    suspend fun downloadFriendAvatar(userId: String): ByteArray? {
        return try {
            val path = "avatars/$userId.jpg"
            val url = supabase.storage.from("photos").createSignedUrl(path, GameConstants.AVATAR_URL_EXPIRY)
            ServiceLocator.httpClient.get(url).readRawBytes()
        } catch (e: Exception) {
            AppLogger.d(TAG, "Friend avatar download failed for $userId", e)
            null
        }
    }

    private fun generateFriendCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..8).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }
}
