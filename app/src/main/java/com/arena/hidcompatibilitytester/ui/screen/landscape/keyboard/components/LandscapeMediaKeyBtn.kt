package com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.components

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.LandscapeKeyboardSettings
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.LandscapeMediaRowGroup
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.landscapeRepeatScrollSafeTap
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
internal fun LandscapeMediaKeyBtn(
    icon: String,
    label: String,
    modifier: Modifier,
    h: Dp,
    settings: LandscapeKeyboardSettings = LandscapeKeyboardSettings(),
    mediaGroup: LandscapeMediaRowGroup? = null,
    scrollable: Boolean = false,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    var holdJob by remember { mutableStateOf<Job?>(null) }

    val bg by animateColorAsState(
        if (pressed) Color(0xFF1B3A5F) else Color(0xFF0E1C2A),
        tween(60), label = "lMb",
    )

    val isSmall = h <= 48.dp
    val iconSize = if (isSmall) 16.sp else 22.sp
    val labelSize = if (isSmall) 7.sp else 8.sp
    val innerPadding = if (isSmall) 1.dp else 2.dp

    val doRepeat = when (mediaGroup) {
        LandscapeMediaRowGroup.TRANSPORT -> false
        LandscapeMediaRowGroup.VOLUME -> settings.mediaRowRepeatVolume && settings.repeatEnabled
        LandscapeMediaRowGroup.BRIGHTNESS -> settings.mediaRowRepeatBrightness && settings.repeatEnabled
        null -> false
    }

    val doFeedback: () -> Unit = {
        if (settings.hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        if (settings.soundOnPress) {
            try {
                ToneGenerator(AudioManager.STREAM_SYSTEM, 40)
                    .apply { startTone(ToneGenerator.TONE_PROP_ACK, 30) }
            } catch (_: Exception) {}
        }
    }

    val gestureModifier = if (scrollable) {
        Modifier.pointerInput(icon, settings.repeatInitialDelayMs, settings.repeatIntervalMs, doRepeat) {
            landscapeRepeatScrollSafeTap(
                scope = scope,
                slopPx = 18f,
                repeatMe = doRepeat,
                initialDelayMs = settings.repeatInitialDelayMs,
                repeatIntervalMs = settings.repeatIntervalMs,
                onTap = {
                    doFeedback()
                    onClick()
                },
            )
        }
    } else {
        Modifier.pointerInput(icon, settings.repeatInitialDelayMs, settings.repeatIntervalMs, doRepeat) {
            detectTapGestures(
                onPress = {
                    pressed = true
                    doFeedback()
                    onClick()
                    if (doRepeat) {
                        holdJob = scope.launch {
                            delay(settings.repeatInitialDelayMs)
                            while (isActive) {
                                onClick()
                                delay(settings.repeatIntervalMs)
                            }
                        }
                    }
                    tryAwaitRelease()
                    pressed = false
                    holdJob?.cancel()
                    holdJob = null
                },
            )
        }
    }

    Column(
        modifier = modifier
            .height(h)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(0.5.dp, Color.White.copy(0.08f), RoundedCornerShape(8.dp))
            .then(gestureModifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(icon, fontSize = iconSize, textAlign = TextAlign.Center)
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