package pg.geobingo.one.data

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
//  Category templates with multiple variants – one is picked randomly per app
//  start so categories feel fresh every game session.
// ─────────────────────────────────────────────────────────────────────────────

enum class CategoryGroup(val labelDe: String, val labelEn: String) {
    CLOTHING("Kleidung", "Clothing"),
    VEHICLES("Fahrzeuge", "Vehicles"),
    ANIMALS("Tiere", "Animals"),
    NATURE("Natur", "Nature"),
    FOOD("Essen & Trinken", "Food & Drinks"),
    ARCHITECTURE("Architektur", "Architecture"),
    STREET_ART("Strassenkunst", "Street Art"),
    SIGNS("Schilder", "Signs"),
    LIGHT("Licht & Reflexion", "Light & Reflection"),
    SPORT("Sport", "Sports"),
    TECH("Technik", "Technology"),
    URBAN("Stadtmoebel", "Urban Furniture"),
    PEOPLE("Menschen", "People"),
    SHOPS("Shops", "Shops"),
    COLORS("Farben", "Colors"),
    GEOMETRY("Geometrie", "Geometry"),
    CULTURE("Kultur", "Culture"),
    CURIOUS("Kurioses", "Curious"),
}

enum class CategoryDifficulty(val labelDe: String, val labelEn: String) {
    EASY("Leicht", "Easy"),
    MEDIUM("Mittel", "Medium"),
    HARD("Schwer", "Hard"),
}

private data class CategoryTemplate(
    val id: String,
    val emoji: String,
    val variants: List<String>,
    val description: String = "",
    val group: CategoryGroup = CategoryGroup.CURIOUS,
    val difficulty: CategoryDifficulty = CategoryDifficulty.MEDIUM,
)

