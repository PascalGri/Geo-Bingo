package pg.geobingo.one.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

fun getCategoryIcon(categoryId: String): ImageVector = when (categoryId) {
    // ── Kleidung ──────────────────────────────────────────────────────────────
    "pants"             -> Icons.Default.Checkroom
    "jacket"            -> Icons.Default.Checkroom
    "hat"               -> Icons.Default.ChildCare
    "sneaker"           -> Icons.AutoMirrored.Filled.DirectionsWalk
    // ── Fahrzeuge ─────────────────────────────────────────────────────────────
    "car"               -> Icons.Default.DirectionsCar
    "vehicle"           -> Icons.Default.ElectricScooter
    "bicycle"           -> Icons.Default.PedalBike
    // ── Tiere ─────────────────────────────────────────────────────────────────
    "animal"            -> Icons.Default.Pets
    "dog_breed"         -> Icons.Default.Pets
    "bird"              -> Icons.Default.Flight
    // ── Natur ─────────────────────────────────────────────────────────────────
    "nature"            -> Icons.Default.Eco
    "tree"              -> Icons.Default.Nature
    "water"             -> Icons.Default.Waves
    "sky"               -> Icons.Default.Cloud
    // ── Essen & Trinken ───────────────────────────────────────────────────────
    "food"              -> Icons.Default.Icecream
    "food_truck"        -> Icons.Default.LocalShipping
    "cafe"              -> Icons.Default.Fastfood
    "street_food"       -> Icons.Default.House
    // ── Architektur ───────────────────────────────────────────────────────────
    "building"          -> Icons.Default.AccountBalance
    "door"              -> Icons.Default.Yard
    "window"            -> Icons.Default.Window
    "balcony"           -> Icons.Default.Balcony
    "arch"              -> Icons.Default.Architecture
    // ── Straßenkunst ─────────────────────────────────────────────────────────
    "street_art"        -> Icons.Default.Casino
    "mural"             -> Icons.Default.Extension
    "sculpture"         -> Icons.Default.NearMe
    "pattern"           -> Icons.Default.Draw
    // ── Schilder ─────────────────────────────────────────────────────────────
    "sign"              -> Icons.Default.Signpost
    "funny_sign"        -> Icons.Default.EmojiEmotions
    "number"            -> Icons.Default.Tag
    // ── Reflexionen & Licht ───────────────────────────────────────────────────
    "shadow"            -> Icons.Default.Tonality
    "reflection"        -> Icons.Default.Flip
    "light"             -> Icons.Default.SportsBasketball
    // ── Sport ─────────────────────────────────────────────────────────────────
    "sport"             -> Icons.AutoMirrored.Filled.DirectionsRun
    "sport_field"       -> Icons.Default.ElectricBolt
    // ── Technik ───────────────────────────────────────────────────────────────
    "tech"              -> Icons.Default.Memory
    "smart_city"        -> Icons.Default.Sensors
    "construction"      -> Icons.Default.Construction
    // ── Stadtmöbel ───────────────────────────────────────────────────────────
    "bench"             -> Icons.Default.AirlineSeatReclineNormal
    "stairs"            -> Icons.Default.Stairs
    "manhole"           -> Icons.Default.Visibility
    "mailbox"           -> Icons.Default.MarkunreadMailbox
    "clock"             -> Icons.Default.Schedule
    "lamp"              -> Icons.Default.LightbulbCircle
    // ── Menschen ─────────────────────────────────────────────────────────────
    "people"            -> Icons.Default.Groups
    "person_accessory"  -> Icons.Default.ChildCare
    "musician"          -> Icons.Default.LibraryMusic
    "funny_pose"        -> Icons.Default.AccessibilityNew
    "tourist"           -> Icons.Default.CameraAlt
    // ── Shops ─────────────────────────────────────────────────────────────────
    "shop"              -> Icons.Default.BakeryDining
    "market"            -> Icons.Default.ShoppingBasket
    // ── Farbe & Form ─────────────────────────────────────────────────────────
    "color_object"      -> Icons.Default.Palette
    "colorful_scene"    -> Icons.Default.AutoAwesome
    "round"             -> Icons.Default.ChangeCircle
    "triangle"          -> Icons.Default.Roofing
    // ── Kultur ────────────────────────────────────────────────────────────────
    "music"             -> Icons.Default.Mic
    "culture"           -> Icons.AutoMirrored.Filled.Article
    // ── Kurioses ─────────────────────────────────────────────────────────────
    "weird"             -> Icons.Default.QuestionMark
    "miniature"         -> Icons.Default.ZoomIn
    "symmetry"          -> Icons.Default.Flip
    // ── Legacy IDs ────────────────────────────────────────────────────────────
    "flower"            -> Icons.Default.LocalFlorist
    "coffee"            -> Icons.Default.LocalCafe
    "fountain"          -> Icons.Default.Waves
    "bridge"            -> Icons.Default.Landscape
    "letterbox"         -> Icons.Default.MarkunreadMailbox
    "sunset"            -> Icons.Default.WbTwilight
    "people_group"      -> Icons.Default.Groups
    "traffic"           -> Icons.Default.Traffic
    "plant"             -> Icons.Default.Eco
    "yellow"            -> Icons.Default.Circle
    "round_obj"         -> Icons.Default.Circle
    else                -> Icons.Default.PhotoCamera
}

/** Returns rotation in degrees to apply when rendering this category's icon. */
fun getCategoryIconRotation(categoryId: String): Float = when (categoryId) {
    "funny_pose" -> 180f
    else -> 0f
}
