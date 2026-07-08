package com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.*

@Composable
internal fun LandscapeNumpadCell(
    mainLabel: String,
    topLabel: String = "",
    color: LandscapeKC = LandscapeKC.NORMAL,
    isActive: Boolean = false,
    modifier: Modifier,
    h: Dp,
    settings: LandscapeKeyboardSettings = LandscapeKeyboardSettings(),
    doRepeat: Boolean = true,
    onTap: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val bg = landscapeKeyBg(color, isActive, false, settings.highContrastMode)

    val effectiveRepeat = doRepeat && settings.repeatEnabled

    Box(
        modifier = modifier
            .height(h)
            .padding(1.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Brush.verticalGradient(listOf(bg.copy(0.85f), bg)))
            .border(
                if (isActive) 1.5.dp else 0.5.dp,
                if (isActive) Color(0xFF4A90D9) else Color.White.copy(0.08f),
                RoundedCornerShape(5.dp),
            )
            .pointerInput(mainLabel, settings.repeatInitialDelayMs, settings.repeatIntervalMs) {
                landscapeRepeatScrollSafeTap(
                    scope = scope,
                    slopPx = 18f,
                    repeatMe = effectiveRepeat,
                    initialDelayMs = settings.repeatInitialDelayMs,
                    repeatIntervalMs = settings.repeatIntervalMs,
                    onTap = {
                        if (settings.hapticEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onTap()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(2.dp),
        ) {
            if (topLabel.isNotEmpty() && settings.numpadShowHints) {
                Text(
                    topLabel, fontSize = 7.sp,
                    color = Color.White.copy(0.28f),
                    textAlign = TextAlign.Center, lineHeight = 7.sp,
                )
            }
            Text(
                mainLabel,
                fontSize = when {
                    mainLabel.length > 4 -> 8.sp
                    mainLabel.length > 2 -> 10.sp
                    else -> 13.sp
                },
                color = landscapeKeyFg(color, isActive, settings.highContrastMode),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        if (isActive) {
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

@Composable
internal fun LandscapeNumpadLayout(
    numLock: Boolean,
    shift: Boolean,
    settings: LandscapeKeyboardSettings = LandscapeKeyboardSettings(),
    onNumLock: () -> Unit,
    onKey: (code: Int, label: String) -> Unit,
) {
    val cellH = 52.dp
    val gap = 4.dp
    val tallH = cellH * 2 + gap

    fun lbl(on: String, off: String): String {
        val effective = if (shift) !numLock else numLock
        return if (effective) on else off
    }

    fun top(on: String, off: String): String {
        if (on == off) return ""
        val effective = if (shift) !numLock else numLock
        return if (effective) off else on
    }

    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        // Row 0: NumLk ÷ × −
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            LandscapeNumpadCell(
                mainLabel = "NumLk", color = LandscapeKC.MOD, isActive = numLock,
                modifier = Modifier.weight(1f), h = cellH, settings = settings,
                doRepeat = false, onTap = onNumLock,
            )
            LandscapeNumpadCell(
                mainLabel = "÷", color = LandscapeKC.SPECIAL, isActive = false,
                modifier = Modifier.weight(1f), h = cellH, settings = settings,
                doRepeat = true, onTap = { onKey(0x54, "/") },
            )
            LandscapeNumpadCell(
                mainLabel = "×", color = LandscapeKC.SPECIAL, isActive = false,
                modifier = Modifier.weight(1f), h = cellH, settings = settings,
                doRepeat = true, onTap = { onKey(0x55, "*") },
            )
            LandscapeNumpadCell(
                mainLabel = "−", color = LandscapeKC.SPECIAL, isActive = false,
                modifier = Modifier.weight(1f), h = cellH, settings = settings,
                doRepeat = true, onTap = { onKey(0x56, "-") },
            )
        }

        // Rows 1-2: 7 8 9 [+tall] / 4 5 6
        Row(
            Modifier.fillMaxWidth().height(tallH),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            Column(
                Modifier.weight(3f),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    LandscapeNumpadCell(mainLabel = lbl("7", "Home"), topLabel = top("7", "Home"), modifier = Modifier.weight(1f), h = cellH, settings = settings, onTap = { onKey(0x5F, lbl("7", "Home")) })
                    LandscapeNumpadCell(mainLabel = lbl("8", "↑"), topLabel = top("8", "↑"), modifier = Modifier.weight(1f), h = cellH, settings = settings, onTap = { onKey(0x60, lbl("8", "↑")) })
                    LandscapeNumpadCell(mainLabel = lbl("9", "PgUp"), topLabel = top("9", "PgUp"), modifier = Modifier.weight(1f), h = cellH, settings = settings, onTap = { onKey(0x61, lbl("9", "PgUp")) })
                }
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    LandscapeNumpadCell(mainLabel = lbl("4", "←"), topLabel = top("4", "←"), modifier = Modifier.weight(1f), h = cellH, settings = settings, onTap = { onKey(0x5C, lbl("4", "←")) })
                    LandscapeNumpadCell(mainLabel = lbl("5", "·"), topLabel = top("5", "·"), modifier = Modifier.weight(1f), h = cellH, settings = settings, onTap = { onKey(0x5D, lbl("5", "·")) })
                    LandscapeNumpadCell(mainLabel = lbl("6", "→"), topLabel = top("6", "→"), modifier = Modifier.weight(1f), h = cellH, settings = settings, onTap = { onKey(0x5E, lbl("6", "→")) })
                }
            }
            LandscapeNumpadCell(
                mainLabel = "+", color = LandscapeKC.ACCENT, isActive = false,
                modifier = Modifier.weight(1f), h = tallH, settings = settings,
                doRepeat = true, onTap = { onKey(0x57, "+") },
            )
        }

        // Rows 3-4: 1 2 3 [↵tall] / 0wide .
        Row(
            Modifier.fillMaxWidth().height(tallH),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            Column(
                Modifier.weight(3f),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    LandscapeNumpadCell(mainLabel = lbl("1", "End"), topLabel = top("1", "End"), modifier = Modifier.weight(1f), h = cellH, settings = settings, onTap = { onKey(0x59, lbl("1", "End")) })
                    LandscapeNumpadCell(mainLabel = lbl("2", "↓"), topLabel = top("2", "↓"), modifier = Modifier.weight(1f), h = cellH, settings = settings, onTap = { onKey(0x5A, lbl("2", "↓")) })
                    LandscapeNumpadCell(mainLabel = lbl("3", "PgDn"), topLabel = top("3", "PgDn"), modifier = Modifier.weight(1f), h = cellH, settings = settings, onTap = { onKey(0x5B, lbl("3", "PgDn")) })
                }
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    LandscapeNumpadCell(mainLabel = lbl("0", "Ins"), topLabel = top("0", "Ins"), modifier = Modifier.weight(2f), h = cellH, settings = settings, onTap = { onKey(0x62, lbl("0", "Ins")) })
                    LandscapeNumpadCell(mainLabel = lbl(".", "Del"), topLabel = top(".", "Del"), modifier = Modifier.weight(1f), h = cellH, settings = settings, onTap = { onKey(0x63, lbl(".", "Del")) })
                }
            }
            LandscapeNumpadCell(
                mainLabel = "↵", color = LandscapeKC.ACCENT, isActive = false,
                modifier = Modifier.weight(1f), h = tallH, settings = settings,
                doRepeat = true, onTap = { onKey(0x58, "↵") },
            )
        }
    }
}