private val CATEGORY_TEMPLATES = listOf(
    // ── Kleidung ──────────────────────────────────────────────────────────────
    CategoryTemplate("pants", "👖", listOf(
        "Gelbe Hose", "Rote Hose", "Blaue Hose", "Grüne Hose",
        "Karierte Hose", "Orange Hose", "Weiße Hose", "Gestreifte Hose",
    ), "Die gesuchte Farbe oder das Muster muss klar erkennbar sein. Knie bis Knöchel sollten sichtbar sein – kein Foto aus 50 m Entfernung.", CategoryGroup.CLOTHING, CategoryDifficulty.EASY),
    CategoryTemplate("jacket", "🧥", listOf(
        "Rote Jacke", "Gelbe Regenjacke", "Grüne Weste",
        "Blaue Daunenjacke", "Weiße Jacke", "Lila Mantel", "Braune Lederjacke",
    ), "Die Jacke oder der Mantel muss klar erkennbar im Bild sein. Farbe oder Material muss deutlich sichtbar sein.", CategoryGroup.CLOTHING, CategoryDifficulty.EASY),
    CategoryTemplate("hat", "🎩", listOf(
        "Person mit Cowboyhut", "Person mit Baskenmütze", "Person mit Strohhut",
        "Person mit Wollmütze", "Person mit Baseballcap rückwärts", "Person mit Zylinder",
    ), "Person und Kopfbedeckung müssen beide im Bild sein. Der Huttyp muss eindeutig erkennbar sein.", CategoryGroup.CLOTHING, CategoryDifficulty.MEDIUM),
    CategoryTemplate("sneaker", "👟", listOf(
        "Knallrote Sneaker", "Weiße Air Force Ones", "Bunte Schuhe",
        "Neon-gelbe Laufschuhe", "Glänzende Stiefel", "Schuhe mit Muster",
    ), "Die Schuhe müssen im Fokus des Fotos stehen und die gesuchte Eigenschaft (Farbe/Muster/Modell) klar erkennbar sein.", CategoryGroup.CLOTHING, CategoryDifficulty.MEDIUM),

    // ── Fahrzeuge ─────────────────────────────────────────────────────────────
    CategoryTemplate("car", "🚗", listOf(
        "Roter Porsche", "Weißer Tesla", "Schwarzer BMW",
        "Silberner Mercedes", "Blauer Ferrari", "Gelber Fiat 500",
        "Grüner Mini Cooper", "Pinker Smart",
    ), "Das Fahrzeug muss vollständig oder mindestens zur Hälfte sichtbar sein. Marke und Farbe müssen eindeutig erkennbar sein.", CategoryGroup.VEHICLES, CategoryDifficulty.MEDIUM),
    CategoryTemplate("vehicle", "🚌", listOf(
        "E-Roller", "Motorrad", "Lieferwagen",
        "Straßenbahn", "Oldtimer", "Krankenwagen", "Polizeiauto",
    ), "Das gesuchte Fahrzeug muss klar erkennbar im Bild sein. Keine Spielzeugversionen.", CategoryGroup.VEHICLES, CategoryDifficulty.EASY),
    CategoryTemplate("bicycle", "🚲", listOf(
        "Geparktes Fahrrad mit Blumendeko", "Fixie-Rad an Laterne",
        "Fahrrad mit Anhänger", "Retro-Fahrrad", "Tandem-Fahrrad",
        "Fahrrad mit vollem Korb",
    ), "Das Fahrrad und das gesuchte Merkmal (z.B. Deko, Anhänger) müssen beide klar sichtbar sein.", CategoryGroup.VEHICLES, CategoryDifficulty.MEDIUM),

    // ── Tiere ─────────────────────────────────────────────────────────────────
    CategoryTemplate("animal", "🐾", listOf(
        "Hund an Leine", "Katze auf Fensterbrett", "Taube auf Boden",
        "Ente am Wasser", "Eichhörnchen im Park", "Möwe im Flug",
    ), "Das Tier und seine Situation müssen erkennbar sein. Tier muss lebendig und real sein – kein Bild oder Spielzeug.", CategoryGroup.ANIMALS, CategoryDifficulty.EASY),
    CategoryTemplate("dog_breed", "🐶", listOf(
        "Golden Retriever", "Kleiner Mops", "Dalmatiner",
        "Riesenschnauzer", "Bulldogge", "Windhund",
    ), "Die Rasse des Hundes muss erkennbar sein. Bei gemischten Rassen zählt die Hauptrasse – Abstimmung entscheidet.", CategoryGroup.ANIMALS, CategoryDifficulty.HARD),
    CategoryTemplate("bird", "🐦", listOf(
        "Spatz auf Tisch", "Krähe auf Mülleimer", "Schwan auf Wasser",
        "Elster auf Dach", "Papagei auf Schulter", "Rotkehlchen auf Ast",
    ), "Vogelart und Ort müssen im Foto erkennbar sein. Foto aus nächster Nähe bevorzugt.", CategoryGroup.ANIMALS, CategoryDifficulty.MEDIUM),

    // ── Natur ─────────────────────────────────────────────────────────────────
    CategoryTemplate("nature", "🌿", listOf(
        "Blühende Blume", "Moos auf Stein", "Pilz",
        "Efeu an Wand", "Kaktus", "Bambus", "Wildblumen",
    ), "Das Naturobjekt muss klar erkennbar und im Freien sein. Topfpflanzen in Geschäften zählen nicht.", CategoryGroup.NATURE, CategoryDifficulty.EASY),
    CategoryTemplate("tree", "🌳", listOf(
        "Kirschbaum in Blüte", "Riesige alte Eiche", "Weeping Willow am Wasser",
        "Palme (echte oder Kunst)", "Baum mit bunten Blättern", "Bonsai in Schaufenster",
    ), "Der Baum oder das gesuchte Merkmal muss klar erkennbar sein. Kleine Sträucher zählen nicht als Bäume.", CategoryGroup.NATURE, CategoryDifficulty.MEDIUM),
    CategoryTemplate("water", "💧", listOf(
        "Trinkbrunnen", "Pfützenspiegelung", "Bachlauf",
        "Kanal", "Teich mit Enten", "Springbrunnen",
    ), "Das Wasser-Feature muss klar im Fokus stehen. Bei Spiegelungen muss die Reflexion erkennbar sein.", CategoryGroup.NATURE, CategoryDifficulty.EASY),
    CategoryTemplate("sky", "☁️", listOf(
        "Dramatische Gewitterwolke", "Herzförmige Wolke", "Kondensstreifen-Kreuz",
        "Regenbogen-Fetzen", "Vollmond tagsüber sichtbar", "Sonnenstrahl durch Wolken",
    ), "Das Himmelsphänomen muss eindeutig erkennbar sein. Bearbeitete oder gefilterte Fotos sind ungültig.", CategoryGroup.NATURE, CategoryDifficulty.HARD),

    // ── Essen & Trinken ───────────────────────────────────────────────────────
    CategoryTemplate("food", "🍦", listOf(
        "Eiscreme essend", "Brezel in Hand", "Döner-Stand",
        "Obststand", "Kaffee to go", "Waffel essend",
    ), "Essen und die Person oder der Ort müssen erkennbar sein. Das gesuchte Lebensmittel muss im Mittelpunkt stehen.", CategoryGroup.FOOD, CategoryDifficulty.EASY),
    CategoryTemplate("food_truck", "🚚", listOf(
        "Burger-Truck", "Taco-Stand", "Crêpes-Wagen",
        "Currywurst-Stand", "Bubble-Tea-Kiosk", "Sushi-Roller",
    ), "Der Truck oder Stand muss erkennbar sein. Der angebotene Typ muss aus dem Foto oder einem Schild erkennbar sein.", CategoryGroup.FOOD, CategoryDifficulty.MEDIUM),
    CategoryTemplate("cafe", "☕", listOf(
        "Outdoor-Café mit Sonnenschirmen", "Café mit Kreidetafel-Menü",
        "Café mit Katze drin", "Hipster-Coffee-Shop", "Eisdiele mit Schlange",
    ), "Das Café und das gesuchte Merkmal (z.B. Sonnenschirme, Kreidetafel) müssen im Bild erkennbar sein.", CategoryGroup.FOOD, CategoryDifficulty.EASY),
    CategoryTemplate("street_food", "🥨", listOf(
        "Jemand der Pommes isst", "Frühstück auf Parkbank",
        "Picknick-Gruppe im Gras", "Person mit riesigem Sandwich",
        "Kind mit Eis am Stiel",
    ), "Die Person und das Essen müssen klar erkennbar sein. Szene muss draußen stattfinden.", CategoryGroup.FOOD, CategoryDifficulty.MEDIUM),

    // ── Architektur & Gebäude ─────────────────────────────────────────────────
    CategoryTemplate("building", "🏛️", listOf(
        "Altes Fachwerkhaus", "Modernes Glas-Gebäude", "Alte Kirche",
        "Burg oder Turm", "Rathaus", "Historisches Stadttor",
    ), "Der Gebäudetyp muss eindeutig erkennbar sein. Mindestens die Fassade muss vollständig sichtbar sein.", CategoryGroup.ARCHITECTURE, CategoryDifficulty.EASY),
    CategoryTemplate("door", "🚪", listOf(
        "Knallrote Eingangstür", "Alte Holztür mit Messingklinke",
        "Türkisfarbene Haustür", "Tür mit Rankenpflanzen",
        "Tür mit buntem Türklopfer", "Doppelflügeltür mit Buntglas",
    ), "Die Tür muss klar erkennbar sein. Das gesuchte Merkmal (Farbe, Material, Deko) muss deutlich sichtbar sein.", CategoryGroup.ARCHITECTURE, CategoryDifficulty.MEDIUM),
    CategoryTemplate("window", "🪟", listOf(
        "Fenster mit Blumen", "Fenster mit Katze", "Buntglasfenster",
        "Schaufenster mit Mannequin", "Fenster mit Lichterkette",
    ), "Das Fenster und das gesuchte Merkmal müssen beide klar im Foto erkennbar sein.", CategoryGroup.ARCHITECTURE, CategoryDifficulty.EASY),
    CategoryTemplate("balcony", "🏠", listOf(
        "Balkon voll mit Pflanzen", "Balkon mit Fahrrad drauf",
        "Balkon mit Wäscheleine", "Balkon mit Mini-Pool",
        "Balkon mit Bienenkästen", "Balkon mit Flagge",
    ), "Der Balkon und sein besonderes Merkmal müssen beide erkennbar sein. Nur Balkons an echten Wohnhäusern.", CategoryGroup.ARCHITECTURE, CategoryDifficulty.MEDIUM),
    CategoryTemplate("arch", "🏟️", listOf(
        "Steinerner Torbogen", "Moderner Metalltunnel",
        "Brückenbogen über Wasser", "Bogengang in Altstadt",
        "Gewölbekeller-Eingang sichtbar", "Rundbogen-Fenster",
    ), "Die Bogenform muss klar erkennbar sein. Beide Seiten des Bogens müssen im Bild sein.", CategoryGroup.ARCHITECTURE, CategoryDifficulty.MEDIUM),

    // ── Straßenkunst & Ästhetik ───────────────────────────────────────────────
    CategoryTemplate("street_art", "🎨", listOf(
        "Graffiti an Wand", "Wandbild", "Straßenmalerei",
        "Mosaik", "Kunstinstallation", "Bemalter Stromkasten",
    ), "Die Kunstform muss erkennbar sein und sich an einem öffentlichen Ort befinden. Werbeplakate zählen nicht.", CategoryGroup.STREET_ART, CategoryDifficulty.EASY),
    CategoryTemplate("mural", "🖼️", listOf(
        "Riesiges Fassadenbild", "Politisches Wandgemälde",
        "Abstraktes Farbexplosions-Mural", "Tierportrait an Wand",
        "Optical-Illusion-Wandbild", "Retro-Werbeschild gemalt",
    ), "Das Wandbild muss großformatig und klar künstlerisch sein. Das Foto muss das Gesamtbild zeigen, nicht nur einen Ausschnitt.", CategoryGroup.STREET_ART, CategoryDifficulty.MEDIUM),
    CategoryTemplate("sculpture", "🗿", listOf(
        "Moderne Skulptur im Park", "Klassische Statue am Brunnen",
        "Interaktive Kunstinstallation", "Überlebensgroße Tier-Skulptur",
        "Abstrakte Metallfigur", "Steingesicht in Hauswand gemeißelt",
    ), "Die Skulptur muss eindeutig als Kunstobjekt erkennbar sein und vollständig oder nahezu vollständig sichtbar sein.", CategoryGroup.STREET_ART, CategoryDifficulty.MEDIUM),
    CategoryTemplate("pattern", "🔷", listOf(
        "Schachbrettmuster auf Boden", "Symmetrisches Fliesenmuster",
        "Fischgrät-Pflaster", "Kreisförmige Mosaikfläche",
        "Streifenmuster an Fassade", "Geometrisches Gitternetz",
    ), "Das Muster muss klar erkennbar und wiederholend sein. Das Foto muss mindestens 4 Wiederholungen zeigen.", CategoryGroup.STREET_ART, CategoryDifficulty.MEDIUM),

    // ── Schilder & Zeichen ────────────────────────────────────────────────────
    CategoryTemplate("sign", "🪧", listOf(
        "Lustiges Schild", "Wegweiser", "Hausnummer 13",
        "Verbotsschild", "Baustellen-Schild", "Historisches Straßenschild",
    ), "Das Schild und seine Aufschrift müssen lesbar sein. Digitale Anzeigetafeln zählen.", CategoryGroup.SIGNS, CategoryDifficulty.EASY),
    CategoryTemplate("funny_sign", "😂", listOf(
        "Schild mit Schreibfehler", "Übertrieben langer Wegweiser",
        "Widersprüchliches Verbotsschild", "Schild mit witziger Aufschrift",
        "Handgemaltes Schild", "Schild das sich selbst widerspricht",
    ), "Der witzige Aspekt muss klar erkennbar und lesbar sein. Die Abstimmung entscheidet, ob es lustig genug ist.", CategoryGroup.SIGNS, CategoryDifficulty.HARD),
    CategoryTemplate("number", "🔢", listOf(
        "Hausnummer 42", "Busziel mit Zahl 7", "Uhrzeit 12:00 auf Uhr",
        "Zahl 100 irgendwo", "Straßenname mit Zahl", "Parkplatz-Nummer",
    ), "Die gesuchte Zahl muss klar lesbar sein. Sie muss offiziell angebracht sein – keine selbst hingeschriebenen Zahlen.", CategoryGroup.SIGNS, CategoryDifficulty.EASY),

    // ── Reflexionen & Licht ───────────────────────────────────────────────────
    CategoryTemplate("shadow", "🌑", listOf(
        "Langer Menschenschatten", "Gitter-Schatten", "Baum-Schatten",
        "Tier-Schatten", "Doppelter Schatten", "Schatten in Form",
    ), "Der Schatten muss klar erkennbar sein und das gesuchte Merkmal zeigen. Die Lichtquelle darf nicht im Bild sein.", CategoryGroup.LIGHT, CategoryDifficulty.MEDIUM),
    CategoryTemplate("reflection", "🪞", listOf(
        "Spiegelung in Pfütze", "Spiegelung in Schaufenster",
        "Spiegelung in Sonnenbrille", "Spiegelung in Autorückspiegel",
        "Spiegelung in Wasserfontäne", "Doppelt-Spiegelung in Glas-Gebäude",
    ), "Die Spiegelung und die spiegelnde Oberfläche müssen beide erkennbar sein. Bearbeitete Fotos sind ungültig.", CategoryGroup.LIGHT, CategoryDifficulty.MEDIUM),
    CategoryTemplate("light", "✨", listOf(
        "Sonnenstrahlen durch Baumkronen", "Schatten-Kaleidoskop auf Boden",
        "Lichterkette an Baum", "Neon-Leuchtreklame", "Laterne mit Schatten",
        "Regenbogen in Sprinkler",
    ), "Das Lichtphänomen muss klar erkennbar sein und im Freien oder an einem öffentlichen Ort aufgenommen worden sein.", CategoryGroup.LIGHT, CategoryDifficulty.HARD),

    // ── Sport & Bewegung ──────────────────────────────────────────────────────
    CategoryTemplate("sport", "🏃", listOf(
        "Jogger", "Radfahrer mit Helm", "Skater",
        "Person mit Yoga-Matte", "Kletterer", "Inline-Skater",
    ), "Person und Sportart müssen erkennbar sein. Die Person muss aktiv in Bewegung oder klar als Sportler gekleidet sein.", CategoryGroup.SPORT, CategoryDifficulty.EASY),
    CategoryTemplate("sport_field", "⚽", listOf(
        "Basketballkorb im Park", "Beachvolleyball-Netz",
        "Tischtennis-Platte im Freien", "Boule-Spieler", "Skateboard-Park",
        "Fußball-Cage im Stadtviertel",
    ), "Die Sportstätte oder -ausrüstung muss eindeutig erkennbar sein. Menschen beim Spielen sind ein Plus, aber nicht nötig.", CategoryGroup.SPORT, CategoryDifficulty.MEDIUM),

    // ── Technik & Modern ──────────────────────────────────────────────────────
    CategoryTemplate("tech", "⚡", listOf(
        "E-Ladesäule", "Solaranlage auf Dach", "Überwachungskamera",
        "Fotoautomat", "Geldautomat", "Paketstation",
    ), "Das Gerät muss klar erkennbar sein. Es muss sich an einem öffentlichen Ort befinden und in Betrieb sein.", CategoryGroup.TECH, CategoryDifficulty.EASY),
    CategoryTemplate("smart_city", "📡", listOf(
        "Smarte Straßenlaterne mit Sensor", "WLAN-Hotspot-Säule",
        "E-Scooter-Parkstation", "Bike-Sharing-Station",
        "Digitale Infotafel", "Roboter-Lieferfahrzeug",
    ), "Das smarte Stadtmöbel muss eindeutig erkennbar sein. Ein sichtbares Logo oder Schild als Nachweis ist ideal.", CategoryGroup.TECH, CategoryDifficulty.HARD),
    CategoryTemplate("construction", "🏗️", listOf(
        "Baukran über Skyline", "Frisch gestrichene Wand",
        "Baustelle mit Absperrband", "Gerüst an Fassade",
        "Bagger auf Straße", "Neues Gebäude im Entstehen",
    ), "Die Baustelle oder das Element muss aktiv und klar erkennbar sein. Reine Bauzäune ohne weiteres reichen nicht.", CategoryGroup.TECH, CategoryDifficulty.EASY),

    // ── Stadtmöbel & Details ──────────────────────────────────────────────────
    CategoryTemplate("bench", "🪑", listOf(
        "Parkbank mit Person", "Leere Parkbank", "Bank am Wasser",
        "Schaukel im Park", "Liegestuhl", "Steinbank",
    ), "Die Sitzgelegenheit muss vollständig im Bild sein. Das gesuchte Merkmal (Person, Ort) muss erkennbar sein.", CategoryGroup.URBAN, CategoryDifficulty.EASY),
    CategoryTemplate("stairs", "🪜", listOf(
        "Außentreppe", "Spiraltreppe", "Feuerwehrleiter",
        "Rolltreppe", "Historische Steintreppe", "Brücke mit Stufen",
    ), "Mindestens 5 Stufen müssen sichtbar sein. Der gesuchte Typ muss klar erkennbar sein.", CategoryGroup.URBAN, CategoryDifficulty.EASY),
    CategoryTemplate("manhole", "⚙️", listOf(
        "Dekorativer Kanaldeckel", "Bemalter Gullideckel",
        "Kanaldeckel mit Stadtlogo", "Rostige Eisenplatte im Pflaster",
        "Gitter-Abdeckung mit Muster",
    ), "Der Deckel muss vollständig sichtbar und das Muster oder Design klar erkennbar sein.", CategoryGroup.URBAN, CategoryDifficulty.MEDIUM),
    CategoryTemplate("mailbox", "📬", listOf(
        "Roter Briefkasten", "Alter Postkasten an Wand",
        "Vollgestopfter Briefkasten", "Briefkasten mit lustigem Namen",
        "Mini-Bücher-Tauschbox", "Paketsafe am Hauseingang",
    ), "Der Briefkasten muss vollständig sichtbar und das gesuchte Merkmal (Farbe, Zustand) erkennbar sein.", CategoryGroup.URBAN, CategoryDifficulty.EASY),
    CategoryTemplate("clock", "🕐", listOf(
        "Öffentliche Turmuhr", "Sonnenuhr im Park",
        "Uhrengeschäft-Schaufenster", "Riesige Wanduhr an Gebäude",
        "Bahnhofsuhr", "Digitale Stadtzeit-Anzeige",
    ), "Die Uhr und ihre Ziffern oder die Uhrzeit müssen erkennbar sein. Armbanduhren zählen nicht.", CategoryGroup.URBAN, CategoryDifficulty.EASY),
    CategoryTemplate("lamp", "💡", listOf(
        "Alte Gaslaterne", "Moderne Design-Straßenlampe",
        "Lichterkette zwischen Häusern", "Neon-Reklame",
        "LED-Kunstinstallation", "Papierlaterne aufgehängt",
    ), "Die Lichtquelle und ihr besonderes Merkmal müssen klar erkennbar sein. Normale Glühbirnen in Läden zählen nicht.", CategoryGroup.URBAN, CategoryDifficulty.MEDIUM),

    // ── Menschen & Szenen ─────────────────────────────────────────────────────
    CategoryTemplate("people", "👥", listOf(
        "Gruppe lachender Menschen", "Warteschlange", "Schulkinder zusammen",
        "Touristengruppe mit Foto", "Sportgruppe", "Pärchen Hand in Hand",
    ), "Mindestens 2 Personen müssen erkennbar im Bild sein. Achte auf Einverständnis – keine heimlichen Nahaufnahmen.", CategoryGroup.PEOPLE, CategoryDifficulty.EASY),
    CategoryTemplate("person_accessory", "🕶️", listOf(
        "Person mit Hut", "Person mit Sonnenbrille", "Person mit Kopfhörern",
        "Person mit Rucksack", "Person mit Regenschirm", "Person mit Rollkoffer",
    ), "Person und Accessoire müssen klar erkennbar sein. Foto mit Zustimmung der abgebildeten Person.", CategoryGroup.PEOPLE, CategoryDifficulty.EASY),
    CategoryTemplate("musician", "🎸", listOf(
        "Straßengitarrist", "Akkordeonspieler", "Schlagzeuger mit Eimern",
        "Geigerin", "Saxophonist an Ecke", "Straßenchor",
    ), "Musiker und Instrument müssen erkennbar sein. Person muss aktiv musizieren oder eindeutig als Musiker erkennbar sein.", CategoryGroup.PEOPLE, CategoryDifficulty.HARD),
    CategoryTemplate("funny_pose", "🤸", listOf(
        "Person macht Handstand", "Jemand springt und lacht",
        "Person macht Selfie in komischer Pose", "Tanzende Person allein",
        "Person schläft auf Bank sitzend", "Jemand trägt riesiges Objekt",
    ), "Die lustige Situation muss klar erkennbar sein. Am besten mit Zustimmung der Person fotografieren.", CategoryGroup.PEOPLE, CategoryDifficulty.HARD),
    CategoryTemplate("tourist", "📸", listOf(
        "Tourist fotografiert Gebäude", "Reisegruppe mit Audioguide",
        "Person mit riesiger Karte", "Selfie vor Sehenswürdigkeit",
        "Tour-Guide mit Regenschirm-Gruppe", "Person in stadtypischem Souvenir-Outfit",
    ), "Das typisch touristische Verhalten muss eindeutig erkennbar sein. Fotos mit Zustimmung der Abgebildeten.", CategoryGroup.PEOPLE, CategoryDifficulty.MEDIUM),

    // ── Shops & Services ─────────────────────────────────────────────────────
    CategoryTemplate("shop", "🏪", listOf(
        "Bäckerei", "Blumenladen", "Friseur",
        "Buchhandlung", "Käseladen", "Weinhändler", "Bio-Laden",
    ), "Das Geschäft und sein Typ müssen klar erkennbar sein – entweder durch ein Schild oder durch das Schaufenster.", CategoryGroup.SHOPS, CategoryDifficulty.EASY),
    CategoryTemplate("market", "🛒", listOf(
        "Wochenmarkt-Stand", "Flohmarkt-Tisch", "Blumenmarkt",
        "Obstmarkt mit Farbenvielfalt", "Antiquitäten-Stand",
        "Second-Hand-Kleiderstand",
    ), "Der Stand und sein Angebot müssen erkennbar sein. Supermarkt-Innenräume zählen nicht.", CategoryGroup.SHOPS, CategoryDifficulty.MEDIUM),

    // ── Farbobjekte ───────────────────────────────────────────────────────────
    CategoryTemplate("color_object", "🎯", listOf(
        "Etwas komplett Rotes", "Etwas komplett Blaues", "Etwas komplett Gelbes",
        "Etwas komplett Oranges", "Etwas komplett Pinkes", "Etwas komplett Weißes",
    ), "Das Objekt muss zu mindestens 80% in der gesuchten Farbe gehalten sein. Gemischte Farben zählen nicht.", CategoryGroup.COLORS, CategoryDifficulty.EASY),
    CategoryTemplate("colorful_scene", "🌈", listOf(
        "Regenbogenfahne oder -deko", "5 verschiedene Farben in einem Bild",
        "Bunt bemaltes Haus", "Farbige Fahrräder nebeneinander",
        "Buntes Mosaik", "Konfetti auf Boden",
    ), "Das Foto muss die genannte Anzahl von Farben oder das beschriebene bunte Objekt klar zeigen.", CategoryGroup.COLORS, CategoryDifficulty.MEDIUM),

    // ── Geometrie & Form ─────────────────────────────────────────────────────
    CategoryTemplate("round", "⭕", listOf(
        "Rundes Straßenschild", "Runder Spiegel", "Runder Tisch",
        "Runde Uhr", "Runder Brunnen", "Rundes Fenster",
    ), "Das Objekt muss klar rund sein – keine ovalen oder nur leicht gerundeten Objekte. Vollständig sichtbar.", CategoryGroup.GEOMETRY, CategoryDifficulty.EASY),
    CategoryTemplate("triangle", "🔺", listOf(
        "Dreieckiges Dach", "Warnschilder (Dreieck)", "Dreieckige Fensteranordnung",
        "Pyramidenförmige Architektur", "Dreieck-Muster auf Boden",
    ), "Die Dreiecksform muss klar erkennbar sein. Das Foto muss das gesamte Dreieck oder Muster zeigen.", CategoryGroup.GEOMETRY, CategoryDifficulty.EASY),

    // ── Musik & Kultur ────────────────────────────────────────────────────────
    CategoryTemplate("music", "🎵", listOf(
        "Straßenmusiker", "Konzertplakat", "Kopfhörer an Person",
        "Lautsprecher draußen", "Instrument im Schaufenster", "Notenblatt sichtbar",
    ), "Das Musik-Element muss klar erkennbar sein. Bei Plakaten muss der Text lesbar sein.", CategoryGroup.CULTURE, CategoryDifficulty.EASY),
    CategoryTemplate("culture", "🎭", listOf(
        "Theater-Plakat", "Kino-Eingang", "Galerie-Schaufenster",
        "Tanzaufführung im Park", "Flohmarkt für Kunsthandwerk",
        "Bücherschrank zum Tauschen",
    ), "Das kulturelle Element muss klar erkennbar sein. Aktive Veranstaltungen oder erkennbare Orte.", CategoryGroup.CULTURE, CategoryDifficulty.MEDIUM),

    // ── Ungewöhnliches & Kurioses ─────────────────────────────────────────────
    CategoryTemplate("weird", "🤔", listOf(
        "Etwas völlig fehl am Platz", "Unerwartet lustiges Detail",
        "Aufschrift die keinen Sinn ergibt", "Sehr ungewöhnliche Farbkombination",
        "Objekt das falsch herum steht", "Tier an ungewöhnlichem Ort",
    ), "Das Kuriose muss im Foto für alle erkennbar sein. Die Abstimmung entscheidet, ob es wirklich \"seltsam\" genug ist.", CategoryGroup.CURIOUS, CategoryDifficulty.HARD),
    CategoryTemplate("miniature", "🔍", listOf(
        "Winziges Straßenkunst-Detail", "Mini-Figur in Gehwegritze",
        "Kleines verstecktes Schild", "Mikrokunst an Laternenpfahl",
        "Ganz kleines Graffiti-Tag", "Tiny door in Mauer eingebaut",
    ), "Das kleine Detail muss klar erkennbar und fokussiert sein. Ein Finger oder Münze als Größenvergleich ist ein Plus.", CategoryGroup.CURIOUS, CategoryDifficulty.HARD),
    CategoryTemplate("symmetry", "⚖️", listOf(
        "Perfekt symmetrische Fassade", "Spiegelbildliche Brücke im Wasser",
        "Zwei identische Bäume rechts/links", "Symmetrischer Torbogen",
        "Gespiegelte Fensterreihe", "Doppeltreppe links-rechts",
    ), "Die Symmetrieachse muss erkennbar sein und beide Hälften müssen vollständig im Bild sein.", CategoryGroup.GEOMETRY, CategoryDifficulty.MEDIUM),
)

