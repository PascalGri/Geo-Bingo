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
    // ── Weird Core ───────────────────────────────────────────────────────────
    "wc_phone"          -> Icons.Default.PhoneAndroid
    "wc_sign"           -> Icons.Default.Signpost
    "wc_sign_no"        -> Icons.Default.DoNotDisturb
    "wc_shoe"           -> Icons.AutoMirrored.Filled.DirectionsWalk
    "wc_waiting"        -> Icons.Default.HourglassTop
    "wc_chair"          -> Icons.Default.Chair
    "wc_mirror"         -> Icons.Default.Flip
    "wc_pigeon"         -> Icons.Default.Pets
    "wc_selfie"         -> Icons.Default.PhotoCamera
    "wc_stickers"       -> @Suppress("DEPRECATION") Icons.Default.Label
    "wc_floor"          -> Icons.Default.Person
    "wc_boring"         -> Icons.Default.Apartment
    "wc_bag"            -> Icons.Default.ShoppingBag
    "wc_door"           -> Icons.Default.Yard
    "wc_food"           -> Icons.Default.RestaurantMenu
    "wc_triangle"       -> Icons.Default.ChangeHistory
    "wc_matching"       -> Icons.Default.Style
    "wc_scale"          -> Icons.Default.ZoomIn
    "wc_concrete"       -> Icons.Default.LocalFlorist
    "wc_many"           -> Icons.Default.GridView
    "wc_npc"            -> Icons.Default.SmartToy
    "wc_wrong"          -> Icons.Default.MoodBad
    "wc_queue"          -> Icons.Default.Groups
    "wc_cable"          -> Icons.Default.Cable
    "wc_umbrella"       -> Icons.Default.Umbrella
    "wc_twins"          -> Icons.Default.PeopleAlt
    "wc_cat"            -> Icons.Default.Pets
    "wc_postit"         -> @Suppress("DEPRECATION") Icons.Default.StickyNote2
    "wc_cloud"          -> Icons.Default.Cloud
    "wc_shadow_art"     -> Icons.Default.Tonality
    "wc_expired"        -> Icons.Default.EventBusy
    "wc_locked_bike"    -> Icons.Default.Lock
    "wc_charging"       -> Icons.Default.BatteryChargingFull
    "wc_glove"          -> Icons.Default.BackHand
    "wc_graffiti_name"  -> Icons.Default.TextFields
    "wc_upsidedown"     -> @Suppress("DEPRECATION") Icons.Default.RotateLeft
    "wc_plant_fight"    -> Icons.Default.LocalFlorist
    "wc_award"          -> Icons.Default.EmojiEvents
    "wc_door_steps"     -> Icons.Default.DoorFront
    "wc_sleeping"       -> Icons.Default.Hotel
    "wc_fakebrand"      -> Icons.Default.ContentCopy
    "wc_twins_car"      -> Icons.Default.DirectionsCar
    "wc_dog_twin"       -> Icons.Default.Pets
    "wc_no_entry"       -> Icons.Default.NoTransfer
    // ── Weird Core – Runde 3 (outdoor) ─────────────────────────────────────────
    "wc_chair_stack"    -> Icons.Default.Chair
    "wc_construction"   -> Icons.Default.Construction
    "wc_pet_clothes"    -> Icons.Default.Pets
    "wc_parallel"       -> Icons.Default.DirectionsCar
    "wc_balloon"        -> Icons.Default.Circle
    "wc_nothing_door"   -> Icons.Default.DoorFront
    "wc_lonely_table"   -> Icons.Default.RestaurantMenu
    "wc_lock_lock"      -> Icons.Default.Lock
    // ── Weird Core – Runde 4 (outdoor) ─────────────────────────────────────────
    "wc_puddle"         -> Icons.Default.Waves
    "wc_traffic_cone"   -> Icons.Default.Traffic
    "wc_overflow_bin"   -> Icons.Default.Delete
    "wc_gum_art"        -> Icons.Default.Circle
    "wc_face_object"    -> Icons.Default.EmojiEmotions
    "wc_shopping_cart"  -> Icons.Default.ShoppingCart
    "wc_one_window_light" -> Icons.Default.LightbulbCircle
    "wc_weird_bollard"  -> Icons.Default.NearMe
    "wc_taped_thing"    -> Icons.Default.Healing
    "wc_plant_takeover" -> Icons.Default.Eco
    "wc_lost_key"       -> Icons.Default.VpnKey
    "wc_laundry_mix"    -> Icons.Default.Checkroom
    "wc_crooked_sign"   -> Icons.Default.Signpost
    "wc_animal_statue"  -> Icons.Default.Pets
    // ── Weird Core – indoor (wci_*) ────────────────────────────────────────────
    "wci_socks"         -> Icons.Default.Checkroom
    "wci_remote"        -> Icons.Default.SettingsRemote
    "wci_books"         -> Icons.Default.Book
    "wci_cables"        -> Icons.Default.Cable
    "wci_fridge"        -> Icons.Default.Kitchen
    "wci_dying_plant"   -> Icons.Default.LocalFlorist
    "wci_old_device"    -> Icons.Default.Memory
    "wci_wrong_room"    -> Icons.Default.MeetingRoom
    "wci_dust"          -> Icons.Default.Cloud
    "wci_drawer"        -> Icons.Default.Inbox
    "wci_mugs"          -> Icons.Default.LocalCafe
    "wci_smudged_mirror" -> Icons.Default.Flip
    "wci_three_remotes" -> Icons.Default.SettingsRemote
    "wci_kitchen_gadget" -> Icons.Default.Kitchen
    "wci_cushion"       -> Icons.Default.Weekend
    "wci_burnt"         -> Icons.Default.Whatshot
    "wci_box"           -> Icons.Default.Inventory2
    "wci_old_calendar"  -> Icons.Default.EventBusy
    "wci_no_handle"     -> Icons.Default.DoorFront
    "wci_pen"           -> Icons.Default.Edit
    "wci_phone_down"    -> Icons.Default.PhoneAndroid
    "wci_three_chargers" -> Icons.Default.BatteryChargingFull
    // ── Weird Core – Runde 2 indoor (wci_*) ────────────────────────────────────
    "wci_dryer_sock"    -> Icons.Default.Checkroom
    "wci_tv_cables"     -> Icons.Default.Cable
    "wci_fridge_empty"  -> Icons.Default.Kitchen
    "wci_charger_empty" -> Icons.Default.BatteryChargingFull
    "wci_spare_screw"   -> Icons.Default.Build
    "wci_overloaded_hook" -> Icons.Default.BackHand
    "wci_expired_food"  -> Icons.Default.EventBusy
    "wci_tangled_earphones" -> Icons.Default.Headphones
    "wci_dead_batteries" -> Icons.Default.BatteryAlert
    "wci_glove_indoor"  -> Icons.Default.BackHand
    "wci_pillow_fort"   -> Icons.Default.Weekend
    "wci_mystery_stain" -> Icons.Default.MoodBad
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
