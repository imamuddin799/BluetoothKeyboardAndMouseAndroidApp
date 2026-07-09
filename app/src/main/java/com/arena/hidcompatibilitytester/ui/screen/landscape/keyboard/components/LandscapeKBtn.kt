package com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.components

import android.view.SoundEffectConstants
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
import androidx.compose.ui.platform.LocalView
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
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.*

@Composable
internal fun LandscapeKBtn(
    key: LandscapeKey,
    modifier: Modifier,
    h: Dp,
    settings: LandscapeKeyboardSettings = LandscapeKeyboardSettings(),
    active: Boolean = false,
    topLabel: String = "",
    mainLabel: String,
    scrollable: Boolean = false,
    onPress: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    var holdJob by remember { mutableStateOf<Job?>(null) }

    val doFeedback: () -> Unit = {
        if (settings.hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        if (settings.soundOnPress) {
            view.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }

    val bg by animateColorAsState(
        landscapeKeyBg(key.color, active, pressed, settings.highContrastMode),
        tween(70), label = "lBg"
    )
    val sc by animateFloatAsState(
        if (pressed) 0.93f else 1f, tween(55), label = "lSc"
    )

    val doRepeat = when {
        !settings.repeatEnabled -> false
        !key.shouldRepeat() -> false
        key.isConsumer && key.mediaGroup == LandscapeMediaRowGroup.TRANSPORT -> false
        key.isConsumer && key.mediaGroup == LandscapeMediaRowGroup.VOLUME -> settings.mediaRowRepeatVolume
        key.isConsumer && key.mediaGroup == LandscapeMediaRowGroup.BRIGHTNESS -> settings.mediaRowRepeatBrightness
        key.isConsumer -> false
        else -> true
    }

    val gestureModifier = if (scrollable) {
        Modifier.pointerInput(key, settings.repeatInitialDelayMs, settings.repeatIntervalMs) {
            landscapeRepeatScrollSafeTap(
                scope = scope,
                slopPx = 18f,
                repeatMe = doRepeat,
                initialDelayMs = settings.repeatInitialDelayMs,
                repeatIntervalMs = settings.repeatIntervalMs,
                onTap = {
                    doFeedback()
                    onPress()
                },
            )
        }
    } else {
        Modifier.pointerInput(key, settings.repeatInitialDelayMs, settings.repeatIntervalMs) {
            detectTapGestures(
                onPress = {
                    pressed = true
                    doFeedback()
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
                    holdJob?.cancel()
                    holdJob = null
                },
            )
        }
    }

    val isFnKey = key.color == LandscapeKC.SPECIAL || key.color == LandscapeKC.MEDIA

    val showTopHint = !isFnKey && topLabel.isNotEmpty() && settings.showKeyHints

    val compactHeight = h <= 34.dp
    val veryCompactHeight = h <= 32.dp

    val contentVerticalPadding = when {
        isFnKey -> 0.dp
        veryCompactHeight -> 1.dp
        compactHeight -> 1.5.dp
        else -> 2.dp
    }

    val contentHorizontalPadding = if (isFnKey) 0.dp else 2.dp

    val hintFontSize = when {
        veryCompactHeight -> 5.sp
        compactHeight -> 6.sp
        else -> 7.sp
    }

    val hintTopPadding = when {
        veryCompactHeight -> 0.dp
        compactHeight -> 0.5.dp
        else -> 1.dp
    }

    val mainLabelOffsetY = if (showTopHint) {
        when {
            veryCompactHeight -> 1.dp
            compactHeight -> 1.5.dp
            h <= 40.dp -> 2.dp
            else -> 2.5.dp
        }
    } else {
        0.dp
    }

    val baseFontSp = settings.keyFontSize.baseSp

    Box(
        modifier = modifier
            .height(h)
            .scale(sc)
            .padding(2.dp)   // was 1.dp — gives 4dp effective gap between adjacent keys
            .clip(RoundedCornerShape(5.dp))
            .background(Brush.verticalGradient(listOf(bg.copy(alpha = 0.85f), bg)))
            .border(
                width = if (active) 1.5.dp else 0.5.dp,
                color = if (active) Color(0xFF4A90D9) else Color.White.copy(0.08f),
                shape = RoundedCornerShape(5.dp),
            )
            .then(gestureModifier),
        contentAlignment = if (isFnKey) Alignment.Center else Alignment.TopStart,
    ) {
        if (isFnKey) {
            Text(
                text = mainLabel,
                color = landscapeKeyFg(key.color, active, settings.highContrastMode),
                fontSize = when {
                    mainLabel.length > 5 -> (baseFontSp - 5).coerceAtLeast(6).sp
                    mainLabel.length > 3 -> (baseFontSp - 3).coerceAtLeast(7).sp
                    mainLabel.length > 2 -> (baseFontSp - 2).coerceAtLeast(8).sp
                    else -> baseFontSp.sp
                },
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = contentHorizontalPadding, vertical = contentVerticalPadding)
            ) {
                if (showTopHint) {
                    Text(
                        text = topLabel,
                        fontSize = hintFontSize,
                        lineHeight = hintFontSize,
                        color = Color.White.copy(alpha = 0.30f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth()
                            .padding(top = hintTopPadding),
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                    )
                }

                Text(
                    text = mainLabel,
                    color = landscapeKeyFg(key.color, active, settings.highContrastMode),
                    fontSize = when {
                        mainLabel.length > 5 -> (baseFontSp - 5).coerceAtLeast(6).sp
                        mainLabel.length > 3 -> (baseFontSp - 3).coerceAtLeast(7).sp
                        mainLabel.length > 2 -> (baseFontSp - 2).coerceAtLeast(8).sp
                        else -> baseFontSp.sp
                    },
                    fontWeight = if (key.color == LandscapeKC.ACCENT || active) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .offset(y = mainLabelOffsetY)
                        .padding(horizontal = 1.dp)
                )
            }
        }

        if (active) {
            Box(
                Modifier
                    .size(if (compactHeight) 4.dp else 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF4FC3F7))
                    .align(Alignment.BottomCenter)
                    .offset(y = if (compactHeight) (-1).dp else (-2).dp)
            )
        }
    }
}