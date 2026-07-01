// ui/screen/keyboard/components/KBtn.kt
package com.arena.hidcompatibilitytester.ui.screen.keyboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.arena.hidcompatibilitytester.ui.screen.keyboard.*
import com.arena.hidcompatibilitytester.ui.screen.keyboard.KeyboardSettings

@Composable
internal fun KBtn(
    key        : Key,
    modifier   : Modifier,
    h          : Dp,
    settings   : KeyboardSettings = KeyboardSettings(),
    active     : Boolean = false,
    topLabel   : String  = "",
    mainLabel  : String,
    subLabel   : String  = "",
    scrollable : Boolean = false,
    onPress    : () -> Unit,
) {
    val haptic  = LocalHapticFeedback.current
    val scope   = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    var holdJob by remember { mutableStateOf<Job?>(null) }

    val bg by animateColorAsState(
        keyBg(key.color, active, pressed, settings.highContrastMode),
        tween(70), label = "bg"
    )
    val sc by animateFloatAsState(if (pressed) 0.93f else 1f, tween(55), label = "sc")

    val doRepeat = settings.repeatEnabled && key.shouldRepeat()

    val gestureModifier = if (scrollable) {
        Modifier.pointerInput(key, settings.repeatInitialDelayMs, settings.repeatIntervalMs) {
            repeatScrollSafeTap(
                scope            = scope,
                slopPx           = 18f,
                repeatMe         = doRepeat,
                initialDelayMs   = settings.repeatInitialDelayMs,
                repeatIntervalMs = settings.repeatIntervalMs,
                onTap = {
                    if (settings.hapticEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onPress()
                },
            )
        }
    } else {
        Modifier.pointerInput(key, settings.repeatInitialDelayMs, settings.repeatIntervalMs) {
            detectTapGestures(
                onPress = {
                    pressed = true
                    if (settings.hapticEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onPress()
                    if (doRepeat) {
                        holdJob = scope.launch {
                            delay(settings.repeatInitialDelayMs)
                            while (isActive) {
                                onPress()
                                delay(settings.repeatIntervalMs)
                            }
                        }
                    }
                    tryAwaitRelease()
                    pressed = false
                    holdJob?.cancel(); holdJob = null
                },
            )
        }
    }

    val baseFontSp = settings.keyFontSize.baseSp

    Box(
        modifier = modifier
            .height(h)
            .scale(sc)
            .padding(1.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Brush.verticalGradient(listOf(bg.copy(alpha = 0.85f), bg)))
            .border(
                width = if (active) 1.5.dp else 0.5.dp,
                color = if (active) Color(0xFF4A90D9) else Color.White.copy(0.08f),
                shape = RoundedCornerShape(5.dp),
            )
            .then(gestureModifier),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 1.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (topLabel.isNotEmpty() && settings.showKeyHints) {
                Text(
                    topLabel, fontSize = 7.sp,
                    color = Color.White.copy(alpha = 0.30f),
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 7.sp, textAlign = TextAlign.Start,
                )
            } else {
                Spacer(Modifier.height(7.sp.value.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    mainLabel,
                    color = keyFg(key.color, active, settings.highContrastMode),
                    fontSize = when {
                        mainLabel.length > 5 -> (baseFontSp - 5).coerceAtLeast(6).sp
                        mainLabel.length > 3 -> (baseFontSp - 3).coerceAtLeast(7).sp
                        mainLabel.length > 2 -> (baseFontSp - 2).coerceAtLeast(8).sp
                        else -> baseFontSp.sp
                    },
                    fontWeight = if (key.color == KC.ACCENT || active) FontWeight.Bold
                    else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }

            if (subLabel.isNotEmpty()) {
                Text(
                    subLabel, fontSize = 6.sp,
                    color = Color(0xFFA5D6A7).copy(alpha = 0.55f),
                    textAlign = TextAlign.Center, maxLines = 1,
                )
            } else {
                Spacer(Modifier.height(6.sp.value.dp))
            }
        }

        if (active) {
            Box(
                Modifier
                    .size(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF4FC3F7))
                    .align(Alignment.BottomCenter)
                    .offset(y = (-2).dp)
            )
        }
    }
}