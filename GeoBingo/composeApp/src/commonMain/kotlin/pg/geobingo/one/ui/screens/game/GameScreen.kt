package pg.geobingo.one.ui.screens.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pg.geobingo.one.data.Category
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.*
import pg.geobingo.one.i18n.S
import pg.geobingo.one.platform.RequestLocationPermission
import pg.geobingo.one.platform.rememberPhotoCapturer
import pg.geobingo.one.ui.components.MiniShopPopup
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.viewmodel.GameViewModel

private const val RETAKE_COST = 5

@Composable
fun GameScreen(gameState: GameState) {
    RequestLocationPermission()

    val nav = remember { ServiceLocator.navigation }
    val vm = viewModel { ServiceLocator.createGameViewModel() }
    val feedback = rememberFeedback(gameState)

    var photoTargetPlayerId by remember { mutableStateOf("") }
    var photoTargetCategoryId by remember { mutableStateOf("") }
    var jokerDialogVisible by remember { mutableStateOf(false) }
    var showMiniShop by remember { mutableStateOf(false) }
    var miniShopNeeded by remember { mutableStateOf(0) }

    val photoCapturer = rememberPhotoCapturer { bytes ->
        vm.handlePhotoCaptured(
            bytes = bytes,
            playerId = photoTargetPlayerId,
            categoryId = photoTargetCategoryId,
            onFeedbackCapture = { feedback.capture() },
        )
    }

    // Start observing realtime/polling when screen enters composition
    LaunchedEffect(gameState.session.gameId) {
        gameState.session.gameId?.let {
            gameState.ensureSyncManager(it, this)
        }
        // Persist session for rejoin on accidental close
        pg.geobingo.one.game.ActiveSession.save(gameState)
        vm.startObserving()
    }

    val modeGradient = when (gameState.session.gameMode) {
        GameMode.CLASSIC     -> GradientPrimary
        GameMode.BLIND_BINGO -> GradientCool
        GameMode.WEIRD_CORE  -> GradientWeird
        GameMode.QUICK_START -> GradientQuickStart
        GameMode.AI_JUDGE    -> GradientAiJudge
    }
    val modeColor = modeGradient.first()

    // ── Joker Dialog ─────────────────────────────────────────────────────
    if (jokerDialogVisible) {
        AlertDialog(
            onDismissRequest = { jokerDialogVisible = false },
            containerColor = ColorSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Style, null, modifier = Modifier.size(20.dp), tint = modeColor)
                    Text(
                        S.current.useJoker,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorOnSurface,
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        S.current.jokerTopicPrompt,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = vm.jokerLabelInput,
                        onValueChange = { if (it.length <= 40) vm.jokerLabelInput = it },
                        placeholder = { Text(S.current.jokerTopicPlaceholder, color = ColorOnSurfaceVariant) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = modeColor,
                            unfocusedBorderColor = ColorOutline,
                            focusedTextColor = ColorOnSurface,
                            unfocusedTextColor = ColorOnSurface,
                            cursorColor = modeColor,
                        ),
                    )
                }
            },
            confirmButton = {
                GradientButton(
                    text = S.current.takePhoto,
                    onClick = {
                        jokerDialogVisible = false
                        val myId = gameState.session.myPlayerId ?: return@GradientButton
                        photoTargetPlayerId = myId
                        photoTargetCategoryId = "joker_$myId"
                        vm.addJokerCategory(myId)
                        photoCapturer.launch()
                    },
                    enabled = vm.jokerLabelInput.trim().isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            dismissButton = {
                TextButton(onClick = { jokerDialogVisible = false }) {
                    Text(S.current.cancel)
                }
            },
        )
    }

    // Mini shop popup
    if (showMiniShop) {
        MiniShopPopup(
            gameState = gameState,
            neededStars = miniShopNeeded,
            onDismiss = { showMiniShop = false },
            onPurchased = { showMiniShop = false },
        )
    }

    // ── Game content ─────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        GameScreenContent(
            gameState = gameState,
            finishCountdownSeconds = vm.finishCountdownSeconds,
            uploadSuccessCategory = vm.uploadSuccessCategory,
            onJokerClick = { feedback.tap(); jokerDialogVisible = true },
            onVoteToEnd = { feedback.vote(); vm.voteToEnd() },
            onCameraClick = { playerId, catId ->
                val isRetake = gameState.isCaptured(playerId, catId)
                if (isRetake) {
                    // Retake costs stars
                    if (gameState.stars.spend(RETAKE_COST)) {
                        photoTargetPlayerId = playerId
                        photoTargetCategoryId = catId
                        photoCapturer.launch()
                    } else {
                        miniShopNeeded = RETAKE_COST
                        showMiniShop = true
                    }
                } else {
                    feedback.categorySelect()
                    photoTargetPlayerId = playerId
                    photoTargetCategoryId = catId
                    photoCapturer.launch()
                }
            },
        )
        // Chat overlay at bottom
        pg.geobingo.one.ui.components.GameChatOverlay(
            gameState = gameState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
