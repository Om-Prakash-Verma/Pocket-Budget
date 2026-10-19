package com.ghostpocket.app.data.remote

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class D1ApiClient(
    context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ghostpocket_d1_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BASE_URL = "d1_backend_base_url"
        private const val KEY_AUTH_TOKEN = "d1_auth_token"
        private const val KEY_USER_ID = "d1_user_id"
        private const val KEY_USER_EMAIL = "d1_user_email"
        private const val KEY_LAST_SYNC_TS = "d1_last_sync_timestamp"

        const val DEFAULT_PRODUCTION_URL = "https://ghostpocket-backend.omprakashverma-in-fda.workers.dev"
    }

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, DEFAULT_PRODUCTION_URL) ?: DEFAULT_PRODUCTION_URL
        set(value) = prefs.edit().putString(KEY_BASE_URL, value.trimEnd('/')).apply()


    var authToken: String?
        get() = prefs.getString(KEY_AUTH_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()

    var currentUserId: String
        get() = prefs.getString(KEY_USER_ID, null) ?: "guest_user"
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var currentUserEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TS, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TS, value).apply()

    fun clearAuth() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_LAST_SYNC_TS)
            .apply()
    }

    suspend fun register(email: String, pass: String): AuthResponse = withContext(Dispatchers.IO) {
        val payload = json.encodeToString(AuthRequest(email = email, password = pass))
        executePost("/api/auth/register", payload)
    }

    suspend fun login(email: String, pass: String): AuthResponse = withContext(Dispatchers.IO) {
        val payload = json.encodeToString(AuthRequest(email = email, password = pass))
        executePost("/api/auth/login", payload)
    }

    suspend fun googleSignIn(idToken: String): AuthResponse = withContext(Dispatchers.IO) {
        val payload = json.encodeToString(AuthRequest(idToken = idToken))
        executePost("/api/auth/google", payload)
    }

    suspend fun pushSync(request: D1SyncPushRequest): D1SyncPushResponse = withContext(Dispatchers.IO) {
        val token = authToken ?: currentUserId
        val jsonPayload = json.encodeToString(request)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonPayload.toRequestBody(mediaType)

        val req = Request.Builder()
            .url("$baseUrl/api/sync/push")
            .header("Authorization", "Bearer $token")
            .post(body)
            .build()

        client.newCall(req).execute().use { response ->
            val resBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return@withContext D1SyncPushResponse(
                    success = false,
                    error = "HTTP ${response.code}: $resBody"
                )
            }
            json.decodeFromString<D1SyncPushResponse>(resBody)
        }
    }

    suspend fun pullSync(sinceTimestamp: Long = lastSyncTimestamp): D1SyncPullResponse = withContext(Dispatchers.IO) {
        val token = authToken ?: currentUserId
        val req = Request.Builder()
            .url("$baseUrl/api/sync/pull?since=$sinceTimestamp")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(req).execute().use { response ->
            val resBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return@withContext D1SyncPullResponse(
                    error = "HTTP ${response.code}: $resBody"
                )
            }
            val pullRes = json.decodeFromString<D1SyncPullResponse>(resBody)
            if (pullRes.serverTimestamp > 0) {
                lastSyncTimestamp = pullRes.serverTimestamp
            }
            pullRes
        }
    }

    private fun executePost(endpoint: String, jsonPayload: String): AuthResponse {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonPayload.toRequestBody(mediaType)

        val req = Request.Builder()
            .url("$baseUrl$endpoint")
            .post(body)
            .build()

        return try {
            client.newCall(req).execute().use { response ->
                val resBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    AuthResponse(error = "HTTP ${response.code}: $resBody")
                } else {
                    val authRes = json.decodeFromString<AuthResponse>(resBody)
                    if (authRes.userId != null) {
                        authToken = authRes.token ?: authRes.userId
                        currentUserId = authRes.userId
                        currentUserEmail = authRes.email
                    }
                    authRes
                }
            }
        } catch (e: Exception) {
            AuthResponse(error = e.localizedMessage ?: "Network connection failed")
        }
    }
}
