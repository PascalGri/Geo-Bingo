package pg.geobingo.one.navigation

import pg.geobingo.one.game.GameHistoryEntry

/**
 * Typed navigation arguments — replaces ad-hoc state stored in UiState
 * (pendingGameInviteCode, selectedDmFriendId, etc.).
 *
 * Each screen that requires arguments has a corresponding sealed subclass.
 * Screens without arguments don't need an entry here.
 */
sealed interface NavArgs {
    /** Arguments for JoinGameScreen. */
    data class JoinGame(val inviteCode: String? = null) : NavArgs

    /** Arguments for DirectMessageScreen. */
    data class DirectMessage(val friendId: String, val friendName: String) : NavArgs

    /** Arguments for MatchDetailScreen. */
    data class MatchDetail(val gameId: String, val entry: GameHistoryEntry) : NavArgs
}
