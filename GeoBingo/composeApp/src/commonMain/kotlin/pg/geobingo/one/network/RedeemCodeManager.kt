package pg.geobingo.one.network

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import pg.geobingo.one.util.AppLogger

/**
 * Thin client wrapper around the `redeem_code` Supabase RPC. The server is
 * the source of truth — all validation, rate limiting and atomicity live in
 * Postgres. Client only hands the code string up and surfaces the result.
 */
object RedeemCodeManager {
    private const val TAG = "RedeemCode"

    sealed class Result {
        data class Success(val starsGranted: Int, val newBalance: Int) : Result()
        object NotAuthenticated : Result()
        object InvalidCode : Result()
        object UnknownCode : Result()
        object Expired : Result()
        object Depleted : Result()
        object AlreadyRedeemed : Result()
        data class Error(val message: String) : Result()
    }

    suspend fun redeem(code: String): Result {
        if (AccountManager.currentUserId == null) return Result.NotAuthenticated
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return Result.InvalidCode
        return try {
            val payload = buildJsonObject { put("p_code", trimmed) }
            val res = supabase.postgrest.rpc("redeem_code", payload)
            val body = res.data
            val obj = Json.parseToJsonElement(body).jsonObject
            val ok = obj["ok"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
            if (ok) {
                val stars = obj["stars_granted"]?.jsonPrimitive?.int ?: 0
                val newBalance = obj["new_balance"]?.jsonPrimitive?.int ?: 0
                Result.Success(stars, newBalance)
            } else {
                when (obj["error"]?.jsonPrimitive?.content) {
                    "not_authenticated" -> Result.NotAuthenticated
                    "invalid_code" -> Result.InvalidCode
                    "unknown_code" -> Result.UnknownCode
                    "expired" -> Result.Expired
                    "depleted" -> Result.Depleted
                    "already_redeemed" -> Result.AlreadyRedeemed
                    else -> Result.Error(body.take(120))
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "redeem_code RPC failed: ${e.message}", e)
            Result.Error(e.message ?: "network_error")
        }
    }
}
