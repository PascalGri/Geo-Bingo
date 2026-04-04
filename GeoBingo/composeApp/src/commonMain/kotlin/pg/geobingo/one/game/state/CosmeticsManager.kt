package pg.geobingo.one.game.state

import androidx.compose.ui.graphics.Color
import pg.geobingo.one.platform.AppSettings

/**
 * Profile frame definition.
 */
data class ProfileFrame(
    val id: String,
    val name: String,
    val description: String,
    val starsCost: Int,
    val borderColors: List<Color>,
    val borderWidth: Float = 2.5f,
    val animated: Boolean = false,
)

/**
 * Name effect definition.
 */
data class NameEffect(
    val id: String,
    val name: String,
    val description: String,
    val starsCost: Int,
    val gradientColors: List<Color>,
    val animated: Boolean = true,
)

/**
 * Player title definition.
 */
data class PlayerTitle(
    val id: String,
    val name: String,
    val description: String,
    val starsCost: Int,
    val color: Color,
)

/**
 * Bingo card design definition.
 */
data class CardDesign(
    val id: String,
    val name: String,
    val description: String,
    val starsCost: Int,
    val backgroundColors: List<Color>,
    val pattern: String = "solid",
)

/**
 * Manages cosmetic items: profile frames, animated names, player titles, and card designs.
 * All purchases persisted via AppSettings.
 */
object CosmeticsManager {
    private const val OWNED_PREFIX = "cosmetic_owned_"
    private const val EQUIPPED_FRAME = "cosmetic_equipped_frame"
    private const val EQUIPPED_NAME_EFFECT = "cosmetic_equipped_name_effect"
    const val EQUIPPED_TITLE = "cosmetic_equipped_title"
    const val EQUIPPED_CARD_DESIGN = "cosmetic_equipped_card_design"

