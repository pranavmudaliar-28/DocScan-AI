package com.example.docscanai.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary                = Dark_Primary,
    onPrimary              = Dark_OnPrimary,
    primaryContainer       = Dark_PrimaryContainer,
    onPrimaryContainer     = Dark_OnPrimaryContainer,
    inversePrimary         = Dark_InversePrimary,
    secondary              = Dark_Secondary,
    onSecondary            = Dark_OnSecondary,
    secondaryContainer     = Dark_SecondaryContainer,
    onSecondaryContainer   = Dark_OnSecondaryContainer,
    tertiary               = Dark_Tertiary,
    onTertiary             = Dark_OnTertiary,
    tertiaryContainer      = Dark_TertiaryContainer,
    onTertiaryContainer    = Dark_OnTertiaryContainer,
    error                  = Dark_Error,
    onError                = Dark_OnError,
    errorContainer         = Dark_ErrorContainer,
    onErrorContainer       = Dark_OnErrorContainer,
    background             = Dark_Background,
    onBackground           = Dark_OnBackground,
    surface                = Dark_Surface,
    onSurface              = Dark_OnSurface,
    surfaceVariant         = Dark_SurfaceVariant,
    onSurfaceVariant       = Dark_OnSurfaceVariant,
    outline                = Dark_Outline,
    outlineVariant         = Dark_OutlineVariant,
    scrim                  = Dark_Scrim,
    inverseSurface         = Dark_InverseSurface,
    inverseOnSurface       = Dark_InverseOnSurface,
    surfaceTint            = Dark_SurfaceTint,
    surfaceContainerLowest  = Dark_SurfaceContainerLowest,
    surfaceContainerLow     = Dark_SurfaceContainerLow,
    surfaceContainer        = Dark_SurfaceContainer,
    surfaceContainerHigh    = Dark_SurfaceContainerHigh,
    surfaceContainerHighest = Dark_SurfaceContainerHighest,
)

private val LightColorScheme = lightColorScheme(
    primary                = Light_Primary,
    onPrimary              = Light_OnPrimary,
    primaryContainer       = Light_PrimaryContainer,
    onPrimaryContainer     = Light_OnPrimaryContainer,
    inversePrimary         = Light_InversePrimary,
    secondary              = Light_Secondary,
    onSecondary            = Light_OnSecondary,
    secondaryContainer     = Light_SecondaryContainer,
    onSecondaryContainer   = Light_OnSecondaryContainer,
    tertiary               = Light_Tertiary,
    onTertiary             = Light_OnTertiary,
    tertiaryContainer      = Light_TertiaryContainer,
    onTertiaryContainer    = Light_OnTertiaryContainer,
    error                  = Light_Error,
    onError                = Light_OnError,
    errorContainer         = Light_ErrorContainer,
    onErrorContainer       = Light_OnErrorContainer,
    background             = Light_Background,
    onBackground           = Light_OnBackground,
    surface                = Light_Surface,
    onSurface              = Light_OnSurface,
    surfaceVariant         = Light_SurfaceVariant,
    onSurfaceVariant       = Light_OnSurfaceVariant,
    outline                = Light_Outline,
    outlineVariant         = Light_OutlineVariant,
    scrim                  = Light_Scrim,
    inverseSurface         = Light_InverseSurface,
    inverseOnSurface       = Light_InverseOnSurface,
    surfaceTint            = Light_SurfaceTint,
    surfaceContainerLowest  = Light_SurfaceContainerLowest,
    surfaceContainerLow     = Light_SurfaceContainerLow,
    surfaceContainer        = Light_SurfaceContainer,
    surfaceContainerHigh    = Light_SurfaceContainerHigh,
    surfaceContainerHighest = Light_SurfaceContainerHighest,
)

@Composable
fun DocScanAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = DocScanTypography,
        shapes      = DocScanShapes,
        content     = content,
    )
}
