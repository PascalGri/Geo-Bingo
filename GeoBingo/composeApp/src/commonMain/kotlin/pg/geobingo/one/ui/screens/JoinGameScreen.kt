package pg.geobingo.one.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pg.geobingo.one.data.PLAYER_COLORS
import pg.geobingo.one.game.GameMode
import pg.geobingo.one.util.AppLogger
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.toCategory
import pg.geobingo.one.network.toHex
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.platform.rememberPhotoCapturer
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.i18n.S
import pg.geobingo.one.ui.components.SelfiePicker
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.ui.theme.Spacing
import pg.geobingo.one.ui.theme.rememberStaggeredAnimation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGameScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    var codeInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var selectedAvatarBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val photoCapturer = rememberPhotoCapturer { bytes ->
        if (bytes != null) selectedAvatarBytes = bytes
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        val msg = errorMessage ?: return@LaunchedEffect
        errorMessage = null
        snackbarHostState.showSnackbar(msg)
    }

    val canJoin = codeInput.trim().length == 6 && nameInput.trim().isNotEmpty()

    val anim = rememberStaggeredAnimation(count = 6)
    val btnOffset = remember { Animatable(80f) }
    val btnAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch {
            delay(300L)
            launch { btnOffset.animateTo(0f, tween(450)) }
            btnAlpha.animateTo(1f, tween(450))
        }
    }

    fun Modifier.staggered(index: Int): Modifier = this.then(anim.modifier(index))

    SystemBackHandler { nav.goBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = S.current.joinRound,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = GradientHot,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.goBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = S.current.back,
                            tint = ColorPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorSurface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ColorBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))

            // Icon with gradient box
            Box(
                modifier = Modifier
                    .staggered(0)
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(GradientHot)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Login,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.White,
                )
            }

            Spacer(Modifier.height(24.dp))

            AnimatedGradientText(
                text = S.current.enterCode,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                gradientColors = GradientHot,
                durationMillis = 2500,
                modifier = Modifier.staggered(1),
            )

            Spacer(Modifier.height(8.dp))

            Text(
                S.current.otherPlayersJoinViaCode,
                style = MaterialTheme.typography.bodyMedium,
                color = ColorOnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.staggered(2),
            )

            Spacer(Modifier.height(40.dp))

            // Code input
            OutlinedTextField(
                value = codeInput,
                onValueChange = { if (it.length <= 6) codeInput = it.uppercase() },
                label = { Text(S.current.roundCode, color = ColorOnSurfaceVariant) },
                placeholder = { Text("ABC123", color = ColorOutline) },
                modifier = Modifier.fillMaxWidth().staggered(3),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ColorPrimary,
                    unfocusedBorderColor = ColorOutline,
                    focusedTextColor = ColorOnSurface,
                    unfocusedTextColor = ColorOnSurface,
                    focusedLabelColor = ColorPrimary,
                    cursorColor = ColorPrimary,
                    focusedContainerColor = ColorSurface,
                    unfocusedContainerColor = ColorSurface,
                ),
                leadingIcon = {
                    Icon(Icons.Default.Tag, contentDescription = null, tint = ColorPrimary)
                },
                textStyle = AppTextStyles.codeInput.copy(color = ColorOnSurface),
            )

            Spacer(Modifier.height(16.dp))

            // Name input
            OutlinedTextField(
                value = nameInput,
                onValueChange = { if (it.length <= 20) nameInput = it },
                label = { Text(S.current.nameAndAvatar, color = ColorOnSurfaceVariant) },
                placeholder = { Text(S.current.namePlaceholder, color = ColorOutline) },
                modifier = Modifier.fillMaxWidth().staggered(4),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ColorPrimary,
                    unfocusedBorderColor = ColorOutline,
                    focusedTextColor = ColorOnSurface,
                    unfocusedTextColor = ColorOnSurface,
                    focusedLabelColor = ColorPrimary,
                    cursorColor = ColorPrimary,
                    focusedContainerColor = ColorSurface,
                    unfocusedContainerColor = ColorSurface,
                ),
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = ColorPrimary)
                },
            )

            Spacer(Modifier.height(16.dp))

            Box(modifier = Modifier.staggered(5)) {
                SelfiePicker(
                    avatarBytes = selectedAvatarBytes,
                    onTakePhoto = { photoCapturer.launch() },
                    onClear = { selectedAvatarBytes = null },
                )
            }

            Spacer(Modifier.weight(1f))

            GradientButton(
                text = S.current.joinGame,
                modifier = Modifier.fillMaxWidth().graphicsLayer {
                    translationY = btnOffset.value
                    alpha = btnAlpha.value
                },
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            val game = GameRepository.getGameByCode(codeInput.trim())
                            if (game == null) {
                                errorMessage = "${S.current.error}: Code"
                            } else if (game.status != "lobby") {
                                errorMessage = "${S.current.error}: Game started"
                            } else {
                                val colorIndex = (0..7).random()
                                val color = PLAYER_COLORS[colorIndex].toHex()
                                val playerDto = GameRepository.addPlayer(game.id, nameInput.trim(), color)
                                val avatarBytes = selectedAvatarBytes
                                if (avatarBytes != null) {
                                    try {
                                        GameRepository.uploadAvatarPhoto(playerDto.id, avatarBytes)
                                        GameRepository.setPlayerAvatar(playerDto.id, "selfie")
                                    } catch (e: Exception) { AppLogger.w("Join", "Avatar upload failed", e) }
                                    try { LocalPhotoStore.saveAvatar(playerDto.id, avatarBytes) } catch (e: Exception) { AppLogger.d("Join", "Avatar local save failed", e) }
                                }
                                if (avatarBytes != null) {
                                    gameState.photo.setAvatar(playerDto.id, avatarBytes)
                                }
                                val players = GameRepository.getPlayers(game.id)
                                val categories = GameRepository.getCategories(game.id)
                                gameState.session.gameId = game.id
                                gameState.session.gameCode = game.code
                                gameState.session.isHost = false
                                gameState.session.myPlayerId = playerDto.id
                                gameState.gameplay.gameDurationMinutes = game.duration_s / 60
                                gameState.joker.jokerMode = game.joker_mode
                                gameState.session.gameMode = try { GameMode.valueOf(game.game_mode) } catch (e: Exception) { AppLogger.d("Join", "Unknown game mode: ${game.game_mode}", e); GameMode.CLASSIC }
                                gameState.gameplay.selectedCategories = categories.map { it.toCategory() }
                                gameState.gameplay.lobbyPlayers = players
                                nav.navigateTo(Screen.LOBBY)
                            }
                        } catch (e: Exception) {
                            errorMessage = "Fehler: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = canJoin && !isLoading,
                gradientColors = GradientHot,
                leadingIcon = {
                    Box(contentAlignment = Alignment.Center) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Login,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White,
                            )
                        }
                    }
                },
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}
