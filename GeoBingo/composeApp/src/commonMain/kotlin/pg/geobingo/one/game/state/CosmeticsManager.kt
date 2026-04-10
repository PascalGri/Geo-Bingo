package pg.geobingo.one.game.state

import androidx.compose.ui.graphics.Color
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import pg.geobingo.one.network.AccountManager
import pg.geobingo.one.network.supabase
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.util.AppLogger

// ────────────────────────────────────────────────────────────────────────────
// Cosmetic data types
// ────────────────────────────────────────────────────────────────────────────

/** Profile frame definition (renders as a colored ring/border around the avatar). */
data class ProfileFrame(
    val id: String,
    val name: String,
    val description: String,
    val starsCost: Int,
    val borderColors: List<Color>,
    val borderWidth: Float = 2.5f,
    val animated: Boolean = false,
)

/** Animated name gradient. */
data class NameEffect(
    val id: String,
    val name: String,
    val description: String,
    val starsCost: Int,
    val gradientColors: List<Color>,
    val animated: Boolean = true,
)

/** Player title — small badge shown next to / under the player name. */
data class PlayerTitle(
    val id: String,
    val name: String,
    val description: String,
    val starsCost: Int,
    val color: Color,
)

/** Bingo card design (board background). */
data class CardDesign(
    val id: String,
    val name: String,
    val description: String,
    val starsCost: Int,
    val backgroundColors: List<Color>,
    val pattern: String = "solid",
)

/**
 * Rocket-League-style banner background. Renders as a gradient (and optional accent
 * overlay) underneath the player's avatar + name + title in lobbies, results,
 * leaderboards and the profile screen.
 */
data class BannerBackground(
    val id: String,
    val name: String,
    val description: String,
    val starsCost: Int,
    val gradientColors: List<Color>,
    val accentColor: Color = Color.Transparent,
    val animated: Boolean = false,
)

// ────────────────────────────────────────────────────────────────────────────
// CosmeticsManager
// ────────────────────────────────────────────────────────────────────────────

/**
 * Manages cosmetic items: profile frames, name effects, titles, card designs and
 * banner backgrounds. Ownership and equipped state are synced via Supabase
 * (`profiles.equipped_*` columns + `owned_cosmetics` table) and mirrored locally
 * via [AppSettings] for offline reads.
 */
object CosmeticsManager {
    private const val TAG = "CosmeticsManager"

    private const val OWNED_PREFIX = "cosmetic_owned_"
    private const val EQUIPPED_FRAME = "cosmetic_equipped_frame"
    private const val EQUIPPED_NAME_EFFECT = "cosmetic_equipped_name_effect"
    const val EQUIPPED_TITLE = "cosmetic_equipped_title"
    const val EQUIPPED_CARD_DESIGN = "cosmetic_equipped_card_design"
    private const val EQUIPPED_BANNER = "cosmetic_equipped_banner"
    private const val MIGRATED_TO_CLOUD = "cosmetic_migrated_to_cloud_v1"

