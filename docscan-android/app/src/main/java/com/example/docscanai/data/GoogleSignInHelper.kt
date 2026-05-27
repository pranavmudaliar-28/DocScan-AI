package com.example.docscanai.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.util.UUID

object GoogleSignInHelper {

    private fun buildNoncePair(): Pair<String, String> {
        val raw = UUID.randomUUID().toString()
        val hashed = MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return raw to hashed
    }

    /**
     * Launches the Credential Manager Google picker and returns
     * (idToken, rawNonce) on success, or throws on failure/cancellation.
     */
    suspend fun getGoogleIdToken(context: Context): Pair<String, String> {
        val (rawNonce, hashedNonce) = buildNoncePair()

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(SupabaseConfig.GOOGLE_WEB_CLIENT_ID)
            .setAutoSelectEnabled(true)
            .setNonce(hashedNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = CredentialManager.create(context)
            .getCredential(context = context, request = request)

        val idToken = GoogleIdTokenCredential
            .createFrom(result.credential.data)
            .idToken

        return idToken to rawNonce
    }
}
