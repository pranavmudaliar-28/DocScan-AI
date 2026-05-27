package com.example.docscanai.data

import android.content.Context
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object AuthRepository {
    private const val PREFS_NAME     = "docscan_prefs"
    private const val KEY_NAME       = "user_name"
    private const val KEY_ONBOARDING = "onboarding_shown"

    private lateinit var ctx: Context

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    fun init(context: Context) {
        ctx = context.applicationContext
        SupabaseModule.init(ctx)
        // Session is restored automatically by SharedPrefsSessionManager on first auth check
        _isLoggedIn.value = SupabaseModule.client.auth.currentSessionOrNull() != null
    }

    // ─── Session helpers ──────────────────────────────────────────────────────

    fun getToken(): String? =
        SupabaseModule.client.auth.currentSessionOrNull()?.accessToken

    fun hasToken(): Boolean = getToken() != null

    fun getEmail(): String =
        SupabaseModule.client.auth.currentUserOrNull()?.email ?: ""

    fun getUserId(): String =
        SupabaseModule.client.auth.currentUserOrNull()?.id ?: ""

    fun getName(): String =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NAME, "DocScan User") ?: "DocScan User"

    fun hasSeenOnboarding(): Boolean =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING, false)

    fun markOnboardingShown() {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ONBOARDING, true).apply()
    }

    private fun saveName(name: String) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_NAME, name.ifBlank { "DocScan User" }).apply()
    }

    // ─── Auth calls ───────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            SupabaseModule.client.auth.signInWith(Email) {
                this.email    = email
                this.password = password
            }
            saveName(email.substringBefore('@'))
            _isLoggedIn.value = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String, name: String): Result<Unit> {
        return try {
            SupabaseModule.client.auth.signUpWith(Email) {
                this.email    = email
                this.password = password
            }
            saveName(name.ifBlank { email.substringBefore('@') })
            _isLoggedIn.value = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(context: Context): Result<Unit> {
        return try {
            val (idToken, rawNonce) = GoogleSignInHelper.getGoogleIdToken(context)
            SupabaseModule.client.auth.signInWith(IDToken) {
                this.idToken = idToken
                provider     = Google
                nonce        = rawNonce
            }
            val email = SupabaseModule.client.auth.currentUserOrNull()?.email ?: ""
            saveName(email.substringBefore('@').ifBlank { "Google User" })
            _isLoggedIn.value = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun guestLogin(): Result<Unit> {
        return try {
            SupabaseModule.client.auth.signInAnonymously()
            saveName("Guest")
            _isLoggedIn.value = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Synchronous — clears local state immediately. Supabase sign-out fires in background. */
    fun signOut() {
        _isLoggedIn.value = false
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_NAME).apply()
        CoroutineScope(Dispatchers.IO).launch {
            try { SupabaseModule.client.auth.signOut() } catch (_: Exception) {}
        }
    }
}
