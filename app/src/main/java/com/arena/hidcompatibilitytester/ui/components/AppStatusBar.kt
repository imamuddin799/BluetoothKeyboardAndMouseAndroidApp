package com.arena.hidcompatibilitytester.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.hidcompatibilitytester.bluetooth.BleHidManager
import com.arena.hidcompatibilitytester.bluetooth.BleHidState

@SuppressLint("MissingPermission")
@Composable
fun AppStatusBar(
    state: BleHidState,
    supported: Boolean,
    connectedHostList: List<BleHidManager.DeviceInfo>,
    targetMode: BleHidManager.TargetMode,
    targetAddress: String?,
    onSelectAllTargets: () -> Unit,
    onSelectTargetDevice: (String) -> Unit,
) {
    val subscribedHosts = connectedHostList.filter { it.isSubscribed }
    val showTargetDropdown = subscribedHosts.size > 1

    val (statusDot, statusText, statusColor) = when (state) {
        BleHidState.IDLE          -> Triple("○", "Idle", Color.Gray)
        BleHidState.STARTING      -> Triple("◐", "Starting…", Color(0xFFF57F17))
        BleHidState.ADVERTISING   -> Triple("◉", "Advertising", Color(0xFF1565C0))
        BleHidState.CONNECTED     -> Triple("●", "Connected", Color(0xFF2E7D32))
        is BleHidState.ERROR      -> Triple("✗", "Error", Color.Red)
        BleHidState.PAIR_REQUIRED -> Triple("◐", "Pair Required", Color(0xFFF57F17))
    }

    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side — device name + host count
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    android.os.Build.MODEL,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${connectedHostList.size} host(s)",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            // Right side — status + optional target dropdown
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(statusDot, color = statusColor, fontSize = 10.sp)
                        Text(
                            statusText,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Target dropdown — only when 2+ subscribed hosts
                if (showTargetDropdown) {
                    TargetDropdown(
                        subscribedHosts = subscribedHosts,
                        targetMode = targetMode,
                        targetAddress = targetAddress,
                        onSelectAll = onSelectAllTargets,
                        onSelectDevice = onSelectTargetDevice,
                    )
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun TargetDropdown(
    subscribedHosts: List<BleHidManager.DeviceInfo>,
    targetMode: BleHidManager.TargetMode,
    targetAddress: String?,
    onSelectAll: () -> Unit,
    onSelectDevice: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val currentLabel = when (targetMode) {
        BleHidManager.TargetMode.ALL -> "All (${subscribedHosts.size})"
        BleHidManager.TargetMode.SINGLE -> {
            val host = subscribedHosts.find { it.address == targetAddress }
            host?.name?.take(10) ?: "?"
        }
    }

    Box {
        Surface(
            onClick = { expanded = !expanded },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("🎯", fontSize = 11.sp)
                Text(
                    currentLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (expanded) "▲" else "▼",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // All Hosts option
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📡", fontSize = 14.sp)
                        Text(
                            "All Hosts (${subscribedHosts.size})",
                            fontWeight = if (targetMode == BleHidManager.TargetMode.ALL)
                                FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                        if (targetMode == BleHidManager.TargetMode.ALL) {
                            Text("✓", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                        }
                    }
                },
                onClick = {
                    onSelectAll()
                    expanded = false
                }
            )

            HorizontalDivider()

            // Individual hosts
            subscribedHosts.forEach { host ->
                val isSelected = targetMode == BleHidManager.TargetMode.SINGLE &&
                        targetAddress == host.address
                val displayName = host.name ?: "Device (${host.address.takeLast(5)})"

                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("💻", fontSize = 14.sp)
                            Text(
                                displayName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isSelected) {
                                Text("✓", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                            }
                        }
                    },
                    onClick = {
                        onSelectDevice(host.address)
                        expanded = false
                    }
                )
            }
        }
    }
}