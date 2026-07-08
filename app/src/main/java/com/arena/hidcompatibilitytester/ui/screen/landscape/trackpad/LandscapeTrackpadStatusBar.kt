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

@Composable
fun LandscapeTrackpadStatusBar(
    isReady: Boolean,
    settings: LandscapeTrackpadSettings,
    onShowSettings: () -> Unit,
    onToggleSystemKb: () -> Unit,
    onToggleInAppKb: () -> Unit,
    onToggleOptionalRows: () -> Unit,
    systemKbVisible: Boolean,
    inAppKbVisible: Boolean,
    showOptionalRows: Boolean,
    hasOptionalRows: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF050C14))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LandscapeTrackpadLedBadge("TPD", isReady)
            LandscapeTrackpadLedBadge("TAP", settings.tapToClick)
            LandscapeTrackpadLedBadge("ACC", settings.accelerationEnabled)
            if (settings.showArrowKeys) LandscapeTrackpadLedBadge("ARR", true)
            LandscapeTrackpadLedBadge("LCK", settings.dragLockMode)

            Spacer(Modifier.weight(1f))

            Text(
                if (isReady) "● Ready" else "○ Offline",
                fontSize = 9.sp,
                color = if (isReady) Color(0xFF81C784) else Color(0xFFEF9A9A)
            )

            Spacer(Modifier.width(4.dp))

            LandscapeToolbarIcon("⚙", onClick = onShowSettings)

            if (settings.showSystemKeyboard) {
                Spacer(Modifier.width(2.dp))
                LandscapeToolbarIcon("🌐", onClick = onToggleSystemKb, active = systemKbVisible)
            }

            if (settings.showInAppKeyboard) {
                Spacer(Modifier.width(2.dp))
                LandscapeToolbarIcon("⌨", onClick = onToggleInAppKb, active = inAppKbVisible)

                if (inAppKbVisible && hasOptionalRows) {
                    Spacer(Modifier.width(2.dp))
                    LandscapeTrackpadIconToggle(
                        icon = "±",
                        active = showOptionalRows,
                        onClick = onToggleOptionalRows,
                    )
                }
            }
        }

        Surface(
            color = Color(0xFF0A1828),
            shape = RoundedCornerShape(5.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                buildString {
                    append("🖱 ${settings.pointerSpeed}x")
                    if (settings.tapToClick) append(" · tap✓")
                    if (settings.dragLockMode) append(" · lock✓") else append(" · hold")
                },
                fontSize = 10.sp,
                color = Color(0xFF90CAF9),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                maxLines = 1
            )
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
        targetValue = if (active) Color(0xFF1B5E20).copy(alpha = 0.7f)
        else Color.White.copy(alpha = 0.06f),
        animationSpec = tween(150),
        label = "lTrackpadIconToggleBg",
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
            maxLines = 1
        )
    }
}