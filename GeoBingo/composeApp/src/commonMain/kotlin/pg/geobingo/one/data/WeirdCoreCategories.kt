package pg.geobingo.one.data

import pg.geobingo.one.platform.AppSettings

// ─────────────────────────────────────────────────────────────────────────────
//  Weird Core – absurde, ultra-nische Kategorien für den Weird-Core-Modus.
//  Kein "Schönstes Foto" – nur Kreativität und Treffsicherheit zählen.
//  Zwei Pools (outdoor / indoor) damit der Modus auch funktioniert wenn man
//  drinnen festsitzt oder draußen unterwegs ist.
// ─────────────────────────────────────────────────────────────────────────────

val WEIRD_CORE_OUTDOOR_CATEGORIES: List<Category> = listOf(
    Category("wc_phone", "Handy-Zombie", "wc_phone",
        "Fotografiere jemanden, der auf sein Handy starrt – von vorne oder seitlich."),
    Category("wc_sign", "Unlesbares Schild", "wc_sign",
        "Ein Schild, dessen Text du nicht lesen kannst – verblasst, verdeckt oder auf fremder Sprache."),
    Category("wc_shoe", "Verlassener Schuh", "wc_shoe",
        "Ein einzelner Schuh ganz alleine – kein Partner in Sicht."),
    Category("wc_waiting", "Jemand wartet", "wc_waiting",
        "Eine Person, die offensichtlich auf etwas wartet – Blick auf die Uhr, genervt, oder einfach nur da."),
    Category("wc_chair", "Stuhl auf der Straße", "wc_chair",
        "Ein Stuhl (oder Sessel) der einfach draußen steht, wo er eigentlich nicht hingehört."),
    Category("wc_mirror", "Unerwartetes Spiegelbild", "wc_mirror",
        "Dein Spiegelbild in einer Oberfläche, die kein Spiegel ist – Schaufenster, Pfütze, Autodach, ..."),
    Category("wc_pigeon", "Taube beim Fressen", "wc_pigeon",
        "Eine Taube oder andere Stadtvögel, die etwas Konkretes fressen – Krümel, Döner, whatever."),
    Category("wc_selfie_in_selfie", "Selfie im Selfie", "wc_selfie",
        "Fotografiere jemanden anderen, der gerade selbst ein Selfie macht."),
    Category("wc_stickers", "Sticker-Chaos", "wc_stickers",
        "Mindestens 3 verschiedene Sticker, die übereinander oder nebeneinander kleben."),
    Category("wc_floor_person", "Person sitzt auf dem Boden", "wc_floor",
        "Jemand sitzt direkt auf dem Boden – nicht auf einer Bank, nicht auf Stufen, einfach auf dem Boden."),
    Category("wc_boring", "Langweiligstens Gebäude", "wc_boring",
        "Das unscheinbarste, graueste, langweiligste Gebäude, das du finden kannst."),
    Category("wc_bag", "Vergessene Tüte", "wc_bag",
        "Eine Einkaufstüte oder Tasche, die irgendwo alleine steht oder liegt."),
    Category("wc_door", "Hässlichste Türklinke", "wc_door",
        "Die hässlichste, verrostetste oder seltsamste Türklinke, die du findest."),
    Category("wc_food_floor", "Essen auf dem Boden", "wc_food",
        "Essen oder Essensreste, die auf dem Boden liegen – Pommes, Eis, Brösel, ..."),
    Category("wc_obvious_sign", "Überflüssiges Verbotsschild", "wc_sign_no",
        "Ein Schild, das etwas total Offensichtliches verbietet oder warnt."),
    Category("wc_triangle", "Unbeabsichtigtes Dreieck", "wc_triangle",
        "Drei beliebige Objekte oder Linien, die zusammen eine Dreiecksform bilden – nicht gebaut als Dreieck."),
    Category("wc_matching", "Outfit matcht zur Umgebung", "wc_matching",
        "Eine Person, deren Kleidung zufällig perfekt zur Umgebung passt – Farbe, Muster, Stil."),
    Category("wc_small_big", "Winzig neben Riesig", "wc_scale",
        "Ein sehr kleines Objekt direkt neben einem sehr großen – der Größenunterschied muss krass sein."),
    Category("wc_flower_concrete", "Blume aus Beton", "wc_concrete",
        "Eine Blume oder Pflanze, die aus Beton, Asphalt oder einer Mauerspalte wächst."),
    Category("wc_too_many", "Zu viele davon", "wc_many",
        "Fotografiere eine absurd große Anzahl des gleichen Objekts – Fahrräder, Tüten, Stühle, ..."),
    Category("wc_npc", "NPC spotted", "wc_npc",
        "Eine Person, die sich bewegt oder steht, als wäre sie ein NPC in einem Videospiel."),
    Category("wc_wrong_place", "Falsch am falschen Ort", "wc_wrong",
        "Ein Objekt, das an diesem Ort absolut keinen Sinn ergibt – komplett fehl am Platz."),
    Category("wc_queue", "Sinnlose Schlange", "wc_queue",
        "Mehrere Personen, die in einer Schlange stehen – je seltsamer der Grund, desto besser."),
    Category("wc_cable", "Kabel-Wirrwarr", "wc_cable",
        "Ein möglichst chaotisches Durcheinander von Kabeln, Leitungen oder Drähten."),
    Category("wc_broken_umbrella", "Kaputtes Schirm-Skelett", "wc_umbrella",
        "Ein Regenschirm, der nach innen gestülpt ist oder dessen Streben rausstehen."),
    Category("wc_matching_twins", "Unabsichtliche Twins", "wc_twins",
        "Zwei fremde Personen, die zufällig fast gleich angezogen sind."),
    Category("wc_cat_window", "Katze im Fenster", "wc_cat",
        "Eine Katze, die aus einem Fenster schaut – je gelangweilter der Blick, desto besser."),
    Category("wc_post_it", "Post-it an seltsamem Ort", "wc_postit",
        "Ein Zettel oder Post-it an einem Ort, der eigentlich keinen Sinn ergibt."),
    Category("wc_cloud_shape", "Wolke die nach etwas aussieht", "wc_cloud",
        "Eine Wolke, die eindeutig die Form von etwas Konkretem hat – und fotografierbar ist."),
    Category("wc_shadow_art", "Schatten der lügt", "wc_shadow_art",
        "Ein Schatten, der eine komplett andere Form hat als das Objekt das ihn wirft."),
    Category("wc_expired", "Ablaufdatum abgelaufen", "wc_expired",
        "Ein Produkt, Schild oder Aushang, dessen Datum schon längst vorbei ist."),
    Category("wc_locked_bike", "Absurd gesichertes Fahrrad", "wc_locked_bike",
        "Ein Fahrrad, das mit übertrieben vielen Schlössern oder an einem total falschen Objekt gesichert ist."),
    Category("wc_phone_charger", "Handy lädt in der Öffentlichkeit", "wc_charging",
        "Jemand der sein Handy an einer öffentlichen Steckdose lädt – Café, Zug, Straße, ..."),
    Category("wc_single_glove", "Einzelner Handschuh", "wc_glove",
        "Ein einzelner Handschuh, der irgendwo alleine liegt oder hängt."),
    Category("wc_graffiti_name", "Graffiti-Eigenname", "wc_graffiti_name",
        "Graffiti, das einfach nur einen Vornamen schreibt – kein Tag, kein Artwork, nur z.B. 'KEVIN'."),
    Category("wc_upside_down", "Verkehrt herum", "wc_upsidedown",
        "Irgendein Objekt, das offensichtlich falsch herum steht, hängt oder liegt."),
    Category("wc_plant_indoor", "Pflanze die kämpft", "wc_plant_fight",
        "Eine Zimmerpflanze draußen oder eine Wildpflanze, die aussieht als würde sie ums Überleben kämpfen."),
    Category("wc_random_award", "Pokal oder Trophäe irgendwo", "wc_award",
        "Ein Pokal, eine Medaille oder Trophäe, die zufällig irgendwo im öffentlichen Raum steht."),
    Category("wc_door_no_steps", "Tür ohne Treppe", "wc_door_steps",
        "Eine Tür, die ohne Stufen oder Rampe in 1+ Meter Höhe in einer Wand sitzt."),
    Category("wc_someone_sleeping", "Jemand schläft", "wc_sleeping",
        "Eine Person, die in der Öffentlichkeit schläft – Bank, Bus, Boden – nicht im Bett."),
    Category("wc_brand_copy", "Fake-Logo oder Tippfehler-Marke", "wc_fakebrand",
        "Ein Logo oder Markenname, der fast richtig ist – aber eben nur fast."),
    Category("wc_identical_cars", "Gleiche Autos nebeneinander", "wc_twins_car",
        "Mindestens zwei identische Autos (Marke + Farbe) direkt nebeneinander geparkt."),
    Category("wc_dog_owner_twin", "Hund sieht aus wie Herrchen", "wc_dog_twin",
        "Ein Hund und sein Besitzer, die sich optisch so ähnlich sind, dass es schon komisch ist."),
    Category("wc_no_entry_entered", "Verboten aber trotzdem drin", "wc_no_entry",
        "Ein 'Zutritt verboten'-Schild, hinter dem offensichtlich jemand war oder ist."),

    // ── Runde 3 (expansion 2026-05-16) ─────────────────────────────────────
    Category("wc_one_chair_pile", "Stuhl auf Stuhl", "wc_chair_stack",
        "Mindestens drei Stühle, die aufeinander gestapelt im Freien stehen."),
    Category("wc_unfinished", "Halbfertige Baustelle ohne Bauarbeiter", "wc_construction",
        "Eine Baustelle, die offensichtlich pausiert ist – Werkzeuge da, niemand arbeitet."),
    Category("wc_pet_costume", "Tier mit Kleidungsstück", "wc_pet_clothes",
        "Ein Hund, eine Katze, ein Tier mit Pulli, Mantel, Schuhen oder anderem an."),
    Category("wc_perfect_parallel", "Perfekt parallel geparkt", "wc_parallel",
        "Mindestens drei Fahrzeuge in absurd perfekter paralleler Anordnung."),
    Category("wc_sad_balloon", "Trauriger Luftballon", "wc_balloon",
        "Ein halb-entleerter oder verlorener Luftballon, der irgendwo traurig rumhängt."),
    Category("wc_door_to_nowhere", "Tür ins Nichts", "wc_nothing_door",
        "Eine Tür, die offensichtlich nirgendwo hinführt – an einer Wand, im Wald, an einem leeren Gerüst."),
    Category("wc_lonely_table", "Verlassener Tisch", "wc_lonely_table",
        "Ein Tisch (Café, Park, Straße) komplett alleine ohne Stuhl, ohne Gäste, ohne Sinn."),
    Category("wc_too_many_locks", "Schloss am Schloss", "wc_lock_lock",
        "Ein Objekt mit mehreren Schlössern dran – Geländer, Türen, Schränke draußen, Liebesschlösser-Brücke."),

    // ── Runde 4 (expansion 2026-06-02) ─────────────────────────────────────
    Category("wc_puddle", "Pfütze als Spiegel", "wc_puddle",
        "Eine Pfütze, in der sich Himmel, Gebäude oder du selbst spiegeln."),
    Category("wc_traffic_cone", "Einsamer Pylon", "wc_traffic_cone",
        "Ein Verkehrshütchen (Pylon), das völlig sinnlos irgendwo alleine steht."),
    Category("wc_overflow_bin", "Überquellender Mülleimer", "wc_overflow_bin",
        "Ein öffentlicher Mülleimer, der so voll ist, dass alles oben rausquillt."),
    Category("wc_gum_art", "Kaugummi-Muster am Boden", "wc_gum_art",
        "Festgetretene Kaugummis auf dem Gehweg, die zufällig ein Muster bilden."),
    Category("wc_face_object", "Gesicht in einem Ding", "wc_face_object",
        "Ein Objekt, das zufällig wie ein Gesicht aussieht – Steckdose, Auto, Hauswand (Pareidolie)."),
    Category("wc_shopping_cart", "Verirrter Einkaufswagen", "wc_shopping_cart",
        "Ein Einkaufswagen weit weg von jedem Supermarkt – im Fluss, Park, Gebüsch."),
    Category("wc_one_window_light", "Ein Fenster leuchtet", "wc_one_window_light",
        "Eine dunkle Häuserfront, bei der nur ein einziges Fenster beleuchtet ist."),
    Category("wc_weird_bollard", "Skurriler Poller", "wc_weird_bollard",
        "Ein Poller oder Pömpel, der bemalt, beklebt oder völlig verbeult ist."),
    Category("wc_taped_thing", "Mit Tape repariert", "wc_taped_thing",
        "Irgendetwas in der Öffentlichkeit, das notdürftig mit Klebeband geflickt wurde."),
    Category("wc_plant_takeover", "Natur erobert zurück", "wc_plant_takeover",
        "Pflanzen, die ein menschliches Objekt überwuchern – Auto, Schild, Bank, Zaun."),
    Category("wc_lost_key", "Verlorener Schlüssel", "wc_lost_key",
        "Ein einzelner Schlüssel oder Schlüsselbund, den jemand irgendwo liegen oder hängen gelassen hat."),
    Category("wc_laundry_mix", "Absurde Wäscheleine", "wc_laundry_mix",
        "Eine Wäscheleine draußen mit einer absurden Kombination an Wäschestücken."),
    Category("wc_crooked_sign", "Schiefes Schild", "wc_crooked_sign",
        "Ein Verkehrs- oder Straßenschild, das komplett schief steht oder verbogen ist."),
    Category("wc_animal_statue", "Tier-Statue zu ernst genommen", "wc_animal_statue",
        "Eine Tier-Statue oder -Figur, die jemand wie ein echtes Tier behandelt – oder die einfach absurd platziert ist."),
)

