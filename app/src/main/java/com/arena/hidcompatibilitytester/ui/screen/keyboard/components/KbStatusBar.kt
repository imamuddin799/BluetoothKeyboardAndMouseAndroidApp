package com.arena.hidcompatibilitytester.ui.screen.keyboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import com.arena.hidcompatibilitytester.ui.components.ToolbarIcon
import com.arena.hidcompatibilitytester.ui.screen.keyboard.KbState

@Composable
internal fun KbStatusBar(
    st: KbState,
    isReady: Boolean,
    showFullStatus: Boolean,
    showComboPreview: Boolean,
    showKeyboard: Boolean,
    showNumpad: Boolean,
    currentTab: Int,
    onClearMods: () -> Unit,
    onShowSettings: () -> Unit,
    onToggleKeyboard: () -> Unit,
    onToggleNumpad: () -> Unit,
) {
    var toolbarExpanded by remember { mutableStateOf(false) }

    // 0 = Keys     -> show only keyboard toggle
    // 1 = Nav+Num  -> show only numpad toggle
    // 2 = Media    -> no expandable row, show settings directly
    val showKeyboardToggle = currentTab == 0
    val showNumpadToggle = currentTab == 1
    val hasExpandableRow = showKeyboardToggle || showNumpadToggle

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF050C14))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Top row
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
                ).forEach { (lbl, on) ->
                    LedBadge(lbl, on)
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = if (isReady) "● Ready" else "○ Offline",
                fontSize = 9.sp,
                color = if (isReady) Color(0xFF81C784) else Color(0xFFEF9A9A),
                maxLines = 1
            )

            Spacer(Modifier.width(4.dp))

            if (hasExpandableRow) {
                ExpandCollapseToggle(
                    expanded = toolbarExpanded,
                    onClick = { toolbarExpanded = !toolbarExpanded }
                )
            } else {
                ToolbarIcon(
                    icon = "⚙",
                    onClick = onShowSettings
                )
            }
        }

        // Second row
        if (hasExpandableRow) {
            AnimatedVisibility(
                visible = toolbarExpanded,
                enter = expandVertically(
                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                    expandFrom = Alignment.Top,
                ) + fadeIn(tween(200)),
                exit = shrinkVertically(
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.Top,
                ) + fadeOut(tween(150)),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.weight(1f))

                    if (showKeyboardToggle) {
                        StatusBarIconToggle(
                            icon = "⌨",
                            active = showKeyboard,
                            onClick = onToggleKeyboard,
                        )
                    }

                    if (showNumpadToggle) {
                        StatusBarIconToggle(
                            icon = "🔢",
                            active = showNumpad,
                            onClick = onToggleNumpad,
                        )
                    }

                    ToolbarIcon(
                        icon = "⚙",
                        onClick = onShowSettings,
                    )
                }
            }
        }

        // Combo row
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
private fun ExpandCollapseToggle(
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = Color.White.copy(alpha = 0.06f),
        shape = RoundedCornerShape(4.dp),
    ) {
        Box(
            modifier = Modifier.clickable(onClick = onClick)
        ) {
            Text(
                text = if (expanded) "▲" else "▼",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(0.7f),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun StatusBarIconToggle(
    icon: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (active) {
            Color(0xFF1B5E20).copy(alpha = 0.7f)
        } else {
            Color.White.copy(alpha = 0.06f)
        },
        animationSpec = tween(150),
        label = "iconToggleBg",
    )

    val fg = if (active) Color(0xFF81C784) else Color.White.copy(0.45f)

    Surface(
        color = bg,
        shape = RoundedCornerShape(4.dp),
    ) {
        Box(
            modifier = Modifier.clickable(onClick = onClick)
        ) {
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
internal fun LedBadge(label: String, active: Boolean) {
    val bg by animateColorAsState(
        targetValue = if (active) Color(0xFF1565C0) else Color.White.copy(0.04f),
        animationSpec = tween(100),
        label = "led",
    )

    Surface(
        shape = RoundedCornerShape(3.dp),
        color = bg,
    ) {
        Text(
            text = label,
            fontSize = 7.sp,
            color = if (active) Color.White else Color(0xFF3A4A5A),
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            maxLines = 1
        )
    }
}