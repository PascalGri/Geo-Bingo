package pg.geobingo.one.ui.screens.results

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import kotlin.math.*
import pg.geobingo.one.data.Category
import pg.geobingo.one.data.Player
import pg.geobingo.one.network.CaptureDto
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.platform.saveImageToDevice
import pg.geobingo.one.platform.toImageBitmap
import pg.geobingo.one.ui.theme.*

internal val mapHttpClient = HttpClient()

/**
 * Converts lat/lon + zoom to OSM tile coordinates.
 * Returns (tileX, tileY, offsetXInTile, offsetYInTile) where offsets are 0..255 pixel positions.
 */
internal fun latLonToTile(lat: Double, lon: Double, zoom: Int): Triple<Int, Int, Pair<Float, Float>> {
    val n = 1 shl zoom
    val xTile = ((lon + 180.0) / 360.0 * n).toInt()
    val latRad = lat * PI / 180.0
    val yTile = ((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n).toInt()
    // Fractional pixel offset within the tile (tiles are 256x256)
    val xOffset = (((lon + 180.0) / 360.0 * n - xTile) * 256).toFloat()
    val yOffset = (((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n - yTile) * 256).toFloat()
    return Triple(xTile, yTile, Pair(xOffset, yOffset))
}

@Composable
internal fun GalleryPhotoItem(
    modifier: Modifier = Modifier,
    gameId: String,
    capture: CaptureDto,
    players: List<Player>,
    categories: List<Category>,
) {
    var photo by remember(capture.id) { mutableStateOf<ImageBitmap?>(null) }
    var photoBytes by remember(capture.id) { mutableStateOf<ByteArray?>(null) }
    var loading by remember(capture.id) { mutableStateOf(true) }
    var showFullscreen by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(capture.id) {
        loading = true
        // downloadPhoto now has built-in cache-first strategy
        val bytes = GameRepository.downloadPhoto(gameId, capture.player_id, capture.category_id)
        photoBytes = bytes
        photo = if (bytes != null) kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { bytes.toImageBitmap() } else null
        loading = false
    }

    val player = players.find { it.id == capture.player_id }
    val category = categories.find { it.id == capture.category_id }

    if (showFullscreen && photo != null) {
        AlertDialog(
            onDismissRequest = { showFullscreen = false },
            containerColor = Color(0xFF0A0A0A),
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        bitmap = photo!!,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.FillWidth,
                    )
                    if (player != null || category != null) {
                        Spacer(Modifier.height(8.dp))
                        if (player != null) {
                            Text(player.name, color = player.color, fontWeight = FontWeight.Bold)
                        }
                        if (category != null) {
                            Text(category.name, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    // Static map preview
                    if (capture.latitude != null && capture.longitude != null) {
                        Spacer(Modifier.height(10.dp))
                        StaticMapPreview(
                            latitude = capture.latitude!!,
                            longitude = capture.longitude!!,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                    }
                }
            },
            confirmButton = {
                Row {
                    if (photoBytes != null) {
                        TextButton(onClick = {
                            scope.launch {
                                val filename = "${player?.name ?: "foto"}_${category?.name ?: ""}.jpg"
                                val ok = saveImageToDevice(photoBytes!!, filename)
                                saveSuccess = ok
                            }
                        }) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (saveSuccess == true) "Gespeichert" else "Speichern")
                        }
                    }
                    TextButton(onClick = { showFullscreen = false }) { Text("Schließen") }
                }
            },
        )
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(ColorSurface)
            .clickable { if (photo != null) showFullscreen = true },
    ) {
        when {
            loading -> {
                ShimmerPlaceholder(modifier = Modifier.fillMaxSize(), cornerRadius = 10.dp)
            }
            photo != null -> {
                Image(
                    bitmap = photo!!,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = ColorOnSurfaceVariant,
                    )
                }
            }
        }

        // Location pin overlay at top-right
        if (capture.latitude != null && capture.longitude != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(ColorPrimary.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = Color.White,
                )
            }
        }

        // Player + category overlay at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 5.dp, vertical = 3.dp),
        ) {
            Column {
                if (player != null) {
                    Text(
                        player.name,
                        color = player.color,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
                if (category != null) {
                    Text(
                        category.name,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
internal fun StaticMapPreview(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier,
) {
    val zoom = 15
    var tileImages by remember(latitude, longitude) { mutableStateOf<List<Pair<ImageBitmap, Pair<Int, Int>>>?>(null) }
    var pinOffset by remember(latitude, longitude) { mutableStateOf(Pair(0f, 0f)) }
    var loading by remember(latitude, longitude) { mutableStateOf(true) }

    LaunchedEffect(latitude, longitude) {
        loading = true
        tileImages = try {
            val (tileX, tileY, offset) = latLonToTile(latitude, longitude, zoom)
            pinOffset = offset
            // Load a 3x3 grid of tiles around the center for a wider view
            val tiles = mutableListOf<Pair<ImageBitmap, Pair<Int, Int>>>()
            for (dy in -1..1) {
                for (dx in -1..1) {
                    val tx = tileX + dx
                    val ty = tileY + dy
                    val url = "https://tile.openstreetmap.org/$zoom/$tx/$ty.png"
                    val bytes = mapHttpClient.get(url).readRawBytes()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                        bytes.toImageBitmap()
                    }?.let { tiles.add(it to Pair(dx, dy)) }
                }
            }
            tiles
        } catch (_: Exception) {
            null
        }
        loading = false
    }

    Box(modifier = modifier.background(ColorSurface)) {
        when {
            loading -> ShimmerPlaceholder(modifier = Modifier.fillMaxSize())
            tileImages != null && tileImages!!.isNotEmpty() -> {
                // Draw tile grid using Canvas, centering the pin location
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val tileSize = 256f
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    // The pin is at pinOffset within the center tile (dx=0, dy=0)
                    // We need to position tiles so the pin ends up at center
                    val originX = centerX - pinOffset.first
                    val originY = centerY - pinOffset.second

                    for ((bitmap, offset) in tileImages!!) {
                        val (dx, dy) = offset
                        val dstLeft = originX + dx * tileSize
                        val dstTop = originY + dy * tileSize
                        drawImage(
                            image = bitmap,
                            dstOffset = androidx.compose.ui.unit.IntOffset(dstLeft.toInt(), dstTop.toInt()),
                        )
                    }
                }
                // Pin icon centered
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-10).dp) // Shift up so pin tip points at location
                        .size(28.dp),
                    tint = Color(0xFFE53935),
                )
            }
            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Karte nicht verfügbar", color = ColorOnSurfaceVariant, fontSize = 11.sp)
                }
            }
        }
    }
}
