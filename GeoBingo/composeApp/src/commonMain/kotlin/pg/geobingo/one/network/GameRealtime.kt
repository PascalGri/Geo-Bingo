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
        }
        .map { it.decodeRecord<GameDto>() }
        .filter { it.id == gameId }

    /** Emits a PlayerDto whenever a new player joins this game. */
    val playerInserts: Flow<PlayerDto> =
        channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "players"
        }
        .map { it.decodeRecord<PlayerDto>() }
        .filter { it.game_id == gameId }

    suspend fun subscribe() = channel.subscribe(blockUntilSubscribed = true)

    suspend fun unsubscribe() = channel.unsubscribe()
}
