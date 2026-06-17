package com.chtor.app

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Chator brand palette — mirrors `libraries/designsystem/.../ChatorColors.kt` on Android.
object ChatorColors {
    val bluePrimary = Color(0xFF389CFF)
    val blueDark    = Color(0xFF1E6FD9)
    val blueLight   = Color(0xFF6BB3FF)
    val bluePressed = Color(0xFF1558A8)
    val accent      = bluePrimary

    // Compound semantic colors that get overridden
    val bgCanvasDefault  = Color(0xFF111214) // Element dark canvas
    val bgSubtlePrimary  = Color(0xFF1C1E21) // Element surfaceDark
    val bgSubtleSecondary = Color(0xFF16181B)
    val textPrimary      = Color(0xFFE4E4E7)
    val textSecondary    = Color(0xFFA1A1AA)
    val textDisabled     = Color(0xFF6B6F76)
    val iconPrimary      = Color(0xFFE4E4E7)
    val iconSecondary    = Color(0xFFA1A1AA)
    val borderSubtle     = Color(0xFF26282C)
}

val ChatorDarkScheme = darkColorScheme(
    primary               = ChatorColors.bluePrimary,
    onPrimary             = Color.White,
    primaryContainer      = ChatorColors.blueDark,
    onPrimaryContainer    = Color.White,
    secondary             = ChatorColors.blueLight,
    onSecondary           = Color.Black,
    background            = ChatorColors.bgCanvasDefault,
    onBackground          = ChatorColors.textPrimary,
    surface               = ChatorColors.bgSubtlePrimary,
    onSurface             = ChatorColors.textPrimary,
    surfaceVariant        = ChatorColors.bgSubtleSecondary,
    onSurfaceVariant      = ChatorColors.textSecondary,
    error                 = Color(0xFFFF6B6B),
    onError               = Color.White,
    outline               = ChatorColors.borderSubtle,
)

val ChatorLightScheme = lightColorScheme(
    primary               = ChatorColors.bluePrimary,
    onPrimary             = Color.White,
    secondary             = ChatorColors.blueDark,
    background            = Color(0xFFF5F5F0),
    onBackground          = Color(0xFF1A1A1A),
    surface               = Color.White,
    onSurface             = Color(0xFF1A1A1A),
)