    // Per-app coroutine scope for fire-and-forget sync writes.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
        // ── New in v1.3 ────────────────────────────────────────────────
        ProfileFrame("frame_emerald", "Smaragd", "Funkelnder Smaragd-Rahmen", 80,
            listOf(Color(0xFF34D399), Color(0xFF059669), Color(0xFF064E3B)), animated = true),
        ProfileFrame("frame_ruby", "Rubin", "Tiefroter Rubin", 90,
            listOf(Color(0xFFFCA5A5), Color(0xFFEF4444), Color(0xFF7F1D1D)), animated = true),
        ProfileFrame("frame_obsidian", "Obsidian", "Schwarz-pink schimmernd", 110,
            listOf(Color(0xFF000000), Color(0xFF1F2937), Color(0xFFEC4899)), animated = true),
        ProfileFrame("frame_solar", "Solar", "Goldene Sonnenstrahlen", 130,
            listOf(Color(0xFFFFFBEB), Color(0xFFFBBF24), Color(0xFFF59E0B), Color(0xFFB45309)), animated = true),
        ProfileFrame("frame_void", "Void", "Galaktisches Schwarzlicht", 200,
            listOf(Color(0xFF6D28D9), Color(0xFF1E1B4B), Color(0xFF06B6D4), Color(0xFF6D28D9)), animated = true),
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
        // ── New in v1.3 ────────────────────────────────────────────────
        NameEffect("name_emerald", "Smaragd", "Smaragdgrüner Farbverlauf", 90,
            listOf(Color(0xFF34D399), Color(0xFF059669), Color(0xFF065F46))),
        NameEffect("name_sunset", "Sonnenuntergang", "Warme Sonnenfarben", 75,
            listOf(Color(0xFFFBBF24), Color(0xFFF97316), Color(0xFFEC4899))),
        NameEffect("name_galaxy", "Galaxie", "Kosmische Farben", 150,
            listOf(Color(0xFF6D28D9), Color(0xFF06B6D4), Color(0xFFEC4899), Color(0xFF6D28D9))),
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
        // ── New in v1.3 ────────────────────────────────────────────────
        PlayerTitle("title_explorer", "Entdecker", "Hat viele Orte besucht", 80, Color(0xFF10B981)),
        PlayerTitle("title_streetpro", "Strassenprofi", "Kennt jede Ecke", 90, Color(0xFFF59E0B)),
        PlayerTitle("title_hunter", "Jäger", "Auf der Jagd nach Punkten", 85, Color(0xFFDC2626)),
        PlayerTitle("title_collector", "Sammler", "Sammelt alles", 70, Color(0xFF7C3AED)),
        PlayerTitle("title_unstoppable", "Unaufhaltsam", "Niemand kann dich stoppen", 180, Color(0xFFEF4444)),
        PlayerTitle("title_godlike", "Göttlich", "Über allem stehend", 250, Color(0xFFFFD700)),
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

