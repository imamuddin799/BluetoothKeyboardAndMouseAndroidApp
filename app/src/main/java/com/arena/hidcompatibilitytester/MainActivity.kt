package com.arena.hidcompatibilitytester

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.arena.hidcompatibilitytester.ui.screen.landscape.LandscapeMainScreen
import com.arena.hidcompatibilitytester.bluetooth.BleHidManager
import com.arena.hidcompatibilitytester.bluetooth.BleHidState
import com.arena.hidcompatibilitytester.bluetooth.BluetoothDeviceManager
import com.arena.hidcompatibilitytester.service.HidInputService
import com.arena.hidcompatibilitytester.ui.screen.AppMainScreen
import com.arena.hidcompatibilitytester.ui.screen.keyboard.KeyboardSettings
import com.arena.hidcompatibilitytester.ui.screen.keyboard.KeyboardSettingsSheet
import com.arena.hidcompatibilitytester.ui.screen.keyboard.KeyboardSettingsStore
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.LandscapeKeyboardSettings
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.LandscapeKeyboardSettingsStore
import com.arena.hidcompatibilitytester.ui.screen.landscape.trackpad.LandscapeTrackpadSettings
import com.arena.hidcompatibilitytester.ui.screen.landscape.trackpad.LandscapeTrackpadSettingsStore
import com.arena.hidcompatibilitytester.ui.screen.trackpad.TrackpadSettings
import com.arena.hidcompatibilitytester.ui.screen.trackpad.TrackpadSettingsSheet
import com.arena.hidcompatibilitytester.ui.screen.trackpad.TrackpadSettingsStore
import com.arena.hidcompatibilitytester.ui.theme.HIDCompatibilityTesterTheme

