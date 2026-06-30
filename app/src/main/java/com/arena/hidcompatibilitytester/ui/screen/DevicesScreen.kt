package com.arena.hidcompatibilitytester.ui.screen

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@SuppressLint("MissingPermission")
@Composable
fun DevicesScreen(
    nearbyList      : List<BluetoothDevice>,
    pairedList      : List<BluetoothDevice>,
    isScanningState : Boolean,
    onToggleScan    : () -> Unit,
    onPairClick     : (BluetoothDevice) -> Unit,
    onUnpairClick   : (BluetoothDevice) -> Unit,
    onReconnect     : (BluetoothDevice) -> Unit,
) {
    LazyColumn(
        modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Nearby Devices", fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Button(
                    onClick = onToggleScan,
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = if (isScanningState) MaterialTheme.colorScheme.error
                                         else MaterialTheme.colorScheme.primary)
                ) { Text(if (isScanningState) "Stop" else "Scan") }
            }
        }

        if (nearbyList.isEmpty()) {
            item {
                Text("Tap Scan to discover devices.", fontSize = 13.sp,
                    color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }
        } else {
            items(nearbyList) { device ->
                DeviceRow(name = device.name ?: "Unknown", address = device.address) {
                    Button(
                        onClick = { onPairClick(device) },
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary)
                    ) { Text("Pair") }
                }
            }
        }

        item {
            Text("Paired Devices", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
        }

        if (pairedList.isEmpty()) {
            item {
                Text("No paired devices.", fontSize = 13.sp,
                    color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }
        } else {
            items(pairedList) { device ->
                DeviceRow(name = device.name ?: "Unknown", address = device.address) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { onReconnect(device) }) {
                            Text("Connect", fontSize = 12.sp) }
                        OutlinedButton(onClick = { onUnpairClick(device) }) {
                            Text("Forget", fontSize = 12.sp) }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun DeviceRow(
    name   : String,
    address: String,
    actions: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name,    fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1)
            Text(address, fontSize   = 11.sp, color = Color.Gray)
        }
        Spacer(Modifier.width(8.dp))
        actions()
    }
}