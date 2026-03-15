package pg.geobingo.one.data

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
//  Category templates with multiple variants – one is picked randomly per app
//  start so categories feel fresh every game session.
// ─────────────────────────────────────────────────────────────────────────────

private data class CategoryTemplate(
    val id: String,
    val emoji: String,
    val variants: List<String>,
)

private val CATEGORY_TEMPLATES = listOf(
    // ── Kleidung ──────────────────────────────────────────────────────────────
    CategoryTemplate("pants", "👖", listOf(
        "Gelbe Hose", "Rote Hose", "Blaue Hose", "Grüne Hose",
        "Karierte Hose", "Orange Hose", "Weiße Hose", "Gestreifte Hose",
    )),
    CategoryTemplate("jacket", "🧥", listOf(
        "Rote Jacke", "Gelbe Regenjacke", "Grüne Weste",
        "Blaue Daunenjacke", "Weiße Jacke", "Lila Mantel", "Braune Lederjacke",
    )),
    CategoryTemplate("hat", "🎩", listOf(
        "Person mit Cowboyhut", "Person mit Baskenmütze", "Person mit Strohhut",
        "Person mit Wollmütze", "Person mit Baseballcap rückwärts", "Person mit Zylinder",
    )),
    CategoryTemplate("sneaker", "👟", listOf(
        "Knallrote Sneaker", "Weiße Air Force Ones", "Bunte Schuhe",
        "Neon-gelbe Laufschuhe", "Glänzende Stiefel", "Schuhe mit Muster",
    )),

    // ── Fahrzeuge ─────────────────────────────────────────────────────────────
    CategoryTemplate("car", "🚗", listOf(
        "Roter Porsche", "Weißer Tesla", "Schwarzer BMW",
        "Silberner Mercedes", "Blauer Ferrari", "Gelber Fiat 500",
        "Grüner Mini Cooper", "Pinker Smart",
    )),
    CategoryTemplate("vehicle", "🚌", listOf(
        "E-Roller", "Motorrad", "Lieferwagen",
        "Straßenbahn", "Oldtimer", "Krankenwagen", "Polizeiauto",
    )),
    CategoryTemplate("bicycle", "🚲", listOf(
        "Geparktes Fahrrad mit Blumendeko", "Fixie-Rad an Laterne",
        "Fahrrad mit Anhänger", "Retro-Fahrrad", "Tandem-Fahrrad",
        "Fahrrad mit vollem Korb",
    )),
    CategoryTemplate("unusual_vehicle", "🛺", listOf(
        "Rikscha", "Segway-Tour-Gruppe", "Pferdekutsche",
        "Milchwagen / Lieferdreirad", "Golf-Caddy auf Straße", "Ampelmännchen-Auto",
    )),

    // ── Tiere ─────────────────────────────────────────────────────────────────
    CategoryTemplate("animal", "🐾", listOf(
        "Hund an Leine", "Katze auf Fensterbrett", "Taube auf Boden",
        "Ente am Wasser", "Eichhörnchen im Park", "Möwe im Flug",
    )),
    CategoryTemplate("dog_breed", "🐶", listOf(
        "Golden Retriever", "Kleiner Mops", "Dalmatiner",
        "Riesenschnauzer", "Bulldogge", "Windhund",
    )),
    CategoryTemplate("bird", "🐦", listOf(
        "Spatz auf Tisch", "Krähe auf Mülleimer", "Schwan auf Wasser",
        "Elster auf Dach", "Papagei auf Schulter", "Rotkehlchen auf Ast",
    )),

    // ── Natur ─────────────────────────────────────────────────────────────────
    CategoryTemplate("nature", "🌿", listOf(
        "Blühende Blume", "Moos auf Stein", "Pilz",
        "Efeu an Wand", "Kaktus", "Bambus", "Wildblumen",
    )),
    CategoryTemplate("tree", "🌳", listOf(
        "Kirschbaum in Blüte", "Riesige alte Eiche", "Weeping Willow am Wasser",
        "Palme (echte oder Kunst)", "Baum mit bunten Blättern", "Bonsai in Schaufenster",
    )),
    CategoryTemplate("water", "💧", listOf(
        "Trinkbrunnen", "Pfützenspiegelung", "Bachlauf",
        "Kanal", "Teich mit Enten", "Springbrunnen",
    )),
    CategoryTemplate("sky", "☁️", listOf(
        "Dramatische Gewitterwolke", "Herzförmige Wolke", "Kondensstreifen-Kreuz",
        "Regenbogen-Fetzen", "Vollmond tagsüber sichtbar", "Sonnenstrahl durch Wolken",
    )),

    // ── Essen & Trinken ───────────────────────────────────────────────────────
    CategoryTemplate("food", "🍦", listOf(
        "Eiscreme essend", "Brezel in Hand", "Döner-Stand",
        "Obststand", "Kaffee to go", "Waffel essend",
    )),
    CategoryTemplate("food_truck", "🚚", listOf(
        "Burger-Truck", "Taco-Stand", "Crêpes-Wagen",
        "Currywurst-Stand", "Bubble-Tea-Kiosk", "Sushi-Roller",
    )),
    CategoryTemplate("cafe", "☕", listOf(
        "Outdoor-Café mit Sonnenschirmen", "Café mit Kreidetafel-Menü",
        "Café mit Katze drin", "Hipster-Coffee-Shop", "Eisdiele mit Schlange",
    )),
    CategoryTemplate("street_food", "🥨", listOf(
        "Jemand der Pommes isst", "Frühstück auf Parkbank",
        "Picknick-Gruppe im Gras", "Person mit riesigem Sandwich",
        "Kind mit Eis am Stiel",
    )),

    // ── Architektur & Gebäude ─────────────────────────────────────────────────
    CategoryTemplate("building", "🏛️", listOf(
        "Altes Fachwerkhaus", "Modernes Glas-Gebäude", "Alte Kirche",
        "Burg oder Turm", "Rathaus", "Historisches Stadttor",
    )),
    CategoryTemplate("door", "🚪", listOf(
        "Knallrote Eingangstür", "Alte Holztür mit Messingklinke",
        "Türkisfarbene Haustür", "Tür mit Rankenpflanzen",
        "Tür mit buntem Türklopfer", "Doppelflügeltür mit Buntglas",
    )),
    CategoryTemplate("window", "🪟", listOf(
        "Fenster mit Blumen", "Fenster mit Katze", "Buntglasfenster",
        "Schaufenster mit Mannequin", "Fenster mit Lichterkette",
    )),
    CategoryTemplate("balcony", "🏠", listOf(
        "Balkon voll mit Pflanzen", "Balkon mit Fahrrad drauf",
        "Balkon mit Wäscheleine", "Balkon mit Mini-Pool",
        "Balkon mit Bienenkästen", "Balkon mit Flagge",
    )),
    CategoryTemplate("arch", "🏟️", listOf(
        "Steinerner Torbogen", "Moderner Metalltunnel",
        "Brückenbogen über Wasser", "Bogengang in Altstadt",
        "Gewölbekeller-Eingang sichtbar", "Rundbogen-Fenster",
    )),

    // ── Straßenkunst & Ästhetik ───────────────────────────────────────────────
    CategoryTemplate("street_art", "🎨", listOf(
        "Graffiti an Wand", "Wandbild", "Straßenmalerei",
        "Mosaik", "Kunstinstallation", "Bemalter Stromkasten",
    )),
    CategoryTemplate("mural", "🖼️", listOf(
        "Riesiges Fassadenbild", "Politisches Wandgemälde",
        "Abstraktes Farbexplosions-Mural", "Tierportrait an Wand",
        "Optical-Illusion-Wandbild", "Retro-Werbeschild gemalt",
    )),
    CategoryTemplate("sculpture", "🗿", listOf(
        "Moderne Skulptur im Park", "Klassische Statue am Brunnen",
        "Interaktive Kunstinstallation", "Überlebensgroße Tier-Skulptur",
        "Abstrakte Metallfigur", "Steingesicht in Hauswand gemeißelt",
    )),
    CategoryTemplate("pattern", "🔷", listOf(
        "Schachbrettmuster auf Boden", "Symmetrisches Fliesenmuster",
        "Fischgrät-Pflaster", "Kreisförmige Mosaikfläche",
        "Streifenmuster an Fassade", "Geometrisches Gitternetz",
    )),

    // ── Schilder & Zeichen ────────────────────────────────────────────────────
    CategoryTemplate("sign", "🪧", listOf(
        "Lustiges Schild", "Wegweiser", "Hausnummer 13",
        "Verbotsschild", "Baustellen-Schild", "Historisches Straßenschild",
    )),
    CategoryTemplate("funny_sign", "😂", listOf(
        "Schild mit Schreibfehler", "Übertrieben langer Wegweiser",
        "Widersprüchliches Verbotsschild", "Schild mit witziger Aufschrift",
        "Handgemaltes Schild", "Schild das sich selbst widerspricht",
    )),
    CategoryTemplate("number", "🔢", listOf(
        "Hausnummer 42", "Busziel mit Zahl 7", "Uhrzeit 12:00 auf Uhr",
        "Zahl 100 irgendwo", "Straßenname mit Zahl", "Parkplatz-Nummer",
    )),

    // ── Reflexionen & Licht ───────────────────────────────────────────────────
    CategoryTemplate("shadow", "🌑", listOf(
        "Langer Menschenschatten", "Gitter-Schatten", "Baum-Schatten",
        "Tier-Schatten", "Doppelter Schatten", "Schatten in Form",
    )),
    CategoryTemplate("reflection", "🪞", listOf(
        "Spiegelung in Pfütze", "Spiegelung in Schaufenster",
        "Spiegelung in Sonnenbrille", "Spiegelung in Autorückspiegel",
        "Spiegelung in Wasserfontäne", "Doppelt-Spiegelung in Glas-Gebäude",
    )),
    CategoryTemplate("light", "✨", listOf(
        "Sonnenstrahlen durch Baumkronen", "Schatten-Kaleidoskop auf Boden",
        "Lichterkette an Baum", "Neon-Leuchtreklame", "Laterne mit Schatten",
        "Regenbogen in Sprinkler",
    )),

    // ── Sport & Bewegung ──────────────────────────────────────────────────────
    CategoryTemplate("sport", "🏃", listOf(
        "Jogger", "Radfahrer mit Helm", "Skater",
        "Person mit Yoga-Matte", "Kletterer", "Inline-Skater",
    )),
    CategoryTemplate("sport_field", "⚽", listOf(
        "Basketballkorb im Park", "Beachvolleyball-Netz",
        "Tischtennis-Platte im Freien", "Boule-Spieler", "Skateboard-Park",
        "Fußball-Cage im Stadtviertel",
    )),
    CategoryTemplate("yoga_meditation", "🧘", listOf(
        "Yoga-Gruppe im Park", "Person meditierend auf Bank",
        "Tai-Chi-Gruppe früh morgens", "Stretching vor dem Joggen",
        "Person mit Foam Roller", "Bewegungsgruppe für Senioren",
    )),

    // ── Technik & Modern ──────────────────────────────────────────────────────
    CategoryTemplate("tech", "⚡", listOf(
        "E-Ladesäule", "Solaranlage auf Dach", "Überwachungskamera",
        "Fotoautomat", "Geldautomat", "Paketstation",
    )),
    CategoryTemplate("smart_city", "📡", listOf(
        "Smarte Straßenlaterne mit Sensor", "WLAN-Hotspot-Säule",
        "E-Scooter-Parkstation", "Bike-Sharing-Station",
        "Digitale Infotafel", "Roboter-Lieferfahrzeug",
    )),
    CategoryTemplate("construction", "🏗️", listOf(
        "Baukran über Skyline", "Frisch gestrichene Wand",
        "Baustelle mit Absperrband", "Gerüst an Fassade",
        "Bagger auf Straße", "Neues Gebäude im Entstehen",
    )),

    // ── Stadtmöbel & Details ──────────────────────────────────────────────────
    CategoryTemplate("bench", "🪑", listOf(
        "Parkbank mit Person", "Leere Parkbank", "Bank am Wasser",
        "Schaukel im Park", "Liegestuhl", "Steinbank",
    )),
    CategoryTemplate("stairs", "🪜", listOf(
        "Außentreppe", "Spiraltreppe", "Feuerwehrleiter",
        "Rolltreppe", "Historische Steintreppe", "Brücke mit Stufen",
    )),
    CategoryTemplate("manhole", "⚙️", listOf(
        "Dekorativer Kanaldeckel", "Bemalter Gullideckel",
        "Kanaldeckel mit Stadtlogo", "Rostige Eisenplatte im Pflaster",
        "Gitter-Abdeckung mit Muster",
    )),
    CategoryTemplate("mailbox", "📬", listOf(
        "Roter Briefkasten", "Alter Postkasten an Wand",
        "Vollgestopfter Briefkasten", "Briefkasten mit lustigem Namen",
        "Mini-Bücher-Tauschbox", "Paketsafe am Hauseingang",
    )),
    CategoryTemplate("clock", "🕐", listOf(
        "Öffentliche Turmuhr", "Sonnenuhr im Park",
        "Uhrengeschäft-Schaufenster", "Riesige Wanduhr an Gebäude",
        "Bahnhofsuhr", "Digitale Stadtzeit-Anzeige",
    )),
    CategoryTemplate("lamp", "💡", listOf(
        "Alte Gaslaterne", "Moderne Design-Straßenlampe",
        "Lichterkette zwischen Häusern", "Neon-Reklame",
        "LED-Kunstinstallation", "Papierlaterne aufgehängt",
    )),

    // ── Menschen & Szenen ─────────────────────────────────────────────────────
    CategoryTemplate("people", "👥", listOf(
        "Gruppe lachender Menschen", "Warteschlange", "Schulkinder zusammen",
        "Touristengruppe mit Foto", "Sportgruppe", "Pärchen Hand in Hand",
    )),
    CategoryTemplate("person_accessory", "🕶️", listOf(
        "Person mit Hut", "Person mit Sonnenbrille", "Person mit Kopfhörern",
        "Person mit Rucksack", "Person mit Regenschirm", "Person mit Rollkoffer",
    )),
    CategoryTemplate("musician", "🎸", listOf(
        "Straßengitarrist", "Akkordeonspieler", "Schlagzeuger mit Eimern",
        "Geigerin", "Saxophonist an Ecke", "Straßenchor",
    )),
    CategoryTemplate("funny_pose", "🤸", listOf(
        "Person macht Handstand", "Jemand springt und lacht",
        "Person macht Selfie in komischer Pose", "Tanzende Person allein",
        "Person schläft auf Bank sitzend", "Jemand trägt riesiges Objekt",
    )),
    CategoryTemplate("tourist", "📸", listOf(
        "Tourist fotografiert Gebäude", "Reisegruppe mit Audioguide",
        "Person mit riesiger Karte", "Selfie vor Sehenswürdigkeit",
        "Tour-Guide mit Regenschirm-Gruppe", "Person in stadtypischem Souvenir-Outfit",
    )),

    // ── Shops & Services ─────────────────────────────────────────────────────
    CategoryTemplate("shop", "🏪", listOf(
        "Bäckerei", "Blumenladen", "Friseur",
        "Buchhandlung", "Käseladen", "Weinhändler", "Bio-Laden",
    )),
    CategoryTemplate("market", "🛒", listOf(
        "Wochenmarkt-Stand", "Flohmarkt-Tisch", "Blumenmarkt",
        "Obstmarkt mit Farbenvielfalt", "Antiquitäten-Stand",
        "Second-Hand-Kleiderstand",
    )),
    CategoryTemplate("vintage", "🎪", listOf(
        "Retro-Schaufenster", "Vintage-Poster an Wand",
        "Altes Telefonhäuschen", "70er-Jahre-Wohnmobil",
        "Nostalgisches Reklame-Schild", "Antikshop-Auslage",
    )),

    // ── Farbobjekte ───────────────────────────────────────────────────────────
    CategoryTemplate("color_object", "🎯", listOf(
        "Etwas komplett Rotes", "Etwas komplett Blaues", "Etwas komplett Gelbes",
        "Etwas komplett Oranges", "Etwas komplett Pinkes", "Etwas komplett Weißes",
    )),
    CategoryTemplate("colorful_scene", "🌈", listOf(
        "Regenbogenfahne oder -deko", "5 verschiedene Farben in einem Bild",
        "Bunt bemaltes Haus", "Farbige Fahrräder nebeneinander",
        "Buntes Mosaik", "Konfetti auf Boden",
    )),

    // ── Geometrie & Form ─────────────────────────────────────────────────────
    CategoryTemplate("round", "⭕", listOf(
        "Rundes Straßenschild", "Runder Spiegel", "Runder Tisch",
        "Runde Uhr", "Runder Brunnen", "Rundes Fenster",
    )),
    CategoryTemplate("triangle", "🔺", listOf(
        "Dreieckiges Dach", "Warnschilder (Dreieck)", "Dreieckige Fensteranordnung",
        "Pyramidenförmige Architektur", "Dreieck-Muster auf Boden",
    )),

    // ── Musik & Kultur ────────────────────────────────────────────────────────
    CategoryTemplate("music", "🎵", listOf(
        "Straßenmusiker", "Konzertplakat", "Kopfhörer an Person",
        "Lautsprecher draußen", "Instrument im Schaufenster", "Notenblatt sichtbar",
    )),
    CategoryTemplate("culture", "🎭", listOf(
        "Theater-Plakat", "Kino-Eingang", "Galerie-Schaufenster",
        "Tanzaufführung im Park", "Flohmarkt für Kunsthandwerk",
        "Bücherschrank zum Tauschen",
    )),

    // ── Ungewöhnliches & Kurioses ─────────────────────────────────────────────
    CategoryTemplate("weird", "🤔", listOf(
        "Etwas völlig fehl am Platz", "Unerwartet lustiges Detail",
        "Aufschrift die keinen Sinn ergibt", "Sehr ungewöhnliche Farbkombination",
        "Objekt das falsch herum steht", "Tier an ungewöhnlichem Ort",
    )),
    CategoryTemplate("miniature", "🔍", listOf(
        "Winziges Straßenkunst-Detail", "Mini-Figur in Gehwegritze",
        "Kleines verstecktes Schild", "Mikrokunst an Laternenpfahl",
        "Ganz kleines Graffiti-Tag", "Tiny door in Mauer eingebaut",
    )),
    CategoryTemplate("symmetry", "⚖️", listOf(
        "Perfekt symmetrische Fassade", "Spiegelbildliche Brücke im Wasser",
        "Zwei identische Bäume rechts/links", "Symmetrischer Torbogen",
        "Gespiegelte Fensterreihe", "Doppeltreppe links-rechts",
    )),
)

const val VISIBLE_PRESET_COUNT = 12

val PRESET_CATEGORIES: List<Category> = CATEGORY_TEMPLATES.map { t ->
    Category(id = t.id, name = t.variants.random(), emoji = t.emoji)
}.shuffled()

val PLAYER_COLORS = listOf(
    Color(0xFFEC4899), // hot pink
    Color(0xFF3B82F6), // blue
    Color(0xFF10B981), // emerald
    Color(0xFFA78BFA), // violet
    Color(0xFFF59E0B), // amber
    Color(0xFF22D3EE), // cyan
    Color(0xFFF97316), // orange
    Color(0xFF84CC16), // lime
)
