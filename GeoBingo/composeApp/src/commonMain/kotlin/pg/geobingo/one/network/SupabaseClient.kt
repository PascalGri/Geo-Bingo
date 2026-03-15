package pg.geobingo.one.network

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

val supabase = createSupabaseClient(
    supabaseUrl = "https://itkmtovubmhkqtguzeex.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Iml0a210b3Z1Ym1oa3F0Z3V6ZWV4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzM1NTc2NjIsImV4cCI6MjA4OTEzMzY2Mn0.sH0-CL7V58UaCyqLGkGKp36CLIn15uOHbTV7HN2G0MY"
) {
    install(Postgrest)
    install(Realtime)
    install(Storage)
}
