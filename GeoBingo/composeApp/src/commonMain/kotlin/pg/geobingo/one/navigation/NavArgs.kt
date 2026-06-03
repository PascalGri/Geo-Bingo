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

    /** Arguments for MatchDetailScreen. */
    data class MatchDetail(val gameId: String, val entry: GameHistoryEntry) : NavArgs

    /** Arguments for SoloLeaderboardScreen — open straight onto the daily board. */
    data class Leaderboard(val daily: Boolean = false) : NavArgs

    /**
     * Arguments for SettingsScreen — when navigated from an AI-consent
     * gate dialog, the screen scrolls to the AI/Privacy section so the
     * user immediately sees the toggles they need to flip.
     */
    data class Settings(val anchor: SettingsAnchor = SettingsAnchor.NONE) : NavArgs

    enum class SettingsAnchor { NONE, AI_PRIVACY }
}
