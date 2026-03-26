package pg.geobingo.one.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import pg.geobingo.one.game.GameState
import pg.geobingo.one.network.GameRepository

/**
 * Centralized avatar download — call once in App.kt instead of duplicating in every screen.
 * Downloads avatars for all known players (lobby + game) that have a "selfie" avatar set.
 */
@Composable
fun SyncAvatars(gameState: GameState) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(gameState.gameplay.players, gameState.gameplay.lobbyPlayers) {
        // Collect players with "selfie" avatar from lobby DTOs
        val lobbyWithSelfie = gameState.gameplay.lobbyPlayers
            .filter { it.avatar == "selfie" }
            .map { it.id }

        // Collect players with "selfie" avatar from game players
        val gameWithSelfie = gameState.gameplay.players
            .filter { it.avatar == "selfie" }
            .map { it.id }

        val selfiePlayerIds = (lobbyWithSelfie + gameWithSelfie).distinct()

        selfiePlayerIds
            .filter { id -> !gameState.photo.playerAvatarBytes.containsKey(id) }
            .forEach { playerId ->
                scope.launch {
                    val bytes = GameRepository.downloadAvatarPhoto(playerId)
                    if (bytes != null) {
                        gameState.photo.setAvatar(playerId, bytes)
                    }
                }
            }
    }
}
