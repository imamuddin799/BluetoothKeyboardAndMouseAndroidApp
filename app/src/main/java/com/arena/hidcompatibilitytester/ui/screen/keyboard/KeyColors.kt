package com.arena.hidcompatibilitytester.ui.screen.keyboard

import androidx.compose.ui.graphics.Color

fun keyBg(c: KC, active: Boolean, pressed: Boolean, highContrast: Boolean = false): Color {
    val boost = if (highContrast) 0.15f else 0f
    return when {
        pressed -> Color(0xFF3A7ABD)
        active  -> Color(0xFF1E4A7A)
        else    -> when (c) {
            KC.NORMAL  -> Color(0xFF2A3240).brighten(boost)
            KC.MOD     -> Color(0xFF1A2332).brighten(boost)
            KC.SPECIAL -> Color(0xFF1A2535).brighten(boost)
            KC.ACCENT  -> Color(0xFF1A3A5C).brighten(boost)
            KC.DANGER  -> Color(0xFF3A1A1A).brighten(boost)
            KC.MEDIA   -> Color(0xFF2A1F3D).brighten(boost)
        }
    }
}

fun keyFg(c: KC, active: Boolean, highContrast: Boolean = false): Color {
    val boost = if (highContrast) 0.2f else 0f
    return when {
        active         -> Color(0xFF90CAF9)
        c == KC.ACCENT -> Color(0xFF64B5F6).brighten(boost)
        c == KC.DANGER -> Color(0xFFEF9A9A).brighten(boost)
        c == KC.MOD    -> Color(0xFFB0BEC5).brighten(boost)
        c == KC.MEDIA  -> Color(0xFFD1C4E9).brighten(boost)
        else           -> Color(0xFFECEFF1)
    }
}

private fun Color.brighten(amount: Float): Color {
    if (amount <= 0f) return this
    return Color(
        red   = (red + amount).coerceAtMost(1f),
        green = (green + amount).coerceAtMost(1f),
        blue  = (blue + amount).coerceAtMost(1f),
        alpha = alpha,
    )
}