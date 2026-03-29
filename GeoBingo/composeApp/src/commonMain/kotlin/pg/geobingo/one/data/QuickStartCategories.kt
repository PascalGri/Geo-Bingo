package pg.geobingo.one.data

// ─────────────────────────────────────────────────────────────────────────────
//  Quick Start – größerer Pool; bei Rundenstart werden 5 zufällig gewählt.
//  Alle Kategorien sind einfach in 15 Minuten findbar.
// ─────────────────────────────────────────────────────────────────────────────

private val QUICK_START_OUTDOOR_POOL: List<Category> = listOf(
    Category("qs_out_red", "Rotes Objekt", "qs_out_red",
        "Fotografiere irgendetwas Rotes draußen – Auto, Tür, Schild oder Kleidungsstück."),
    Category("qs_out_bike", "Geparktes Fahrrad", "qs_out_bike",
        "Finde ein abgestelltes Fahrrad und fotografiere es."),
    Category("qs_out_animal", "Tier", "qs_out_animal",
        "Hund, Katze, Taube, Eichhörnchen – irgendein Tier zählt."),
    Category("qs_out_shadow", "Dein eigener Schatten", "qs_out_shadow",
        "Fotografiere deinen Schatten auf dem Boden oder an einer Wand."),
    Category("qs_out_tree", "Baum oder Strauch", "qs_out_tree",
        "Fotografiere einen Baum oder Strauch – mit Blättern, Blüten oder kahl."),
    Category("qs_out_sign", "Straßen- oder Hinweisschild", "qs_out_sign",
        "Irgendein Schild draußen – Straßenname, Hinweis, Verbot."),
    Category("qs_out_puddle", "Pfütze oder Wasseransammlung", "qs_out_puddle",
        "Finde eine Pfütze, einen Brunnen oder eine andere Wasseransammlung."),
    Category("qs_out_bench", "Bank oder Sitzgelegenheit", "qs_out_bench",
        "Eine Bank, ein Mäuerchen oder ein anderer Platz zum Sitzen draußen."),
    Category("qs_out_blue_sky", "Blauer Himmel mit Wolke", "qs_out_blue_sky",
        "Fotografiere den Himmel – mindestens eine Wolke muss zu sehen sein."),
    Category("qs_out_door", "Auffällige Tür", "qs_out_door",
        "Eine Tür mit besonderer Farbe, Verzierung oder Form."),
    Category("qs_out_graffiti", "Graffiti oder Sticker", "qs_out_graffiti",
        "Irgendwo draußen: Graffiti, Aufkleber oder Straßenkunst."),
    Category("qs_out_steps", "Treppe oder Stufen", "qs_out_steps",
        "Fotografiere eine Treppe oder Stufen – drinnen zählt nicht."),
)

private val QUICK_START_INDOOR_POOL: List<Category> = listOf(
    Category("qs_in_blue", "Etwas Blaues", "qs_in_blue",
        "Fotografiere ein blaues Objekt – Kleidung, Dekoration, Tasse oder was du findest."),
    Category("qs_in_clock", "Uhr", "qs_in_clock",
        "Finde eine Uhr – analog oder digital, Wand, Tisch oder Handgelenk."),
    Category("qs_in_plant", "Pflanze", "qs_in_plant",
        "Fotografiere eine echte Pflanze oder Blume – auch im Topf."),
    Category("qs_in_window", "Fenster mit Ausblick", "qs_in_window",
        "Fotografiere durch ein Fenster – der Ausblick muss sichtbar sein."),
    Category("qs_in_reflection", "Spiegelbild", "qs_in_reflection",
        "Dein Spiegelbild in einem Spiegel, Fenster oder glänzender Oberfläche."),
    Category("qs_in_book", "Buch oder Heft", "qs_in_book",
        "Fotografiere ein Buch, Heft oder Zeitschrift – Titel muss lesbar sein."),
    Category("qs_in_stripes", "Etwas mit Streifen", "qs_in_stripes",
        "Gestreiftes Objekt oder Muster – Kleidung, Vorhang, Fliesen, egal."),
    Category("qs_in_round", "Etwas Rundes", "qs_in_round",
        "Ein runder Gegenstand – Teller, Uhr, Lampe, Ball oder ähnliches."),
    Category("qs_in_cable", "Kabel oder Steckdose", "qs_in_cable",
        "Fotografiere ein Kabel, Ladegerät oder eine Steckdose."),
    Category("qs_in_food", "Essen oder Getränk", "qs_in_food",
        "Irgendwas Essbares oder Trinkbares – offen oder verpackt."),
    Category("qs_in_chair", "Stuhl oder Hocker", "qs_in_chair",
        "Irgendeine Sitzgelegenheit drinnen – Stuhl, Hocker, Sessel."),
    Category("qs_in_number", "Zahl irgendwo", "qs_in_number",
        "Fotografiere eine Zahl, die irgendwo zu sehen ist – Uhr, Schild, Aufschrift."),
)

fun quickStartCategories(outdoor: Boolean): List<Category> {
    val pool = if (outdoor) QUICK_START_OUTDOOR_POOL else QUICK_START_INDOOR_POOL
    return pool.shuffled().take(5)
}
