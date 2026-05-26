package com.example.docscanai.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.docscanai.data.AuthRepository
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onSignUp: () -> Unit,
) {
    val scope    = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    var email        by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading    by remember { mutableStateOf(false) }
    var error        by remember { mutableStateOf<String?>(null) }

    fun doLogin() {
        if (email.isBlank() || password.isBlank()) { error = "Please enter your email and password"; return }
        keyboard?.hide()
        error = null; isLoading = true
        scope.launch {
            AuthRepository.login(email.trim(), password).fold(
                onSuccess = { onLoginSuccess() },
                onFailure = { e -> error = e.message ?: "Login failed"; isLoading = false },
            )
        }
    }

    fun doGuest() {
        keyboard?.hide()
        error = null; isLoading = true
        scope.launch {
            AuthRepository.guestLogin().fold(
                onSuccess = { onLoginSuccess() },
                onFailure = { e -> error = e.message ?: "Guest login failed"; isLoading = false },
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(56.dp))

            // ── Branding ────────────────────────────────────────────────────────
            Text(
                "DocScan AI",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Sign in to your account",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 44.dp),
            )

            // ── Email ────────────────────────────────────────────────────────────
            OutlinedTextField(
                value         = email,
                onValueChange = { email = it; error = null },
                label         = { Text("Email") },
                leadingIcon   = { Icon(Icons.Default.Email, null) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction    = ImeAction.Next,
                ),
                singleLine  = true,
                isError     = error != null,
                modifier    = Modifier.fillMaxWidth(),
                colors      = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor  = MaterialTheme.colorScheme.primary,
                ),
            )

            Spacer(Modifier.height(16.dp))

            // ── Password ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value         = password,
                onValueChange = { password = it; error = null },
                label         = { Text("Password") },
                leadingIcon   = { Icon(Icons.Default.Lock, null) },
                trailingIcon  = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPassword) "Hide" else "Show",
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction    = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { doLogin() }),
                singleLine  = true,
                isError     = error != null,
                modifier    = Modifier.fillMaxWidth(),
                colors      = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor  = MaterialTheme.colorScheme.primary,
                ),
            )

            // ── Error message ────────────────────────────────────────────────────
            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    error!!,
                    color     = MaterialTheme.colorScheme.error,
                    style     = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Sign In button ───────────────────────────────────────────────────
            Button(
                onClick  = { doLogin() },
                enabled  = !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = MaterialTheme.shapes.large,
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Sign In", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Continue as Guest ────────────────────────────────────────────────
            OutlinedButton(
                onClick  = { doGuest() },
                enabled  = !isLoading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = MaterialTheme.shapes.large,
            ) {
                Icon(Icons.Default.PersonOutline, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Continue as Guest", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(Modifier.height(28.dp))

            // ── Sign Up link ─────────────────────────────────────────────────────
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Don't have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onSignUp) {
                    Text(
                        "Sign Up",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            Spacer(Modifier.height(56.dp))
        }
    }
}
