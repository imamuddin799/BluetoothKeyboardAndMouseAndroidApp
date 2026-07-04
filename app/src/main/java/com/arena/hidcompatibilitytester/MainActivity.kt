package com.arena.hidcompatibilitytester

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.activity.OnBackPressedCallback
import com.arena.hidcompatibilitytester.bluetooth.BleHidManager
import com.arena.hidcompatibilitytester.bluetooth.BleHidState
import com.arena.hidcompatibilitytester.bluetooth.BluetoothDeviceManager
import com.arena.hidcompatibilitytester.service.HidInputService
import com.arena.hidcompatibilitytester.ui.screen.AppMainScreen
import com.arena.hidcompatibilitytester.ui.screen.keyboard.KeyboardSettings
import com.arena.hidcompatibilitytester.ui.screen.keyboard.KeyboardSettingsSheet
import com.arena.hidcompatibilitytester.ui.screen.keyboard.KeyboardSettingsStore
import com.arena.hidcompatibilitytester.ui.screen.trackpad.TrackpadSettings
import com.arena.hidcompatibilitytester.ui.screen.trackpad.TrackpadSettingsSheet
import com.arena.hidcompatibilitytester.ui.screen.trackpad.TrackpadSettingsStore
import com.arena.hidcompatibilitytester.ui.theme.HIDCompatibilityTesterTheme

