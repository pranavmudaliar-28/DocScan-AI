package com.example.docscanai.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.docscanai.ui.theme.AIGlow
import com.example.docscanai.ui.theme.IntelligentBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PasswordResetScreen(onBack: () -> Unit, onResetComplete: () -> Unit) {
    var step by remember { mutableIntStateOf(1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1E3A5F), Color(0xFF0F2240))))
            .systemBarsPadding(),
    ) {
        // Particle dots
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val dots = listOf(
                Pair(0.10f, 0.06f), Pair(0.85f, 0.14f), Pair(0.40f, 0.20f),
                Pair(0.70f, 0.05f), Pair(0.90f, 0.28f), Pair(0.05f, 0.42f),
            )
            dots.forEach { (x, y) ->
                drawCircle(
                    color  = AIGlow.copy(alpha = 0.20f),
                    radius = 3.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(size.width * x, size.height * y),
                )
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Back + progress bar
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    if (step > 1) step-- else onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White.copy(alpha = 0.8f))
                }
                Row(
                    modifier              = Modifier.weight(1f).padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(4) { i ->
                        Box(
                            modifier = Modifier
                                .weight(1f).height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (i < step) AIGlow else Color.White.copy(alpha = 0.20f)
                                )
                        )
                    }
                }
                Spacer(Modifier.size(48.dp))
            }

            when (step) {
                1 -> ResetStep1(onNext = { step = 2 })
                2 -> ResetStep2(onNext = { step = 3 })
                3 -> ResetStep3(onNext = { step = 4 })
                4 -> ResetStep4(onDone = onResetComplete)
            }
        }
    }
}

@Composable
private fun ResetStep1(onNext: () -> Unit) {
    var email     by remember { mutableStateOf("") }
    val scope     = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    StepContent(
        stepLabel = "01 / 04",
        title     = "Reset password",
        subtitle  = "We'll send a verification code to your email",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ResetDarkField(
                value         = email,
                onValueChange = { email = it },
                label         = "Email address",
                leadingIcon   = { Icon(Icons.Default.Email, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            )
            GradientButton(
                text      = "Send code →",
                isLoading = isLoading,
                onClick   = {
                    if (email.isNotBlank()) {
                        isLoading = true
                        scope.launch { delay(800); isLoading = false; onNext() }
                    }
                },
            )
        }
    }
}

@Composable
private fun ResetStep2(onNext: () -> Unit) {
    var resendCooldown by remember { mutableIntStateOf(60) }
    val scope          = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (resendCooldown > 0) { delay(1000); resendCooldown-- }
    }

    StepContent(
        stepLabel = "02 / 04",
        title     = "Enter your code",
        subtitle  = "Check your email for the 6-digit code",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            var otp by remember { mutableStateOf("") }
            val focusRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) { focusRequester.requestFocus() }

            // OTP display boxes with transparent BasicTextField overlay for input
            Box(
                modifier = Modifier.clickable { focusRequester.requestFocus() },
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(6) { i ->
                        val char = otp.getOrNull(i)?.toString() ?: ""
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (char.isNotEmpty()) Color.White.copy(alpha = 0.12f)
                                    else Color.White.copy(alpha = 0.06f)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                char.ifEmpty { "–" },
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color      = if (char.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.25f),
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
                BasicTextField(
                    value         = otp,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) otp = it },
                    modifier      = Modifier
                        .matchParentSize()
                        .alpha(0f)
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (otp.length == 6) onNext() }),
                    decorationBox   = { it() },
                )
            }

            // Resend
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Didn't receive it?", fontSize = 13.sp, color = Color.White.copy(alpha = 0.55f))
                Text(
                    if (resendCooldown > 0) "Resend in ${resendCooldown}s" else "Resend",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (resendCooldown > 0) Color.White.copy(alpha = 0.35f) else AIGlow,
                    modifier   = if (resendCooldown == 0) Modifier.clickable { resendCooldown = 60 } else Modifier,
                )
            }

            GradientButton(
                text    = "Verify →",
                enabled = otp.length == 6,
                onClick = { onNext() },
            )
        }
    }
}

@Composable
private fun ResetStep3(onNext: () -> Unit) {
    var password     by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val hasLength    = password.length >= 8
    val hasUpper     = password.any { it.isUpperCase() }
    val hasNumber    = password.any { it.isDigit() }
    val criteriasMet = listOf(hasLength, hasUpper, hasNumber).count { it }
    val suggestedPw  = "Nx8!kM2#qR5v"

    StepContent(
        stepLabel = "03 / 04",
        title     = "New password",
        subtitle  = "Choose a strong password for your account",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ResetDarkField(
                value         = password,
                onValueChange = { password = it },
                label         = "New password",
                leadingIcon   = { Icon(Icons.Default.Lock, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                trailingIcon  = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp)
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            )

            if (password.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.10f))) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(criteriasMet / 3f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(when (criteriasMet) { 1 -> Color(0xFFDC2626); 2 -> Color(0xFFD97706); else -> Color(0xFF16A34A) })
                    )
                }
            }

            // AI-generated suggestion
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { password = suggestedPw },
                shape    = RoundedCornerShape(12.dp),
                color    = AIGlow.copy(alpha = 0.08f),
                border   = androidx.compose.foundation.BorderStroke(1.dp, AIGlow.copy(alpha = 0.20f)),
            ) {
                Row(
                    modifier              = Modifier.padding(12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = AIGlow, modifier = Modifier.size(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI-generated suggestion", fontSize = 11.sp, color = AIGlow.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
                        Text(suggestedPw, fontSize = 13.sp, color = Color.White.copy(alpha = 0.70f), fontFamily = FontFamily.Monospace)
                    }
                    Text("Use", fontSize = 11.sp, color = AIGlow, fontWeight = FontWeight.SemiBold)
                }
            }

            GradientButton(
                text    = "Set password →",
                enabled = criteriasMet >= 2,
                onClick = { onNext() },
            )
        }
    }
}

@Composable
private fun ResetStep4(onDone: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue  = 0.95f,
        targetValue   = 1.05f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "scale",
    )

    Column(
        modifier              = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size((72 * scale).dp)
                .background(Color(0xFF16A34A).copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.CheckCircle, null,
                tint     = Color(0xFF16A34A),
                modifier = Modifier.size(40.dp))
        }

        Spacer(Modifier.height(28.dp))

        Text("Password updated!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(
            "Your password has been reset. You can now sign in with your new password.",
            fontSize  = 14.sp,
            color     = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(40.dp))

        GradientButton(text = "Sign in →", onClick = onDone)
    }
}

// ── Shared layout helpers ─────────────────────────────────────────────────────

@Composable
private fun StepContent(
    stepLabel: String,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(24.dp))

        Text(
            stepLabel,
            fontSize      = 11.sp,
            color         = AIGlow.copy(alpha = 0.75f),
            fontFamily    = FontFamily.Monospace,
            letterSpacing = 0.5.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(subtitle, fontSize = 13.sp, color = Color.White.copy(alpha = 0.60f), modifier = Modifier.padding(top = 6.dp, bottom = 32.dp))

        content()
    }
}

@Composable
private fun GradientButton(
    text: String,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (enabled && !isLoading)
                    Brush.linearGradient(listOf(IntelligentBlue, AIGlow))
                else
                    Brush.linearGradient(listOf(IntelligentBlue.copy(alpha = 0.4f), AIGlow.copy(alpha = 0.4f)))
            )
            .then(if (enabled && !isLoading) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
        } else {
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
private fun ResetDarkField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
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
