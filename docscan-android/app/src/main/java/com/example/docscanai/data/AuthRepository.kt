package com.example.docscanai.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AuthRepository {
    private const val PREFS_NAME       = "docscan_prefs"
    private const val KEY_TOKEN        = "auth_token"
    private const val KEY_EMAIL        = "user_email"
    private const val KEY_NAME         = "user_name"
    private const val KEY_ONBOARDING   = "onboarding_shown"

    // 10.0.2.2 = host machine localhost when running on the Android Emulator
    const val API_BASE = "http://10.0.2.2:3001"

    private lateinit var ctx: Context

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    fun init(context: Context) {
        ctx = context.applicationContext
        _isLoggedIn.value = getToken() != null
    }

    fun getToken(): String? =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_TOKEN, null)

    fun hasToken(): Boolean = getToken() != null

    fun getEmail(): String =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_EMAIL, "") ?: ""

    fun getName(): String =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NAME, "DocScan User") ?: "DocScan User"

    fun hasSeenOnboarding(): Boolean =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ONBOARDING, false)

    fun markOnboardingShown() {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ONBOARDING, true).apply()
    }

    fun signOut() {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove(KEY_TOKEN).remove(KEY_EMAIL).remove(KEY_NAME).apply()
        _isLoggedIn.value = false
    }

    private fun saveAuth(token: String, email: String, name: String) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_EMAIL, email)
            .putString(KEY_NAME, name.ifBlank { email.substringBefore('@') })
            .apply()
        _isLoggedIn.value = true
    }

    // ─── API calls ───────────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                }.toString()

                val conn = openPost("$API_BASE/auth/login", body)

                if (conn.responseCode in 200..299) {
                    val json = JSONObject(conn.inputStream.bufferedReader().readText())
                    val token = json.optString("access_token")
                    if (token.isNotEmpty()) {
                        saveAuth(token, email, email.substringBefore('@'))
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("No token received"))
                    }
                } else {
                    val msg = errorMessage(conn)
                    Result.failure(Exception(msg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun register(email: String, password: String, name: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                    put("name", name)
                }.toString()

                val conn = openPost("$API_BASE/auth/register", body)

                if (conn.responseCode in 200..299) {
                    val json = JSONObject(conn.inputStream.bufferedReader().readText())
                    val token = json.optString("access_token")
                    if (token.isNotEmpty()) {
                        saveAuth(token, email, name.ifBlank { email.substringBefore('@') })
                    }
                    Result.success(Unit)
                } else {
                    val msg = errorMessage(conn)
                    Result.failure(Exception(msg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun guestLogin(): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val conn = openPost("$API_BASE/auth/guest", "{}")
                if (conn.responseCode in 200..299) {
                    val json = JSONObject(conn.inputStream.bufferedReader().readText())
                    val token = json.optString("access_token")
                    if (token.isNotEmpty()) {
                        saveAuth(token, "guest@docscanai.com", "Guest")
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("No token received"))
                    }
                } else {
                    Result.failure(Exception(errorMessage(conn)))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private fun openPost(urlStr: String, body: String): HttpURLConnection {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.outputStream.use { it.write(body.toByteArray()) }
        return conn
    }

    private fun errorMessage(conn: HttpURLConnection): String {
        return try {
            val raw = conn.errorStream?.bufferedReader()?.readText() ?: "Request failed"
            JSONObject(raw).optString("message", raw)
        } catch (_: Exception) {
            "Request failed (${conn.responseCode})"
        }
    }
}
