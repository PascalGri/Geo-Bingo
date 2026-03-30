package pg.geobingo.one.util

/**
 * Validates player names against a blocklist of common insults and profanity.
 * Covers German and English terms. Uses normalized matching (lowercase, stripped
 * of common leetspeak substitutions) to catch evasion attempts.
 */
object NameValidator {

    // Blocked terms (lowercase). Covers DE + EN profanity, slurs, and common insults.
    private val blockedTerms = setOf(
        // German insults / profanity
        "arschloch", "arsch", "wichser", "hurensohn", "hure", "fotze", "schlampe",
        "missgeburt", "spast", "spasti", "behindert", "schwuchtel", "tunte",
        "drecksau", "dreckig", "scheisse", "scheiss", "scheisze", "fick", "ficken",
        "gefickt", "ficker", "motherficker", "wixer", "wixxer", "nutte", "bastard",
        "idiot", "vollidiot", "depp", "trottel", "penner", "opfer", "mongo",
        "kanacke", "kanake", "neger", "zigeuner", "nazi", "hitler", "heil",
        "schwanzlutscher", "pisser", "kackbratze", "dummkopf",

        // English insults / profanity
        "fuck", "fucker", "fucking", "shit", "shithead", "asshole", "bitch",
        "dick", "dickhead", "pussy", "cunt", "whore", "slut", "retard",
        "retarded", "faggot", "fag", "nigger", "nigga", "negro",
        "motherfucker", "cock", "cocksucker", "twat", "wanker", "prick",
        "bastard", "douche", "douchebag", "jackass", "dumbass", "dipshit",
        "butthole", "damn", "stfu", "wtf", "kys",
    )

    // Leetspeak / evasion character mappings
    private val leetMap = mapOf(
        '0' to 'o', '1' to 'i', '3' to 'e', '4' to 'a',
        '5' to 's', '7' to 't', '8' to 'b', '@' to 'a',
        '$' to 's', '!' to 'i',
    )

    /**
     * Returns true if the name is acceptable (no profanity detected).
     */
    fun isValid(name: String): Boolean {
        val normalized = normalize(name)
        // Check if any blocked term appears as a substring
        return blockedTerms.none { term -> normalized.contains(term) }
    }

    /**
     * Normalize a name: lowercase, strip non-alphanumeric (except spaces),
     * apply leetspeak mapping, collapse repeated characters.
     */
    private fun normalize(input: String): String {
        val lower = input.lowercase()
        val mapped = lower.map { leetMap[it] ?: it }
        // Keep only letters, collapse repeats (e.g. "fuuuck" -> "fuck")
        val letters = mapped.filter { it.isLetter() }
        return buildString {
            for (c in letters) {
                if (isEmpty() || last() != c || c == 'l' || c == 's' || c == 'e') {
                    append(c)
                }
            }
        }
    }
}
