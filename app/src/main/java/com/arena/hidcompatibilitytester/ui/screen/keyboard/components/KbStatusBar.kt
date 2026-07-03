package com.arena.hidcompatibilitytester.ui.screen.keyboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.hidcompatibilitytester.ui.components.ToolbarIcon
import com.arena.hidcompatibilitytester.ui.screen.keyboard.KbState

@Composable
internal fun KbStatusBar(
    st: KbState,
    isReady: Boolean,
    showFullStatus: Boolean,
    showComboPreview: Boolean,
    onClearMods: () -> Unit,
    onShowSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF050C14))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Top row: always show ready indicator + settings button
        // LED badges only when showFullStatus is true
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showFullStatus) {
                listOf(
                    "CAPS" to st.caps,
                    "NUM" to st.numLock,
                    "SCR" to st.scrollLk,
                    "SHF" to st.shift,
                    "CTL" to st.ctrl,
                    "ALT" to st.alt,
                    "AGR" to st.altGr,
                    "WIN" to st.gui,
                    "OVR" to !st.insertMode,
                ).forEach { (lbl, on) -> LedBadge(lbl, on) }
            }

            Spacer(Modifier.weight(1f))

            Text(
                if (isReady) "● Ready" else "○ Offline",
                fontSize = 9.sp,
                color = if (isReady) Color(0xFF81C784) else Color(0xFFEF9A9A),
                maxLines = 1
            )

            Spacer(Modifier.width(4.dp))

            ToolbarIcon("⚙", onClick = onShowSettings)
        }

        // Combo row: always show when there's something to display
        // This ensures pressed keys and mod combos are always visible
        if (showComboPreview && (st.anyMod || st.lastKey.isNotEmpty())) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val combo = st.modPrefix() + st.lastKey
                Surface(
                    color = Color(0xFF0A1828),
                    shape = RoundedCornerShape(5.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = when {
                            st.anyMod && st.lastKey.isEmpty() ->
                                "▶ ${st.modPrefix().trimEnd('+')}+ …waiting"
                            combo.isEmpty() -> "Ready…"
                            else -> "⌨ $combo"
                        },
                        fontSize = 11.sp,
                        color = if (combo.isEmpty()) Color(0xFF546E7A) else Color(0xFF90CAF9),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }

                if (st.anyMod) {
                    TextButton(
                        onClick = onClearMods,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            "✕ Clear",
                            fontSize = 10.sp,
                            color = Color(0xFFEF9A9A),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun LedBadge(label: String, active: Boolean) {
    val bg by animateColorAsState(
        if (active) Color(0xFF1565C0) else Color.White.copy(0.04f),
        tween(100), label = "led",
    )
    Surface(shape = RoundedCornerShape(3.dp), color = bg) {
        Text(
            label,
            fontSize = 7.sp,
            color = if (active) Color.White else Color(0xFF3A4A5A),
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            maxLines = 1
        )
    }
}