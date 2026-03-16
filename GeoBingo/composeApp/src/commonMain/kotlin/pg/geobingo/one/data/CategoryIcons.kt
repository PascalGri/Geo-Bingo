package pg.geobingo.one.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

fun getCategoryIcon(categoryId: String): ImageVector = when (categoryId) {
    // ── Kleidung ──────────────────────────────────────────────────────────────
    "pants"             -> Icons.Default.Style              // Schere/Mode
    "jacket"            -> Icons.Default.Checkroom          // Garderobe/Jacke
    "hat"               -> Icons.Default.EmojiPeople        // Person mit Kopfbedeckung
    "sneaker"           -> Icons.Default.DirectionsWalk     // Gehender Mensch / Schuhe
    // ── Fahrzeuge ─────────────────────────────────────────────────────────────
    "car"               -> Icons.Default.DirectionsCar
    "vehicle"           -> Icons.Default.DirectionsBus
    "bicycle"           -> Icons.Default.DirectionsBike
    "unusual_vehicle"   -> Icons.Default.TwoWheeler         // Motorrad/ungewöhnlich
    // ── Tiere ─────────────────────────────────────────────────────────────────
    "animal"            -> Icons.Default.Pets               // Pfote
    "dog_breed"         -> Icons.Default.Pets
    "bird"              -> Icons.Default.Flight             // Flügel/Fliegen
    // ── Natur ─────────────────────────────────────────────────────────────────
    "nature"            -> Icons.Default.Yard               // Blatt/Pflanze
    "tree"              -> Icons.Default.Park               // Baum
    "water"             -> Icons.Default.Waves
    "sky"               -> Icons.Default.WbCloudy
    // ── Essen & Trinken ───────────────────────────────────────────────────────
    "food"              -> Icons.Default.Icecream           // Eis / Straßenessen
    "food_truck"        -> Icons.Default.LocalShipping      // Lieferwagen
    "cafe"              -> Icons.Default.LocalCafe
    "street_food"       -> Icons.Default.Restaurant
    // ── Architektur ───────────────────────────────────────────────────────────
    "building"          -> Icons.Default.AccountBalance     // Säulengebäude
    "door"              -> Icons.Default.DoorFront          // Haustür
    "window"            -> Icons.Default.Window
    "balcony"           -> Icons.Default.Deck
    "arch"              -> Icons.Default.Architecture       // Architekturbogen
    // ── Straßenkunst ─────────────────────────────────────────────────────────
    "street_art"        -> Icons.Default.FormatPaint        // Farbroller / Graffiti
    "mural"             -> Icons.Default.Brush              // Pinsel / Wandbild
    "sculpture"         -> Icons.Default.Interests          // Formen / Skulptur
    "pattern"           -> Icons.Default.GridView
    // ── Schilder ─────────────────────────────────────────────────────────────
    "sign"              -> Icons.Default.Info
    "funny_sign"        -> Icons.Default.Announcement       // Lautsprecher / witziges Schild
    "number"            -> Icons.Default.Tag
    // ── Reflexionen & Licht ───────────────────────────────────────────────────
    "shadow"            -> Icons.Default.Contrast
    "reflection"        -> Icons.Default.Flip
    "light"             -> Icons.Default.Flare              // Lichtreflex / Sonnenstrahlen
    // ── Sport ─────────────────────────────────────────────────────────────────
    "sport"             -> Icons.Default.DirectionsRun      // Laufender Mensch
    "sport_field"       -> Icons.Default.SportsSoccer
    "yoga_meditation"   -> Icons.Default.SelfImprovement
    // ── Technik ───────────────────────────────────────────────────────────────
    "tech"              -> Icons.Default.Memory             // Chip / Schaltkreis
    "smart_city"        -> Icons.Default.Sensors            // Sensorsignal
    "construction"      -> Icons.Default.Construction
    // ── Stadtmöbel ───────────────────────────────────────────────────────────
    "bench"             -> Icons.Default.Weekend            // Sofa / Bank
    "stairs"            -> Icons.Default.Stairs
    "manhole"           -> Icons.Default.Settings           // Rundes Metallgitter
    "mailbox"           -> Icons.Default.Mail
    "clock"             -> Icons.Default.Schedule
    "lamp"              -> Icons.Default.Lightbulb
    // ── Menschen ─────────────────────────────────────────────────────────────
    "people"            -> Icons.Default.Groups             // Personengruppe
    "person_accessory"  -> Icons.Default.Headset            // Kopfhörer / Accessoire
    "musician"          -> Icons.Default.Piano              // Klavier / Musiker
    "funny_pose"        -> Icons.Default.SentimentVerySatisfied // Lachgesicht / witzige Pose
    "tourist"           -> Icons.Default.CameraAlt
    // ── Shops ─────────────────────────────────────────────────────────────────
    "shop"              -> Icons.Default.Store
    "market"            -> Icons.Default.ShoppingCart
    "vintage"           -> Icons.Default.Storefront
    // ── Farbe & Form ─────────────────────────────────────────────────────────
    "color_object"      -> Icons.Default.ColorLens          // Farblinse
    "colorful_scene"    -> Icons.Default.AutoAwesome        // Funkeln / bunt
    "round"             -> Icons.Default.Circle
    "triangle"          -> Icons.Default.ChangeHistory
    // ── Kultur ────────────────────────────────────────────────────────────────
    "music"             -> Icons.Default.MusicNote
    "culture"           -> Icons.Default.TheaterComedy
    // ── Kurioses ─────────────────────────────────────────────────────────────
    "weird"             -> Icons.Default.QuestionMark
    "miniature"         -> Icons.Default.Search
    "symmetry"          -> Icons.Default.Balance            // Waage / Symmetrie
    // ── Legacy IDs ────────────────────────────────────────────────────────────
    "flower"            -> Icons.Default.LocalFlorist
    "coffee"            -> Icons.Default.LocalCafe
    "fountain"          -> Icons.Default.Waves
    "bridge"            -> Icons.Default.Landscape
    "letterbox"         -> Icons.Default.Markunread
    "sunset"            -> Icons.Default.WbTwilight
    "people_group"      -> Icons.Default.Groups
    "traffic"           -> Icons.Default.Traffic
    "plant"             -> Icons.Default.LocalFlorist
    "yellow"            -> Icons.Default.Circle
    "round_obj"         -> Icons.Default.Circle
    else                -> Icons.Default.PhotoCamera
}