val WEIRD_CORE_INDOOR_CATEGORIES: List<Category> = listOf(
    Category("wci_socks", "Verlassene Socke", "wci_socks",
        "Eine einzelne Socke, die alleine irgendwo rumliegt – nicht im Wäschekorb."),
    Category("wci_remote", "Fernbedienung an seltsamer Stelle", "wci_remote",
        "Eine Fernbedienung an einem Ort, wo sie definitiv nicht hingehört."),
    Category("wci_unread_books", "Bücher die nie gelesen wurden", "wci_books",
        "Bücher, die offensichtlich nur Deko sind – staubig, makellos, oder verkehrt herum."),
    Category("wci_charger_chaos", "Ladekabel-Schlange", "wci_cables",
        "Mindestens 3 verschiedene Ladekabel, die sich gegenseitig verheddert haben."),
    Category("wci_fridge_photo", "Foto am Kühlschrank", "wci_fridge",
        "Ein Foto oder Magnetbild an einem Kühlschrank – je peinlicher, desto besser."),
    Category("wci_plant_dying", "Pflanze die langsam stirbt", "wci_dying_plant",
        "Eine Zimmerpflanze, die deutlich Hilfe brauchen würde – braune Blätter, kein Wasser, traurig."),
    Category("wci_appliance_old", "Gerät aus dem letzten Jahrtausend", "wci_old_device",
        "Ein elektronisches Gerät zuhause, das offensichtlich älter als die Person ist, die es benutzt."),
    Category("wci_wrong_room", "Gegenstand im falschen Raum", "wci_wrong_room",
        "Etwas Küchen-Mässiges im Bad, Bad-Sachen im Schlafzimmer, etc. – komplett fehl am Platz."),
    Category("wci_dust_bunny", "Staubmonster", "wci_dust",
        "Eine sichtbare Ansammlung von Staub, die schon fast eigene Form angenommen hat."),
    Category("wci_overstuffed_drawer", "Schublade voll Chaos", "wci_drawer",
        "Eine Schublade, die du öffnest – innen herrscht Anarchie."),
    Category("wci_mug_collection", "Tassen-Sammlung", "wci_mugs",
        "Mindestens 4 sehr unterschiedliche Tassen, die zusammen stehen oder hängen."),
    Category("wci_mirror_dust", "Spiegel der Lügen erzählt", "wci_smudged_mirror",
        "Ein Spiegel mit Fingerabdrücken, Schlieren oder Staub – kein klares Spiegelbild."),
    Category("wci_three_remotes", "Drei Fernbedienungen", "wci_three_remotes",
        "Drei oder mehr Fernbedienungen, die nebeneinander oder durcheinander liegen."),
    Category("wci_unused_kitchen", "Küchengerät das niemand benutzt", "wci_kitchen_gadget",
        "Eine Küchenmaschine, ein Gadget, ein Aufsatz – der offensichtlich nur staubt."),
    Category("wci_decorative_cushion", "Sinnloses Deko-Kissen", "wci_cushion",
        "Ein Kissen, das nur Deko ist – nicht zum Sitzen, nicht zum Schlafen."),
    Category("wci_burnt_pan", "Verbrannte Pfanne", "wci_burnt",
        "Eine Pfanne oder ein Topf mit Brandspuren – Story dahinter optional."),
    Category("wci_random_box", "Karton der irgendwo steht", "wci_box",
        "Ein Karton, der seit gefühlt Wochen einfach im Raum steht – Inhalt unklar."),
    Category("wci_calendar_old", "Alter Kalender", "wci_old_calendar",
        "Ein Wandkalender, der offensichtlich nicht mehr aktuell ist."),
    Category("wci_no_handle", "Tür ohne Klinke", "wci_no_handle",
        "Eine Tür drinnen ohne Klinke – Schrank, Geheimtür, oder einfach kaputt."),
    Category("wci_lost_pen", "Stift den niemand vermisst", "wci_pen",
        "Ein einzelner Stift, der irgendwo rumliegt und gefühlt seit Jahren ungenutzt ist."),
    Category("wci_phone_face_down", "Handy mit Display nach unten", "wci_phone_down",
        "Ein Handy auf irgendeiner Oberfläche, Display nach unten – Vertrauen oder Misstrauen?"),
    Category("wci_three_chargers", "Drei Ladegeräte im selben Raum", "wci_three_chargers",
        "Mindestens drei Ladegeräte im selben Zimmer – stecken oder liegen."),

    // ── Runde 2 (expansion 2026-06-02) ─────────────────────────────────────
    Category("wci_dryer_sock", "Trockner-Opfer", "wci_dryer_sock",
        "Eine einzelne Socke nach dem Wäschemachen – ihr Partner ist für immer verschwunden."),
    Category("wci_tv_cables", "Kabelsalat hinterm TV", "wci_tv_cables",
        "Das Kabelchaos hinter Fernseher, Schreibtisch oder Router."),
    Category("wci_fridge_empty", "Fast leerer Kühlschrank", "wci_fridge_empty",
        "Ein Kühlschrank, in dem fast nichts mehr ist – nur ein einsames Objekt."),
    Category("wci_charger_empty", "Ladegerät ohne Gerät", "wci_charger_empty",
        "Ein Ladekabel, das eingesteckt ist, aber an nichts angeschlossen."),
    Category("wci_spare_screw", "Übrige Schraube", "wci_spare_screw",
        "Eine einzelne Schraube oder ein Teil, das nach einem Möbelaufbau übrig blieb."),
    Category("wci_overloaded_hook", "Überladener Haken", "wci_overloaded_hook",
        "Ein Garderoben- oder Türhaken, an dem viel zu viel hängt."),
    Category("wci_expired_food", "Abgelaufenes im Schrank", "wci_expired_food",
        "Ein Lebensmittel, dessen Datum längst überschritten ist – aber noch im Schrank steht."),
    Category("wci_tangled_earphones", "Verknotete Kopfhörer", "wci_tangled_earphones",
        "Ein Paar kabelgebundene Kopfhörer, perfekt zu einem Knoten verheddert."),
    Category("wci_dead_batteries", "Sammlung leerer Batterien", "wci_dead_batteries",
        "Mehrere alte Batterien, die rumliegen, weil keiner sie wegbringt."),
    Category("wci_glove_indoor", "Einzelner Handschuh drinnen", "wci_glove_indoor",
        "Ein einzelner Handschuh, der drinnen liegt – Winter-, Putz- oder Gartenhandschuh."),
    Category("wci_pillow_fort", "Kissenburg", "wci_pillow_fort",
        "Ein Bett oder Sofa mit absurd vielen Kissen drauf."),
    Category("wci_mystery_stain", "Fleck unbekannter Herkunft", "wci_mystery_stain",
        "Ein Fleck an Wand, Decke oder Boden, den niemand erklären kann."),
)

