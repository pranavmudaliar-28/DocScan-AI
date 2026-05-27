package com.example.docscanai.ui.auth

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
fun SignupScreen(
    onSignUpSuccess: () -> Unit,
    onBack: () -> Unit,
) {
    val scope    = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    var name            by remember { mutableStateOf("") }
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var showPassword    by remember { mutableStateOf(false) }
    var isLoading       by remember { mutableStateOf(false) }
    var error           by remember { mutableStateOf<String?>(null) }

    // Password criteria
    val hasLength    = password.length >= 8
    val hasUpper     = password.any { it.isUpperCase() }
    val hasNumber    = password.any { it.isDigit() }
    val hasSpecial   = password.any { !it.isLetterOrDigit() }
    val criteriasMet = listOf(hasLength, hasUpper, hasNumber, hasSpecial).count { it }

    fun doSignUp() {
        when {
            name.isBlank()      -> { error = "Please enter your name"; return }
            email.isBlank()     -> { error = "Please enter your email"; return }
            password.length < 8 -> { error = "Password must be at least 8 characters"; return }
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
            .background(Brush.verticalGradient(listOf(Color(0xFF1E3A5F), Color(0xFF0F2240))))
            .systemBarsPadding(),
    ) {
        // Particle dots
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val dots = listOf(
                Pair(0.12f, 0.08f), Pair(0.88f, 0.12f), Pair(0.32f, 0.18f),
                Pair(0.65f, 0.06f), Pair(0.78f, 0.25f), Pair(0.05f, 0.35f),
                Pair(0.92f, 0.40f), Pair(0.22f, 0.55f), Pair(0.50f, 0.02f),
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
            Spacer(Modifier.height(16.dp))

            // Top nav row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White.copy(alpha = 0.8f))
                }
                // Step indicator
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "Step 2 of 2",
                        fontSize      = 11.sp,
                        color         = Color.White.copy(alpha = 0.50f),
                        fontFamily    = FontFamily.Monospace,
                        letterSpacing = 0.5.sp,
                    )
                }
                Spacer(Modifier.size(48.dp))
            }

            // Progress bar
            Row(
                modifier              = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f).height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.30f))
                )
                Box(
                    modifier = Modifier
                        .weight(1f).height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AIGlow)
                )
            }

            // Pro trial badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(AIGlow.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = AIGlow, modifier = Modifier.size(12.dp))
                    Text(
                        "14-day Pro trial · cancel anytime",
                        fontSize   = 11.sp,
                        color      = AIGlow,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Create your AI workspace",
                fontSize      = 26.sp,
                fontWeight    = FontWeight.Bold,
                color         = Color.White,
                letterSpacing = (-0.5).sp,
            )
            Text(
                "Join DocScan AI and unlock intelligent scanning",
                fontSize = 13.sp,
                color    = Color.White.copy(alpha = 0.60f),
                modifier = Modifier.padding(top = 6.dp, bottom = 22.dp),
            )

            // Glass card
            Surface(
                shape  = RoundedCornerShape(24.dp),
                color  = Color.White.copy(alpha = 0.06f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

                    // Full name
                    DarkField(
                        value         = name,
                        onValueChange = { name = it; error = null },
                        label         = "Full Name",
                        leadingIcon   = { Icon(Icons.Default.Person, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType   = KeyboardType.Text,
                            imeAction      = ImeAction.Next,
                            capitalization = KeyboardCapitalization.Words,
                        ),
                    )

                    // Email
                    DarkField(
                        value         = email,
                        onValueChange = { email = it; error = null },
                        label         = "Work Email",
                        leadingIcon   = { Icon(Icons.Default.Email, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    )

                    // Password + criteria
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DarkField(
                            value         = password,
                            onValueChange = { password = it; error = null },
                            label         = "Password",
                            leadingIcon   = { Icon(Icons.Default.Lock, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                            trailingIcon  = {
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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { doSignUp() }),
                        )

                        if (password.isNotEmpty()) {
                            // Strength bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.10f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(criteriasMet / 4f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            when (criteriasMet) {
                                                1    -> Color(0xFFDC2626)
                                                2    -> Color(0xFFD97706)
                                                3    -> IntelligentBlue
                                                else -> Color(0xFF16A34A)
                                            }
                                        )
                                )
                            }

                            // Criteria checklist
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(
                                    Pair(hasLength,  "8+ characters"),
                                    Pair(hasUpper,   "Uppercase letter"),
                                    Pair(hasNumber,  "Number"),
                                    Pair(hasSpecial, "Special character"),
                                ).forEach { (met, label) ->
                                    Row(
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            if (met) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            null,
                                            tint     = if (met) Color(0xFF16A34A) else Color.White.copy(alpha = 0.30f),
                                            modifier = Modifier.size(13.dp),
                                        )
                                        Text(
                                            label,
                                            fontSize = 11.sp,
                                            color    = if (met) Color.White.copy(alpha = 0.80f) else Color.White.copy(alpha = 0.40f),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Error
                    if (error != null) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFDC2626).copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFF87171), modifier = Modifier.size(16.dp))
                            Text(error!!, color = Color(0xFFF87171), fontSize = 13.sp)
                        }
                    }

                    // CTA
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isLoading)
                                    Brush.linearGradient(listOf(IntelligentBlue.copy(alpha = 0.5f), AIGlow.copy(alpha = 0.5f)))
                                else
                                    Brush.linearGradient(listOf(IntelligentBlue, AIGlow))
                            )
                            .then(if (!isLoading) Modifier.clickable { doSignUp() } else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("Create account", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("Already have an account?", fontSize = 14.sp, color = Color.White.copy(alpha = 0.55f))
                TextButton(onClick = onBack) {
                    Text("Sign In", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Reusable dark glassmorphism text field ────────────────────────────────────

@Composable
private fun DarkField(
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
            textStyle            = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
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