class MainActivity : ComponentActivity(),
    BluetoothDeviceManager.BluetoothStateListener {

    private var bleHidManager: BleHidManager? = null
    private var hidService: HidInputService? = null
    private var serviceBound = false

    // ── Portrait settings ────────────────────────────────────────────────────
    private var trackpadSettings          by mutableStateOf(TrackpadSettings())
    private var keyboardSettings          by mutableStateOf(KeyboardSettings())
    private var showTrackpadSettingsSheet by mutableStateOf(false)
    private var showKeyboardSettingsSheet by mutableStateOf(false)

    // ── Landscape settings ───────────────────────────────────────────────────
    private var landscapeTrackpadSettings by mutableStateOf(LandscapeTrackpadSettings())
    private var landscapeKeyboardSettings by mutableStateOf(LandscapeKeyboardSettings())

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

    private var targetMode    by mutableStateOf(BleHidManager.TargetMode.ALL)
    private var targetAddress by mutableStateOf<String?>(null)

    private lateinit var deviceManager: BluetoothDeviceManager

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, iBinder: IBinder?) {
            val binder = iBinder as HidInputService.LocalBinder
            hidService = binder.getService()
            bleHidManager = binder.getService().bleHidManager
            serviceBound = true
            bleSupported = bleHidManager?.isSupported() ?: false
            setupBleHidCallbacks()
            refreshDeviceLists()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
            hidService = null
            bleHidManager = null
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
        ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        executeBluetoothOperations()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyImmersiveMode(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isLand = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        applyImmersiveMode(isLand)

        // Load portrait settings
        trackpadSettings = TrackpadSettingsStore.load(this)
        keyboardSettings = KeyboardSettingsStore.load(this)

        // Load landscape settings
        landscapeTrackpadSettings = LandscapeTrackpadSettingsStore.load(this)
        landscapeKeyboardSettings = LandscapeKeyboardSettingsStore.load(this)

        deviceManager = BluetoothDeviceManager(this) { newDevice ->
            val known = pairedDevices.any { it.address == newDevice.address } ||
                        nearbyDevices.any { it.address == newDevice.address }
            if (!known) nearbyDevices.add(newDevice)
        }
        deviceManager.onScanFinished = {
            runOnUiThread { isScanningState = false }
        }
        deviceManager.onLocationServicesRequired = {
            runOnUiThread { isScanningState = false; showLocationServicesDialog = true }
        }

        startPersistentHidService()
        bindToHidService()

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

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            deviceManager.stopNearbyScanning()
            deviceManager.unregisterStateListener()
            hidService?.clearActivityCallbacks()
            if (serviceBound) {
                unbindService(serviceConnection)
                serviceBound = false
            }
        }
    }

    private fun startPersistentHidService() {
        val intent = Intent(this, HidInputService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(intent)
        else
            startService(intent)
    }

    private fun bindToHidService() {
        val intent = Intent(this, HidInputService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun setupBleHidCallbacks() {
        val manager = bleHidManager ?: return
        val service = hidService ?: return

        bleHidState  = manager.getCurrentState()
        bleSupported = manager.isSupported()

        val currentDevices = manager.getConnectedDeviceInfoList()
        runOnUiThread {
            connectedHostList.clear()
            connectedHostList.addAll(currentDevices)
        }

        service.activityStateCallback = { state ->
            runOnUiThread {
                bleHidState = state
                when (state) {
                    is BleHidState.ADVERTISING -> statusMessage = "📡 Advertising…"
                    is BleHidState.CONNECTED   -> statusMessage = "✓ Host connected"
                    is BleHidState.ERROR       -> statusMessage = "✗ ${state.message}"
                    else -> {}
                }
            }
        }

        service.activityDeviceListCallback = { list ->
            runOnUiThread {
                connectedHostList.clear()
                connectedHostList.addAll(list)
            }
        }

        service.activityDeviceSubscribedCallback = { device ->
            runOnUiThread {
                statusMessage = "✓ Host ready: ${device.address}"
            }
        }

        service.activityPairRequiredCallback = { address ->
            runOnUiThread { pairRequiredAddress = address }
        }

        targetMode    = manager.getTargetMode()
        targetAddress = manager.getTargetAddress()
    }

    private fun onSelectAllTargets() {
        bleHidManager?.setTargetAll()
        targetMode    = BleHidManager.TargetMode.ALL
        targetAddress = null
    }

    private fun onSelectTargetDevice(address: String) {
        bleHidManager?.setTargetDevice(address)
        targetMode    = BleHidManager.TargetMode.SINGLE
        targetAddress = address
    }

    @Composable
    private fun MainContent() {
        val configuration = LocalConfiguration.current
        val isLandscape   = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        // ── Dialogs ──────────────────────────────────────────────────────────
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
                    TextButton(onClick = { showLocationServicesDialog = false }) {
                        Text("Cancel")
                    }
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
                title = { Text("Close App?") },
                text = {
                    Text(
                        "Keep Running — closes the UI but keeps Bluetooth HID " +
                        "active in the background.\n\n" +
                        "Stop & Exit — disconnects all hosts and stops the service."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showExitConfirmation = false
                            stopHidServiceAndExit()
                        }
                    ) {
                        Text("Stop & Exit", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showExitConfirmation = false }) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = {
                                showExitConfirmation = false
                                finish()
                            }
                        ) {
                            Text("Keep Running")
                        }
                    }
                }
            )
        }

        // ── Main Scaffold ────────────────────────────────────────────────────
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {

                if (isLandscape) {
                    LandscapeMainScreen(
                        bleHidState              = bleHidState,
                        bleSupported             = bleSupported,
                        connectedHostList        = connectedHostList,
                        pairedList               = pairedDevices,
                        nearbyList               = nearbyDevices,
                        isScanningState          = isScanningState,
                        trackpadSettings         = landscapeTrackpadSettings,
                        keyboardSettings         = landscapeKeyboardSettings,
                        targetMode               = targetMode,
                        targetAddress            = targetAddress,
                        onSelectAllTargets       = { onSelectAllTargets() },
                        onSelectTargetDevice     = { address -> onSelectTargetDevice(address) },
                        onToggleBleHid           = { toggleBleHid() },
                        onSendMouse              = { dx, dy, buttons, wheel ->
                            if (bleHidManager?.sendMouseReport(dx, dy, buttons, wheel) == false)
                                statusMessage = "✗ No subscribed host"
                        },
                        onSendKey                = { mod, keys ->
                            bleHidManager?.sendKeyboardReport(mod, keys)
                        },
                        onReleaseKeys            = { bleHidManager?.releaseKeys() },
                        onConsumerKey            = { usage ->
                            bleHidManager?.sendConsumerKey(usage)
                        },
                        onTypeText               = { text -> bleHidManager?.typeText(text) },
                        onDisconnectHost         = { address ->
                            bleHidManager?.disconnectDevice(address)
                        },
                        onReconnectHost          = { device ->
                            bleHidManager?.inviteReconnect(device)
                            statusMessage = "Inviting ${device.address}…"
                        },
                        onScanDevices            = { toggleScanState() },
                        onPairClick              = { deviceManager.pairDevice(it) },
                        onUnpairClick            = {
                            deviceManager.removePairedDevice(it)
                            bleHidManager?.forgetDevice(it.address)
                            refreshDeviceLists()
                        },
                        onTrackpadSettingsChange = { newSettings ->
                            landscapeTrackpadSettings = newSettings
                            LandscapeTrackpadSettingsStore.save(this@MainActivity, newSettings)
                        },
                        onKeyboardSettingsChange = { newSettings ->
                            landscapeKeyboardSettings = newSettings
                            LandscapeKeyboardSettingsStore.save(this@MainActivity, newSettings)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                } else {
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
                        targetMode             = targetMode,
                        targetAddress          = targetAddress,
                        onSelectAllTargets     = { onSelectAllTargets() },
                        onSelectTargetDevice   = { address -> onSelectTargetDevice(address) },
                        onToggleBleHid         = { toggleBleHid() },
                        onSendMouse            = { dx, dy, buttons, wheel ->
                            if (bleHidManager?.sendMouseReport(dx, dy, buttons, wheel) == false)
                                statusMessage = "✗ No subscribed host"
                        },
                        onSendKey              = { mod, keys ->
                            bleHidManager?.sendKeyboardReport(mod, keys)
                        },
                        onReleaseKeys          = { bleHidManager?.releaseKeys() },
                        onConsumerKey          = { usage ->
                            bleHidManager?.sendConsumerKey(usage)
                        },
                        onTypeText             = { text -> bleHidManager?.typeText(text) },
                        onToggleScan           = { toggleScanState() },
                        onPairClick            = { deviceManager.pairDevice(it) },
                        onUnpairClick          = {
                            deviceManager.removePairedDevice(it)
                            bleHidManager?.forgetDevice(it.address)
                            refreshDeviceLists()
                        },
                        onDisconnectHost       = { address ->
                            bleHidManager?.disconnectDevice(address)
                        },
                        onReconnectHost        = { device ->
                            bleHidManager?.inviteReconnect(device)
                            statusMessage = "Inviting ${device.address}…"
                        },
                        onShowTrackpadSettings = { showTrackpadSettingsSheet = true },
                        onShowKeyboardSettings = { showKeyboardSettingsSheet = true },
                        onSettingsChange       = { newSettings ->
                            keyboardSettings = newSettings
                            KeyboardSettingsStore.save(this@MainActivity, newSettings)
                        },
                    )
                }

                // ── Snackbar ─────────────────────────────────────────────────
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

                // ── Settings sheets — portrait only ──────────────────────────
                if (!isLandscape) {
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
    }

    private fun stopHidServiceAndExit() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
            as android.app.NotificationManager
        notificationManager.cancelAll()

        bleHidManager?.clearAllKnownHosts()
        bleHidManager?.stop()

        hidService?.clearActivityCallbacks()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }

        val stopIntent = Intent(this, HidInputService::class.java).apply {
            action = HidInputService.ACTION_STOP
        }
        stopService(stopIntent)

        finish()
    }

    private fun toggleBleHid() {
        val manager = bleHidManager ?: return
        when (bleHidState) {
            is BleHidState.IDLE, is BleHidState.ERROR -> manager.start()
            else -> manager.stop()
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
        deviceManager.registerStateListener(this)
        refreshDeviceLists()
    }

    private fun toggleScanState() {
        if (isScanningState) {
            deviceManager.stopNearbyScanning()
            isScanningState = false
        } else {
            nearbyDevices.clear()
            if (deviceManager.startNearbyScanning()) isScanningState = true
        }
    }

    private fun refreshDeviceLists() {
        pairedDevices.clear()
        pairedDevices.addAll(deviceManager.getPairedDevices())
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
    private fun applyImmersiveMode(landscape: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (landscape) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
