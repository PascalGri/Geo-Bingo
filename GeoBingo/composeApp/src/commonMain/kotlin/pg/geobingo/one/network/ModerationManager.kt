package pg.geobingo.one.network

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.util.AppLogger

/**
 * User-generated-content moderation — required for Apple App-Store Guideline 1.2.
 *
 * - Reporting: logged-in users can submit a report for any chat message.
 *   The row goes to Supabase `content_reports` (see migration SQL at bottom of
 *   this file) where a human can review.
 * - Blocking: local blocklist keyed by user_id. Messages from blocked users are
 *   filtered out client-side. Persistent across games on the same device.
 */
object ModerationManager {
    private const val TAG = "Moderation"
    private const val BLOCKED_USERS_KEY = "blocked_user_ids"

    @Serializable
    private data class ReportDto(
        val reporter_user_id: String,
        val reported_user_id: String? = null,
        val content_type: String,       // "chat_message" | "player_name" | "avatar"
        val content_id: String,         // message id / player id / etc.
        val content_snapshot: String,   // raw text at time of report (for evidence)
        val reason: String? = null,
    )

    suspend fun reportContent(
        contentType: String,
        contentId: String,
        contentSnapshot: String,
        reportedUserId: String?,
        reason: String? = null,
    ): Result<Unit> {
        val reporterId = AccountManager.currentUserId
            ?: return Result.failure(IllegalStateException("Not logged in"))
        return try {
            supabase.postgrest["content_reports"].insert(
                ReportDto(
                    reporter_user_id = reporterId,
                    reported_user_id = reportedUserId,
                    content_type = contentType,
                    content_id = contentId,
                    content_snapshot = contentSnapshot.take(500),
                    reason = reason,
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Report submission failed", e)
            Result.failure(e)
        }
    }

    fun blockUser(userId: String) {
        if (userId.isBlank()) return
        val current = blockedUserIds().toMutableSet()
        current.add(userId)
        AppSettings.setString(BLOCKED_USERS_KEY, current.joinToString(","))
    }

    fun unblockUser(userId: String) {
        val current = blockedUserIds().toMutableSet()
        current.remove(userId)
        AppSettings.setString(BLOCKED_USERS_KEY, current.joinToString(","))
    }

    fun isBlocked(userId: String?): Boolean =
        userId != null && userId in blockedUserIds()

    fun blockedUserIds(): Set<String> =
        AppSettings.getString(BLOCKED_USERS_KEY, "")
            .split(",")
            .mapNotNull { it.trim().takeIf(String::isNotEmpty) }
            .toSet()
}

/*
Supabase migration to create the moderation tables:

create table public.content_reports (
    id uuid primary key default gen_random_uuid(),
    created_at timestamptz not null default now(),
    reporter_user_id uuid not null references auth.users(id) on delete cascade,
    reported_user_id uuid references auth.users(id) on delete set null,
    content_type text not null check (content_type in ('chat_message','player_name','avatar')),
    content_id text not null,
    content_snapshot text not null,
    reason text,
    status text not null default 'pending' check (status in ('pending','reviewed','dismissed','actioned'))
);
alter table public.content_reports enable row level security;
-- Reporters can insert their own reports:
create policy "reports_insert" on public.content_reports for insert to authenticated
    with check (reporter_user_id = auth.uid());
-- Only admins can read reports (adjust to your admin convention):
create policy "reports_select_admin" on public.content_reports for select to authenticated
    using (false);
*/
