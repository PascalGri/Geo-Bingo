package pg.geobingo.one.data

import pg.geobingo.one.i18n.Language
import pg.geobingo.one.i18n.S

// ─────────────────────────────────────────────────────────────────────────────
//  English localization for game categories.
//
//  The German category data (SoloCategories, QuickStartCategories,
//  WeirdCoreCategories, Categories) stays the source of truth. Each pool/template
//  gets a parallel EN map keyed by the same id. At selection / build time the
//  category is run through one of the helpers below: when the active language is
//  English it swaps in the translation, otherwise it returns the German original
//  unchanged. Missing keys fall back to German gracefully.
// ─────────────────────────────────────────────────────────────────────────────

/** (name, description) – for pools whose German entries have a single fixed name. */
typealias CategoryEn = Pair<String, String>

/** (name variants, description) – for [Categories] templates that randomise their name per game. */
typealias TemplateEn = Pair<List<String>, String>

/**
 * Returns an English copy of this pool category when the UI language is English,
 * otherwise the unchanged German original. Used by the solo / quick-start /
 * weird-core selectors so every downstream screen renders the localized text.
 */
fun Category.localized(en: Map<String, CategoryEn>): Category {
    if (S.language != Language.EN) return this
    val t = en[id] ?: return this
    return copy(name = t.first, description = t.second)
}

/**
 * Returns an English copy of a multiplayer template category when the UI language
 * is English. Re-randomises the name from the English variant list so English
 * games feel as fresh as German ones; falls back to German for missing keys.
 */
fun Category.localizedTemplate(en: Map<String, TemplateEn>): Category {
    if (S.language != Language.EN) return this
    val t = en[id] ?: return this
    val name = t.first.randomOrNull() ?: return copy(description = t.second)
    return copy(name = name, description = t.second)
}

/**
 * Language-aware description lookup for template ids (outdoor + indoor). Used when
 * reconstructing categories from the database, where only the icon_id/template id
 * is stored. Names follow the host's language (stored label); descriptions follow
 * the viewer's language so each player reads the rules in their own language.
 */
fun categoryDescription(id: String): String =
    if (S.language == Language.EN) {
        CATEGORY_DESCRIPTIONS_EN[id] ?: CATEGORY_DESCRIPTIONS[id] ?: ""
    } else {
        CATEGORY_DESCRIPTIONS[id] ?: ""
    }
