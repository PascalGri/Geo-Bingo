package pg.geobingo.one.network

import kotlin.test.*

class SupabaseConfigTest {

    @AfterTest
    fun cleanup() {
        SupabaseConfig.clearOverride()
    }

    @Test
    fun defaultConfig_hasValues() {
        val config = SupabaseConfig.current
        assertTrue(config.url.startsWith("https://"))
        assertTrue(config.anonKey.isNotEmpty())
    }

    @Test
    fun override_replacesDefault() {
        val custom = SupabaseConfig(url = "https://test.supabase.co", anonKey = "test-key")
        SupabaseConfig.override(custom)

        assertEquals("https://test.supabase.co", SupabaseConfig.current.url)
        assertEquals("test-key", SupabaseConfig.current.anonKey)
    }

    @Test
    fun clearOverride_restoresDefault() {
        val custom = SupabaseConfig(url = "https://test.supabase.co", anonKey = "test-key")
        SupabaseConfig.override(custom)
        SupabaseConfig.clearOverride()

        assertTrue(SupabaseConfig.current.url.contains("supabase.co"))
        assertNotEquals("test-key", SupabaseConfig.current.anonKey)
    }
}
