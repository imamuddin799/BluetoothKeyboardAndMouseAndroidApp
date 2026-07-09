package com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

// Fixed height for the whole status-bar row (prevents jumping when combo shows)
// At the top of the file
private val STATUS_BAR_ROW_HEIGHT = 28.dp   // was 22.dp

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
    onToggleSystemKeyboard: () -> Unit,
    systemKeyboardActive: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF050C14))
            .padding(horizontal = 8.dp, vertical = 5.dp)   // was vertical = 3.dp
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

            // ── 2. Combo preview (inline, matched height) ──────────────────
            if (showComboPreview && (st.anyMod || st.lastKey.isNotEmpty())) {
                val combo = st.modPrefix() + st.lastKey
                Surface(
                    color = Color(0xFF0A1828),
                    shape = RoundedCornerShape(3.dp),
                ) {
                    Text(
                        text = when {
                            st.anyMod && st.lastKey.isEmpty() ->
                                "▶ ${st.modPrefix().trimEnd('+')}+ …waiting"
                            combo.isEmpty() -> ""
                            else -> "⌨ $combo"
                        },
                        fontSize = 11.sp,                                              // was 8.sp
                        color = Color(0xFF90CAF9),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), // was 5/2
                        maxLines = 1,
                    )
                }

                if (st.anyMod) {
                    Box(
                        modifier = Modifier
                            .clickable(onClick = onClearMods)
                            .padding(horizontal = 6.dp, vertical = 2.dp),   // was 4/1
                    ) {
                        Text("✕", fontSize = 12.sp, color = Color(0xFFEF9A9A), maxLines = 1)  // was 10.sp
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

            // ── 4. Action toggles ──────────────────────────────────────────
            // System keyboard toggle — ALWAYS visible
            LandscapeStatusBarIconToggle(
                icon = "📱",
                active = systemKeyboardActive,
                onClick = onToggleSystemKeyboard,
            )

            if (systemKeyboardActive) {
                // System mode: show only in-app keyboard toggle
                LandscapeStatusBarIconToggle(
                    icon = "⌨",
                    active = showKeyboard,
                    onClick = onToggleKeyboard,
                )
            } else {
                // Normal mode: show all keyboard-related toggles
                if (showKeyboard && hasOptionalRows) {
                    LandscapeStatusBarIconToggle(
                        icon = "±",
                        active = showOptionalRows,
                        onClick = onToggleOptionalRows,
                    )
                }

                if (showKeyboard) {
                    LandscapeStatusBarIconToggle(
                        icon = if (currentLayoutMode == LandscapeLayoutMode.TWO_COLUMN) "▥" else "▤",
                        active = currentLayoutMode == LandscapeLayoutMode.TWO_COLUMN,
                        onClick = onToggleLayoutMode,
                    )
                }

                if (showKeyboard && currentLayoutMode == LandscapeLayoutMode.TWO_COLUMN) {
                    LandscapeStatusBarIconToggle(
                        icon = if (currentRightColumn == LandscapeRightColumnMode.NUMPAD) "🔢" else "↕",
                        active = currentRightColumn == LandscapeRightColumnMode.NUMPAD,
                        onClick = onToggleRightColumn,
                    )
                }

                LandscapeStatusBarIconToggle(
                    icon = "⌨",
                    active = showKeyboard,
                    onClick = onToggleKeyboard,
                )
            }

            LandscapeToolbarIcon(icon = "⚙", onClick = onShowSettings)
        }
    }
}

@Composable
private fun LandscapeStatusBarIconToggle(
    icon: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (active) Color(0xFF1B5E20).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.06f),
        animationSpec = tween(150),
        label = "lIconToggleBg",
    )

    val fg = if (active) Color(0xFF81C784) else Color.White.copy(0.45f)

    Surface(color = bg, shape = RoundedCornerShape(3.dp)) {
        Box(modifier = Modifier.clickable(onClick = onClick)) {
            Text(
                text = icon,
                fontSize = 13.sp,                                              // was 11.sp
                color = fg,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), // was 5/2
            )
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