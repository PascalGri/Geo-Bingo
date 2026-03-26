package pg.geobingo.one.network

/**
 * Configuration for Supabase connection.
 * Centralizes credentials to enable environment-specific overrides
 * and key rotation without modifying application code.
 */
data class SupabaseConfig(
    val url: String,
    val anonKey: String,
) {
    companion object {
        private var _override: SupabaseConfig? = null

        val current: SupabaseConfig
            get() = _override ?: SupabaseConfig(
                url = "https://itkmtovubmhkqtguzeex.supabase.co",
                anonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Iml0a210b3Z1Ym1oa3F0Z3V6ZWV4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzM1NTc2NjIsImV4cCI6MjA4OTEzMzY2Mn0.sH0-CL7V58UaCyqLGkGKp36CLIn15uOHbTV7HN2G0MY"
            )

        /**
         * Override configuration for testing or staging environments.
         */
        fun override(config: SupabaseConfig) {
            _override = config
        }

        fun clearOverride() {
            _override = null
        }
    }
}
