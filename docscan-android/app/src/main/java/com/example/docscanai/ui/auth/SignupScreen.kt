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
fun SignupScreen(
    onSignUpSuccess: () -> Unit,
    onBack: () -> Unit,
) {
    val scope    = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    var name            by remember { mutableStateOf("") }
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword    by remember { mutableStateOf(false) }
    var isLoading       by remember { mutableStateOf(false) }
    var error           by remember { mutableStateOf<String?>(null) }

    fun doSignUp() {
        when {
            name.isBlank()     -> { error = "Please enter your name"; return }
            email.isBlank()    -> { error = "Please enter your email"; return }
            password.length < 6 -> { error = "Password must be at least 6 characters"; return }
            password != confirmPassword -> { error = "Passwords do not match"; return }
        }
        keyboard?.hide()
        error = null; isLoading = true
        scope.launch {
            AuthRepository.register(email.trim(), password, name.trim()).fold(
                onSuccess = { onSignUpSuccess() },
                onFailure = { e -> error = e.message ?: "Registration failed"; isLoading = false },
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
            Spacer(Modifier.height(24.dp))

            // ── Back ─────────────────────────────────────────────────────────────
            Box(Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Branding ────────────────────────────────────────────────────────
            Text(
                "Create Account",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "Join DocScan AI to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 36.dp),
            )

            // ── Name ─────────────────────────────────────────────────────────────
            OutlinedTextField(
                value         = name,
                onValueChange = { name = it; error = null },
                label         = { Text("Full Name") },
                leadingIcon   = { Icon(Icons.Default.Person, null) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction    = ImeAction.Next,
                    capitalization = KeyboardCapitalization.Words,
                ),
                singleLine = true,
                isError    = error != null,
                modifier   = Modifier.fillMaxWidth(),
                colors     = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor  = MaterialTheme.colorScheme.primary,
                ),
            )

            Spacer(Modifier.height(14.dp))

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
                singleLine = true,
                isError    = error != null,
                modifier   = Modifier.fillMaxWidth(),
                colors     = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor  = MaterialTheme.colorScheme.primary,
                ),
            )

            Spacer(Modifier.height(14.dp))

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
                    imeAction    = ImeAction.Next,
                ),
                singleLine = true,
                isError    = error != null,
                modifier   = Modifier.fillMaxWidth(),
                colors     = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor  = MaterialTheme.colorScheme.primary,
                ),
            )

            Spacer(Modifier.height(14.dp))

            // ── Confirm Password ─────────────────────────────────────────────────
            OutlinedTextField(
                value         = confirmPassword,
                onValueChange = { confirmPassword = it; error = null },
                label         = { Text("Confirm Password") },
                leadingIcon   = { Icon(Icons.Default.LockOutline, null) },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction    = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { doSignUp() }),
                singleLine = true,
                isError    = error != null,
                modifier   = Modifier.fillMaxWidth(),
                colors     = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor  = MaterialTheme.colorScheme.primary,
                ),
            )

            // ── Error ────────────────────────────────────────────────────────────
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

            // ── Create Account button ────────────────────────────────────────────
            Button(
                onClick  = { doSignUp() },
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
                    Text("Create Account", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Sign In link ─────────────────────────────────────────────────────
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Already have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onBack) {
                    Text(
                        "Sign In",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}
