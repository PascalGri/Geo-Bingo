package pg.geobingo.one.network

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import pg.geobingo.one.game.GameConstants
import pg.geobingo.one.util.AppLogger
import kotlin.math.pow

/**
 * Consolidates Realtime subscriptions + fallback polling into unified flows.
 * Automatically cancels when the provided [scope] is cancelled (e.g. on navigation).
 */
class GameSyncManager(
    private val gameId: String,
    private val realtime: GameRealtimeManager?,
    private val scope: CoroutineScope,
) {
    private val _gameUpdates = MutableSharedFlow<GameDto>(replay = 1)
    val gameUpdates: SharedFlow<GameDto> = _gameUpdates

    private val _playerJoined = MutableSharedFlow<Unit>(replay = 0)
    val playerJoined: SharedFlow<Unit> = _playerJoined

    private val _captureInserted = MutableSharedFlow<CaptureDto>(replay = 0)
    val captureInserted: SharedFlow<CaptureDto> = _captureInserted

    private val _voteSubmissionInserted = MutableSharedFlow<VoteSubmissionDto>(replay = 0)
    val voteSubmissionInserted: SharedFlow<VoteSubmissionDto> = _voteSubmissionInserted

    private val _chatMessageInserted = MutableSharedFlow<GameRepository.ChatMessageDto>(replay = 0)
    val chatMessageInserted: SharedFlow<GameRepository.ChatMessageDto> = _chatMessageInserted

    private var pollingJob: Job? = null
    private var realtimeJobs: List<Job> = emptyList()

    fun start() {
        startRealtime()
        startFallbackPolling()
    }

    fun stop() {
        pollingJob?.cancel()
        realtimeJobs.forEach { it.cancel() }
        scope.launch {
            try { realtime?.unsubscribe() } catch (e: Exception) {
                AppLogger.w("Sync", "Unsubscribe failed", e)
            }
        }
    }

    private fun startRealtime() {
        if (realtime == null) return

        scope.launch {
            var attempt = 0
            while (isActive) {
                try {
                    realtime.subscribe()
                    break // success
                } catch (e: Exception) {
                    attempt++
                    val delayMs = (GameConstants.RETRY_INITIAL_DELAY_MS * GameConstants.RETRY_BACKOFF_FACTOR.pow(attempt - 1))
                        .toLong().coerceAtMost(GameConstants.RETRY_MAX_DELAY_MS)
                    AppLogger.w("Sync", "Realtime subscribe failed (attempt $attempt), retrying in ${delayMs}ms", e)
                    if (attempt >= GameConstants.RETRY_MAX_ATTEMPTS) {
                        AppLogger.e("Sync", "Realtime subscribe gave up after $attempt attempts")
                        break
                    }
                    delay(delayMs)
                }
            }
        }

        realtimeJobs = listOf(
            scope.launch {
                realtime.gameUpdates.collect { _gameUpdates.emit(it) }
            },
            scope.launch {
                realtime.playerInserts.collect { _playerJoined.emit(Unit) }
            },
            scope.launch {
                realtime.captureInserts.collect { _captureInserted.emit(it) }
            },
            scope.launch {
                realtime.voteSubmissionInserts.collect { _voteSubmissionInserted.emit(it) }
            },
            scope.launch {
                realtime.chatMessageInserts.collect { _chatMessageInserted.emit(it) }
            },
        )
    }

    private fun startFallbackPolling() {
        pollingJob = scope.launch {
            var interval = GameConstants.POLLING_INITIAL_INTERVAL_MS
            while (isActive) {
                delay(interval)
                try {
                    val game = GameRepository.getGameById(gameId)
                    if (game != null) {
                        _gameUpdates.emit(game)
                    }
                    interval = GameConstants.POLLING_INITIAL_INTERVAL_MS
                } catch (e: Exception) {
                    AppLogger.w("Sync", "Polling error for $gameId", e)
                    interval = (interval * GameConstants.POLLING_BACKOFF_FACTOR)
                        .toLong().coerceAtMost(GameConstants.POLLING_MAX_INTERVAL_MS)
                }
            }
        }
    }
}
