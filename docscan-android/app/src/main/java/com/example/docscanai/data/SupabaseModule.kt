package com.example.docscanai.data

import android.content.Context
import android.content.SharedPreferences
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ── Session persistence ───────────────────────────────────────────────────────

private class SharedPrefsSessionManager(context: Context) : SessionManager {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun loadSession(): UserSession? {
        val raw = prefs.getString("session", null) ?: return null
        return try {
            json.decodeFromString<UserSession>(raw)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun saveSession(session: UserSession) {
        prefs.edit()
            .putString("session", json.encodeToString(session))
            .apply()
    }

    override suspend fun deleteSession() {
        prefs.edit().remove("session").apply()
    }
}

// ── Supabase client singleton ─────────────────────────────────────────────────

object SupabaseModule {
    private var _client: io.github.jan.supabase.SupabaseClient? = null

    val client: io.github.jan.supabase.SupabaseClient
        get() = _client ?: error("SupabaseModule not initialised — call SupabaseModule.init(context) first")

    fun init(context: Context) {
        if (_client != null) return
        _client = createSupabaseClient(
            supabaseUrl  = SupabaseConfig.URL,
            supabaseKey  = SupabaseConfig.ANON_KEY,
        ) {
            install(Auth) {
                sessionManager = SharedPrefsSessionManager(context.applicationContext)
            }
            install(Postgrest)
            install(Storage)
        }
    }
}
