package com.arena.hidcompatibilitytester.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.hidcompatibilitytester.bluetooth.BleHidManager
import com.arena.hidcompatibilitytester.bluetooth.BleHidState

@Composable
fun AppStatusBar(
    state             : BleHidState,
    supported         : Boolean,
    connectedHostList : List<BleHidManager.DeviceInfo>,
    onToggle          : () -> Unit,
) {
    val isRunning = state is BleHidState.ADVERTISING || state is BleHidState.CONNECTED
    val (badgeText, badgeColor) = when (state) {
        BleHidState.IDLE          -> "IDLE"          to Color.Gray
        BleHidState.STARTING      -> "STARTING…"     to Color(0xFFF57F17)
        BleHidState.ADVERTISING   -> "ADVERTISING"   to Color(0xFF1565C0)
        BleHidState.CONNECTED     -> "CONNECTED ✓"   to Color(0xFF2E7D32)
        is BleHidState.ERROR      -> "ERROR"          to Color.Red
        BleHidState.PAIR_REQUIRED -> "PAIR REQUIRED"  to Color(0xFFF57F17)
    }

    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("HID Clone", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${connectedHostList.size} host(s)", fontSize = 11.sp, color = Color.Gray)
            }
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(shape = RoundedCornerShape(20.dp), color = badgeColor.copy(alpha = 0.15f)) {
                    Text(
                        badgeText, color = badgeColor, fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                if (supported) {
                    Button(
                        onClick        = onToggle,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors         = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) MaterialTheme.colorScheme.error
                                             else MaterialTheme.colorScheme.primary)
                    ) { Text(if (isRunning) "Stop" else "Start", fontSize = 12.sp) }
                }
            }
        }
    }
}