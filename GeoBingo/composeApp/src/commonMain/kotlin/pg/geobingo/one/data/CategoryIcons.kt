package pg.geobingo.one.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

fun getCategoryIcon(categoryId: String): ImageVector = when (categoryId) {
    // ── Kleidung ──────────────────────────────────────────────────────────────
    "pants"             -> Icons.Default.Checkroom          // Kleiderschrank / Garderobe
    "jacket"            -> Icons.Default.DryCleaning        // Kleidungsstück an Bügel
    "hat"               -> Icons.Default.Face               // Gesicht / Kopf
    "sneaker"           -> Icons.Default.Hiking             // Schuh / Boot
    // ── Fahrzeuge ─────────────────────────────────────────────────────────────
    "car"               -> Icons.Default.DirectionsCar
    "vehicle"           -> Icons.Default.DirectionsBus
    "bicycle"           -> Icons.Default.DirectionsBike
    "unusual_vehicle"   -> Icons.Default.ElectricScooter    // E-Scooter / ungewöhnliches Fahrzeug
    // ── Tiere ─────────────────────────────────────────────────────────────────
    "animal"            -> Icons.Default.Pets               // Pfote
    "dog_breed"         -> Icons.Default.Cruelty_Free       // Hunde-/Tierpfote (alternativ zu Pets)
    "bird"              -> Icons.Default.Flight             // Flügel / Fliegen
    // ── Natur ─────────────────────────────────────────────────────────────────
    "nature"            -> Icons.Default.Eco                // Blatt / Natur
    "tree"              -> Icons.Default.Park               // Baum / Park
    "water"             -> Icons.Default.Waves
    "sky"               -> Icons.Default.WbCloudy
    // ── Essen & Trinken ───────────────────────────────────────────────────────
    "food"              -> Icons.Default.Fastfood           // Burger / Fastfood
    "food_truck"        -> Icons.Default.DeliveryDining     // Lieferfahrzeug mit Essen
    "cafe"              -> Icons.Default.LocalCafe
    "street_food"       -> Icons.Default.OutdoorGrill       // Grill / Straßenessen
    // ── Architektur ───────────────────────────────────────────────────────────
    "building"          -> Icons.Default.AccountBalance     // Säulengebäude
    "door"              -> Icons.Default.DoorFront          // Haustür
    "window"            -> Icons.Default.Window
    "balcony"           -> Icons.Default.Balcony            // Balkon
    "arch"              -> Icons.Default.Architecture       // Architekturbogen
    // ── Straßenkunst ─────────────────────────────────────────────────────────
    "street_art"        -> Icons.Default.FormatPaint        // Farbroller / Graffiti
    "mural"             -> Icons.Default.Brush              // Pinsel / Wandbild
    "sculpture"         -> Icons.Default.Interests          // Formen / Skulptur
    "pattern"           -> Icons.Default.Pattern            // Muster
    // ── Schilder ─────────────────────────────────────────────────────────────
    "sign"              -> Icons.Default.Signpost           // Wegweiser / Schild
    "funny_sign"        -> Icons.Default.EmojiEmotions      // Lachendes Gesicht / witziges Schild
    "number"            -> Icons.Default.Tag
    // ── Reflexionen & Licht ───────────────────────────────────────────────────
    "shadow"            -> Icons.Default.Contrast
    "reflection"        -> Icons.Default.Flip               // Spiegelung
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
    "bench"             -> Icons.Default.Chair              // Stuhl / Bank
    "stairs"            -> Icons.Default.Stairs
    "manhole"           -> Icons.Default.Lens               // Kreisform / Kanaldeckel
    "mailbox"           -> Icons.Default.MarkunreadMailbox  // Briefkasten
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
    "market"            -> Icons.Default.ShoppingBasket     // Einkaufskorb / Markt
    "vintage"           -> Icons.Default.Storefront
    // ── Farbe & Form ─────────────────────────────────────────────────────────
    "color_object"      -> Icons.Default.Palette            // Farbpalette
    "colorful_scene"    -> Icons.Default.AutoAwesome        // Funkeln / bunt
    "round"             -> Icons.Default.Circle
    "triangle"          -> Icons.Default.ChangeHistory
    // ── Kultur ────────────────────────────────────────────────────────────────
    "music"             -> Icons.Default.MusicNote
    "culture"           -> Icons.Default.TheaterComedy
    // ── Kurioses ─────────────────────────────────────────────────────────────
    "weird"             -> Icons.Default.QuestionMark
    "miniature"         -> Icons.Default.ZoomIn             // Vergrößerung / klein
    "symmetry"          -> Icons.Default.Flip               // Spiegelung / Symmetrie
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