const val VISIBLE_PRESET_COUNT = 12

val PRESET_CATEGORIES: List<Category> = CATEGORY_TEMPLATES.map { t ->
    Category(id = t.id, name = t.variants.random(), emoji = t.emoji, description = t.description)
}.shuffled()

// Lookup map: template id → description (used client-side without DB storage)
val CATEGORY_DESCRIPTIONS: Map<String, String> = CATEGORY_TEMPLATES.associate { it.id to it.description }

// Lookup maps for group and difficulty metadata
val CATEGORY_GROUPS: Map<String, CategoryGroup> = CATEGORY_TEMPLATES.associate { it.id to it.group }
val CATEGORY_DIFFICULTIES: Map<String, CategoryDifficulty> = CATEGORY_TEMPLATES.associate { it.id to it.difficulty }

// Categories grouped by type for the improved category picker
val CATEGORIES_BY_GROUP: Map<CategoryGroup, List<Category>> = PRESET_CATEGORIES.groupBy { cat ->
    CATEGORY_GROUPS[cat.id] ?: CategoryGroup.CURIOUS
}

// Quick preset combos for one-tap selection
data class CategoryPreset(val nameDe: String, val nameEn: String, val categoryIds: List<String>)

val CATEGORY_PRESETS = listOf(
    CategoryPreset("Stadtbummel", "City Walk", listOf("building", "door", "cafe", "sign", "street_art")),
    CategoryPreset("Naturwanderung", "Nature Hike", listOf("nature", "tree", "water", "bird", "sky")),
    CategoryPreset("Party-Modus", "Party Mode", listOf("funny_pose", "food", "colorful_scene", "funny_sign", "people")),
    CategoryPreset("Detektiv", "Detective", listOf("miniature", "manhole", "reflection", "pattern", "number")),
    CategoryPreset("Foto-Pro", "Photo Pro", listOf("shadow", "light", "symmetry", "mural", "sculpture")),
    CategoryPreset("Sport & Action", "Sport & Action", listOf("sport", "sport_field", "bicycle", "vehicle", "jogger")),
)

val PLAYER_COLORS = listOf(
    Color(0xFFEC4899), // hot pink
    Color(0xFFD946EF), // fuchsia
    Color(0xFFA855F7), // purple
    Color(0xFF7C3AED), // violet
    Color(0xFFF43F5E), // rose
    Color(0xFF22D3EE), // cyan
    Color(0xFFFB923C), // orange
    Color(0xFFFF6B6B), // coral
)
