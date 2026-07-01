// ui/screen/keyboard/components/NumpadLayout.kt
package com.arena.hidcompatibilitytester.ui.screen.keyboard.components

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
import com.arena.hidcompatibilitytester.ui.screen.keyboard.*

@Composable
internal fun NumpadCell(
    mainLabel : String,
    topLabel  : String    = "",
    color     : KC        = KC.NORMAL,
    isActive  : Boolean   = false,
    modifier  : Modifier,
    h         : Dp,
    settings  : KeyboardSettings = KeyboardSettings(),
    doRepeat  : Boolean   = true,
    onTap     : () -> Unit,
) {
    val scope  = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val bg     = keyBg(color, isActive, false, settings.highContrastMode)

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
                repeatScrollSafeTap(
                    scope            = scope,
                    slopPx           = 18f,
                    repeatMe         = effectiveRepeat,
                    initialDelayMs   = settings.repeatInitialDelayMs,
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
                color = keyFg(color, isActive, settings.highContrastMode),
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
internal fun NumpadLayout(
    numLock   : Boolean,
    shift     : Boolean,
    settings  : KeyboardSettings = KeyboardSettings(),
    onNumLock : () -> Unit,
    onKey     : (code: Int, label: String) -> Unit,
) {
    val cellH = 52.dp
    val tallH = cellH * 2 + 2.dp

    fun lbl(on: String, off: String): String {
        val effective = if (shift) !numLock else numLock
        return if (effective) on else off
    }

    fun top(on: String, off: String): String {
        if (on == off) return ""
        val effective = if (shift) !numLock else numLock
        return if (effective) off else on
    }

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {

        // Row 0: NumLk / * -
        Row(Modifier.fillMaxWidth()) {
            NumpadCell(
                mainLabel = "NumLk", color = KC.MOD, isActive = numLock,
                modifier = Modifier.weight(1f), h = cellH, settings = settings,
                doRepeat = false, onTap = onNumLock,
            )
            NumpadCell("÷", color = KC.SPECIAL, modifier = Modifier.weight(1f),
                h = cellH, settings = settings, onTap = { onKey(0x54, "/") })
            NumpadCell("×", color = KC.SPECIAL, modifier = Modifier.weight(1f),
                h = cellH, settings = settings, onTap = { onKey(0x55, "*") })
            NumpadCell("−", color = KC.SPECIAL, modifier = Modifier.weight(1f),
                h = cellH, settings = settings, onTap = { onKey(0x56, "-") })
        }

        // Rows 1-2: 7 8 9 [+tall] / 4 5 6
        Row(Modifier.fillMaxWidth().height(tallH)) {
            Column(Modifier.weight(3f)) {
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    NumpadCell(lbl("7", "Home"), top("7", "Home"),
                        modifier = Modifier.weight(1f), h = cellH, settings = settings,
                        onTap = { onKey(0x5F, lbl("7", "Home")) })
                    NumpadCell(lbl("8", "↑"), top("8", "↑"),
                        modifier = Modifier.weight(1f), h = cellH, settings = settings,
                        onTap = { onKey(0x60, lbl("8", "↑")) })
                    NumpadCell(lbl("9", "PgUp"), top("9", "PgUp"),
                        modifier = Modifier.weight(1f), h = cellH, settings = settings,
                        onTap = { onKey(0x61, lbl("9", "PgUp")) })
                }
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    NumpadCell(lbl("4", "←"), top("4", "←"),
                        modifier = Modifier.weight(1f), h = cellH, settings = settings,
                        onTap = { onKey(0x5C, lbl("4", "←")) })
                    NumpadCell(lbl("5", "·"), top("5", "·"),
                        modifier = Modifier.weight(1f), h = cellH, settings = settings,
                        onTap = { onKey(0x5D, lbl("5", "·")) })
                    NumpadCell(lbl("6", "→"), top("6", "→"),
                        modifier = Modifier.weight(1f), h = cellH, settings = settings,
                        onTap = { onKey(0x5E, lbl("6", "→")) })
                }
            }
            NumpadCell("+", color = KC.ACCENT, modifier = Modifier.weight(1f),
                h = tallH, settings = settings, onTap = { onKey(0x57, "+") })
        }

        // Rows 3-4: 1 2 3 [↵tall] / 0wide .
        Row(Modifier.fillMaxWidth().height(tallH)) {
            Column(Modifier.weight(3f)) {
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    NumpadCell(lbl("1", "End"), top("1", "End"),
                        modifier = Modifier.weight(1f), h = cellH, settings = settings,
                        onTap = { onKey(0x59, lbl("1", "End")) })
                    NumpadCell(lbl("2", "↓"), top("2", "↓"),
                        modifier = Modifier.weight(1f), h = cellH, settings = settings,
                        onTap = { onKey(0x5A, lbl("2", "↓")) })
                    NumpadCell(lbl("3", "PgDn"), top("3", "PgDn"),
                        modifier = Modifier.weight(1f), h = cellH, settings = settings,
                        onTap = { onKey(0x5B, lbl("3", "PgDn")) })
                }
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    NumpadCell(lbl("0", "Ins"), top("0", "Ins"),
                        modifier = Modifier.weight(2f), h = cellH, settings = settings,
                        onTap = { onKey(0x62, lbl("0", "Ins")) })
                    NumpadCell(lbl(".", "Del"), top(".", "Del"),
                        modifier = Modifier.weight(1f), h = cellH, settings = settings,
                        onTap = { onKey(0x63, lbl(".", "Del")) })
                }
            }
            NumpadCell("↵", color = KC.ACCENT, modifier = Modifier.weight(1f),
                h = tallH, settings = settings, onTap = { onKey(0x58, "↵") })
        }
    }
}