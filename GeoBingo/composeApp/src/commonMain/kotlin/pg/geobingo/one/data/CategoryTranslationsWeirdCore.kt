package pg.geobingo.one.data

// ─────────────────────────────────────────────────────────────────────────────
//  English translations for the Weird Core pools (outdoor + indoor). Keyed by the
//  same id as the German source in WeirdCoreCategories.kt (the first Category arg,
//  NOT the icon_id). Applied via Category.localized() when S.language == EN;
//  missing keys fall back to German.
// ─────────────────────────────────────────────────────────────────────────────

val WEIRD_CORE_CATEGORIES_EN: Map<String, CategoryEn> = mapOf(
    // ── Outdoor ──────────────────────────────────────────────────────────────
    "wc_phone" to ("Phone zombie" to
        "Photograph someone staring at their phone – from the front or the side."),
    "wc_sign" to ("Unreadable sign" to
        "A sign whose text you can't read – faded, obscured or in a foreign language."),
    "wc_shoe" to ("Abandoned shoe" to
        "A single shoe all on its own – no partner in sight."),
    "wc_waiting" to ("Someone waiting" to
        "A person obviously waiting for something – checking the time, annoyed, or just standing there."),
    "wc_chair" to ("Chair in the street" to
        "A chair (or armchair) just standing outside where it really doesn't belong."),
    "wc_mirror" to ("Unexpected reflection" to
        "Your reflection in a surface that isn't a mirror – a shop window, puddle, car roof, ..."),
    "wc_pigeon" to ("Pigeon having a snack" to
        "A pigeon or other city bird eating something specific – crumbs, a kebab, whatever."),
    "wc_selfie_in_selfie" to ("Selfie in a selfie" to
        "Photograph someone else who is busy taking a selfie themselves."),
    "wc_stickers" to ("Sticker chaos" to
        "At least 3 different stickers plastered over or next to each other."),
    "wc_floor_person" to ("Person sitting on the ground" to
        "Someone sitting directly on the ground – not on a bench, not on steps, just on the ground."),
    "wc_boring" to ("Most boring building" to
        "The dullest, greyest, most boring building you can find."),
    "wc_bag" to ("Forgotten bag" to
        "A shopping bag or tote standing or lying somewhere all alone."),
    "wc_door" to ("Ugliest door handle" to
        "The ugliest, rustiest or weirdest door handle you can find."),
    "wc_food_floor" to ("Food on the ground" to
        "Food or leftovers lying on the ground – fries, ice cream, crumbs, ..."),
    "wc_obvious_sign" to ("Pointless warning sign" to
        "A sign that forbids or warns about something totally obvious."),
    "wc_triangle" to ("Accidental triangle" to
        "Any three objects or lines that together form a triangle shape – not built as a triangle."),
    "wc_matching" to ("Outfit matches the surroundings" to
        "A person whose clothes happen to match their surroundings perfectly – colour, pattern, style."),
    "wc_small_big" to ("Tiny next to huge" to
        "A very small object right next to a very large one – the size difference must be striking."),
    "wc_flower_concrete" to ("Flower from concrete" to
        "A flower or plant growing out of concrete, asphalt or a crack in a wall."),
    "wc_too_many" to ("Too many of them" to
        "Photograph an absurdly large number of the same object – bikes, bags, chairs, ..."),
    "wc_npc" to ("NPC spotted" to
        "A person moving or standing as if they were an NPC in a video game."),
    "wc_wrong_place" to ("Wrong thing, wrong place" to
        "An object that makes absolutely no sense in this spot – completely out of place."),
    "wc_queue" to ("Pointless queue" to
        "Several people standing in a line – the stranger the reason, the better."),
    "wc_cable" to ("Cable tangle" to
        "The most chaotic tangle of cables, wires or leads you can find."),
    "wc_broken_umbrella" to ("Broken umbrella skeleton" to
        "An umbrella turned inside out or with its ribs sticking out."),
    "wc_matching_twins" to ("Accidental twins" to
        "Two strangers who happen to be dressed almost identically."),
    "wc_cat_window" to ("Cat in a window" to
        "A cat looking out of a window – the more bored the stare, the better."),
    "wc_post_it" to ("Note in a strange spot" to
        "A note or Post-it in a place that makes no real sense."),
    "wc_cloud_shape" to ("Cloud that looks like something" to
        "A cloud that clearly has the shape of something specific – and is photographable."),
    "wc_shadow_art" to ("Shadow that lies" to
        "A shadow with a completely different shape from the object casting it."),
    "wc_expired" to ("Past its expiry date" to
        "A product, sign or notice whose date is long gone."),
    "wc_locked_bike" to ("Absurdly secured bike" to
        "A bike locked up with way too many locks, or to a totally wrong object."),
    "wc_phone_charger" to ("Phone charging in public" to
        "Someone charging their phone at a public socket – café, train, street, ..."),
    "wc_single_glove" to ("Single glove" to
        "A single glove lying or hanging somewhere on its own."),
    "wc_graffiti_name" to ("Graffiti first name" to
        "Graffiti that just spells a first name – no tag, no artwork, just e.g. 'KEVIN'."),
    "wc_upside_down" to ("Upside down" to
        "Any object clearly standing, hanging or lying the wrong way up."),
    "wc_plant_indoor" to ("Plant that's struggling" to
        "An outdoor houseplant or wild plant that looks like it's fighting for survival."),
    "wc_random_award" to ("Trophy somewhere random" to
        "A trophy, medal or cup randomly standing somewhere in a public space."),
    "wc_door_no_steps" to ("Door without stairs" to
        "A door sitting 1+ metres up in a wall with no steps or ramp."),
    "wc_someone_sleeping" to ("Someone sleeping" to
        "A person sleeping in public – bench, bus, ground – not in a bed."),
    "wc_brand_copy" to ("Fake logo or typo brand" to
        "A logo or brand name that is almost right – but only almost."),
    "wc_identical_cars" to ("Identical cars side by side" to
        "At least two identical cars (make + colour) parked right next to each other."),
    "wc_dog_owner_twin" to ("Dog looks like its owner" to
        "A dog and its owner so similar-looking that it's just funny."),
    "wc_no_entry_entered" to ("Forbidden but entered anyway" to
        "A 'no entry' sign that someone has obviously been or is behind."),
    "wc_one_chair_pile" to ("Chair on chair" to
        "At least three chairs stacked on top of each other outdoors."),
    "wc_unfinished" to ("Half-finished site, no workers" to
        "A construction site obviously on pause – tools there, nobody working."),
    "wc_pet_costume" to ("Animal wearing clothes" to
        "A dog, cat or animal wearing a jumper, coat, shoes or anything else."),
    "wc_perfect_parallel" to ("Perfectly parallel parked" to
        "At least three vehicles in absurdly perfect parallel alignment."),
    "wc_sad_balloon" to ("Sad balloon" to
        "A half-deflated or lost balloon drooping sadly somewhere."),
    "wc_door_to_nowhere" to ("Door to nowhere" to
        "A door that obviously leads nowhere – on a wall, in a forest, on an empty scaffold."),
    "wc_lonely_table" to ("Abandoned table" to
        "A table (café, park, street) completely alone – no chair, no guests, no point."),
    "wc_too_many_locks" to ("Lock upon lock" to
        "An object with several locks on it – a railing, door, outdoor cabinet, love-lock bridge."),
    "wc_puddle" to ("Puddle as a mirror" to
        "A puddle reflecting the sky, a building or yourself."),
    "wc_traffic_cone" to ("Lonely traffic cone" to
        "A traffic cone standing pointlessly somewhere all alone."),
    "wc_overflow_bin" to ("Overflowing bin" to
        "A public bin so full that everything is spilling out of the top."),
    "wc_gum_art" to ("Chewing-gum pattern on the ground" to
        "Trodden-in chewing gum on the pavement that happens to form a pattern."),
    "wc_face_object" to ("Face in an object" to
        "An object that happens to look like a face – a socket, car, house wall (pareidolia)."),
    "wc_shopping_cart" to ("Stray shopping trolley" to
        "A shopping trolley far from any supermarket – in a river, park or bushes."),
    "wc_one_window_light" to ("One window glowing" to
        "A dark building front where only a single window is lit."),
    "wc_weird_bollard" to ("Quirky bollard" to
        "A bollard or post that's painted, plastered with stickers or completely dented."),
    "wc_taped_thing" to ("Repaired with tape" to
        "Anything in public that's been patched up with tape as a quick fix."),
    "wc_plant_takeover" to ("Nature taking back over" to
        "Plants overgrowing a human-made object – a car, sign, bench or fence."),
    "wc_lost_key" to ("Lost key" to
        "A single key or keyring that someone has left lying or hanging somewhere."),
    "wc_laundry_mix" to ("Absurd washing line" to
        "An outdoor washing line with an absurd combination of laundry."),
    "wc_crooked_sign" to ("Crooked sign" to
        "A traffic or street sign that's completely crooked or bent."),
    "wc_animal_statue" to ("Animal statue taken too seriously" to
        "An animal statue or figure someone treats like a real animal – or that's just absurdly placed."),

    // ── Indoor ───────────────────────────────────────────────────────────────
    "wci_socks" to ("Abandoned sock" to
        "A single sock lying around on its own – not in the laundry basket."),
    "wci_remote" to ("Remote in a strange place" to
        "A remote control somewhere it definitely doesn't belong."),
    "wci_unread_books" to ("Books that were never read" to
        "Books obviously just for show – dusty, pristine, or shelved backwards."),
    "wci_charger_chaos" to ("Charging-cable snake" to
        "At least 3 different charging cables tangled together."),
    "wci_fridge_photo" to ("Photo on the fridge" to
        "A photo or magnet picture on a fridge – the more embarrassing, the better."),
    "wci_plant_dying" to ("Plant slowly dying" to
        "A houseplant that clearly needs help – brown leaves, no water, sad."),
    "wci_appliance_old" to ("Device from the last millennium" to
        "An electronic device at home obviously older than the person using it."),
    "wci_wrong_room" to ("Object in the wrong room" to
        "Something kitchen-ish in the bathroom, bathroom stuff in the bedroom, etc. – totally out of place."),
    "wci_dust_bunny" to ("Dust monster" to
        "A visible clump of dust that's almost taken on a shape of its own."),
    "wci_overstuffed_drawer" to ("Drawer full of chaos" to
        "A drawer you open – inside, anarchy reigns."),
    "wci_mug_collection" to ("Mug collection" to
        "At least 4 very different mugs standing or hanging together."),
    "wci_mirror_dust" to ("Mirror that tells lies" to
        "A mirror with fingerprints, smears or dust – no clear reflection."),
    "wci_three_remotes" to ("Three remote controls" to
        "Three or more remotes lying side by side or in a jumble."),
    "wci_unused_kitchen" to ("Kitchen gadget nobody uses" to
        "A food processor, gadget or attachment that obviously just gathers dust."),
    "wci_decorative_cushion" to ("Pointless decorative cushion" to
        "A cushion that's purely decorative – not for sitting, not for sleeping."),
    "wci_burnt_pan" to ("Burnt pan" to
        "A pan or pot with burn marks – backstory optional."),
    "wci_random_box" to ("Box just standing around" to
        "A box that's been sitting in the room for what feels like weeks – contents unknown."),
    "wci_calendar_old" to ("Old calendar" to
        "A wall calendar that's clearly out of date."),
    "wci_no_handle" to ("Door without a handle" to
        "An indoor door with no handle – a cupboard, secret door, or just broken."),
    "wci_lost_pen" to ("Pen nobody misses" to
        "A single pen lying around, apparently unused for years."),
    "wci_phone_face_down" to ("Phone face down" to
        "A phone on some surface, screen facing down – trust or mistrust?"),
    "wci_three_chargers" to ("Three chargers in one room" to
        "At least three chargers in the same room – plugged in or lying around."),
    "wci_dryer_sock" to ("Dryer victim" to
        "A single sock after doing laundry – its partner gone forever."),
    "wci_tv_cables" to ("Cable mess behind the TV" to
        "The cable chaos behind a TV, desk or router."),
    "wci_fridge_empty" to ("Almost empty fridge" to
        "A fridge with almost nothing left – just one lonely item."),
    "wci_charger_empty" to ("Charger with no device" to
        "A charging cable that's plugged in but connected to nothing."),
    "wci_spare_screw" to ("Leftover screw" to
        "A single screw or part left over after assembling furniture."),
    "wci_overloaded_hook" to ("Overloaded hook" to
        "A coat or door hook with far too much hanging on it."),
    "wci_expired_food" to ("Expired in the cupboard" to
        "A food item long past its date – but still sitting in the cupboard."),
    "wci_tangled_earphones" to ("Tangled earphones" to
        "A pair of wired earphones knotted into a perfect tangle."),
    "wci_dead_batteries" to ("Pile of dead batteries" to
        "Several old batteries lying around because nobody takes them away."),
    "wci_glove_indoor" to ("Single glove indoors" to
        "A single glove lying indoors – winter, cleaning or gardening glove."),
    "wci_pillow_fort" to ("Pillow fort" to
        "A bed or sofa with an absurd number of pillows on it."),
    "wci_mystery_stain" to ("Stain of unknown origin" to
        "A stain on a wall, ceiling or floor that nobody can explain."),
)
