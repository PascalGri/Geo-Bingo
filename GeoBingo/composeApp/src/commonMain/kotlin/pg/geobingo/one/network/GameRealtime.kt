package pg.geobingo.one.network

import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

class GameRealtimeManager(private val gameId: String) {

    private val channel = supabase.channel("game-$gameId")

    /** Emits the updated GameDto whenever this game's row changes. */
    val gameUpdates: Flow<GameDto> =
        channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "games"
            filter = "id=eq.$gameId"
        }
        .map { it.decodeRecord<GameDto>() }

    /** Emits a PlayerDto whenever a new player joins this game. */
    val playerInserts: Flow<PlayerDto> =
        channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "players"
            filter = "game_id=eq.$gameId"
        }
        .map { it.decodeRecord<PlayerDto>() }

    /** Emits a CaptureDto whenever any player records a capture in this game. */
    val captureInserts: Flow<CaptureDto> =
        channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "captures"
            filter = "game_id=eq.$gameId"
        }
        .map { it.decodeRecord<CaptureDto>() }

    suspend fun subscribe() = channel.subscribe(blockUntilSubscribed = true)

    suspend fun unsubscribe() = channel.unsubscribe()
}
