// ui/screen/keyboard/components/MediaKeyBtn.kt
package com.arena.hidcompatibilitytester.ui.screen.keyboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.hidcompatibilitytester.ui.screen.keyboard.KeyboardSettings
import com.arena.hidcompatibilitytester.ui.screen.keyboard.repeatScrollSafeTap

@Composable
internal fun MediaKeyBtn(
    icon     : String,
    label    : String,
    modifier : Modifier,
    h        : Dp,
    settings : KeyboardSettings = KeyboardSettings(),
    onClick  : () -> Unit,
) {
    val haptic  = LocalHapticFeedback.current
    val scope   = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    val bg by animateColorAsState(
        if (pressed) Color(0xFF1B3A5F) else Color(0xFF0E1C2A),
        tween(60), label = "mb",
    )

    val isSmall = h <= 48.dp
    val iconSize = if (isSmall) 16.sp else 22.sp
    val labelSize = if (isSmall) 7.sp else 8.sp
    val innerPadding = if (isSmall) 1.dp else 2.dp

    Column(
        modifier = modifier
            .height(h)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(0.5.dp, Color.White.copy(0.08f), RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                repeatScrollSafeTap(
                    scope    = scope,
                    slopPx   = 18f,
                    repeatMe = false,
                    onTap = {
                        if (settings.hapticEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onClick()
                    },
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            icon,
            fontSize = iconSize,
            textAlign = TextAlign.Center,
        )
        if (!isSmall) {
            Text(
                label,
                fontSize = labelSize,
                color = Color(0xFF78909C),
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = innerPadding),
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}