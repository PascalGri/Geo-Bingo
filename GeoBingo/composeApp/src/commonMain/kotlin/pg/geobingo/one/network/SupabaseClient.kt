package pg.geobingo.one.network

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

val supabase = createSupabaseClient(
    supabaseUrl = SupabaseConfig.current.url,
    supabaseKey = SupabaseConfig.current.anonKey,
) {
    install(Auth)
    install(Postgrest)
    install(Realtime)
    install(Storage)
    defaultSerializer = KotlinXSerializer(Json {
        ignoreUnknownKeys = true
    })
}
