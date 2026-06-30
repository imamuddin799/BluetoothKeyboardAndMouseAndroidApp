package com.arena.hidcompatibilitytester.ui.screen.trackpad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TrackpadStatusBar(
    isReady            : Boolean,
    settings           : TrackpadSettings,
    onShowSettings     : () -> Unit,
    onToggleSystemKb   : () -> Unit,
    onToggleInAppKb    : () -> Unit,
    systemKbVisible    : Boolean,
    inAppKbVisible     : Boolean,
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
            verticalAlignment     = Alignment.CenterVertically
        ) {
            LedBadge("TPD", isReady)
            LedBadge("TAP", settings.tapToClick)
            LedBadge("ACC", settings.accelerationEnabled)
            LedBadge("LCK", settings.dragLockMode)
            if (settings.showArrowKeys) LedBadge("ARR", true)

            Spacer(Modifier.weight(1f))

            Text(
                if (isReady) "● Ready" else "○ Offline",
                fontSize = 9.sp,
                color = if (isReady) Color(0xFF81C784) else Color(0xFFEF9A9A)
            )

            Spacer(Modifier.width(4.dp))

            // Settings
            ToolbarIcon("⚙", onClick = onShowSettings)

            // System keyboard toggle
            if (settings.showSystemKeyboard) {
                Spacer(Modifier.width(2.dp))
                ToolbarIcon("🌐", onClick = onToggleSystemKb, active = systemKbVisible)
            }

            // In-app keyboard toggle
            if (settings.showInAppKeyboard) {
                Spacer(Modifier.width(2.dp))
                ToolbarIcon("⌨", onClick = onToggleInAppKb, active = inAppKbVisible)
            }
        }

        Surface(
            color    = Color(0xFF0A1828),
            shape    = RoundedCornerShape(5.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                buildString {
                    append("🖱 ${settings.pointerSpeed}x")
                    if (settings.tapToClick) append(" · tap✓")
                    if (settings.dragLockMode) append(" · lock✓") else append(" · hold")
                },
                fontSize   = 10.sp,
                color      = Color(0xFF90CAF9),
                fontWeight = FontWeight.Medium,
                modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ToolbarIcon(
    icon    : String,
    onClick : () -> Unit,
    active  : Boolean = false,
) {
    Box(
        modifier = Modifier
            .height(28.dp)    // was 24.dp
            .clip(RoundedCornerShape(5.dp))
            .border(
                width = if (active) 1.dp else 0.5.dp,
                color = if (active) Color(0xFF4A90D9) else Color(0xFF607D8B).copy(0.3f),
                shape = RoundedCornerShape(5.dp)
            )
            .background(
                if (active) Color(0xFF1565C0).copy(0.25f) else Color(0xFF1A2332)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp),    // was 6.dp
        contentAlignment = Alignment.Center
    ) {
        Text(
            icon,
            fontSize = 14.sp,    // was 13.sp
            color    = if (active) Color.White else Color(0xFF90CAF9)
        )
    }
}