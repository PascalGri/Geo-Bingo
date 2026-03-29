package pg.geobingo.one.network

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.platform.PushManager
import pg.geobingo.one.util.AppLogger

@Serializable
private data class DeviceTokenDto(
    val user_id: String,
    val token: String,
    val platform: String,
)

object PushService {
    private const val TAG = "PushService"

    /** Register the device token for push notifications. */
    suspend fun registerToken() {
        if (!PushManager.isPushSupported) return
        val userId = AccountManager.currentUserId ?: return
        val token = PushManager.getDeviceToken() ?: return
        try {
            supabase.from("device_tokens").upsert(
                DeviceTokenDto(user_id = userId, token = token, platform = PushManager.platformName)
            )
            AppLogger.d(TAG, "Push token registered")
        } catch (e: Exception) {
            AppLogger.w(TAG, "Token registration failed", e)
        }
    }

    /** Send a push notification to a user via the Edge Function. */
    suspend fun sendPushToUser(toUserId: String, title: String, body: String, data: Map<String, String> = emptyMap()) {
        try {
            val url = "${SupabaseConfig.current.url}/functions/v1/send-push"
            val jsonBody = buildJsonObject {
                put("to_user_id", JsonPrimitive(toUserId))
                put("title", JsonPrimitive(title))
                put("body", JsonPrimitive(body))
                put("data", buildJsonObject {
                    data.forEach { (k, v) -> put(k, JsonPrimitive(v)) }
                })
            }.toString()

            val session = supabase.auth.currentSessionOrNull()
            ServiceLocator.httpClient.post(url) {
                headers {
                    append("apikey", SupabaseConfig.current.anonKey)
                    if (session != null) {
                        append(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
                    }
                }
                setBody(TextContent(jsonBody, io.ktor.http.ContentType.Application.Json))
            }
        } catch (e: Exception) {
            AppLogger.d(TAG, "Push send failed", e)
        }
    }
}
