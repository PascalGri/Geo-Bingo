package pg.geobingo.one.ui.screens.results

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pg.geobingo.one.data.*
import pg.geobingo.one.game.ActiveSession
import pg.geobingo.one.game.GameConstants
import pg.geobingo.one.game.*
import pg.geobingo.one.util.AppLogger
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.generateCode
import pg.geobingo.one.network.toHex
import pg.geobingo.one.platform.AdManager
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.platform.SettingsKeys
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.platform.rememberShareManager
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.i18n.S
import pg.geobingo.one.util.Analytics
import pg.geobingo.one.ui.theme.*

internal fun formatRating(value: Double): String {
    val rounded = (value * 10).toInt()
    return "${rounded / 10}.${rounded % 10}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val scope = rememberCoroutineScope()
    val ranked = remember(gameState.gameplay.players, gameState.review.allVotes, gameState.review.allCaptures) {
        gameState.rankedPlayers
    }
    val winner = ranked.firstOrNull()?.first
    val shareManager = rememberShareManager()
    var showConfetti by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(600)
        showConfetti = true
    }

    val modeGradient = when (gameState.session.gameMode) {
        GameMode.CLASSIC     -> GradientPrimary
        GameMode.BLIND_BINGO -> GradientCool
        GameMode.WEIRD_CORE  -> GradientWeird
        GameMode.QUICK_START -> GradientQuickStart
    }
    val modeColor = modeGradient.first()

    val anim = rememberStaggeredAnimation(count = 4, staggerDelay = 80L)
    val btnOffset = remember { Animatable(80f) }
    val btnAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch {
            kotlinx.coroutines.delay(250L)
            launch { btnOffset.animateTo(0f, tween(450)) }
            btnAlpha.animateTo(1f, tween(450))
        }
    }

    fun Modifier.staggered(index: Int): Modifier = this.then(anim.modifier(index))

    // Load joker categories if not yet present
    LaunchedEffect(Unit) {
        if (gameState.joker.jokerMode) {
            val gid = gameState.session.gameId ?: return@LaunchedEffect
            try {
                val labels = GameRepository.getJokerLabels(gid)
                gameState.joker.jokerLabels = labels
                val jokerCats = labels.entries.map { (playerId, label) ->
                    Category(id = "joker_$playerId", name = label, emoji = "joker")
                }.filter { jokerCat -> gameState.gameplay.selectedCategories.none { it.id == jokerCat.id } }
                if (jokerCats.isNotEmpty()) gameState.gameplay.selectedCategories = gameState.gameplay.selectedCategories + jokerCats
            } catch (e: Exception) { AppLogger.w("Results", "Joker labels fetch failed", e) }
            // Refresh captures to include joker captures
            try { gameState.review.allCaptures = GameRepository.getCaptures(gid) } catch (e: Exception) { AppLogger.w("Results", "Captures fetch failed", e) }
        }
    }

    // Save to history once on entry, then cleanup server storage
    LaunchedEffect(Unit) {
        ActiveSession.clear() // Game is over, no rejoin needed
        Analytics.track(Analytics.GAME_COMPLETED, mapOf("mode" to gameState.session.gameMode.name, "players" to gameState.gameplay.players.size.toString()))
        gameState.saveToHistory()
        // Participation reward: +2 Stars for completing a round
        gameState.stars.add(2)
        // Update persistent stats
        val myId = gameState.session.myPlayerId
        if (myId != null) {
            val gamesPlayed = AppSettings.getInt(SettingsKeys.GAMES_PLAYED, 0) + 1
            AppSettings.setInt(SettingsKeys.GAMES_PLAYED, gamesPlayed)

            val isWinner = ranked.firstOrNull()?.first?.id == myId
            if (isWinner) {
                val gamesWon = AppSettings.getInt(SettingsKeys.GAMES_WON, 0) + 1
                AppSettings.setInt(SettingsKeys.GAMES_WON, gamesWon)
                val currentStreak = AppSettings.getInt(SettingsKeys.CURRENT_WIN_STREAK, 0) + 1
                AppSettings.setInt(SettingsKeys.CURRENT_WIN_STREAK, currentStreak)
                val longestStreak = AppSettings.getInt(SettingsKeys.LONGEST_WIN_STREAK, 0)
                if (currentStreak > longestStreak) AppSettings.setInt(SettingsKeys.LONGEST_WIN_STREAK, currentStreak)
            } else {
                AppSettings.setInt(SettingsKeys.CURRENT_WIN_STREAK, 0)
            }

            val myAvgRating = gameState.getPlayerAverageRating(myId)
            if (myAvgRating != null) {
                val totalStars = AppSettings.getInt(SettingsKeys.TOTAL_STARS_EARNED, 0) + (myAvgRating * 10).toInt()
                val totalCount = AppSettings.getInt(SettingsKeys.TOTAL_STARS_COUNT, 0) + 10
                AppSettings.setInt(SettingsKeys.TOTAL_STARS_EARNED, totalStars)
                AppSettings.setInt(SettingsKeys.TOTAL_STARS_COUNT, totalCount)
            }

            // ── Daily Challenge Completion ────────────────────────────
            if (!gameState.stars.dailyChallengeCompleted) {
                val challenge = pg.geobingo.one.game.state.DailyChallengeManager.getTodayChallenge()
                val challengeCompleted = when (challenge.type) {
                    pg.geobingo.one.game.state.ChallengeType.WIN_ROUND -> isWinner
                    pg.geobingo.one.game.state.ChallengeType.PLAY_MODE -> {
                        gameState.session.gameMode.name == challenge.targetMode
                    }
                    pg.geobingo.one.game.state.ChallengeType.CAPTURE_CATEGORIES -> {
                        val myCaptureCount = gameState.review.allCaptures.count { it.player_id == myId }
                        myCaptureCount >= 3
                    }
                }
                if (challengeCompleted) {
                    gameState.stars.completeDailyChallenge(challenge.reward)
                    gameState.ui.pendingToast = "${pg.geobingo.one.i18n.S.current.dailyChallengeCompleted} +${challenge.reward} ${pg.geobingo.one.i18n.S.current.stars}"
                }
            }

            // ── Weekly Challenge Progress ─────────────────────────────
            if (!gameState.stars.weeklyChallengeCompleted) {
                val weekly = pg.geobingo.one.game.state.WeeklyChallengeManager.getThisWeekChallenge()
                val myCaptureCount = gameState.review.allCaptures.count { it.player_id == myId }
                when (weekly.type) {
                    pg.geobingo.one.game.state.WeeklyChallengeType.WIN_ROUNDS -> if (isWinner) gameState.stars.incrementWeeklyProgress()
                    pg.geobingo.one.game.state.WeeklyChallengeType.PLAY_ROUNDS -> gameState.stars.incrementWeeklyProgress()
                    pg.geobingo.one.game.state.WeeklyChallengeType.CAPTURE_TOTAL -> gameState.stars.incrementWeeklyProgress(myCaptureCount)
                    pg.geobingo.one.game.state.WeeklyChallengeType.PLAY_ALL_MODES -> {
                        val modesKey = "weekly_modes_played"
                        val played = AppSettings.getString(modesKey, "").split(",").filter { it.isNotBlank() }.toMutableSet()
                        played.add(gameState.session.gameMode.name)
                        AppSettings.setString(modesKey, played.joinToString(","))
                        gameState.stars.incrementWeeklyProgress(0) // just refresh
                        val newProgress = played.size
                        pg.geobingo.one.game.state.WeeklyChallengeManager.setProgress(newProgress)
                        gameState.stars.weeklyChallengeProgress = newProgress
                    }
                    pg.geobingo.one.game.state.WeeklyChallengeType.WIN_STREAK -> {
                        if (isWinner) gameState.stars.incrementWeeklyProgress()
                        else pg.geobingo.one.game.state.WeeklyChallengeManager.setProgress(0).also { gameState.stars.weeklyChallengeProgress = 0 }
                    }
                }
                if (gameState.stars.weeklyChallengeProgress >= weekly.target) {
                    gameState.stars.completeWeeklyChallenge(weekly.reward)
                    gameState.ui.pendingToast = "${pg.geobingo.one.i18n.S.current.weeklyChallengeCompleted} +${weekly.reward} ${pg.geobingo.one.i18n.S.current.stars}"
                }
            }

            // ── Submit Multiplayer Stats ──────────────────────────────
            val userId = pg.geobingo.one.network.AccountManager.currentUserId
            if (userId != null) {
                try {
                    val myCaptureCount = gameState.review.allCaptures.count { it.player_id == myId }
                    val myAvg = gameState.getPlayerAverageRating(myId) ?: 0.0
                    val stats = GameRepository.MultiplayerStatsDto(
                        user_id = userId,
                        display_name = AppSettings.getString("last_player_name", ""),
                        games_played = AppSettings.getInt(SettingsKeys.GAMES_PLAYED, 0),
                        games_won = AppSettings.getInt(SettingsKeys.GAMES_WON, 0),
                        current_win_streak = AppSettings.getInt(SettingsKeys.CURRENT_WIN_STREAK, 0),
                        longest_win_streak = AppSettings.getInt(SettingsKeys.LONGEST_WIN_STREAK, 0),
                        total_captures = myCaptureCount,
                        avg_rating = myAvg,
                    )
                    GameRepository.upsertMultiplayerStats(stats)
                    // Post activity
                    val activityType = if (isWinner) "game_won" else "game_played"
                    val activityDesc = "${AppSettings.getString("last_player_name", "Player")} ${if (isWinner) pg.geobingo.one.i18n.S.current.activityWon else pg.geobingo.one.i18n.S.current.activityPlayed}"
                    GameRepository.postActivity(userId, activityType, activityDesc)
                } catch (e: Exception) {
                    pg.geobingo.one.util.AppLogger.w("Results", "MP stats submit failed", e)
                }
            }
        }
        // Save game metadata locally
        val gid = gameState.session.gameId
        if (gid != null) {
            try {
                val metaJson = buildString {
                    append("{")
                    append("\"gameCode\":\"${gameState.session.gameCode ?: ""}\",")
                    append("\"date\":\"${kotlinx.datetime.Clock.System.now()}\",")
                    append("\"jokerMode\":${gameState.joker.jokerMode},")
                    append("\"players\":[")
                    append(ranked.joinToString(",") { (p, s) -> "{\"name\":\"${p.name}\",\"id\":\"${p.id}\",\"score\":$s,\"color\":\"${p.color.toHex()}\"}" })
                    append("],")
                    append("\"categories\":[")
                    append(gameState.gameplay.selectedCategories.joinToString(",") { "{\"id\":\"${it.id}\",\"name\":\"${it.name}\"}" })
                    append("]}")
                }
                LocalPhotoStore.saveGameMeta(gid, metaJson)
            } catch (e: Exception) { AppLogger.d("Results", "Game meta save failed", e) }
        }
        // Host cleans up server storage after a delay (let other players download photos first)
        if (gameState.session.isHost && gid != null) {
            kotlinx.coroutines.delay(GameConstants.RESULTS_CLEANUP_DELAY_MS)
            try {
                GameRepository.cleanupStoragePhotos(gid, gameState.gameplay.players.map { it.id })
            } catch (e: Exception) { AppLogger.w("Results", "Storage cleanup failed", e) }
        }
        // Auto-sync to cloud if logged in
        val userId = pg.geobingo.one.network.AccountManager.currentUserId
        if (userId != null) {
            try { pg.geobingo.one.network.AccountManager.syncLocalToCloud(userId) } catch (_: Exception) {}
        }
    }

    SystemBackHandler { gameState.resetGame(); nav.resetTo(Screen.HOME) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = S.current.results,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        gradientColors = modeGradient,
                    )
                },
                actions = { pg.geobingo.one.ui.components.TopBarStarsAndProfile(gameState = gameState, onNavigate = { nav.navigateTo(it) }) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = ColorSurface, modifier = Modifier.graphicsLayer { translationY = btnOffset.value; alpha = btnAlpha.value }) {
                var rematchLoading by remember { mutableStateOf(false) }
                var showRematchDialog by remember { mutableStateOf(false) }

                if (showRematchDialog) {
                    fun launchRematch(categories: List<Category>) {
                        if (rematchLoading) return
                        rematchLoading = true
                        showRematchDialog = false
                        scope.launch {
                            try {
                                val myPlayer = gameState.gameplay.players.find { it.id == gameState.session.myPlayerId }
                                val name = myPlayer?.name ?: "Player"
                                val colorHex = myPlayer?.color?.toHex() ?: "#4CAF50"
                                val newCode = generateCode()
                                val mode = gameState.session.gameMode
                                val newGame = GameRepository.createGame(newCode, gameState.gameplay.gameDurationMinutes * 60, gameMode = mode.name)
                                val newPlayer = GameRepository.addPlayer(newGame.id, name, colorHex)
                                GameRepository.addCategories(newGame.id, categories)
                                gameState.resetForRematch(newGame.id, newCode, newPlayer.id)
                                gameState.session.gameMode = mode
                                nav.resetTo(Screen.LOBBY)
                            } catch (e: Exception) {
                                AppLogger.e("Results", "Rematch creation failed", e)
                                rematchLoading = false
                            }
                        }
                    }

                    AlertDialog(
                        onDismissRequest = { showRematchDialog = false },
                        title = {
                            Text(
                                S.current.rematchVoteQuestion,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        text = null,
                        confirmButton = {
                            TextButton(onClick = {
                                Analytics.track(Analytics.REMATCH_SAME, mapOf("mode" to gameState.session.gameMode.name))
                                launchRematch(gameState.gameplay.selectedCategories)
                            }) {
                                Text(S.current.sameCategories, color = modeColor)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                // Pick fresh categories from the same mode pool
                                val currentIds = gameState.gameplay.selectedCategories.map { it.id }.toSet()
                                val count = gameState.gameplay.selectedCategories.size
                                val freshCategories = when (gameState.session.gameMode) {
                                    GameMode.QUICK_START -> {
                                        val outdoor = gameState.session.quickStartOutdoor
                                        quickStartCategories(outdoor)
                                    }
                                    GameMode.WEIRD_CORE -> {
                                        val pool = WEIRD_CORE_CATEGORIES.shuffled()
                                        val fresh = pool.filter { it.id !in currentIds }
                                        if (fresh.size >= count) fresh.take(count)
                                        else (fresh + pool.filter { it.id in currentIds }.shuffled()).take(count)
                                    }
                                    else -> {
                                        // Classic / Blind Bingo: pick from PRESET_CATEGORIES
                                        val pool = CATEGORY_TEMPLATES_SHUFFLED()
                                        val fresh = pool.filter { it.id !in currentIds }
                                        if (fresh.size >= count) fresh.take(count)
                                        else (fresh + pool.filter { it.id in currentIds }.shuffled()).take(count)
                                    }
                                }
                                Analytics.track(Analytics.REMATCH_NEW, mapOf("mode" to gameState.session.gameMode.name))
                                launchRematch(freshCategories)
                            }) {
                                Text(S.current.newCategories, color = modeColor)
                            }
                        },
                        containerColor = ColorSurface,
                        titleContentColor = ColorOnSurface,
                    )
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    OutlinedButton(
                        onClick = { showRematchDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.5.dp, modeColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = modeColor),
                        enabled = !rematchLoading,
                    ) {
                        if (rematchLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = modeColor)
                        } else {
                            Icon(Icons.Default.Replay, null, modifier = Modifier.size(16.dp), tint = modeColor)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                S.current.rematch,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = modeColor,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    GradientButton(
                        text = S.current.share,
                        onClick = {
                            val text = buildString {
                                append("${S.current.shareResultText}\n\n")
                                ranked.take(3).forEachIndexed { index, (player, score) ->
                                    val medal = when(index) { 0 -> "#1"; 1 -> "#2"; 2 -> "#3"; else -> "" }
                                    val avg = gameState.getPlayerAverageRating(player.id)
                                    val starText = if (avg != null) " (${formatRating(avg)})" else ""
                                    append("$medal ${player.name}: $score ${S.current.pointsAbbrev}$starText\n")
                                }
                                append("\n${S.current.showYourSkills}")
                            }
                            Analytics.track(Analytics.SHARE_RESULTS)
                            shareManager.shareText(text)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        gradientColors = modeGradient,
                        leadingIcon = {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        },
                    )
                    if (AdManager.isAdSupported) {
                        Spacer(Modifier.height(8.dp))
                        var rewardedAdLoading by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = {
                                if (!rewardedAdLoading) {
                                    rewardedAdLoading = true
                                    AdManager.showRewardedAd(
                                        onReward = {
                            Analytics.track(Analytics.AD_WATCHED)
                            gameState.stars.add(10)
                            gameState.stars.recordAdWatched()
                            gameState.ui.pendingToast = S.current.starsEarned
                        },
                                        onDismiss = { rewardedAdLoading = false }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, ColorOutlineVariant),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorOnSurface),
                            enabled = !rewardedAdLoading,
                        ) {
                            if (rewardedAdLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ColorOnSurface)
                            } else {
                                Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(S.current.watchBonus, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    GradientButton(
                        text = S.current.newGame,
                        onClick = {
                            val goHome = { gameState.resetGame(); nav.resetTo(Screen.HOME) }
                            // Interstitial nach jeder Runde (ausser bei No-Ads Kauf)
                            if (AdManager.isAdSupported && !gameState.stars.noAdsPurchased) {
                                AdManager.showInterstitialAd { goHome() }
                            } else {
                                goHome()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        gradientColors = modeGradient,
                        leadingIcon = {
                            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        },
                    )
                }
            }
        },
        containerColor = ColorBackground,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Winner banner
            if (gameState.gameplay.teamModeEnabled) {
                // Team mode: show winning team
                val rankedTeams = gameState.rankedTeams
                val winnerTeam = rankedTeams.firstOrNull()
                if (winnerTeam != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggered(0)
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = S.current.wins,
                            modifier = Modifier.size(44.dp),
                            tint = Color(0xFFFBBF24),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            winnerTeam.second,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorOnBackground,
                        )
                        Text(
                            S.current.wins,
                            style = MaterialTheme.typography.bodyLarge,
                            color = ColorOnSurfaceVariant,
                        )
                    }
                }

                // Team ranking cards
                val teamColors = listOf(modeGradient.first(), modeGradient.last(), Color(0xFF22D3EE), Color(0xFFFB923C), Color(0xFF84CC16), Color(0xFFFF6B6B))
                Column(
                    modifier = Modifier.padding(16.dp).staggered(1),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AnimatedGradientText(
                        text = S.current.teamScore,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        gradientColors = modeGradient,
                    )
                    rankedTeams.forEachIndexed { idx, (teamNum, teamName, score) ->
                        val isWinner = idx == 0
                        val teamColor = teamColors[idx % teamColors.size]
                        val teamPlayers = gameState.getTeamPlayers(teamNum)
                        GradientBorderCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 14.dp,
                            borderColors = if (isWinner) GradientGold else listOf(teamColor, teamColor.copy(alpha = 0.5f)),
                            backgroundColor = ColorSurface,
                            borderWidth = if (isWinner) 2.dp else 1.dp,
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    val rankColor = when (idx) {
                                        0 -> Color(0xFFFBBF24)
                                        1 -> Color(0xFF94A3B8)
                                        2 -> Color(0xFFCD7F32)
                                        else -> ColorOnSurfaceVariant
                                    }
                                    Text(
                                        "#${idx + 1}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = rankColor,
                                    )
                                    Column {
                                        Text(
                                            teamName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = teamColor,
                                        )
                                        Text(
                                            teamPlayers.joinToString(", ") { it.name },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ColorOnSurfaceVariant,
                                        )
                                    }
                                }
                                Text(
                                    "$score ${S.current.pointsAbbrev}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWinner) Color(0xFFFBBF24) else ColorOnSurface,
                                )
                            }
                        }
                    }
                }
            } else {
                // Individual mode: show winning player
                if (winner != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggered(0)
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = S.current.wins,
                            modifier = Modifier.size(44.dp),
                            tint = Color(0xFFFBBF24),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            winner.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorOnBackground,
                        )
                        Text(
                            S.current.wins,
                            style = MaterialTheme.typography.bodyLarge,
                            color = ColorOnSurfaceVariant,
                        )
                    }
                }

                // Podium
                if (ranked.size >= 2) {
                    Spacer(Modifier.height(20.dp))
                    Box(modifier = Modifier.staggered(1)) {
                        DarkPodiumSection(ranked = ranked.take(3), playerAvatarBytes = gameState.photo.playerAvatarBytes, gameState = gameState)
                    }
                }
            }

            // Full ranking
            Column(
                modifier = Modifier.padding(16.dp).staggered(if (gameState.gameplay.teamModeEnabled) 3 else 2),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    S.current.allResults,
                    style = MaterialTheme.typography.labelLarge,
                    color = ColorOnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                val firstCapturers = remember(gameState.review.allCaptures) { gameState.getFirstCapturers() }
                ranked.forEachIndexed { index, (player, score) ->
                    val capturedCount = gameState.review.allCaptures.count { it.player_id == player.id }
                    val speedBonus = gameState.getSpeedBonusCount(player.id)
                    val avgRating = gameState.getPlayerAverageRating(player.id)
                    val breakdown = remember(
                        gameState.gameplay.selectedCategories,
                        gameState.review.allVotes,
                        gameState.review.allCaptures,
                    ) {
                        gameState.gameplay.selectedCategories.map { category ->
                            val catAvgRating = gameState.getCategoryAverageRating(player.id, category.id)
                            val hasSpeed = firstCapturers[category.id] == player.id
                            val votes = gameState.review.allVotes.filter {
                                it.target_player_id == player.id && it.category_id == category.id
                            }
                            val isControversial = if (votes.size >= 2) {
                                val mean = votes.map { it.rating.toDouble() }.average()
                                val variance = votes.map { (it.rating - mean) * (it.rating - mean) }.average()
                                kotlin.math.sqrt(variance) > 1.5
                            } else false
                            CategoryBreakdownItem(
                                categoryName = category.name,
                                averageRating = catAvgRating,
                                hasSpeedBonus = hasSpeed,
                                isControversial = isControversial,
                            )
                        }
                    }
                    DarkRankCard(
                        rank = index + 1,
                        player = player,
                        score = score,
                        capturedCount = capturedCount,
                        totalCategories = gameState.gameplay.selectedCategories.size,
                        captures = gameState.getPlayerCaptures(player.id).map { it.name },
                        isWinner = index == 0,
                        speedBonus = speedBonus,
                        averageRating = avgRating,
                        photoBytes = gameState.photo.playerAvatarBytes[player.id],
                        categoryBreakdown = breakdown,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // Best Photo Highlight
            if (gameState.review.allCaptures.isNotEmpty() && gameState.review.allVotes.isNotEmpty()) {
                val gameId = gameState.session.gameId
                if (gameId != null) {
                    // Find the capture with highest average rating
                    val bestCapture = gameState.review.allCaptures.maxByOrNull { capture ->
                        gameState.getCategoryAverageRating(capture.player_id, capture.category_id) ?: 0.0
                    }
                    val bestRating = bestCapture?.let {
                        gameState.getCategoryAverageRating(it.player_id, it.category_id)
                    }
                    if (bestCapture != null && bestRating != null && bestRating >= 1.0) {
                        val bestPlayer = gameState.gameplay.players.find { it.id == bestCapture.player_id }
                        val bestCategory = gameState.gameplay.selectedCategories.find { it.id == bestCapture.category_id }
                        Column(
                            modifier = Modifier.padding(16.dp).staggered(3),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFFFBBF24),
                                )
                                Spacer(Modifier.width(6.dp))
                                AnimatedGradientText(
                                    text = S.current.bestPhoto,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    gradientColors = GradientGold,
                                )
                            }
                            GradientBorderCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 14.dp,
                                borderColors = GradientGold,
                                backgroundColor = ColorSurface,
                                borderWidth = 2.dp,
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    GalleryPhotoItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        gameId = gameId,
                                        capture = bestCapture,
                                        players = gameState.gameplay.players,
                                        categories = gameState.gameplay.selectedCategories,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column {
                                            if (bestPlayer != null) {
                                                Text(
                                                    bestPlayer.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = bestPlayer.color,
                                                )
                                            }
                                            if (bestCategory != null) {
                                                Text(
                                                    bestCategory.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = ColorOnSurfaceVariant,
                                                )
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp), tint = Color(0xFFFBBF24))
                                            Spacer(Modifier.width(2.dp))
                                            Text(
                                                formatRating(bestRating),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFBBF24),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Photo Gallery
            if (gameState.review.allCaptures.isNotEmpty()) {
                val gameId = gameState.session.gameId
                if (gameId != null) {
                    Column(
                        modifier = Modifier.padding(16.dp).staggered(3),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            S.current.allPhotos,
                            style = MaterialTheme.typography.labelLarge,
                            color = ColorOnSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        val rows = gameState.review.allCaptures.chunked(2)
                        rows.forEach { rowCaptures ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                rowCaptures.forEach { capture ->
                                    GalleryPhotoItem(
                                        modifier = Modifier.weight(1f),
                                        gameId = gameId,
                                        capture = capture,
                                        players = gameState.gameplay.players,
                                        categories = gameState.gameplay.selectedCategories,
                                    )
                                }
                                if (rowCaptures.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
        // Winner confetti overlay
        if (winner != null) {
            ConfettiEffect(trigger = showConfetti, modifier = Modifier.fillMaxSize())
        }
        } // end Box
    }
}
