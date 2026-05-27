package com.example.docscanai.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.docscanai.data.AuthRepository
import com.example.docscanai.ui.theme.AIGlow
import com.example.docscanai.ui.theme.IntelligentBlue
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onSignUp: () -> Unit,
    onForgotPassword: () -> Unit = {},
) {
    val scope    = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val context  = LocalContext.current

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

    fun doGoogleSignIn() {
        keyboard?.hide()
        error = null; isLoading = true
        scope.launch {
            AuthRepository.signInWithGoogle(context).fold(
                onSuccess = { onLoginSuccess() },
                onFailure = { e -> error = e.message ?: "Google sign-in failed"; isLoading = false },
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1E3A5F), Color(0xFF0F2240))))
            .systemBarsPadding()
    ) {
        // Particle dots
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val dots = listOf(
                Pair(0.15f, 0.06f), Pair(0.82f, 0.10f), Pair(0.38f, 0.16f),
                Pair(0.70f, 0.04f), Pair(0.92f, 0.22f), Pair(0.06f, 0.32f),
                Pair(0.55f, 0.88f), Pair(0.25f, 0.72f), Pair(0.78f, 0.68f),
                Pair(0.44f, 0.92f), Pair(0.90f, 0.55f), Pair(0.12f, 0.50f),
            )
            dots.forEach { (x, y) ->
                drawCircle(
                    color  = AIGlow.copy(alpha = 0.22f),
                    radius = 3.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(size.width * x, size.height * y),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(52.dp))

            // Logo box
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Description, null, tint = AIGlow, modifier = Modifier.size(30.dp))
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Welcome back",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = (-0.5).sp,
            )
            Text(
                "Sign in to your account",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 6.dp, bottom = 28.dp),
            )

            // Glass card
            Surface(
                shape  = RoundedCornerShape(24.dp),
                color  = Color.White.copy(alpha = 0.06f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Email
                    LoginDarkField(
                        value         = email,
                        onValueChange = { email = it; error = null },
                        label         = "Email address",
                        leadingIcon   = {
                            Icon(Icons.Default.Email, null,
                                tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction    = ImeAction.Next,
                        ),
                    )

                    // Password
                    LoginDarkField(
                        value         = password,
                        onValueChange = { password = it; error = null },
                        label         = "Password",
                        leadingIcon   = {
                            Icon(Icons.Default.Lock, null,
                                tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) "Hide" else "Show",
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction    = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { doLogin() }),
                    )

                    // Forgot password link
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Forgot password?",
                            fontSize   = 13.sp,
                            color      = AIGlow.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium,
                            modifier   = Modifier.align(Alignment.CenterEnd).clickable(onClick = onForgotPassword),
                        )
                    }

                    // Error banner
                    if (error != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFDC2626).copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Icon(Icons.Default.ErrorOutline, null,
                                tint = Color(0xFFF87171), modifier = Modifier.size(16.dp))
                            Text(error!!, color = Color(0xFFF87171), fontSize = 13.sp)
                        }
                    }

                    // Gradient sign-in button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    if (isLoading)
                                        listOf(IntelligentBlue.copy(alpha = 0.5f), AIGlow.copy(alpha = 0.5f))
                                    else
                                        listOf(IntelligentBlue, AIGlow)
                                )
                            )
                            .clickable(enabled = !isLoading) { doLogin() },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color       = Color.White,
                            )
                        } else {
                            Text(
                                "Sign In",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = Color.White,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // OR divider
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(
                    modifier  = Modifier.weight(1f),
                    color     = Color.White.copy(alpha = 0.20f),
                )
                Text(
                    "  OR  ",
                    fontSize = 12.sp,
                    color    = Color.White.copy(alpha = 0.40f),
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color    = Color.White.copy(alpha = 0.20f),
                )
            }

            Spacer(Modifier.height(16.dp))

            // Google Sign-In button
            GoogleAuthButton(
                label     = "Continue with Google",
                enabled   = !isLoading,
                onClick   = { doGoogleSignIn() },
            )

            Spacer(Modifier.height(10.dp))

            // Guest button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isLoading) { doGuest() },
                shape  = RoundedCornerShape(14.dp),
                color  = Color.White.copy(alpha = 0.04f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.PersonOutline, null,
                            tint     = Color.White.copy(alpha = 0.70f),
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "Continue as Guest",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color      = Color.White.copy(alpha = 0.80f),
                        )
                    }
                    Text(
                        "Limited features · no account required",
                        fontSize = 11.sp,
                        color    = Color.White.copy(alpha = 0.40f),
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Trust badges
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                listOf("AES-256", "SOC 2", "GDPR").forEachIndexed { i, label ->
                    if (i > 0) {
                        Box(
                            modifier = Modifier
                                .width(1.dp).height(12.dp)
                                .background(Color.White.copy(alpha = 0.18f))
                        )
                    }
                    Row(
                        modifier              = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Default.Shield, null,
                            tint     = AIGlow.copy(alpha = 0.65f),
                            modifier = Modifier.size(11.dp),
                        )
                        Text(
                            label,
                            fontSize      = 10.sp,
                            color         = Color.White.copy(alpha = 0.45f),
                            fontWeight    = FontWeight.Medium,
                            letterSpacing = 0.3.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Sign up link
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("New here?", fontSize = 14.sp, color = Color.White.copy(alpha = 0.55f))
                TextButton(onClick = onSignUp) {
                    Text(
                        "Create an account",
                        color      = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 14.sp,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Google auth button (shared between Login / Signup) ────────────────────────

@Composable
internal fun GoogleAuthButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        shape  = RoundedCornerShape(14.dp),
        color  = Color.White.copy(alpha = if (enabled) 0.95f else 0.50f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            // Google "G" in brand colours
            Text(
                text       = "G",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFF4285F4),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text       = label,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Medium,
                color      = Color(0xFF1F1F1F),
            )
        }
    }
}

// ── Reusable dark field ───────────────────────────────────────────────────────

@Composable
private fun LoginDarkField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label.uppercase(),
            fontSize      = 10.sp,
            color         = Color.White.copy(alpha = 0.55f),
            fontWeight    = FontWeight.Medium,
            letterSpacing = 1.sp,
            fontFamily    = FontFamily.Monospace,
        )
        OutlinedTextField(
            value                = value,
            onValueChange        = onValueChange,
            leadingIcon          = leadingIcon,
            trailingIcon         = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions      = keyboardOptions,
            keyboardActions      = keyboardActions,
            singleLine           = true,
            modifier             = Modifier.fillMaxWidth().height(52.dp),
            textStyle            = androidx.compose.ui.text.TextStyle(
                color    = Color.White,
                fontSize = 15.sp,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                focusedContainerColor   = Color.White.copy(alpha = 0.08f),
                unfocusedBorderColor    = Color.White.copy(alpha = 0.12f),
                focusedBorderColor      = AIGlow.copy(alpha = 0.6f),
                cursorColor             = AIGlow,
            ),
            shape = RoundedCornerShape(12.dp),
        )
    }
}
