package com.example.docscanai.ui.theme

import androidx.compose.ui.graphics.Color

// ── Calm Intelligence — Design token colours ───────────────────────────────────
// Deep Navy #1E3A5F · Intelligent Blue #2D6BE4 · AI Glow #60A5FA
// Soft Background #EEF4FF · Ink #111827 · Neutral Gray #6B7280

// ── Dark Scheme ────────────────────────────────────────────────────────────────

val Dark_Background         = Color(0xFF0B1220) // rich dark
val Dark_Surface            = Color(0xFF131B2E) // card surface
val Dark_SurfaceVariant     = Color(0xFF1A2540) // elevated / input fills

val Dark_SurfaceContainerLowest  = Color(0xFF07101E)
val Dark_SurfaceContainerLow     = Color(0xFF0D1728)
val Dark_SurfaceContainer        = Color(0xFF131B2E)
val Dark_SurfaceContainerHigh    = Color(0xFF1A2540)
val Dark_SurfaceContainerHighest = Color(0xFF223058)

// Primary — AI Glow
val Dark_Primary              = Color(0xFF60A5FA)
val Dark_OnPrimary            = Color(0xFF0B1220)
val Dark_PrimaryContainer     = Color(0xFF1B3A6B)
val Dark_OnPrimaryContainer   = Color(0xFFBDD8FF)
val Dark_InversePrimary       = Color(0xFF2D6BE4)

// Secondary — soft violet (AI intelligence)
val Dark_Secondary             = Color(0xFFA385FF)
val Dark_OnSecondary           = Color(0xFF150060)
val Dark_SecondaryContainer    = Color(0xFF211270)
val Dark_OnSecondaryContainer  = Color(0xFFDDD1FF)

// Tertiary — success green
val Dark_Tertiary              = Color(0xFF4ADE80)
val Dark_OnTertiary            = Color(0xFF002010)
val Dark_TertiaryContainer     = Color(0xFF003820)
val Dark_OnTertiaryContainer   = Color(0xFF86EFAC)

// Error
val Dark_Error                 = Color(0xFFF87171)
val Dark_OnError               = Color(0xFF450A0A)
val Dark_ErrorContainer        = Color(0xFF7F1D1D)
val Dark_OnErrorContainer      = Color(0xFFFECACA)

// Neutrals
val Dark_OnBackground          = Color(0xFFEAF0FF)
val Dark_OnSurface             = Color(0xFFCCDCFF)
val Dark_OnSurfaceVariant      = Color(0xFF7E9EC8)
val Dark_Outline               = Color(0xFF1C3060)
val Dark_OutlineVariant        = Color(0xFF0F1F3A)
val Dark_Scrim                 = Color(0xFF000000)
val Dark_InverseSurface        = Color(0xFFCCDCFF)
val Dark_InverseOnSurface      = Color(0xFF0B1220)
val Dark_SurfaceTint           = Color(0xFF60A5FA)

// ── Light Scheme ───────────────────────────────────────────────────────────────

val Light_Background           = Color(0xFFF7F9FF) // cool off-white
val Light_Surface              = Color(0xFFFFFFFF)
val Light_SurfaceVariant       = Color(0xFFEEF4FF) // Soft Background / AI surface

val Light_SurfaceContainerLowest  = Color(0xFFFFFFFF)
val Light_SurfaceContainerLow     = Color(0xFFF2F5FF)
val Light_SurfaceContainer        = Color(0xFFEBEFFF)
val Light_SurfaceContainerHigh    = Color(0xFFE2E8FF)
val Light_SurfaceContainerHighest = Color(0xFFD8E0FF)

// Primary — Intelligent Blue
val Light_Primary              = Color(0xFF2D6BE4)
val Light_OnPrimary            = Color(0xFFFFFFFF)
val Light_PrimaryContainer     = Color(0xFFEEF4FF) // Soft Background
val Light_OnPrimaryContainer   = Color(0xFF1E3A5F) // Deep Navy
val Light_InversePrimary       = Color(0xFF60A5FA) // AI Glow

// Secondary — violet AI accent
val Light_Secondary            = Color(0xFF7C3AED)
val Light_OnSecondary          = Color(0xFFFFFFFF)
val Light_SecondaryContainer   = Color(0xFFEDE9FE)
val Light_OnSecondaryContainer = Color(0xFF3B0764)

// Tertiary — success green
val Light_Tertiary             = Color(0xFF16A34A)
val Light_OnTertiary           = Color(0xFFFFFFFF)
val Light_TertiaryContainer    = Color(0xFFDCFCE7)
val Light_OnTertiaryContainer  = Color(0xFF052E16)

// Error
val Light_Error                = Color(0xFFDC2626)
val Light_OnError              = Color(0xFFFFFFFF)
val Light_ErrorContainer       = Color(0xFFFEE2E2)
val Light_OnErrorContainer     = Color(0xFF450A0A)

// Neutrals
val Light_OnBackground         = Color(0xFF111827) // Ink
val Light_OnSurface            = Color(0xFF111827) // Ink
val Light_OnSurfaceVariant     = Color(0xFF6B7280) // Neutral Gray
val Light_Outline              = Color(0xFF6B7280)
val Light_OutlineVariant       = Color(0xFFE5E7EB)
val Light_Scrim                = Color(0xFF000000)
val Light_InverseSurface       = Color(0xFF111827)
val Light_InverseOnSurface     = Color(0xFFF7F9FF)
val Light_SurfaceTint          = Color(0xFF2D6BE4)

// ── Semantic aliases (used directly in composables) ────────────────────────────
val DeepNavy       = Color(0xFF1E3A5F)
val IntelligentBlue = Color(0xFF2D6BE4)
val AIGlow         = Color(0xFF60A5FA)
val SoftBackground = Color(0xFFEEF4FF)
val SuccessGreen   = Color(0xFF16A34A)
val WarningAmber   = Color(0xFFD97706)
val ErrorRed       = Color(0xFFDC2626)
