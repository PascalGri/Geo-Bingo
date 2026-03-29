package pg.geobingo.one.data

// ─────────────────────────────────────────────────────────────────────────────
//  Solo Challenge – einfache, gut machbare Kategorien fuer 5-Minuten-Runden.
//  Jede Kategorie soll in der jeweiligen Umgebung schnell findbar sein.
// ─────────────────────────────────────────────────────────────────────────────

private val SOLO_OUTDOOR_POOL: List<Category> = listOf(
    Category("solo_out_red", "Etwas Rotes", "solo_out_red",
        "Fotografiere irgendetwas Rotes – Auto, Schild, Blume, Kleidung."),
    Category("solo_out_green", "Etwas Gruenes", "solo_out_green",
        "Fotografiere irgendetwas Gruenes – Pflanze, Ampel, Tuer, Schild."),
    Category("solo_out_sign", "Ein Schild", "solo_out_sign",
        "Irgendein Schild – Strassenname, Verbot, Hinweis, Werbung."),
    Category("solo_out_tree", "Baum", "solo_out_tree",
        "Fotografiere einen Baum – mit Blaettern, Blueten oder kahl."),
    Category("solo_out_car", "Geparktes Auto", "solo_out_car",
        "Finde ein geparktes Auto und fotografiere es."),
    Category("solo_out_shadow", "Dein Schatten", "solo_out_shadow",
        "Fotografiere deinen eigenen Schatten."),
    Category("solo_out_flower", "Blume oder Pflanze", "solo_out_flower",
        "Eine Blume, Pflanze oder Strauch – egal ob im Topf oder wild."),
    Category("solo_out_bench", "Sitzgelegenheit", "solo_out_bench",
        "Eine Bank, Mauer oder ein anderer Ort zum Sitzen."),
    Category("solo_out_bike", "Fahrrad", "solo_out_bike",
        "Finde ein Fahrrad – abgestellt oder angeschlossen."),
    Category("solo_out_door", "Eine Tuer", "solo_out_door",
        "Fotografiere eine Tuer – Haus, Laden, Garage."),
    Category("solo_out_sky", "Himmel", "solo_out_sky",
        "Fotografiere den Himmel – mit oder ohne Wolken."),
    Category("solo_out_stairs", "Treppe", "solo_out_stairs",
        "Fotografiere eine Treppe oder Stufen."),
    Category("solo_out_window", "Fenster", "solo_out_window",
        "Fotografiere ein Fenster von aussen."),
    Category("solo_out_lamp", "Laterne oder Lampe", "solo_out_lamp",
        "Eine Strassenlaterne, Lampe oder Lichtquelle draussen."),
    Category("solo_out_bin", "Muelleimer", "solo_out_bin",
        "Finde einen Muelleimer oder Abfalleimer draussen."),
)

private val SOLO_INDOOR_POOL: List<Category> = listOf(
    Category("solo_in_blue", "Etwas Blaues", "solo_in_blue",
        "Fotografiere ein blaues Objekt – Tasse, Kleidung, Dekoration."),
    Category("solo_in_clock", "Uhr", "solo_in_clock",
        "Finde eine Uhr – analog, digital, Wand oder Handgelenk."),
    Category("solo_in_plant", "Pflanze", "solo_in_plant",
        "Eine Pflanze oder Blume drinnen – auch im Topf."),
    Category("solo_in_book", "Buch", "solo_in_book",
        "Fotografiere ein Buch, Heft oder Zeitschrift."),
    Category("solo_in_cup", "Tasse oder Glas", "solo_in_cup",
        "Eine Tasse, ein Glas oder eine Flasche."),
    Category("solo_in_chair", "Stuhl", "solo_in_chair",
        "Irgendeine Sitzgelegenheit – Stuhl, Hocker, Sessel."),
    Category("solo_in_lamp", "Lampe", "solo_in_lamp",
        "Eine Lampe oder Lichtquelle drinnen."),
    Category("solo_in_door", "Tuer", "solo_in_door",
        "Fotografiere eine Tuer – offen oder geschlossen."),
    Category("solo_in_screen", "Bildschirm", "solo_in_screen",
        "Ein Bildschirm – Fernseher, Monitor, Tablet, Laptop."),
    Category("solo_in_pillow", "Kissen oder Decke", "solo_in_pillow",
        "Ein Kissen, eine Decke oder ein Polster."),
    Category("solo_in_shoe", "Schuh", "solo_in_shoe",
        "Fotografiere einen Schuh – einzeln oder als Paar."),
    Category("solo_in_remote", "Fernbedienung", "solo_in_remote",
        "Eine Fernbedienung, Controller oder aehnliches."),
    Category("solo_in_mirror", "Spiegel", "solo_in_mirror",
        "Fotografiere einen Spiegel oder eine spiegelnde Oberflaeche."),
    Category("solo_in_food", "Essen oder Getraenk", "solo_in_food",
        "Irgendetwas Essbares oder Trinkbares."),
    Category("solo_in_cable", "Kabel", "solo_in_cable",
        "Fotografiere ein Kabel, Ladegeraet oder eine Steckdose."),
)

fun soloCategories(outdoor: Boolean): List<Category> {
    val pool = if (outdoor) SOLO_OUTDOOR_POOL else SOLO_INDOOR_POOL
    return pool.shuffled().take(5)
}
