package com.arena.hidcompatibilitytester.ui.screen.landscape.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.hidcompatibilitytester.bluetooth.BleHidState
import com.arena.hidcompatibilitytester.ui.screen.landscape.LandscapeMode

@Composable
fun LandscapeStatusBar(
    mode: LandscapeMode,
    bleHidState: BleHidState,
    connectedHostCount: Int,
    pointerSpeed: Float,
    holdEnabled: Boolean,
    isReady: Boolean,
    sidebarVisible: Boolean,
    onHoldToggle: () -> Unit,
    onMenuClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showMenuButton = !sidebarVisible
    val showCloseButton = sidebarVisible

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color(0xFF050C14))
            .padding(start = 2.dp, end = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // Menu / Close button
            Surface(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(
                        onClick = if (showMenuButton) onMenuClick else onCloseClick
                    ),
                color = if (showCloseButton) Color(0xFFB71C1C).copy(alpha = 0.3f)
                else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = if (showCloseButton) Icons.Default.Close
                        else Icons.Default.Menu,
                        contentDescription = if (showCloseButton) "Close menu"
                        else "Open menu",
                        tint = if (showCloseButton) Color(0xFFEF9A9A)
                        else Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Status dot
            Surface(
                modifier = Modifier.size(8.dp),
                shape = RoundedCornerShape(4.dp),
                color = when {
                    isReady -> Color(0xFF4CAF50)
                    bleHidState is BleHidState.CONNECTED -> Color(0xFFFFC107)
                    else -> Color(0xFFEF5350)
                }
            ) {}

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = when {
                    isReady -> "Ready"
                    bleHidState is BleHidState.CONNECTED -> "Connected"
                    bleHidState is BleHidState.ADVERTISING -> "Advertising…"
                    bleHidState is BleHidState.STARTING -> "Starting…"
                    bleHidState is BleHidState.ERROR -> "Error"
                    else -> "Offline"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = when {
                    isReady -> Color(0xFF81C784)
                    bleHidState is BleHidState.CONNECTED -> Color(0xFFFFD54F)
                    else -> Color(0xFFEF9A9A)
                }
            )

            if (connectedHostCount > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF1565C0).copy(alpha = 0.3f)
                ) {
                    Text(
                        text = "$connectedHostCount host${if (connectedHostCount > 1) "s" else ""}",
                        fontSize = 9.sp,
                        color = Color(0xFF90CAF9),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Right section
        if (mode == LandscapeMode.MOUSE) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Speed:",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    SpeedIndicator(pointerSpeed)
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onHoldToggle),
                    shape = RoundedCornerShape(6.dp),
                    color = if (holdEnabled) Color(0xFF1565C0).copy(alpha = 0.4f)
                    else Color.White.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = if (holdEnabled) "Hold: ON" else "Hold: OFF",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (holdEnabled) Color(0xFF90CAF9)
                        else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusBadge("Caps", false)
                StatusBadge("Num", true)
            }
        }
    }
}

@Composable
private fun SpeedIndicator(pointerSpeed: Float) {
    val level = ((pointerSpeed - 0.5f) / 2.5f * 4).toInt().coerceIn(0, 4)
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(5) { index ->
            Surface(
                modifier = Modifier.size(width = 6.dp, height = 8.dp),
                shape = RoundedCornerShape(2.dp),
                color = if (index <= level) Color(0xFF64B5F6)
                else Color.White.copy(alpha = 0.2f)
            ) {}
        }
    }
}

@Composable
private fun StatusBadge(label: String, active: Boolean) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (active) Color(0xFF1565C0).copy(alpha = 0.3f)
        else Color.White.copy(alpha = 0.05f)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) Color(0xFF90CAF9) else Color.White.copy(alpha = 0.4f),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}