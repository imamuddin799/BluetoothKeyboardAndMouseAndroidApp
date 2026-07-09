package com.arena.hidcompatibilitytester.ui.screen.landscape.trackpad

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
fun LandscapeTrackpadStatusBar(
    isReady: Boolean,
    settings: LandscapeTrackpadSettings,
    // Existing
    onShowSettings: () -> Unit,
    onToggleSystemKb: () -> Unit,
    onToggleInAppKb: () -> Unit,
    onToggleOptionalRows: () -> Unit,
    systemKbVisible: Boolean,
    inAppKbVisible: Boolean,
    showOptionalRows: Boolean,
    hasOptionalRows: Boolean,
    // New — for in-app keyboard toggles
    kbState: LandscapeKbState,
    currentLayoutMode: LandscapeLayoutMode,
    currentRightColumn: LandscapeRightColumnMode,
    onToggleLayoutMode: () -> Unit,
    onToggleRightColumn: () -> Unit,
    onClearMods: () -> Unit,
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
            // ── 1. Trackpad LED indicators ─────────────────────────────────
            LandscapeTrackpadLedBadge("TPD", isReady)
            LandscapeTrackpadLedBadge("TAP", settings.tapToClick)
            LandscapeTrackpadLedBadge("ACC", settings.accelerationEnabled)
            if (settings.showArrowKeys) LandscapeTrackpadLedBadge("ARR", true)
            LandscapeTrackpadLedBadge("LCK", settings.dragLockMode)

            Spacer(Modifier.width(6.dp))

            // ── 2. Combo preview (inline) ──────────────────────────────────
            if (settings.trackpadKbShowComboPreview && inAppKbVisible &&
                (kbState.anyMod || kbState.lastKey.isNotEmpty())
            ) {
                val combo = kbState.modPrefix() + kbState.lastKey
                Surface(
                    color = Color(0xFF0A1828),
                    shape = RoundedCornerShape(3.dp),
                ) {
                    Text(
                        text = when {
                            kbState.anyMod && kbState.lastKey.isEmpty() ->
                                "▶ ${kbState.modPrefix().trimEnd('+')}+ …"
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

                if (kbState.anyMod) {
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

            // ── 3. Ready / Offline ─────────────────────────────────────────
            Text(
                if (isReady) "● Ready" else "○ Offline",
                fontSize = 9.sp,
                color = if (isReady) Color(0xFF81C784) else Color(0xFFEF9A9A),
                maxLines = 1,
            )

            Spacer(Modifier.width(4.dp))

            // ── 4. Toggles ─────────────────────────────────────────────────
            // System keyboard — always visible if enabled
            if (settings.showSystemKeyboard) {
                LandscapeToolbarIcon("📱", onClick = onToggleSystemKb, active = systemKbVisible)
                Spacer(Modifier.width(2.dp))
            }

            // In-app keyboard toggle — always visible if enabled
            if (settings.showInAppKeyboard) {
                // Enhanced toggles when in-app kb visible AND system kb not visible
                if (inAppKbVisible && !systemKbVisible) {
                    if (hasOptionalRows) {
                        LandscapeToolbarIcon(
                            icon = "±",
                            onClick = onToggleOptionalRows,
                            active = showOptionalRows,
                        )
                        Spacer(Modifier.width(2.dp))
                    }

                    LandscapeToolbarIcon(
                        icon = if (currentLayoutMode == LandscapeLayoutMode.TWO_COLUMN) "▥" else "▤",
                        onClick = onToggleLayoutMode,
                        active = currentLayoutMode == LandscapeLayoutMode.TWO_COLUMN,
                    )
                    Spacer(Modifier.width(2.dp))

                    if (currentLayoutMode == LandscapeLayoutMode.TWO_COLUMN) {
                        LandscapeToolbarIcon(
                            icon = if (currentRightColumn == LandscapeRightColumnMode.NUMPAD) "🔢" else "↕",
                            onClick = onToggleRightColumn,
                            active = currentRightColumn == LandscapeRightColumnMode.NUMPAD,
                        )
                        Spacer(Modifier.width(2.dp))
                    }
                }

                LandscapeToolbarIcon("⌨", onClick = onToggleInAppKb, active = inAppKbVisible)
                Spacer(Modifier.width(2.dp))
            }

            LandscapeToolbarIcon("⚙", onClick = onShowSettings)
        }
    }
}

@Composable
private fun LandscapeTrackpadIconToggle(
    icon: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (active) Color(0xFF1B5E20).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.06f),
        animationSpec = tween(150),
        label = "lTrackpadIconToggleBg",
    )

    val fg = if (active) Color(0xFF81C784) else Color.White.copy(0.45f)

    Surface(color = bg, shape = RoundedCornerShape(4.dp)) {
        Box(modifier = Modifier.clickable(onClick = onClick)) {
            Text(
                text = icon,
                fontSize = 13.sp,
                color = fg,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun LandscapeTrackpadLedBadge(label: String, active: Boolean) {
    val bg by animateColorAsState(
        targetValue = if (active) Color(0xFF1565C0) else Color.White.copy(0.04f),
        animationSpec = tween(100),
        label = "lTrackpadLed",
    )
    Surface(shape = RoundedCornerShape(3.dp), color = bg) {
        Text(
            label,
            fontSize = 7.sp,
            color = if (active) Color.White else Color(0xFF3A4A5A),
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            maxLines = 1,
        )
    }
}