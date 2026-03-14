package pg.geobingo.one.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

fun getCategoryIcon(categoryId: String): ImageVector = when (categoryId) {
    "tree"       -> Icons.Default.Park
    "building"   -> Icons.Default.AccountBalance
    "bird"       -> Icons.Default.Flight
    "flower"     -> Icons.Default.LocalFlorist
    "bicycle"    -> Icons.Default.DirectionsBike
    "coffee"     -> Icons.Default.LocalCafe
    "mural"      -> Icons.Default.Brush
    "dog"        -> Icons.Default.Pets
    "fountain"   -> Icons.Default.Waves
    "bridge"     -> Icons.Default.Landscape
    "letterbox"  -> Icons.Default.Mail
    "store"      -> Icons.Default.Store
    "sunset"     -> Icons.Default.WbSunny
    "people"     -> Icons.Default.Group
    "traffic"    -> Icons.Default.Traffic
    "plant"      -> Icons.Default.Eco
    "music"      -> Icons.Default.MusicNote
    "sport"      -> Icons.Default.FitnessCenter
    "reflection" -> Icons.Default.Flip
    "shadow"     -> Icons.Default.Contrast
    "stairs"     -> Icons.Default.Stairs
    "bench"      -> Icons.Default.Weekend
    "round"      -> Icons.Default.Circle
    "yellow"     -> Icons.Default.Star
    else         -> Icons.Default.PhotoCamera
}
