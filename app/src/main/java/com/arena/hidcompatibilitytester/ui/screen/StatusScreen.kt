package com.arena.hidcompatibilitytester.ui.screen

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.hidcompatibilitytester.bluetooth.BleHidManager
import com.arena.hidcompatibilitytester.bluetooth.BleHidState

@SuppressLint("MissingPermission")
@Composable
fun StatusScreen(
    bleHidState       : BleHidState,
    bleSupported      : Boolean,
    connectedHostList : List<BleHidManager.DeviceInfo>,
    onToggleBleHid    : () -> Unit,
    onDisconnectHost  : (String) -> Unit,
    onReconnectHost   : (BluetoothDevice) -> Unit,
) {
    LazyColumn(
        modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        if (!bleSupported) item { ErrorCard("⚠ BLE peripheral not supported on this device.") }

        if (bleHidState is BleHidState.ERROR)
            item { ErrorCard("✗ ${bleHidState.message}") }

        if (bleHidState is BleHidState.ADVERTISING || bleHidState is BleHidState.CONNECTED)
            item { HowToConnectCard() }

        if (connectedHostList.isNotEmpty()) {
            item {
                Text(
                    "Connected Hosts (${connectedHostList.size}/4)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp,
                    color      = MaterialTheme.colorScheme.primary
                )
            }
            items(connectedHostList) { info ->
                ConnectedHostCard(
                    info         = info,
                    onDisconnect = { onDisconnectHost(info.address) },
                    onReconnect  = { onReconnectHost(info.device) }
                )
            }
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        colors   = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C).copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(message, color = MaterialTheme.colorScheme.error,
            fontSize = 13.sp, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun HowToConnectCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(10.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF0D47A1).copy(alpha = 0.08f))
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("How to connect:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("1. Open Bluetooth settings on host (PC / Mac / Phone)", fontSize = 12.sp)
            Text("2. Find \"${android.os.Build.MODEL}\" in the device list", fontSize = 12.sp)
            Text("3. Tap/click to pair",                                   fontSize = 12.sp)
            Text("4. Host sees it as mouse + keyboard",                    fontSize = 12.sp)
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun ConnectedHostCard(
    info        : BleHidManager.DeviceInfo,
    onDisconnect: () -> Unit,
    onReconnect : () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(10.dp),
        colors   = CardDefaults.cardColors(
            containerColor = if (info.isSubscribed) Color(0xFF1B5E20).copy(alpha = 0.10f)
                             else Color(0xFFF57F17).copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        info.name ?: "Device (${info.address.takeLast(8)})",
                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1
                    )
                    Text(info.address, fontSize = 11.sp, color = Color.Gray)
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (info.isSubscribed) Color(0xFF2E7D32).copy(alpha = 0.15f)
                            else Color(0xFFF57F17).copy(alpha = 0.15f)
                ) {
                    Text(
                        if (info.isSubscribed) "✓ Ready" else "⏳ Pairing…",
                        color      = if (info.isSubscribed) Color(0xFF2E7D32) else Color(0xFFF57F17),
                        fontSize   = 11.sp, fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(28.dp)) {
                    Text(if (expanded) "▲" else "▼", fontSize = 12.sp)
                }
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick  = { onReconnect(); expanded = false },
                        modifier = Modifier.weight(1f)
                    ) { Text("Reconnect", fontSize = 12.sp) }
                    Button(
                        onClick  = { onDisconnect(); expanded = false },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Disconnect", fontSize = 12.sp, color = Color.White) }
                }
            }
        }
    }
}