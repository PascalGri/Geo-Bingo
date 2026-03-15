package pg.geobingo.one.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

fun getCategoryIcon(categoryId: String): ImageVector = when (categoryId) {
    // ── Kleidung ──────────────────────────────────────────────────────────────
    "pants"             -> Icons.Default.Style
    "jacket"            -> Icons.Default.Checkroom
    "hat"               -> Icons.Default.Checkroom
    "sneaker"           -> Icons.Default.DirectionsWalk
    // ── Fahrzeuge ─────────────────────────────────────────────────────────────
    "car"               -> Icons.Default.DirectionsCar
    "vehicle"           -> Icons.Default.DirectionsBus
    "bicycle"           -> Icons.Default.DirectionsBike
    "unusual_vehicle"   -> Icons.Default.Sailing
    // ── Tiere ─────────────────────────────────────────────────────────────────
    "animal"            -> Icons.Default.Pets
    "dog_breed"         -> Icons.Default.Pets
    "bird"              -> Icons.Default.Flight
    // ── Natur ─────────────────────────────────────────────────────────────────
    "nature"            -> Icons.Default.Eco
    "tree"              -> Icons.Default.Park
    "water"             -> Icons.Default.Waves
    "sky"               -> Icons.Default.WbCloudy
    // ── Essen & Trinken ───────────────────────────────────────────────────────
    "food"              -> Icons.Default.LocalCafe
    "food_truck"        -> Icons.Default.LocalShipping
    "cafe"              -> Icons.Default.LocalCafe
    "street_food"       -> Icons.Default.Restaurant
    // ── Architektur ───────────────────────────────────────────────────────────
    "building"          -> Icons.Default.AccountBalance
    "door"              -> Icons.Default.MeetingRoom
    "window"            -> Icons.Default.Window
    "balcony"           -> Icons.Default.Deck
    "arch"              -> Icons.Default.Layers
    // ── Straßenkunst ─────────────────────────────────────────────────────────
    "street_art"        -> Icons.Default.Brush
    "mural"             -> Icons.Default.Brush
    "sculpture"         -> Icons.Default.Architecture
    "pattern"           -> Icons.Default.GridView
    // ── Schilder ─────────────────────────────────────────────────────────────
    "sign"              -> Icons.Default.Info
    "funny_sign"        -> Icons.Default.EmojiEmotions
    "number"            -> Icons.Default.Tag
    // ── Reflexionen & Licht ───────────────────────────────────────────────────
    "shadow"            -> Icons.Default.Contrast
    "reflection"        -> Icons.Default.Flip
    "light"             -> Icons.Default.WbSunny
    // ── Sport ─────────────────────────────────────────────────────────────────
    "sport"             -> Icons.Default.FitnessCenter
    "sport_field"       -> Icons.Default.SportsSoccer
    "yoga_meditation"   -> Icons.Default.SelfImprovement
    // ── Technik ───────────────────────────────────────────────────────────────
    "tech"              -> Icons.Default.EvStation
    "smart_city"        -> Icons.Default.Router
    "construction"      -> Icons.Default.Construction
    // ── Stadtmöbel ───────────────────────────────────────────────────────────
    "bench"             -> Icons.Default.Weekend
    "stairs"            -> Icons.Default.Stairs
    "manhole"           -> Icons.Default.RadioButtonChecked
    "mailbox"           -> Icons.Default.Mail
    "clock"             -> Icons.Default.Schedule
    "lamp"              -> Icons.Default.Lightbulb
    // ── Menschen ─────────────────────────────────────────────────────────────
    "people"            -> Icons.Default.Group
    "person_accessory"  -> Icons.Default.Person
    "musician"          -> Icons.Default.MusicNote
    "funny_pose"        -> Icons.Default.EmojiPeople
    "tourist"           -> Icons.Default.CameraAlt
    // ── Shops ─────────────────────────────────────────────────────────────────
    "shop"              -> Icons.Default.Store
    "market"            -> Icons.Default.ShoppingCart
    "vintage"           -> Icons.Default.Storefront
    // ── Farbe & Form ─────────────────────────────────────────────────────────
    "color_object"      -> Icons.Default.Palette
    "colorful_scene"    -> Icons.Default.Palette
    "round"             -> Icons.Default.Circle
    "triangle"          -> Icons.Default.ChangeHistory
    // ── Kultur ────────────────────────────────────────────────────────────────
    "music"             -> Icons.Default.MusicNote
    "culture"           -> Icons.Default.TheaterComedy
    // ── Kurioses ─────────────────────────────────────────────────────────────
    "weird"             -> Icons.Default.QuestionMark
    "miniature"         -> Icons.Default.Search
    "symmetry"          -> Icons.Default.Compress
    // ── Legacy IDs ────────────────────────────────────────────────────────────
    "flower"            -> Icons.Default.LocalFlorist
    "coffee"            -> Icons.Default.LocalCafe
    "fountain"          -> Icons.Default.Waves
    "bridge"            -> Icons.Default.Landscape
    "letterbox"         -> Icons.Default.Mail
    "sunset"            -> Icons.Default.WbSunny
    "people_group"      -> Icons.Default.Group
    "traffic"           -> Icons.Default.Traffic
    "plant"             -> Icons.Default.Eco
    "yellow"            -> Icons.Default.Star
    "round_obj"         -> Icons.Default.Circle
    else                -> Icons.Default.PhotoCamera
}
