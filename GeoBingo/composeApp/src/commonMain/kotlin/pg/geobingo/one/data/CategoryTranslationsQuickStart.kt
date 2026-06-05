package pg.geobingo.one.data

// ─────────────────────────────────────────────────────────────────────────────
//  English translations for the Quick Start pool. Keyed by the same id as the
//  German source in QuickStartCategories.kt. Applied via Category.localized()
//  when S.language == EN; missing keys fall back to German.
// ─────────────────────────────────────────────────────────────────────────────

val QUICK_START_CATEGORIES_EN: Map<String, CategoryEn> = mapOf(
    // Outdoor
    "qs_out_red" to ("Red object" to
        "Photograph anything red outdoors – a car, door, sign or piece of clothing."),
    "qs_out_bike" to ("Parked bike" to
        "Find a parked bicycle and photograph it."),
    "qs_out_animal" to ("Animal" to
        "Dog, cat, pigeon, squirrel – any animal counts."),
    "qs_out_shadow" to ("Your own shadow" to
        "Photograph your shadow on the ground or against a wall."),
    "qs_out_tree" to ("Tree or shrub" to
        "Photograph a tree or shrub – with leaves, blossoms or bare."),
    "qs_out_sign" to ("Street or info sign" to
        "Any sign outdoors – a street name, notice or prohibition."),
    "qs_out_puddle" to ("Puddle or water" to
        "Find a puddle, a fountain or some other body of water."),
    "qs_out_bench" to ("Bench or seat" to
        "A bench, a low wall or some other place to sit outdoors."),
    "qs_out_blue_sky" to ("Blue sky with a cloud" to
        "Photograph the sky – at least one cloud must be visible."),
    "qs_out_door" to ("Eye-catching door" to
        "A door with a striking colour, decoration or shape."),
    "qs_out_graffiti" to ("Graffiti or sticker" to
        "Somewhere outdoors: graffiti, a sticker or street art."),
    "qs_out_steps" to ("Stairs or steps" to
        "Photograph a staircase or steps – indoors doesn't count."),

    // Indoor
    "qs_in_blue" to ("Something blue" to
        "Photograph a blue object – clothing, decoration, a mug or whatever you find."),
    "qs_in_clock" to ("Clock" to
        "Find a clock – analogue or digital, on a wall, table or wrist."),
    "qs_in_plant" to ("Plant" to
        "Photograph a real plant or flower – potted counts too."),
    "qs_in_window" to ("Window with a view" to
        "Photograph through a window – the view must be visible."),
    "qs_in_reflection" to ("Your reflection" to
        "Your reflection in a mirror, window or shiny surface."),
    "qs_in_book" to ("Book or magazine" to
        "Photograph a book, notebook or magazine – the title must be readable."),
    "qs_in_stripes" to ("Something striped" to
        "A striped object or pattern – clothing, a curtain, tiles, anything."),
    "qs_in_round" to ("Something round" to
        "A round object – a plate, clock, lamp, ball or similar."),
    "qs_in_cable" to ("Cable or socket" to
        "Photograph a cable, charger or power socket."),
    "qs_in_food" to ("Food or drink" to
        "Anything edible or drinkable – open or packaged."),
    "qs_in_chair" to ("Chair or stool" to
        "Any seat indoors – a chair, stool or armchair."),
    "qs_in_number" to ("A number somewhere" to
        "Photograph a number visible somewhere – a clock, sign or label."),
)
