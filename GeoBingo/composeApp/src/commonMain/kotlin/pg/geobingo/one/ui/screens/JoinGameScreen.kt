package pg.geobingo.one.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pg.geobingo.one.data.PLAYER_COLORS
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.toCategory
import pg.geobingo.one.network.toHex
import pg.geobingo.one.platform.rememberPhotoCapturer
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGameScreen(gameState: GameState) {
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

    val canJoin = codeInput.trim().length == 6 && nameInput.trim().isNotEmpty()

    SystemBackHandler { gameState.currentScreen = Screen.HOME }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Runde beitreten",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorOnSurface,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { gameState.currentScreen = Screen.HOME }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Zurück",
                            tint = ColorPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorSurface,
                ),
            )
        },
        containerColor = ColorBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))

            // Icon with gradient box
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(GradientHot)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Login,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.White,
                )
            }

            Spacer(Modifier.height(24.dp))

            AnimatedGradientText(
                text = "Code eingeben",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                gradientColors = GradientHot,
                durationMillis = 2500,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Gib den Code ein, den dir\nder Rundenersteller gegeben hat.",
                style = MaterialTheme.typography.bodyMedium,
                color = ColorOnSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(40.dp))

            // Code input
            OutlinedTextField(
                value = codeInput,
                onValueChange = { if (it.length <= 6) codeInput = it.uppercase() },
                label = { Text("Rundencode", color = ColorOnSurfaceVariant) },
                placeholder = { Text("ABC123", color = ColorOutline) },
                modifier = Modifier.fillMaxWidth(),
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
                textStyle = LocalTextStyle.current.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = 4.sp,
                    color = ColorOnSurface,
                ),
            )

            Spacer(Modifier.height(16.dp))

            // Name input
            OutlinedTextField(
                value = nameInput,
                onValueChange = { if (it.length <= 20) nameInput = it },
                label = { Text("Dein Name", color = ColorOnSurfaceVariant) },
                placeholder = { Text("z.B. Anna", color = ColorOutline) },
                modifier = Modifier.fillMaxWidth(),
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

            SelfiePicker(
                avatarBytes = selectedAvatarBytes,
                onTakePhoto = { photoCapturer.launch() },
                onClear = { selectedAvatarBytes = null },
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ColorErrorContainer,
                ) {
                    Text(
                        errorMessage!!,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        color = ColorOnErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            GradientButton(
                text = "Beitreten",
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            val game = GameRepository.getGameByCode(codeInput.trim())
                            if (game == null) {
                                errorMessage = "Runde nicht gefunden. Code prüfen."
                            } else if (game.status != "lobby") {
                                errorMessage = "Diese Runde ist bereits gestartet."
                            } else {
                                val colorIndex = (0..7).random()
                                val color = PLAYER_COLORS[colorIndex].toHex()
                                val playerDto = GameRepository.addPlayer(game.id, nameInput.trim(), color)
                                val avatarBytes = selectedAvatarBytes
                                if (avatarBytes != null) {
                                    try {
                                        GameRepository.uploadAvatarPhoto(playerDto.id, avatarBytes)
                                        GameRepository.setPlayerAvatar(playerDto.id, "selfie")
                                    } catch (e: Exception) { e.printStackTrace() }
                                }
                                if (avatarBytes != null) {
                                    gameState.playerAvatarBytes = gameState.playerAvatarBytes + (playerDto.id to avatarBytes)
                                }
                                val players = GameRepository.getPlayers(game.id)
                                val categories = GameRepository.getCategories(game.id)
                                gameState.gameId = game.id
                                gameState.gameCode = game.code
                                gameState.isHost = false
                                gameState.myPlayerId = playerDto.id
                                gameState.gameDurationMinutes = game.duration_s / 60
                                gameState.jokerMode = game.joker_mode
                                gameState.selectedCategories = categories.map { it.toCategory() }
                                gameState.lobbyPlayers = players
                                gameState.currentScreen = Screen.LOBBY
                            }
                        } catch (e: Exception) {
                            errorMessage = "Fehler: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = canJoin && !isLoading,
                modifier = Modifier.fillMaxWidth(),
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
                                Icons.Default.Login,
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