class MainActivity : ComponentActivity(),
    BluetoothDeviceManager.BluetoothStateListener {

    private lateinit var deviceManager: BluetoothDeviceManager
    private lateinit var bleHidManager: BleHidManager
    private var trackpadSettings  by mutableStateOf(TrackpadSettings())
    private var keyboardSettings  by mutableStateOf(KeyboardSettings())
    private var showTrackpadSettingsSheet  by mutableStateOf(false)
    private var showKeyboardSettingsSheet by mutableStateOf(false)

    private val pairedDevices     = androidx.compose.runtime.snapshots.SnapshotStateList<BluetoothDevice>()
    private val nearbyDevices     = androidx.compose.runtime.snapshots.SnapshotStateList<BluetoothDevice>()
    private val connectedHostList = androidx.compose.runtime.snapshots.SnapshotStateList<BleHidManager.DeviceInfo>()

    private var isScanningState            by mutableStateOf(false)
    private var showLocationServicesDialog by mutableStateOf(false)
    private var bleHidState                by mutableStateOf<BleHidState>(BleHidState.IDLE)
    private var statusMessage              by mutableStateOf<String?>(null)
    private var bleSupported               by mutableStateOf(false)
    private var pairRequiredAddress        by mutableStateOf<String?>(null)
    private var showExitConfirmation       by mutableStateOf(false)

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_ON  -> { statusMessage = "Bluetooth on"; bleHidManager.start() }
                BluetoothAdapter.STATE_OFF -> {
                    bleHidManager.onBluetoothOff()
                    bleHidState = BleHidState.IDLE
                    statusMessage = "Bluetooth turned off"
                    runOnUiThread { connectedHostList.clear() }
                }
            }
        }
    }

    private val bondStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothDevice.ACTION_BOND_STATE_CHANGED) return
            val device: BluetoothDevice = (
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                else
                    @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            ) ?: return
            val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
            bleHidManager.onBondStateChanged(device, bondState)
        }
    }

    private fun requiredPermissions() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) else arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { _ -> executeBluetoothOperations() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        trackpadSettings  = TrackpadSettingsStore.load(this)
        keyboardSettings  = KeyboardSettingsStore.load(this)

        bleHidManager = BleHidManager(this)
        bleSupported  = bleHidManager.isSupported()

        bleHidManager.onStateChanged = { state ->
            bleHidState = state
            when (state) {
                is BleHidState.ADVERTISING -> statusMessage = "📡 Advertising…"
                is BleHidState.CONNECTED   -> statusMessage = "✓ Host connected"
                is BleHidState.ERROR       -> statusMessage = "✗ ${state.message}"
                else -> {}
            }
        }
        bleHidManager.onDeviceListChanged = { list ->
            runOnUiThread { connectedHostList.clear(); connectedHostList.addAll(list) }
        }
        bleHidManager.onDeviceSubscribed = { device ->
            runOnUiThread { statusMessage = "✓ Host ready: ${device.address}" }
        }
        bleHidManager.onPairRequired = { address -> pairRequiredAddress = address }

        deviceManager = BluetoothDeviceManager(this) { newDevice ->
            val known = pairedDevices.any { it.address == newDevice.address } ||
                        nearbyDevices.any { it.address == newDevice.address }
            if (!known) nearbyDevices.add(newDevice)
        }
        deviceManager.onScanFinished = { runOnUiThread { isScanningState = false } }
        deviceManager.onLocationServicesRequired = {
            runOnUiThread { isScanningState = false; showLocationServicesDialog = true }
        }

        registerReceiverCompat(bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        registerReceiverCompat(bondStateReceiver,
            IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))

        checkAndRequestPermissions()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmation = true
            }
        })

        setContent {
            HIDCompatibilityTesterTheme {
                MainContent()
            }
        }
    }

    @Composable
    private fun MainContent() {
        if (showLocationServicesDialog) {
            AlertDialog(
                onDismissRequest = { showLocationServicesDialog = false },
                title   = { Text("Location Services Required") },
                text    = { Text("Android requires Location Services for Bluetooth scanning.") },
                confirmButton = {
                    TextButton(onClick = {
                        showLocationServicesDialog = false
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }) { Text("Open Settings") }
                },
                dismissButton = {
                    TextButton(onClick = { showLocationServicesDialog = false }) { Text("Cancel") }
                }
            )
        }

        pairRequiredAddress?.let { addr ->
            AlertDialog(
                onDismissRequest = { pairRequiredAddress = null },
                title = { Text("Re-Pairing Required") },
                text  = { Text("Host ($addr) removed pairing.\nRe-pair from host's Bluetooth settings.") },
                confirmButton = {
                    TextButton(onClick = { pairRequiredAddress = null }) { Text("OK") }
                }
            )
        }

        if (showExitConfirmation) {
            AlertDialog(
                onDismissRequest = { showExitConfirmation = false },
                title = { Text("Exit App?") },
                text  = { Text("This will stop the HID service and disconnect all hosts. Are you sure you want to exit?") },
                confirmButton = {
                    TextButton(onClick = {
                        showExitConfirmation = false
                        finish()
                    }) {
                        Text("Exit", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                AppMainScreen(
                    modifier               = Modifier.padding(innerPadding),
                    bleHidState            = bleHidState,
                    bleSupported           = bleSupported,
                    connectedHostList      = connectedHostList,
                    pairedList             = pairedDevices,
                    nearbyList             = nearbyDevices,
                    isScanningState        = isScanningState,
                    trackpadSettings       = trackpadSettings,
                    keyboardSettings       = keyboardSettings,
                    showSettingsSheet      = showTrackpadSettingsSheet,
                    onToggleBleHid         = { toggleBleHid() },
                    onSendMouse            = { dx, dy, buttons, wheel ->
                        if (!bleHidManager.sendMouseReport(dx, dy, buttons, wheel))
                            statusMessage = "✗ No subscribed host"
                    },
                    onSendKey              = { mod, keys -> bleHidManager.sendKeyboardReport(mod, keys) },
                    onReleaseKeys          = { bleHidManager.releaseKeys() },
                    onConsumerKey          = { usage -> bleHidManager.sendConsumerKey(usage) },
                    onTypeText             = { text -> bleHidManager.typeText(text) },
                    onToggleScan           = { toggleScanState() },
                    onPairClick            = { deviceManager.pairDevice(it) },
                    onUnpairClick          = {
                        deviceManager.removePairedDevice(it)
                        bleHidManager.forgetDevice(it.address)
                        refreshDeviceLists()
                    },
                    onDisconnectHost       = { address -> bleHidManager.disconnectDevice(address) },
                    onReconnectHost        = { device ->
                        bleHidManager.inviteReconnect(device)
                        statusMessage = "Inviting ${device.address}…"
                    },
                    onShowTrackpadSettings = { showTrackpadSettingsSheet = true },
                    onShowKeyboardSettings = { showKeyboardSettingsSheet = true },
                    onSettingsChange = { newSettings ->
                        keyboardSettings = newSettings
                        KeyboardSettingsStore.save(this@MainActivity, newSettings)
                    },
                )

                statusMessage?.let { msg ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = { statusMessage = null }) { Text("OK") }
                        }
                    ) { Text(msg) }
                }

                // Trackpad settings overlay
                if (showTrackpadSettingsSheet) {
                    TrackpadSettingsSheet(
                        settings  = trackpadSettings,
                        onDismiss = { showTrackpadSettingsSheet = false },
                        onSave    = { newSettings ->
                            trackpadSettings = newSettings
                            TrackpadSettingsStore.save(this@MainActivity, newSettings)
                        }
                    )
                }

                // Keyboard settings overlay
                if (showKeyboardSettingsSheet) {
                    KeyboardSettingsSheet(
                        settings  = keyboardSettings,
                        onDismiss = { showKeyboardSettingsSheet = false },
                        onSave    = { newSettings ->
                            keyboardSettings = newSettings
                            KeyboardSettingsStore.save(this@MainActivity, newSettings)
                        }
                    )
                }
            }
        }
    }

    private fun toggleBleHid() {
        when (bleHidState) {
            is BleHidState.IDLE, is BleHidState.ERROR -> bleHidManager.start()
            else -> bleHidManager.stop()
        }
    }

    private fun checkAndRequestPermissions() {
        val allGranted = requiredPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) executeBluetoothOperations()
        else requestPermissionLauncher.launch(requiredPermissions())
    }

    private fun executeBluetoothOperations() {
        startPersistentHidService()
        deviceManager.registerStateListener(this)
        refreshDeviceLists()
        if (bleHidManager.isSupported() &&
            (bleHidState is BleHidState.IDLE || bleHidState is BleHidState.ERROR))
            bleHidManager.start()
    }

    private fun toggleScanState() {
        if (isScanningState) { deviceManager.stopNearbyScanning(); isScanningState = false }
        else { nearbyDevices.clear(); if (deviceManager.startNearbyScanning()) isScanningState = true }
    }

    private fun refreshDeviceLists() {
        pairedDevices.clear()
        pairedDevices.addAll(deviceManager.getPairedDevices())
    }

    private fun startPersistentHidService() {
        val intent = Intent(this, HidInputService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
    }

    override fun onBondStateChanged(device: BluetoothDevice, state: Int) {
        runOnUiThread {
            refreshDeviceLists()
            if (state == BluetoothDevice.BOND_BONDED)
                nearbyDevices.removeAll { it.address == device.address }
        }
    }

    override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
        runOnUiThread { refreshDeviceLists() }
    }

    private fun registerReceiverCompat(receiver: BroadcastReceiver, filter: IntentFilter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        else
            registerReceiver(receiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(bluetoothStateReceiver) } catch (_: Exception) {}
        try { unregisterReceiver(bondStateReceiver)       } catch (_: Exception) {}
        deviceManager.stopNearbyScanning()
        deviceManager.unregisterStateListener()
        val stopIntent = Intent(this, HidInputService::class.java).apply {
            action = HidInputService.ACTION_STOP
        }
        stopService(stopIntent)
    }
}