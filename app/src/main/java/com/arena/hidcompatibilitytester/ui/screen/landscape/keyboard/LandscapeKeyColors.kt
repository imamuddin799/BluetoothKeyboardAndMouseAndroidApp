package com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard

import androidx.compose.ui.graphics.Color

fun landscapeKeyBg(c: LandscapeKC, active: Boolean, pressed: Boolean, highContrast: Boolean = false): Color {
    val boost = if (highContrast) 0.15f else 0f
    return when {
        pressed -> Color(0xFF3A7ABD)
        active -> Color(0xFF1E4A7A)
        else -> when (c) {
            LandscapeKC.NORMAL -> Color(0xFF2A3240).landscapeBrighten(boost)
            LandscapeKC.MOD -> Color(0xFF1A2332).landscapeBrighten(boost)
            LandscapeKC.SPECIAL -> Color(0xFF1A2535).landscapeBrighten(boost)
            LandscapeKC.ACCENT -> Color(0xFF1A3A5C).landscapeBrighten(boost)
            LandscapeKC.DANGER -> Color(0xFF3A1A1A).landscapeBrighten(boost)
            LandscapeKC.MEDIA -> Color(0xFF2A1F3D).landscapeBrighten(boost)
        }
    }
}

fun landscapeKeyFg(c: LandscapeKC, active: Boolean, highContrast: Boolean = false): Color {
    val boost = if (highContrast) 0.2f else 0f
    return when {
        active -> Color(0xFF90CAF9)
        c == LandscapeKC.ACCENT -> Color(0xFF64B5F6).landscapeBrighten(boost)
        c == LandscapeKC.DANGER -> Color(0xFFEF9A9A).landscapeBrighten(boost)
        c == LandscapeKC.MOD -> Color(0xFFB0BEC5).landscapeBrighten(boost)
        c == LandscapeKC.MEDIA -> Color(0xFFD1C4E9).landscapeBrighten(boost)
        else -> Color(0xFFECEFF1)
    }
}

private fun Color.landscapeBrighten(amount: Float): Color {
    if (amount <= 0f) return this
    return Color(
        red = (red + amount).coerceAtMost(1f),
        green = (green + amount).coerceAtMost(1f),
        blue = (blue + amount).coerceAtMost(1f),
        alpha = alpha,
    )
}