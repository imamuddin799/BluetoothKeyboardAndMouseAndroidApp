package com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.hidcompatibilitytester.ui.screen.landscape.LandscapeToolbarIcon
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.LandscapeKbState
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.LandscapeLayoutMode
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.LandscapeRightColumnMode

private val STATUS_BAR_ROW_HEIGHT = 28.dp

@Composable
internal fun LandscapeKbStatusBar(
    st: LandscapeKbState,
    isReady: Boolean,
    showFullStatus: Boolean,
    showComboPreview: Boolean,
    showKeyboard: Boolean,
    showOptionalRows: Boolean,
    hasOptionalRows: Boolean,
    onClearMods: () -> Unit,
    onShowSettings: () -> Unit,
    onToggleKeyboard: () -> Unit,
    onToggleOptionalRows: () -> Unit,
    onToggleLayoutMode: () -> Unit,
    currentLayoutMode: LandscapeLayoutMode,
    onToggleRightColumn: () -> Unit,
    currentRightColumn: LandscapeRightColumnMode,
    systemKeyboardActive: Boolean,
    onToggleSystemKeyboard: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF050C14))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(STATUS_BAR_ROW_HEIGHT),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ── 1. LED badges ──────────────────────────────────────────────
            if (showFullStatus) {
                listOf(
                    "NUM" to st.numLock,
                    "CAPS" to st.caps,
                    "SCR" to st.scrollLk,
                    "SHF" to st.shift,
                    "CTL" to st.ctrl,
                    "ALT" to st.alt,
                    "AGR" to st.altGr,
                    "WIN" to st.gui,
                    "OVR" to !st.insertMode,
                ).forEach { (lbl, on) ->
                    LandscapeKbLedBadge(lbl, on)
                }
            }

            Spacer(Modifier.width(6.dp))

            // ── 2. Combo preview (inline) ──────────────────────────────────
            if (showComboPreview && (st.anyMod || st.lastKey.isNotEmpty())) {
                val combo = st.modPrefix() + st.lastKey
                Surface(
                    color = Color(0xFF0A1828),
                    shape = RoundedCornerShape(3.dp),
                ) {
                    Text(
                        text = when {
                            st.anyMod && st.lastKey.isEmpty() ->
                                "▶ ${st.modPrefix().trimEnd('+')}+ …"
                            combo.isEmpty() -> ""
                            else -> "⌨ $combo"
                        },
                        fontSize = 11.sp,
                        color = Color(0xFF90CAF9),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        maxLines = 1,
                    )
                }

                if (st.anyMod) {
                    Box(
                        modifier = Modifier
                            .clickable(onClick = onClearMods)
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    ) {
                        Text("✕", fontSize = 12.sp, color = Color(0xFFEF9A9A), maxLines = 1)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // ── 3. Ready/Offline ───────────────────────────────────────────
            Text(
                text = if (isReady) "● Ready" else "○ Offline",
                fontSize = 9.sp,
                color = if (isReady) Color(0xFF81C784) else Color(0xFFEF9A9A),
                maxLines = 1,
            )

            Spacer(Modifier.width(4.dp))

            // ── 4. Action toggles (using LandscapeToolbarIcon) ─────────────
            // System keyboard — always visible
            LandscapeToolbarIcon(
                icon = "📱",
                onClick = onToggleSystemKeyboard,
                active = systemKeyboardActive,
            )
            Spacer(Modifier.width(2.dp))

            if (systemKeyboardActive) {
                // System mode: show only in-app keyboard toggle
                LandscapeToolbarIcon(
                    icon = "⌨",
                    onClick = onToggleKeyboard,
                    active = showKeyboard,
                )
                Spacer(Modifier.width(2.dp))
            } else {
                // Normal mode: show all keyboard-related toggles
                if (showKeyboard && hasOptionalRows) {
                    LandscapeToolbarIcon(
                        icon = "±",
                        onClick = onToggleOptionalRows,
                        active = showOptionalRows,
                    )
                    Spacer(Modifier.width(2.dp))
                }

                if (showKeyboard) {
                    LandscapeToolbarIcon(
                        icon = if (currentLayoutMode == LandscapeLayoutMode.TWO_COLUMN) "▥" else "▤",
                        onClick = onToggleLayoutMode,
                        active = currentLayoutMode == LandscapeLayoutMode.TWO_COLUMN,
                    )
                    Spacer(Modifier.width(2.dp))
                }

                if (showKeyboard && currentLayoutMode == LandscapeLayoutMode.TWO_COLUMN) {
                    LandscapeToolbarIcon(
                        icon = if (currentRightColumn == LandscapeRightColumnMode.NUMPAD) "🔢" else "↕",
                        onClick = onToggleRightColumn,
                        active = currentRightColumn == LandscapeRightColumnMode.NUMPAD,
                    )
                    Spacer(Modifier.width(2.dp))
                }

                LandscapeToolbarIcon(
                    icon = "⌨",
                    onClick = onToggleKeyboard,
                    active = showKeyboard,
                )
                Spacer(Modifier.width(2.dp))
            }

            LandscapeToolbarIcon(icon = "⚙", onClick = onShowSettings)
        }
    }
}

@Composable
internal fun LandscapeKbLedBadge(label: String, active: Boolean) {
    val bg by animateColorAsState(
        targetValue = if (active) Color(0xFF1565C0) else Color.White.copy(0.04f),
        animationSpec = tween(100),
        label = "lLed",
    )

    Surface(shape = RoundedCornerShape(3.dp), color = bg) {
        Text(
            text = label,
            fontSize = 7.sp,
            color = if (active) Color.White else Color(0xFF3A4A5A),
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            maxLines = 1,
        )
    }
}