// ── Weird Core solo selector ─────────────────────────────────────────────────
private const val RECENT_WC_OUTDOOR_IDS_KEY = "wc_recent_outdoor_ids"
private const val RECENT_WC_INDOOR_IDS_KEY = "wc_recent_indoor_ids"

/**
 * Selects [count] Weird-Core categories for the solo Weird-Core mode, preferring
 * ones not used in recent runs (same anti-repeat strategy as [soloCategories]).
 */
fun weirdCoreCategories(outdoor: Boolean, count: Int = 5): List<Category> {
    val pool = if (outdoor) WEIRD_CORE_OUTDOOR_CATEGORIES else WEIRD_CORE_INDOOR_CATEGORIES
    val historyKey = if (outdoor) RECENT_WC_OUTDOOR_IDS_KEY else RECENT_WC_INDOOR_IDS_KEY

    val recentRaw = AppSettings.getString(historyKey, "")
    val recentIds = if (recentRaw.isBlank()) emptySet() else recentRaw.split(",").toSet()

    val fresh = pool.filter { it.id !in recentIds }.shuffled()
    val stale = pool.filter { it.id in recentIds }.shuffled()
    val selected = (fresh + stale).take(count)

    val maxHistory = count * 3
    val newHistory = (selected.map { it.id } + recentIds.toList()).take(maxHistory)
    AppSettings.setString(historyKey, newHistory.joinToString(","))

    return selected
}

// Backwards-compatible alias for any caller that hasn't been ported to
// the outdoor/indoor split yet. New code should use the two pools above.
@Deprecated(
    "Use WEIRD_CORE_OUTDOOR_CATEGORIES (or WEIRD_CORE_INDOOR_CATEGORIES) explicitly.",
    ReplaceWith("WEIRD_CORE_OUTDOOR_CATEGORIES"),
)
val WEIRD_CORE_CATEGORIES: List<Category> = WEIRD_CORE_OUTDOOR_CATEGORIES
