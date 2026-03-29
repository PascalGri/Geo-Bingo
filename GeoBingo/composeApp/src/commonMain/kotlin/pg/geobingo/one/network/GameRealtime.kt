package pg.geobingo.one.network

import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pg.geobingo.one.util.AppLogger

class GameRealtimeManager(private val gameId: String) {

    // Shared channel for this gameId across different screens
    private val channel = supabase.channel("room-$gameId")

    val gameUpdates: Flow<GameDto> =
        channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "games"
            filter("id", FilterOperator.EQ, gameId)
        }.map { it.decodeRecord<GameDto>() }

    val playerInserts: Flow<PlayerDto> =
        channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "players"
            filter("game_id", FilterOperator.EQ, gameId)
        }.map { it.decodeRecord<PlayerDto>() }

    val captureInserts: Flow<CaptureDto> =
        channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "captures"
            filter("game_id", FilterOperator.EQ, gameId)
        }.map { it.decodeRecord<CaptureDto>() }

    val voteInserts: Flow<VoteDto> =
        channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "votes"
            filter("game_id", FilterOperator.EQ, gameId)
        }.map { it.decodeRecord<VoteDto>() }

    val voteSubmissionInserts: Flow<VoteSubmissionDto> =
        channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "vote_submissions"
            filter("game_id", FilterOperator.EQ, gameId)
        }.map { it.decodeRecord<VoteSubmissionDto>() }

    val chatMessageInserts: Flow<GameRepository.ChatMessageDto> =
        channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "chat_messages"
            filter("game_id", FilterOperator.EQ, gameId)
        }.map { it.decodeRecord<GameRepository.ChatMessageDto>() }

    suspend fun subscribe() {
        try {
            channel.subscribe(blockUntilSubscribed = true)
        } catch (e: Exception) {
            AppLogger.e("Realtime", "Subscribe failed for game $gameId", e)
        }
    }

    suspend fun unsubscribe() {
        try {
            channel.unsubscribe()
        } catch (e: Exception) {
            AppLogger.w("Realtime", "Unsubscribe failed for game $gameId", e)
        }
    }
}
