package com.cashopinion.console.data.repo

import com.cashopinion.console.core.SessionManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class ServerClient(
    private val sessionManager: SessionManager,
    private val auth: FirebaseAuth
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private suspend fun getAuthToken(): String {
        val user = auth.currentUser ?: throw IllegalStateException("Not authenticated")
        val tokenResult = user.getIdToken(false).await()
        return tokenResult.token ?: throw IllegalStateException("Failed to obtain ID token")
    }

    private suspend fun post(path: String, jsonBody: JSONObject = JSONObject()): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val token = getAuthToken()
            val baseUrl = sessionManager.serverUrl.trimEnd('/')
            val requestId = UUID.randomUUID().toString()

            val body = jsonBody.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$baseUrl$path")
                .header("Authorization", "Bearer $token")
                .header("x-request-id", requestId)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val resStr = response.body?.string().orEmpty()
            val resJson = if (resStr.isNotBlank()) JSONObject(resStr) else JSONObject()

            if (response.isSuccessful && resJson.optBoolean("success", true)) {
                Result.success(resJson)
            } else {
                val errorMsg = resJson.optString("error", "Server returned HTTP ${response.code}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkHealth(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = sessionManager.serverUrl.trimEnd('/')
            val request = Request.Builder().url("$baseUrl/v1/health").get().build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approveWithdrawal(id: String, utr: String): Result<JSONObject> {
        val body = JSONObject().put("utr", utr)
        return post("/v1/withdrawals/$id/approve", body)
    }

    suspend fun rejectWithdrawal(id: String, reason: String): Result<JSONObject> {
        val body = JSONObject().put("reason", reason)
        return post("/v1/withdrawals/$id/reject", body)
    }

    suspend fun reverseWithdrawal(id: String, reason: String): Result<JSONObject> {
        val body = JSONObject().put("reason", reason)
        return post("/v1/withdrawals/$id/reverse", body)
    }

    suspend fun adjustCoins(uid: String, amount: Long, direction: String, reason: String): Result<JSONObject> {
        val body = JSONObject()
            .put("amount", amount)
            .put("direction", direction)
            .put("reason", reason)
        return post("/v1/users/$uid/adjust-coins", body)
    }

    suspend fun banUser(uid: String, reason: String): Result<JSONObject> {
        val body = JSONObject().put("reason", reason)
        return post("/v1/users/$uid/ban", body)
    }

    suspend fun unbanUser(uid: String): Result<JSONObject> {
        return post("/v1/users/$uid/unban", JSONObject())
    }

    suspend fun approveReview(id: String): Result<JSONObject> {
        return post("/v1/tasks/reviews/$id/approve", JSONObject())
    }

    suspend fun rejectReview(id: String, reason: String): Result<JSONObject> {
        val body = JSONObject().put("reason", reason)
        return post("/v1/tasks/reviews/$id/reject", body)
    }

    suspend fun rewardLeaderboard(period: String, winners: JSONArray): Result<JSONObject> {
        val body = JSONObject().put("winners", winners)
        return post("/v1/leaderboard/$period/reward", body)
    }

    suspend fun resetSeason(confirm: String, seasonId: String, seasonName: String, startsAt: Long): Result<JSONObject> {
        val body = JSONObject()
            .put("confirm", confirm)
            .put("seasonId", seasonId)
            .put("seasonName", seasonName)
            .put("startsAt", startsAt)
        return post("/v1/season/reset", body)
    }

    suspend fun createBackup(): Result<JSONObject> {
        return post("/v1/backup/create", JSONObject())
    }
}
