package pg.geobingo.one.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import pg.geobingo.one.game.GameState
import pg.geobingo.one.network.GameRepository

/**
 * Centralized avatar download — call once in App.kt instead of duplicating in every screen.
 * Downloads avatars for all known players (lobby + game) that haven't been fetched yet.
 */
@Composable
fun SyncAvatars(gameState: GameState) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(gameState.gameplay.players, gameState.gameplay.lobbyPlayers) {
        val allPlayers = (gameState.gameplay.players.map { it.id } +
                gameState.gameplay.lobbyPlayers.map { it.id })
            .distinct()

        allPlayers
            .filter { id -> id !in gameState.photo.playerAvatarBytes && id !in gameState.photo.triedAvatarDownloads }
            .forEach { playerId ->
                scope.launch {
                    gameState.photo.triedAvatarDownloads = gameState.photo.triedAvatarDownloads + playerId
                    val bytes = GameRepository.downloadAvatarPhoto(playerId)
                    if (bytes != null) {
                        gameState.photo.playerAvatarBytes = gameState.photo.playerAvatarBytes + (playerId to bytes)
                    }
                }
            }
    }
}