    // ── Profile Frames ──────────────────────────────────────────────────
    val ALL_FRAMES = listOf(
        ProfileFrame("frame_none", "Standard", "Kein Rahmen", 0, listOf(Color.Transparent)),
        ProfileFrame("frame_gold", "Gold", "Goldener Rahmen", 50,
            listOf(Color(0xFFFBBF24), Color(0xFFF59E0B), Color(0xFFD97706)), animated = true),
        ProfileFrame("frame_rainbow", "Regenbogen", "Animierter Regenbogen-Rahmen", 100,
            listOf(Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF22C55E), Color(0xFF3B82F6), Color(0xFF8B5CF6)), animated = true),
        ProfileFrame("frame_ice", "Eis", "Kuehler blauer Rahmen", 40,
            listOf(Color(0xFF22D3EE), Color(0xFF6366F1)), animated = true),
        ProfileFrame("frame_fire", "Feuer", "Feuriger roter Rahmen", 60,
            listOf(Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFFBBF24)), animated = true),
        ProfileFrame("frame_nature", "Natur", "Gruener Natur-Rahmen", 40,
            listOf(Color(0xFF22C55E), Color(0xFF10B981), Color(0xFF059669))),
        ProfileFrame("frame_purple", "Mystisch", "Mystischer lila Rahmen", 50,
            listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)), animated = true),
        ProfileFrame("frame_diamond", "Diamant", "Seltener Diamant-Rahmen", 150,
            listOf(Color(0xFFE0F2FE), Color(0xFF7DD3FC), Color(0xFF38BDF8), Color(0xFFE0F2FE)), animated = true),
        ProfileFrame("frame_dark", "Schatten", "Dunkler eleganter Rahmen", 30,
            listOf(Color(0xFF374151), Color(0xFF1F2937), Color(0xFF111827))),
        ProfileFrame("frame_sunset", "Sonnenuntergang", "Warme Sonnenuntergang-Farben", 45,
            listOf(Color(0xFFF97316), Color(0xFFEC4899), Color(0xFF8B5CF6)), animated = true),
    )

    // ── Name Effects ────────────────────────────────────────────────────
    val ALL_NAME_EFFECTS = listOf(
        NameEffect("name_none", "Standard", "Normaler Name ohne Effekt", 0, listOf(Color.White)),
        NameEffect("name_gold", "Gold", "Goldener animierter Name", 80,
            listOf(Color(0xFFFBBF24), Color(0xFFF59E0B), Color(0xFFD97706))),
        NameEffect("name_rainbow", "Regenbogen", "Regenbogen-Farbverlauf", 120,
            listOf(Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF22C55E), Color(0xFF3B82F6), Color(0xFF8B5CF6))),
        NameEffect("name_ice", "Eis", "Kuehler blauer Farbverlauf", 60,
            listOf(Color(0xFF22D3EE), Color(0xFF6366F1))),
        NameEffect("name_fire", "Feuer", "Feuriger Farbverlauf", 80,
            listOf(Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFFBBF24))),
        NameEffect("name_neon", "Neon", "Neon-gruen leuchtend", 70,
            listOf(Color(0xFF4ADE80), Color(0xFF22D3EE))),
        NameEffect("name_purple", "Mystisch", "Lila-pinker Farbverlauf", 60,
            listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))),
        NameEffect("name_diamond", "Diamant", "Seltener Diamant-Effekt", 200,
            listOf(Color(0xFFE0F2FE), Color(0xFF7DD3FC), Color(0xFF38BDF8), Color(0xFFE0F2FE))),
    )

    // ── Player Titles ───────────────────────────────────────────────────
    val ALL_TITLES = listOf(
        PlayerTitle("title_none", "Standard", "Kein Titel", 0, Color(0xFFAAAAAA)),
        PlayerTitle("title_champion", "Champion", "Gewinne eine Runde", 80, Color(0xFFFBBF24)),
        PlayerTitle("title_profi", "Profi", "Fuer erfahrene Spieler", 100, Color(0xFF3B82F6)),
        PlayerTitle("title_legend", "Legende", "Seltenster Titel", 150, Color(0xFFEC4899)),
        PlayerTitle("title_newbie", "Neuling", "Fuer absolute Anfaenger", 0, Color(0xFF22C55E)),
        PlayerTitle("title_masterphotog", "Meisterfotograf", "Fuer Foto-Liebhaber", 120, Color(0xFF8B5CF6)),
        PlayerTitle("title_speedrunner", "Speedrunner", "Fuer Schnellste", 90, Color(0xFFF97316)),
        PlayerTitle("title_veteran", "Veteran", "Viele Runden gespielt", 70, Color(0xFF22D3EE)),
    )

    // ── Card Designs ─────────────────────────────────────────────────────
    val ALL_CARD_DESIGNS = listOf(
        CardDesign("card_none", "Standard", "Standard-Karte", 0,
            listOf(Color(0xFF1E293B), Color(0xFF1E293B)), "solid"),
        CardDesign("card_neon", "Neon", "Leuchtende Neon-Farben", 60,
            listOf(Color(0xFF0D1117), Color(0xFF4ADE80), Color(0xFF22D3EE)), "gradient"),
        CardDesign("card_nebula", "Nebula", "Kosmischer Sternennebel", 100,
            listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E)), "gradient"),
        CardDesign("card_ocean", "Ocean", "Tiefes Meeresblau", 80,
            listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)), "gradient"),
        CardDesign("card_lava", "Lava", "Gluehende Lavahitze", 90,
            listOf(Color(0xFF200122), Color(0xFFEF4444), Color(0xFFF97316)), "gradient"),
        CardDesign("card_forest", "Forest", "Ruhiger Waldweg", 70,
            listOf(Color(0xFF0A2E0A), Color(0xFF22C55E), Color(0xFF10B981)), "gradient"),
    )

    // ── Ownership ───────────────────────────────────────────────────────
    fun isOwned(id: String): Boolean {
        if (id == "frame_none" || id == "name_none" || id == "title_none" || id == "card_none") return true
        return AppSettings.getBoolean("$OWNED_PREFIX$id", false)
    }

    fun purchase(id: String) = AppSettings.setBoolean("$OWNED_PREFIX$id", true)

    // ── Equipped ────────────────────────────────────────────────────────
    fun getEquippedFrameId(): String = AppSettings.getString(EQUIPPED_FRAME, "frame_none")
    fun setEquippedFrame(id: String) = AppSettings.setString(EQUIPPED_FRAME, id)

    fun getEquippedNameEffectId(): String = AppSettings.getString(EQUIPPED_NAME_EFFECT, "name_none")
    fun setEquippedNameEffect(id: String) = AppSettings.setString(EQUIPPED_NAME_EFFECT, id)

    fun getEquippedTitleId(): String = AppSettings.getString(EQUIPPED_TITLE, "title_none")
    fun setEquippedTitle(id: String) = AppSettings.setString(EQUIPPED_TITLE, id)

    fun getEquippedCardDesignId(): String = AppSettings.getString(EQUIPPED_CARD_DESIGN, "card_none")
    fun setEquippedCardDesign(id: String) = AppSettings.setString(EQUIPPED_CARD_DESIGN, id)

    fun getEquippedFrame(): ProfileFrame =
        ALL_FRAMES.find { it.id == getEquippedFrameId() } ?: ALL_FRAMES.first()

    fun getEquippedNameEffect(): NameEffect =
        ALL_NAME_EFFECTS.find { it.id == getEquippedNameEffectId() } ?: ALL_NAME_EFFECTS.first()

    fun getEquippedTitle(): PlayerTitle =
        ALL_TITLES.find { it.id == getEquippedTitleId() } ?: ALL_TITLES.first()

    fun getEquippedCardDesign(): CardDesign =
        ALL_CARD_DESIGNS.find { it.id == getEquippedCardDesignId() } ?: ALL_CARD_DESIGNS.first()
}
