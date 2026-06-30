package com.arena.hidcompatibilitytester.ui.screen

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.arena.hidcompatibilitytester.bluetooth.BleHidManager
import com.arena.hidcompatibilitytester.bluetooth.BleHidState
import com.arena.hidcompatibilitytester.ui.components.AppStatusBar
import com.arena.hidcompatibilitytester.ui.screen.keyboard.KeyboardScreen
import com.arena.hidcompatibilitytester.ui.screen.trackpad.TrackpadScreen
import com.arena.hidcompatibilitytester.ui.screen.trackpad.TrackpadSettings

@SuppressLint("MissingPermission")
@Composable
fun AppMainScreen(
    modifier               : Modifier,
    bleHidState            : BleHidState,
    bleSupported           : Boolean,
    connectedHostList      : List<BleHidManager.DeviceInfo>,
    pairedList             : List<BluetoothDevice>,
    nearbyList             : List<BluetoothDevice>,
    isScanningState        : Boolean,
    trackpadSettings       : TrackpadSettings,
    showSettingsSheet      : Boolean,
    onToggleBleHid         : () -> Unit,
    onSendMouse            : (Int, Int, Int, Int) -> Unit,
    onSendKey              : (Int, List<Int>) -> Unit,
    onReleaseKeys          : () -> Unit,
    onConsumerKey          : (Int) -> Unit,
    onTypeText             : (String) -> Unit,
    onToggleScan           : () -> Unit,
    onPairClick            : (BluetoothDevice) -> Unit,
    onUnpairClick          : (BluetoothDevice) -> Unit,
    onDisconnectHost       : (String) -> Unit,
    onReconnectHost        : (BluetoothDevice) -> Unit,
    onShowTrackpadSettings : () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs    = listOf("Status", "Mouse", "Keyboard", "Devices")
    val isReady = connectedHostList.any { it.isSubscribed }

    Column(modifier = modifier.fillMaxSize()) {
        AppStatusBar(
            state             = bleHidState,
            supported         = bleSupported,
            connectedHostList = connectedHostList,
            onToggle          = onToggleBleHid,
        )

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected = selectedTab == i,
                    onClick  = { selectedTab = i },
                    text     = { Text(title, fontSize = 13.sp) }
                )
            }
        }

        when (selectedTab) {
            0 -> StatusScreen(
                bleHidState       = bleHidState,
                bleSupported      = bleSupported,
                connectedHostList = connectedHostList,
                onToggleBleHid    = onToggleBleHid,
                onDisconnectHost  = onDisconnectHost,
                onReconnectHost   = onReconnectHost,
            )
            1 -> TrackpadScreen(
                isReady        = isReady,
                settings       = trackpadSettings,
                onSendMouse    = onSendMouse,
                onShowSettings = onShowTrackpadSettings,
                onSendKey      = onSendKey,
                onReleaseKeys  = onReleaseKeys,
                onConsumerKey  = onConsumerKey,
                onTypeText     = onTypeText,
            )
            2 -> KeyboardScreen(
                isReady       = isReady,
                onSendKey     = onSendKey,
                onConsumerKey = onConsumerKey,
                onTypeText    = onTypeText,
            )
            3 -> DevicesScreen(
                nearbyList      = nearbyList,
                pairedList      = pairedList,
                isScanningState = isScanningState,
                onToggleScan    = onToggleScan,
                onPairClick     = onPairClick,
                onUnpairClick   = onUnpairClick,
                onReconnect     = onReconnectHost,
            )
        }
    }
}