    // ── Banner Backgrounds (NEW in v1.3) ────────────────────────────────
    val ALL_BANNER_BACKGROUNDS = listOf(
        BannerBackground("banner_none", "Standard", "Schlichter dunkler Hintergrund", 0,
            listOf(Color(0xFF1E293B), Color(0xFF0F172A))),
        BannerBackground("banner_aurora", "Aurora", "Polarlicht-Effekt", 60,
            listOf(Color(0xFF0F172A), Color(0xFF1E40AF), Color(0xFF06B6D4), Color(0xFF10B981)), animated = true),
        BannerBackground("banner_sunset", "Sonnenuntergang", "Warme Sonnenfarben", 50,
            listOf(Color(0xFFFBBF24), Color(0xFFF97316), Color(0xFFEC4899)), animated = false),
        BannerBackground("banner_lava", "Lavafluss", "Glühende Lava", 70,
            listOf(Color(0xFF7F1D1D), Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFFBBF24)), animated = true),
        BannerBackground("banner_ocean", "Ozean", "Tiefes Meeresblau", 60,
            listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364), Color(0xFF22D3EE)), animated = true),
        BannerBackground("banner_forest", "Wald", "Tiefer Wald bei Nacht", 50,
            listOf(Color(0xFF0A2E0A), Color(0xFF15803D), Color(0xFF22C55E))),
        BannerBackground("banner_galaxy", "Galaxie", "Sternenhimmel", 120,
            listOf(Color(0xFF1E1B4B), Color(0xFF4C1D95), Color(0xFF6D28D9), Color(0xFFEC4899)), animated = true),
        BannerBackground("banner_gold", "Gold", "Goldene Eleganz", 100,
            listOf(Color(0xFF78350F), Color(0xFFB45309), Color(0xFFFBBF24), Color(0xFFFEF3C7)), animated = true),
        BannerBackground("banner_ice", "Eis", "Kalte Eiskristalle", 70,
            listOf(Color(0xFF0C4A6E), Color(0xFF0EA5E9), Color(0xFF7DD3FC)), animated = true),
        BannerBackground("banner_diamond", "Diamant", "Seltener Diamant-Glanz", 200,
            listOf(Color(0xFFE0F2FE), Color(0xFF7DD3FC), Color(0xFF38BDF8), Color(0xFFE0F2FE), Color(0xFFFFFFFF)), animated = true),
        BannerBackground("banner_void", "Void", "Leere zwischen den Sternen", 250,
            listOf(Color(0xFF000000), Color(0xFF6D28D9), Color(0xFF000000), Color(0xFF06B6D4)), animated = true),
        BannerBackground("banner_neon_city", "Neonstadt", "Cyberpunk-Vibes", 90,
            listOf(Color(0xFF0F0F1E), Color(0xFF7C3AED), Color(0xFFEC4899), Color(0xFF06B6D4)), animated = true),
    )

    // ────────────────────────────────────────────────────────────────────
    // Lookup helpers
    // ────────────────────────────────────────────────────────────────────

    fun frameById(id: String?): ProfileFrame =
        ALL_FRAMES.find { it.id == id } ?: ALL_FRAMES.first()

    fun nameEffectById(id: String?): NameEffect =
        ALL_NAME_EFFECTS.find { it.id == id } ?: ALL_NAME_EFFECTS.first()

    fun titleById(id: String?): PlayerTitle =
        ALL_TITLES.find { it.id == id } ?: ALL_TITLES.first()

    fun cardDesignById(id: String?): CardDesign =
        ALL_CARD_DESIGNS.find { it.id == id } ?: ALL_CARD_DESIGNS.first()

    fun bannerBackgroundById(id: String?): BannerBackground =
        ALL_BANNER_BACKGROUNDS.find { it.id == id } ?: ALL_BANNER_BACKGROUNDS.first()

    // ────────────────────────────────────────────────────────────────────
    // Ownership — backed by `owned_cosmetics` (cloud) + AppSettings (mirror)
    // ────────────────────────────────────────────────────────────────────

    private val freeIds = setOf("frame_none", "name_none", "title_none", "card_none", "banner_none")

    fun isOwned(id: String): Boolean {
        if (id in freeIds) return true
        return AppSettings.getBoolean("$OWNED_PREFIX$id", false)
    }

    /**
     * Mark an item as purchased. Writes locally immediately and pushes to the
     * `owned_cosmetics` table in the background if logged in.
     *
     * Use [purchaseCosmeticCloud] for a server-authoritative flow that also
     * deducts stars atomically via the `purchase_cosmetic` RPC.
     */
    fun purchase(id: String) {
        AppSettings.setBoolean("$OWNED_PREFIX$id", true)
        val userId = AccountManager.currentUserId ?: return
        scope.launch {
            try {
                supabase.from("owned_cosmetics").insert(
                    OwnedCosmeticInsert(user_id = userId, cosmetic_id = id),
                )
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to sync purchase to cloud: $id", e)
            }
        }
    }

    /**
     * Result of a cloud purchase attempt.
     */
    sealed class PurchaseResult {
        data class Success(val newStarBalance: Int) : PurchaseResult()
        object InsufficientStars : PurchaseResult()
        object NotAuthenticated : PurchaseResult()
        object UnknownCosmetic : PurchaseResult()
        data class Error(val message: String) : PurchaseResult()
    }

    /**
     * Atomically purchase a cosmetic via the `purchase_cosmetic` Postgres RPC.
     * The server-side function verifies the cost from the `cosmetics_catalog`
     * table, deducts stars from the profile, and inserts the ownership row —
     * all in a single transaction. Client cannot fake cost=0.
     *
     * On success:
     * - Updates the local `owned_` mirror so the shop UI updates instantly
     * - Returns the authoritative new star balance; caller should update
     *   [pg.geobingo.one.game.state.StarsState] via [StarsState.setBalance]
     */
    suspend fun purchaseCosmeticCloud(cosmeticId: String): PurchaseResult {
        if (AccountManager.currentUserId == null) return PurchaseResult.NotAuthenticated
        return try {
            val payload = buildJsonObject { put("p_cosmetic_id", cosmeticId) }
            val result = supabase.postgrest.rpc("purchase_cosmetic", payload)
            val body = result.data
            val newBalance = Json.parseToJsonElement(body).toString().trim('"').toIntOrNull()
                ?: return PurchaseResult.Error("invalid_response: $body")
            // Mirror locally
            AppSettings.setBoolean("$OWNED_PREFIX$cosmeticId", true)
            PurchaseResult.Success(newBalance)
        } catch (e: Exception) {
            val msg = e.message ?: ""
            AppLogger.w(TAG, "purchase_cosmetic RPC failed: $msg", e)
            when {
                msg.contains("insufficient_stars") -> PurchaseResult.InsufficientStars
                msg.contains("unknown_cosmetic") -> PurchaseResult.UnknownCosmetic
                msg.contains("not_authenticated") -> PurchaseResult.NotAuthenticated
                else -> PurchaseResult.Error(msg)
            }
        }
    }

    @Serializable
    private data class OwnedCosmeticInsert(
        val user_id: String,
        val cosmetic_id: String,
    )

    @Serializable
    private data class OwnedCosmeticRow(
        val cosmetic_id: String = "",
    )

    @Serializable
    private data class EquippedUpdate(
        val equipped_frame: String? = null,
        val equipped_name_effect: String? = null,
        val equipped_title: String? = null,
        val equipped_card_design: String? = null,
        val equipped_banner_background: String? = null,
    )

    // ────────────────────────────────────────────────────────────────────
    // Equipped — backed by profiles.equipped_* (cloud) + AppSettings (mirror)
    // Each setter writes locally first (instant UI feedback) then patches the
    // profile row in the background. Each getter reads from local mirror.
    // ────────────────────────────────────────────────────────────────────

    private val _equippedRevision = MutableStateFlow(0L)
    /** Bumps whenever any equipped cosmetic changes — UIs can observe to recompose. */
    val equippedRevision: StateFlow<Long> = _equippedRevision.asStateFlow()

    private fun bump() {
        _equippedRevision.value = _equippedRevision.value + 1
    }

    fun getEquippedFrameId(): String = AppSettings.getString(EQUIPPED_FRAME, "frame_none")
    fun setEquippedFrame(id: String) {
        AppSettings.setString(EQUIPPED_FRAME, id)
        bump()
        pushEquipped(EquippedUpdate(equipped_frame = id))
    }

    fun getEquippedNameEffectId(): String = AppSettings.getString(EQUIPPED_NAME_EFFECT, "name_none")
    fun setEquippedNameEffect(id: String) {
        AppSettings.setString(EQUIPPED_NAME_EFFECT, id)
        bump()
        pushEquipped(EquippedUpdate(equipped_name_effect = id))
    }

    fun getEquippedTitleId(): String = AppSettings.getString(EQUIPPED_TITLE, "title_none")
    fun setEquippedTitle(id: String) {
        AppSettings.setString(EQUIPPED_TITLE, id)
        bump()
        pushEquipped(EquippedUpdate(equipped_title = id))
    }

    fun getEquippedCardDesignId(): String = AppSettings.getString(EQUIPPED_CARD_DESIGN, "card_none")
    fun setEquippedCardDesign(id: String) {
        AppSettings.setString(EQUIPPED_CARD_DESIGN, id)
        bump()
        pushEquipped(EquippedUpdate(equipped_card_design = id))
    }

    fun getEquippedBannerBackgroundId(): String = AppSettings.getString(EQUIPPED_BANNER, "banner_none")
    fun setEquippedBannerBackground(id: String) {
        AppSettings.setString(EQUIPPED_BANNER, id)
        bump()
        pushEquipped(EquippedUpdate(equipped_banner_background = id))
    }

    fun getEquippedFrame(): ProfileFrame = frameById(getEquippedFrameId())
    fun getEquippedNameEffect(): NameEffect = nameEffectById(getEquippedNameEffectId())
    fun getEquippedTitle(): PlayerTitle = titleById(getEquippedTitleId())
    fun getEquippedCardDesign(): CardDesign = cardDesignById(getEquippedCardDesignId())
    fun getEquippedBannerBackground(): BannerBackground = bannerBackgroundById(getEquippedBannerBackgroundId())

    private fun pushEquipped(update: EquippedUpdate) {
        val userId = AccountManager.currentUserId ?: return
        scope.launch {
            try {
                supabase.from("profiles").update(update) { filter { eq("id", userId) } }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to push equipped cosmetic", e)
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // Sync — pull cloud state into local mirror, push local-only ownership
    // ────────────────────────────────────────────────────────────────────

    /**
     * One-time migration: push every locally-owned cosmetic up to cloud
     * the first time the user signs in after the v1.3 update.
     */
    private suspend fun migrateLocalOwnedToCloud(userId: String) {
        if (AppSettings.getBoolean(MIGRATED_TO_CLOUD, false)) return
        val locallyOwned = (
            ALL_FRAMES.map { it.id } +
            ALL_NAME_EFFECTS.map { it.id } +
            ALL_TITLES.map { it.id } +
            ALL_CARD_DESIGNS.map { it.id } +
            ALL_BANNER_BACKGROUNDS.map { it.id }
        ).filter { it !in freeIds && AppSettings.getBoolean("$OWNED_PREFIX$it", false) }

        if (locallyOwned.isEmpty()) {
            AppSettings.setBoolean(MIGRATED_TO_CLOUD, true)
            return
        }
        try {
            supabase.from("owned_cosmetics").insert(
                locallyOwned.map { OwnedCosmeticInsert(user_id = userId, cosmetic_id = it) },
            )
            AppSettings.setBoolean(MIGRATED_TO_CLOUD, true)
            AppLogger.d(TAG, "Migrated ${locallyOwned.size} cosmetics to cloud")
        } catch (e: Exception) {
            AppLogger.w(TAG, "Cloud migration failed — will retry next session", e)
        }
    }

    /**
     * Pull cloud state into the local mirror. Call after sign-in / app start.
     * On failure, the local mirror remains untouched (offline-friendly).
     */
    suspend fun syncFromCloud() {
        val userId = AccountManager.currentUserId ?: return
        try {
            // 1. Pull owned cosmetics
            val owned = supabase.from("owned_cosmetics")
                .select { filter { eq("user_id", userId) } }
                .decodeList<OwnedCosmeticRow>()
            owned.forEach { AppSettings.setBoolean("$OWNED_PREFIX${it.cosmetic_id}", true) }

            // 2. Pull equipped cosmetics from profile
            val profile = supabase.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<pg.geobingo.one.network.UserProfile>()
            if (profile != null) {
                AppSettings.setString(EQUIPPED_FRAME, profile.equipped_frame)
                AppSettings.setString(EQUIPPED_NAME_EFFECT, profile.equipped_name_effect)
                AppSettings.setString(EQUIPPED_TITLE, profile.equipped_title)
                AppSettings.setString(EQUIPPED_CARD_DESIGN, profile.equipped_card_design)
                AppSettings.setString(EQUIPPED_BANNER, profile.equipped_banner_background)
                bump()
            }

            // 3. One-time push of locally-owned items
            migrateLocalOwnedToCloud(userId)
        } catch (e: Exception) {
            AppLogger.w(TAG, "syncFromCloud failed", e)
        }
    }
}
