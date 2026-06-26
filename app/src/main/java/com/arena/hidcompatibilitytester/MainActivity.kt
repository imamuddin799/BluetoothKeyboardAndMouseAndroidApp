package com.arena.hidcompatibilitytester

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.arena.hidcompatibilitytester.ui.theme.HIDCompatibilityTesterTheme

class MainActivity : ComponentActivity(), BluetoothDeviceManager.BluetoothStateListener {

    private lateinit var deviceManager: BluetoothDeviceManager
    private lateinit var bleHidManager: BleHidManager

    private val pairedDevices    = mutableStateListOf<BluetoothDevice>()
    private val nearbyDevices    = mutableStateListOf<BluetoothDevice>()
    private val bleConnectedDevices = mutableStateListOf<BluetoothDevice>()

    private var isScanningState            by mutableStateOf(false)
    private var showLocationServicesDialog by mutableStateOf(false)
    private var bleHidState: BleHidState by mutableStateOf(BleHidState.IDLE)
    private var statusMessage              by mutableStateOf<String?>(null)
    private var bleSupported               by mutableStateOf(false)

    private fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> executeBluetoothOperations() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        bleHidManager = BleHidManager(this)
        bleSupported  = bleHidManager.isSupported()

        bleHidManager.onStateChanged = { state ->
            bleHidState = state
            when (state) {
                is BleHidState.ADVERTISING -> statusMessage = "📡 Advertising as HID Mouse — waiting for host to connect"
                is BleHidState.CONNECTED   -> statusMessage = "✓ Host connected via BLE HID"
                is BleHidState.ERROR       -> statusMessage = "✗ Error: ${state.message}"
                else -> {}
            }
        }
        bleHidManager.onDeviceConnected = { device ->
            if (!bleConnectedDevices.any { it.address == device.address }) {
                bleConnectedDevices.add(device)
            }
            statusMessage = "Host connected: ${device.address}"
        }
        bleHidManager.onDeviceDisconnected = { device ->
            bleConnectedDevices.removeAll { it.address == device.address }
            statusMessage = "Host disconnected: ${device.address}"
        }

        deviceManager = BluetoothDeviceManager(this) { newDevice ->
            val alreadyKnown = pairedDevices.any  { it.address == newDevice.address } ||
                    nearbyDevices.any  { it.address == newDevice.address }
            if (!alreadyKnown) nearbyDevices.add(newDevice)
        }
        deviceManager.onScanFinished = {
            runOnUiThread { isScanningState = false }
        }
        deviceManager.onLocationServicesRequired = {
            runOnUiThread {
                isScanningState = false
                showLocationServicesDialog = true
            }
        }

        checkAndRequestPermissions()

        setContent {
            HIDCompatibilityTesterTheme {
                if (showLocationServicesDialog) {
                    AlertDialog(
                        onDismissRequest = { showLocationServicesDialog = false },
                        title   = { Text("Location Services Required") },
                        text    = {
                            Text(
                                "Android requires Location Services enabled for Bluetooth " +
                                        "device scanning.\n\nEnable Location in Settings, then scan again."
                            )
                        },
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

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        MainScreen(
                            modifier            = Modifier.padding(innerPadding),
                            bleHidState         = bleHidState,
                            bleSupported        = bleSupported,
                            bleConnectedDevices = bleConnectedDevices,
                            pairedList          = pairedDevices,
                            nearbyList          = nearbyDevices,
                            isScanningState     = isScanningState,
                            onToggleBleHid      = { toggleBleHid() },
                            onSendMouse         = { dx, dy, buttons ->
                                val ok = bleHidManager.sendMouseReport(dx, dy, buttons)
                                if (!ok) statusMessage = "✗ No host connected or not subscribed yet"
                            },
                            onSendKey           = { mod, keys ->
                                bleHidManager.sendKeyboardReport(mod, keys)
                                // Release keys after 100ms
                                android.os.Handler(android.os.Looper.getMainLooper())
                                    .postDelayed({ bleHidManager.releaseKeys() }, 100)
                            },
                            onToggleScan        = { toggleScanState() },
                            onPairClick         = { deviceManager.pairDevice(it) },
                            onUnpairClick       = {
                                deviceManager.removePairedDevice(it)
                                refreshDeviceLists()
                            }
                        )

                        statusMessage?.let { msg ->
                            Snackbar(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp),
                                action = {
                                    TextButton(onClick = { statusMessage = null }) {
                                        Text("OK")
                                    }
                                }
                            ) { Text(msg) }
                        }
                    }
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
    }

    private fun toggleScanState() {
        if (isScanningState) {
            deviceManager.stopNearbyScanning()
            isScanningState = false
        } else {
            nearbyDevices.clear()
            val started = deviceManager.startNearbyScanning()
            if (started) isScanningState = true
        }
    }

    private fun refreshDeviceLists() {
        pairedDevices.clear()
        pairedDevices.addAll(deviceManager.getPairedDevices())
    }

    private fun startPersistentHidService() {
        val serviceIntent = Intent(this, HidInputService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
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

    override fun onDestroy() {
        super.onDestroy()
        bleHidManager.stop()
        deviceManager.stopNearbyScanning()
        deviceManager.unregisterStateListener()
    }
}

// ── Main Screen ───────────────────────────────────────────────────────────────

@SuppressLint("MissingPermission")
@Composable
fun MainScreen(
    modifier            : Modifier,
    bleHidState         : BleHidState,
    bleSupported        : Boolean,
    bleConnectedDevices : List<BluetoothDevice>,
    pairedList          : List<BluetoothDevice>,
    nearbyList          : List<BluetoothDevice>,
    isScanningState     : Boolean,
    onToggleBleHid      : () -> Unit,
    onSendMouse         : (Int, Int, Int) -> Unit,
    onSendKey           : (Int, List<Int>) -> Unit,
    onToggleScan        : () -> Unit,
    onPairClick         : (BluetoothDevice) -> Unit,
    onUnpairClick       : (BluetoothDevice) -> Unit,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // ── BLE HID Control Panel ──────────────────────────────────────────
        item {
            BleHidControlCard(
                state        = bleHidState,
                supported    = bleSupported,
                onToggle     = onToggleBleHid,
                onSendMouse  = onSendMouse,
                onSendKey    = onSendKey,
                connectedDevices = bleConnectedDevices
            )
        }

        // ── How to connect instructions ────────────────────────────────────
        item {
            if (bleHidState is BleHidState.ADVERTISING || bleHidState is BleHidState.CONNECTED) {
                InstructionsCard()
            }
        }

        // ── Scan Controls ──────────────────────────────────────────────────
        item {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                SectionHeader("Nearby Devices")
                Button(
                    onClick = onToggleScan,
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = if (isScanningState)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (isScanningState) "Stop Scan" else "Scan")
                }
            }
        }

        if (nearbyList.isEmpty()) {
            item { EmptyStateLabel("Tap Scan to discover nearby devices.") }
        } else {
            items(nearbyList) { device ->
                DeviceRow(
                    name    = device.name ?: "Unknown Device",
                    address = device.address,
                    actions = {
                        Button(
                            onClick = { onPairClick(device) },
                            colors  = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary)
                        ) { Text("Pair") }
                    }
                )
            }
        }

        // ── Paired Devices ─────────────────────────────────────────────────
        item { SectionHeader("Paired Devices") }
        if (pairedList.isEmpty()) {
            item { EmptyStateLabel("No paired devices.") }
        } else {
            items(pairedList) { device ->
                DeviceRow(
                    name    = device.name ?: "Unknown Device",
                    address = device.address,
                    actions = {
                        OutlinedButton(onClick = { onUnpairClick(device) }) { Text("Forget") }
                    }
                )
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ── BLE HID Control Card ──────────────────────────────────────────────────────

@SuppressLint("MissingPermission")
@Composable
fun BleHidControlCard(
    state            : BleHidState,
    supported        : Boolean,
    onToggle         : () -> Unit,
    onSendMouse      : (Int, Int, Int) -> Unit,
    onSendKey        : (Int, List<Int>) -> Unit,
    connectedDevices : List<BluetoothDevice>
) {
    val isRunning = state is BleHidState.ADVERTISING || state is BleHidState.CONNECTED
    val isConnected = state is BleHidState.CONNECTED

    val cardBg by animateColorAsState(
        targetValue = when (state) {
            is BleHidState.CONNECTED   -> Color(0xFF1B5E20).copy(alpha = 0.15f)
            is BleHidState.ADVERTISING -> Color(0xFF0D47A1).copy(alpha = 0.12f)
            is BleHidState.ERROR       -> Color(0xFFB71C1C).copy(alpha = 0.10f)
            else                       -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "CardBg"
    )

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Title row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("BLE HID Peripheral", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("HOGP Mouse + Keyboard", fontSize = 12.sp, color = Color.Gray)
                }
                // State badge
                val (badgeText, badgeColor) = when (state) {
                    BleHidState.IDLE       -> "IDLE"        to Color.Gray
                    BleHidState.STARTING   -> "STARTING…"  to Color(0xFFF57F17)
                    BleHidState.ADVERTISING-> "ADVERTISING" to Color(0xFF1565C0)
                    BleHidState.CONNECTED  -> "CONNECTED ✓" to Color(0xFF2E7D32)
                    is BleHidState.ERROR   -> "ERROR"       to MaterialTheme.colorScheme.error
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text     = badgeText,
                        color    = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (!supported) {
                Text(
                    "⚠ BLE peripheral mode not supported on this device.",
                    color    = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            } else {
                // Start / Stop button
                Button(
                    onClick  = onToggle,
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (isRunning) "Stop BLE HID" else "Start BLE HID Peripheral")
                }

                // Connected hosts list
                if (connectedDevices.isNotEmpty()) {
                    Text("Connected Hosts:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    connectedDevices.forEach { device ->
                        Text(
                            "• ${device.name ?: "Unknown"} (${device.address})",
                            fontSize = 12.sp,
                            color    = Color.Gray
                        )
                    }
                }

                // ── Mouse controls (only when connected) ──────────────────
                if (isConnected) {
                    HorizontalDivider()
                    Text("Mouse Controls", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                    // D-pad
                    Column(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalAlignment   = Alignment.CenterHorizontally,
                        verticalArrangement   = Arrangement.spacedBy(4.dp)
                    ) {
                        // Up
                        BigMouseButton("▲") { onSendMouse(0, -40, 0) }
                        // Left / Right
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            BigMouseButton("◀") { onSendMouse(-40, 0, 0) }
                            BigMouseButton("●\nClick") { onSendMouse(0, 0, 1) }
                            BigMouseButton("▶") { onSendMouse(40, 0, 0) }
                        }
                        // Down
                        BigMouseButton("▼") { onSendMouse(0, 40, 0) }
                    }

                    // Mouse buttons row
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick  = { onSendMouse(0, 0, 1) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Left Click") }
                        Button(
                            onClick  = { onSendMouse(0, 0, 2) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Right Click") }
                    }

                    HorizontalDivider()

                    // ── Keyboard controls ──────────────────────────────────
                    Text("Keyboard Controls", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // HID key codes: A=0x04, B=0x05, Space=0x2C, Enter=0x28
                        KeyButton("A")     { onSendKey(0x00, listOf(0x04)) }
                        KeyButton("B")     { onSendKey(0x00, listOf(0x05)) }
                        KeyButton("Space") { onSendKey(0x00, listOf(0x2C)) }
                        KeyButton("Enter") { onSendKey(0x00, listOf(0x28)) }
                    }
                }

                if (state is BleHidState.ERROR) {
                    Text(
                        state.message,
                        color    = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun BigMouseButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick        = onClick,
        modifier       = Modifier.size(64.dp),
        contentPadding = PaddingValues(4.dp),
        shape          = RoundedCornerShape(8.dp)
    ) {
        Text(label, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun RowScope.KeyButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick  = onClick,
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(4.dp)
    ) { Text(label, fontSize = 11.sp) }
}

@Composable
fun InstructionsCard() {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(10.dp),
        colors    = CardDefaults.cardColors(
            containerColor = Color(0xFF0D47A1).copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("How to connect from host (PC/Mac/Android):",
                fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("1. Open Bluetooth settings on host", fontSize = 12.sp)
            Text("2. Look for \"HID Clone\" in device list", fontSize = 12.sp)
            Text("3. Tap/Click to pair — accept pairing on both ends", fontSize = 12.sp)
            Text("4. Host will recognize it as a mouse + keyboard", fontSize = 12.sp)
            Text("5. Use the controls above to send input", fontSize = 12.sp)
        }
    }
}

// ── Shared composables (unchanged from original) ──────────────────────────────

@Composable
fun SectionHeader(title: String) {
    Text(
        text       = title,
        fontSize   = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color      = MaterialTheme.colorScheme.primary,
        modifier   = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
fun EmptyStateLabel(text: String) {
    Text(text = text, fontSize = 13.sp, color = Color.Gray,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceRow(name: String, address: String, actions: @Composable () -> Unit) {
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


//package com.arena.hidcompatibilitytester
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.bluetooth.BluetoothDevice
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.os.Build
//import android.os.Bundle
//import android.provider.Settings
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.core.content.ContextCompat
//import com.arena.hidcompatibilitytester.ui.theme.HIDCompatibilityTesterTheme
//
//class MainActivity : ComponentActivity(), BluetoothDeviceManager.BluetoothStateListener {
//
//    private lateinit var deviceManager: BluetoothDeviceManager
//
//    private val pairedDevices    = mutableStateListOf<BluetoothDevice>()
//    private val connectedDevices = mutableStateListOf<BluetoothDevice>()
//    private val nearbyDevices    = mutableStateListOf<BluetoothDevice>()
//
//    private var isScanningState            by mutableStateOf(false)
//    private var showLocationServicesDialog by mutableStateOf(false)
//    private var hidStatus by mutableStateOf<BluetoothDeviceManager.HidSupportStatus?>(null)
//
//    // ── Toast-like snackbar message ────────────────────────────────────────
//    private var statusMessage by mutableStateOf<String?>(null)
//
//    private fun requiredPermissions(): Array<String> =
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            arrayOf(
//                Manifest.permission.BLUETOOTH_SCAN,
//                Manifest.permission.BLUETOOTH_CONNECT,
//                Manifest.permission.ACCESS_FINE_LOCATION,
//                Manifest.permission.ACCESS_COARSE_LOCATION
//            )
//        } else {
//            arrayOf(
//                Manifest.permission.ACCESS_FINE_LOCATION,
//                Manifest.permission.ACCESS_COARSE_LOCATION,
//                Manifest.permission.BLUETOOTH,
//                Manifest.permission.BLUETOOTH_ADMIN
//            )
//        }
//
//    private val requestPermissionLauncher = registerForActivityResult(
//        ActivityResultContracts.RequestMultiplePermissions()
//    ) { _ -> executeBluetoothOperations() }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//
//        deviceManager = BluetoothDeviceManager(this) { newDevice ->
//            val alreadyKnown = pairedDevices.any  { it.address == newDevice.address } ||
//                    nearbyDevices.any  { it.address == newDevice.address }
//            if (!alreadyKnown) nearbyDevices.add(newDevice)
//        }
//
//        deviceManager.onScanFinished = {
//            runOnUiThread { isScanningState = false }
//        }
//        deviceManager.onLocationServicesRequired = {
//            runOnUiThread {
//                isScanningState = false
//                showLocationServicesDialog = true
//            }
//        }
//        // ── Key callback: refresh HID status whenever app registration changes ──
//        deviceManager.onHidAppRegistered = { registered ->
//            runOnUiThread {
//                statusMessage = if (registered)
//                    "✓ HID app registered — device is FUNCTIONAL"
//                else
//                    "✗ HID app registration FAILED — device NOT supported"
//                refreshHidStatus()
//            }
//        }
//
//        checkAndRequestPermissions()
//
//        setContent {
//            HIDCompatibilityTesterTheme {
//
//                if (showLocationServicesDialog) {
//                    AlertDialog(
//                        onDismissRequest = { showLocationServicesDialog = false },
//                        title   = { Text("Location Services Required") },
//                        text    = {
//                            Text(
//                                "Android requires Location Services to be ON for Bluetooth " +
//                                        "scanning to find nearby devices.\n\nPlease enable Location " +
//                                        "in Settings, then tap Scan again."
//                            )
//                        },
//                        confirmButton = {
//                            TextButton(onClick = {
//                                showLocationServicesDialog = false
//                                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
//                            }) { Text("Open Location Settings") }
//                        },
//                        dismissButton = {
//                            TextButton(onClick = { showLocationServicesDialog = false }) {
//                                Text("Cancel")
//                            }
//                        }
//                    )
//                }
//
//                Scaffold(
//                    modifier = Modifier.fillMaxSize(),
//                    topBar = {
//                        Row(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .statusBarsPadding()
//                                .padding(16.dp),
//                            horizontalArrangement = Arrangement.SpaceBetween,
//                            verticalAlignment     = Alignment.CenterVertically
//                        ) {
//                            Text("HID Clone", fontSize = 22.sp, fontWeight = FontWeight.Bold)
//                            Button(
//                                onClick = { toggleScanState() },
//                                colors  = ButtonDefaults.buttonColors(
//                                    containerColor = if (isScanningState)
//                                        MaterialTheme.colorScheme.error
//                                    else
//                                        MaterialTheme.colorScheme.primary
//                                )
//                            ) {
//                                Text(if (isScanningState) "Stop Scan" else "Scan Devices")
//                            }
//                        }
//                    }
//                ) { innerPadding ->
//
//                    Box(modifier = Modifier.fillMaxSize()) {
//                        DeviceManagerScreen(
//                            modifier         = Modifier.padding(innerPadding),
//                            hidStatus        = hidStatus,
//                            connectedList    = connectedDevices,
//                            pairedList       = pairedDevices,
//                            nearbyList       = nearbyDevices,
//                            onRefreshHid     = { refreshHidStatus() },
//                            onRetryRegister  = { deviceManager.registerHidApp() },
//                            onPairClick      = { deviceManager.pairDevice(it) },
//                            onUnpairClick    = {
//                                deviceManager.removePairedDevice(it)
//                                refreshDeviceLists()
//                            },
//                            onConnectClick   = { deviceManager.connectToDevice(it) },
//                            onDisconnectClick= { deviceManager.disconnectFromDevice(it) },
//                            onSendMouseReport= { device, dx, dy ->
//                                val ok = deviceManager.sendMouseReport(device, dx, dy)
//                                statusMessage = if (ok) "Mouse report sent ✓" else "Failed to send report ✗"
//                            }
//                        )
//
//                        // ── Status snackbar ───────────────────────────────
//                        statusMessage?.let { msg ->
//                            Snackbar(
//                                modifier = Modifier
//                                    .align(Alignment.BottomCenter)
//                                    .padding(16.dp),
//                                action = {
//                                    TextButton(onClick = { statusMessage = null }) {
//                                        Text("OK")
//                                    }
//                                }
//                            ) { Text(msg) }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    private fun refreshHidStatus() {
//        hidStatus = null
//        Thread {
//            val result = deviceManager.checkHidSupport()
//            runOnUiThread { hidStatus = result }
//        }.start()
//    }
//
//    private fun checkAndRequestPermissions() {
//        val allGranted = requiredPermissions().all {
//            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
//        }
//        if (allGranted) executeBluetoothOperations()
//        else requestPermissionLauncher.launch(requiredPermissions())
//    }
//
//    private fun executeBluetoothOperations() {
//        startPersistentHidService()
//        deviceManager.registerStateListener(this)
//        refreshDeviceLists()
//        // Delay initial status check slightly so proxy has time to bind
//        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
//            refreshHidStatus()
//        }, 1500)
//    }
//
//    private fun toggleScanState() {
//        if (isScanningState) {
//            deviceManager.stopNearbyScanning()
//            isScanningState = false
//        } else {
//            nearbyDevices.clear()
//            val started = deviceManager.startNearbyScanning()
//            if (started) isScanningState = true
//        }
//    }
//
//    private fun refreshDeviceLists() {
//        pairedDevices.clear()
//        pairedDevices.addAll(deviceManager.getPairedDevices())
//        connectedDevices.clear()
//        connectedDevices.addAll(deviceManager.getConnectedDevices())
//    }
//
//    private fun startPersistentHidService() {
//        val serviceIntent = Intent(this, HidInputService::class.java)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(serviceIntent)
//        } else {
//            startService(serviceIntent)
//        }
//    }
//
//    override fun onBondStateChanged(device: BluetoothDevice, state: Int) {
//        runOnUiThread {
//            refreshDeviceLists()
//            if (state == BluetoothDevice.BOND_BONDED)
//                nearbyDevices.removeAll { it.address == device.address }
//        }
//    }
//
//    override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
//        runOnUiThread { refreshDeviceLists() }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        deviceManager.stopNearbyScanning()
//        deviceManager.unregisterStateListener()
//    }
//}
//
//// ── Screens ────────────────────────────────────────────────────────────────
//
//@SuppressLint("MissingPermission")
//@Composable
//fun DeviceManagerScreen(
//    modifier          : Modifier = Modifier,
//    hidStatus         : BluetoothDeviceManager.HidSupportStatus?,
//    connectedList     : List<BluetoothDevice>,
//    pairedList        : List<BluetoothDevice>,
//    nearbyList        : List<BluetoothDevice>,
//    onRefreshHid      : () -> Unit,
//    onRetryRegister   : () -> Unit,
//    onPairClick       : (BluetoothDevice) -> Unit,
//    onUnpairClick     : (BluetoothDevice) -> Unit,
//    onConnectClick    : (BluetoothDevice) -> Unit,
//    onDisconnectClick : (BluetoothDevice) -> Unit,
//    onSendMouseReport : (BluetoothDevice, Int, Int) -> Unit
//) {
//    LazyColumn(
//        modifier = modifier
//            .fillMaxSize()
//            .padding(horizontal = 16.dp),
//        verticalArrangement = Arrangement.spacedBy(12.dp)
//    ) {
//
//        item { HidStatusCard(status = hidStatus, onRefresh = onRefreshHid, onRetryRegister = onRetryRegister) }
//
//        // ── Active Connections ─────────────────────────────────────────────
//        item { SectionHeader("Active Connections (${connectedList.size})") }
//        if (connectedList.isEmpty()) {
//            item { EmptyStateLabel("No active connection targets detected.") }
//        } else {
//            items(connectedList) { device ->
//                DeviceRow(
//                    name    = device.name ?: "Unknown Host",
//                    address = device.address,
//                    actions = {
//                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
//                            // ── Mouse test controls ────────────────────────
//                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
//                                SmallButton("↑") { onSendMouseReport(device,  0, -30) }
//                                SmallButton("↓") { onSendMouseReport(device,  0,  30) }
//                                SmallButton("←") { onSendMouseReport(device, -30, 0) }
//                                SmallButton("→") { onSendMouseReport(device,  30, 0) }
//                            }
//                            Button(
//                                onClick = { onDisconnectClick(device) },
//                                colors  = ButtonDefaults.buttonColors(
//                                    containerColor = MaterialTheme.colorScheme.error),
//                                modifier = Modifier.fillMaxWidth()
//                            ) { Text("Disconnect", color = Color.White) }
//                        }
//                    }
//                )
//            }
//        }
//
//        // ── Paired Devices ─────────────────────────────────────────────────
//        item { SectionHeader("Saved / Paired Devices") }
//        if (pairedList.isEmpty()) {
//            item { EmptyStateLabel("No paired host systems found.") }
//        } else {
//            items(pairedList) { device ->
//                val isConnected = connectedList.any { it.address == device.address }
//                DeviceRow(
//                    name    = device.name ?: "Unknown Host",
//                    address = device.address,
//                    actions = {
//                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
//                            if (!isConnected) {
//                                Button(onClick = { onConnectClick(device) }) { Text("Connect") }
//                            }
//                            OutlinedButton(onClick = { onUnpairClick(device) }) { Text("Forget") }
//                        }
//                    }
//                )
//            }
//        }
//
//        // ── Nearby Scan ────────────────────────────────────────────────────
//        item { SectionHeader("Scanned Nearby Targets") }
//        if (nearbyList.isEmpty()) {
//            item { EmptyStateLabel("Click Scan Devices to discover nearby devices.") }
//        } else {
//            items(nearbyList) { device ->
//                DeviceRow(
//                    name    = device.name ?: "Generic Peripheral Target",
//                    address = device.address,
//                    actions = {
//                        Button(
//                            onClick = { onPairClick(device) },
//                            colors  = ButtonDefaults.buttonColors(
//                                containerColor = MaterialTheme.colorScheme.secondary)
//                        ) { Text("Pair", color = Color.White) }
//                    }
//                )
//            }
//        }
//
//        item { Spacer(modifier = Modifier.height(16.dp)) }
//    }
//}
//
//// ── HID Status Card ────────────────────────────────────────────────────────
//
//@Composable
//fun HidStatusCard(
//    status          : BluetoothDeviceManager.HidSupportStatus?,
//    onRefresh       : () -> Unit,
//    onRetryRegister : () -> Unit
//) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(top = 4.dp),
//        shape     = RoundedCornerShape(12.dp),
//        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
//        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//    ) {
//        Column(modifier = Modifier.padding(14.dp)) {
//
//            Row(
//                modifier            = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment   = Alignment.CenterVertically
//            ) {
//                Text("HID Support Status", fontWeight = FontWeight.Bold, fontSize = 15.sp)
//                OutlinedButton(
//                    onClick        = onRefresh,
//                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
//                    modifier       = Modifier.height(32.dp)
//                ) { Text("Refresh", fontSize = 12.sp) }
//            }
//
//            Spacer(modifier = Modifier.height(10.dp))
//
//            when {
//                status == null -> {
//                    Row(verticalAlignment = Alignment.CenterVertically) {
//                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text("Checking capabilities...", fontSize = 13.sp, color = Color.Gray)
//                    }
//                }
//                !status.bluetoothEnabled -> {
//                    Text(
//                        "⚠ Bluetooth is disabled. Enable it to run the HID check.",
//                        fontSize = 13.sp,
//                        color    = MaterialTheme.colorScheme.error
//                    )
//                }
//                else -> {
//                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
//
//                        HidStatusRow(
//                            label       = "HID Host (receive input)",
//                            description = "Can receive keyboards/mice input",
//                            supported   = status.hidHostSupported
//                        )
//
//                        // Proxy-bound row — note the caveat
//                        HidStatusRow(
//                            label       = "HID Device — proxy bound",
//                            description = "System accepted proxy request (may be false positive)",
//                            supported   = status.hidDeviceProxyConnected
//                        )
//
//                        // Functional row — this is the REAL test
//                        HidStatusRow(
//                            label       = "HID Device — FUNCTIONAL ★",
//                            description = "App registered & ready to send reports (true capability)",
//                            supported   = status.hidDeviceFunctional
//                        )
//
//                        HidStatusRow(
//                            label       = "BLE Peripheral",
//                            description = "Can advertise as a BLE peripheral (HOGP)",
//                            supported   = status.blePeripheralSupported
//                        )
//
//                        // If proxy bound but app not registered, offer retry
//                        if (status.hidDeviceProxyConnected && !status.hidDeviceFunctional) {
//                            Spacer(modifier = Modifier.height(4.dp))
//                            Text(
//                                "⚠ Proxy bound but app registration failed — this means your " +
//                                        "device's Bluetooth stack is blocking HID Device mode. " +
//                                        "Check Logcat for \"registerApp\" errors.",
//                                fontSize = 12.sp,
//                                color    = MaterialTheme.colorScheme.error
//                            )
//                            Spacer(modifier = Modifier.height(4.dp))
//                            OutlinedButton(onClick = onRetryRegister, modifier = Modifier.fillMaxWidth()) {
//                                Text("Retry HID App Registration")
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun HidStatusRow(label: String, description: String, supported: Boolean) {
//    val bgColor    = if (supported) Color(0xFF1B5E20).copy(alpha = 0.12f)
//    else           Color(0xFFB71C1C).copy(alpha = 0.10f)
//    val badgeColor = if (supported) Color(0xFF2E7D32) else Color(0xFFC62828)
//    val badgeText  = if (supported) "✓  Supported" else "✗  Not Supported"
//
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .background(bgColor, RoundedCornerShape(8.dp))
//            .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
//            .padding(horizontal = 10.dp, vertical = 8.dp),
//        horizontalArrangement = Arrangement.SpaceBetween,
//        verticalAlignment     = Alignment.CenterVertically
//    ) {
//        Column(modifier = Modifier.weight(1f)) {
//            Text(label,       fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
//            Text(description, fontSize   = 11.sp, color = Color.Gray)
//        }
//        Spacer(modifier = Modifier.width(8.dp))
//        Text(badgeText, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = badgeColor)
//    }
//}
//
//// ── Shared composables ─────────────────────────────────────────────────────
//
//@Composable
//fun SmallButton(label: String, onClick: () -> Unit) {
//    OutlinedButton(
//        onClick        = onClick,
//        contentPadding = PaddingValues(4.dp),
//        modifier       = Modifier.size(36.dp)
//    ) { Text(label, fontSize = 14.sp) }
//}
//
//@Composable
//fun SectionHeader(title: String) {
//    Text(
//        text           = title,
//        fontSize       = 15.sp,
//        fontWeight     = FontWeight.SemiBold,
//        color          = MaterialTheme.colorScheme.primary,
//        modifier       = Modifier.padding(top = 4.dp, bottom = 4.dp)
//    )
//}
//
//@Composable
//fun EmptyStateLabel(text: String) {
//    Text(
//        text     = text,
//        fontSize = 13.sp,
//        color    = Color.Gray,
//        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
//    )
//}
//
//@Composable
//fun DeviceRow(
//    name    : String,
//    address : String,
//    actions : @Composable () -> Unit
//) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .background(
//                color  = MaterialTheme.colorScheme.surfaceVariant,
//                shape  = RoundedCornerShape(10.dp)
//            )
//            .padding(12.dp),
//        horizontalArrangement = Arrangement.SpaceBetween,
//        verticalAlignment     = Alignment.CenterVertically
//    ) {
//        Column(modifier = Modifier.weight(1f)) {
//            Text(text = name,    fontWeight = FontWeight.Medium, fontSize = 16.sp, maxLines = 1)
//            Text(text = address, fontSize   = 12.sp, color = Color.Gray)
//        }
//        Spacer(modifier = Modifier.width(8.dp))
//        actions()
//    }
//}


//package com.arena.hidcompatibilitytester
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.bluetooth.BluetoothDevice
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.os.Build
//import android.os.Bundle
//import android.util.Log
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.core.content.ContextCompat
//import com.arena.hidcompatibilitytester.ui.theme.HIDCompatibilityTesterTheme
//
//class MainActivity : ComponentActivity(), BluetoothDeviceManager.BluetoothStateListener {
//
//    private lateinit var deviceManager: BluetoothDeviceManager
//
//    private val pairedDevices = mutableStateListOf<BluetoothDevice>()
//    private val connectedDevices = mutableStateListOf<BluetoothDevice>()
//    private val nearbyDevices = mutableStateListOf<BluetoothDevice>()
//
//    private var isScanningState by mutableStateOf(false)
//
//    private val requestPermissionLauncher = registerForActivityResult(
//        ActivityResultContracts.RequestMultiplePermissions()
//    ) { permissions ->
//        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
//            executeBluetoothOperations()
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//
//        deviceManager = BluetoothDeviceManager(this) { newDevice ->
//            val isAlreadyPaired = pairedDevices.any { it.address == newDevice.address }
//            val isAlreadyNearby = nearbyDevices.any { it.address == newDevice.address }
//            if (!isAlreadyPaired && !isAlreadyNearby) {
//                nearbyDevices.add(newDevice)
//            }
//        }
//
//        checkAndRequestPermissions()
//
//        setContent {
//            HIDCompatibilityTesterTheme {
//                Scaffold(
//                    modifier = Modifier.fillMaxSize(),
//                    topBar = {
//                        Row(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .statusBarsPadding()
//                                .padding(16.dp),
//                            horizontalArrangement = Arrangement.SpaceBetween,
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            Text(
//                                text = "HID Clone",
//                                fontSize = 22.sp,
//                                fontWeight = FontWeight.Bold
//                            )
//
//                            // TOOGLABLE SCAN BUTTON SWITCHER LAYER
//                            Button(
//                                onClick = { toggleScanState() },
//                                colors = ButtonDefaults.buttonColors(
//                                    containerColor = if (isScanningState) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
//                                )
//                            ) {
//                                Text(if (isScanningState) "Stop Scan" else "Scan Devices")
//                            }
//                        }
//                    }
//                ) { innerPadding ->
//                    DeviceManagerScreen(
//                        modifier = Modifier.padding(innerPadding),
//                        connectedList = connectedDevices,
//                        pairedList = pairedDevices,
//                        nearbyList = nearbyDevices,
//                        onPairClick = { deviceManager.pairDevice(it) },
//                        onUnpairClick = {
//                            deviceManager.removePairedDevice(it)
//                            refreshDeviceLists()
//                        },
//                        onConnectClick = { deviceManager.connectToDevice(it) },
//                        onDisconnectClick = { deviceManager.disconnectFromDevice(it) }
//                    )
//                }
//            }
//        }
//    }
//
//    private fun checkAndRequestPermissions() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//            == PackageManager.PERMISSION_GRANTED) {
//            executeBluetoothOperations()
//        } else {
//            requestPermissionLauncher.launch(
//                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
//            )
//        }
//    }
//
//    private fun executeBluetoothOperations() {
//        startPersistentHidService()
//        deviceManager.registerStateListener(this)
//        refreshDeviceLists()
//    }
//
//    private fun startScanningSequence() {
//        nearbyDevices.clear()
//        deviceManager.startNearbyScanning()
//        isScanningState = true
//    }
//
//    private fun toggleScanState() {
//        if (isScanningState) {
//            deviceManager.stopNearbyScanning()
//            isScanningState = false
//        } else {
//            startScanningSequence()
//        }
//    }
//
//    private fun refreshDeviceLists() {
//        pairedDevices.clear()
//        pairedDevices.addAll(deviceManager.getPairedDevices())
//
//        connectedDevices.clear()
//        connectedDevices.addAll(deviceManager.getConnectedDevices())
//    }
//
//    private fun startPersistentHidService() {
//        val serviceIntent = Intent(this, HidInputService::class.java)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(serviceIntent)
//        } else {
//            startService(serviceIntent)
//        }
//    }
//
//    override fun onBondStateChanged(device: BluetoothDevice, state: Int) {
//        runOnUiThread {
//            refreshDeviceLists()
//            if (state == BluetoothDevice.BOND_BONDED) {
//                nearbyDevices.removeAll { it.address == device.address }
//            }
//        }
//    }
//
//    override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
//        runOnUiThread {
//            refreshDeviceLists()
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        deviceManager.stopNearbyScanning()
//        deviceManager.unregisterStateListener()
//    }
//}
//
//@SuppressLint("MissingPermission")
//@Composable
//fun DeviceManagerScreen(
//    modifier: Modifier = Modifier,
//    connectedList: List<BluetoothDevice>,
//    pairedList: List<BluetoothDevice>,
//    nearbyList: List<BluetoothDevice>,
//    onPairClick: (BluetoothDevice) -> Unit,
//    onUnpairClick: (BluetoothDevice) -> Unit,
//    onConnectClick: (BluetoothDevice) -> Unit,
//    onDisconnectClick: (BluetoothDevice) -> Unit
//) {
//    LazyColumn(
//        modifier = modifier
//            .fillMaxSize()
//            .padding(horizontal = 16.dp),
//        verticalArrangement = Arrangement.spacedBy(12.dp)
//    ) {
//        item { SectionHeader("Active Connections (${connectedList.size})") }
//        if (connectedList.isEmpty()) {
//            item { EmptyStateLabel("No active connection targets detected.") }
//        } else {
//            items(connectedList) { device ->
//                DeviceRow(
//                    name = device.name ?: "Unknown Laptop Host",
//                    address = device.address,
//                    actions = {
//                        Button(
//                            onClick = { onDisconnectClick(device) },
//                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
//                        ) {
//                            Text("Disconnect", color = Color.White)
//                        }
//                    }
//                )
//            }
//        }
//
//        item { SectionHeader("Saved / Paired Devices") }
//        if (pairedList.isEmpty()) {
//            item { EmptyStateLabel("No paired host systems found.") }
//        } else {
//            items(pairedList) { device ->
//                val isConnected = connectedList.any { it.address == device.address }
//                DeviceRow(
//                    name = device.name ?: "Unknown Laptop Host",
//                    address = device.address,
//                    actions = {
//                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
//                            if (!isConnected) {
//                                Button(onClick = { onConnectClick(device) }) {
//                                    Text("Connect")
//                                }
//                            }
//                            OutlinedButton(onClick = { onUnpairClick(device) }) {
//                                Text("Forget")
//                            }
//                        }
//                    }
//                )
//            }
//        }
//
//        item { SectionHeader("Scanned Nearby Targets") }
//
//        if (nearbyList.isEmpty()) {
//            item {
//                EmptyStateLabel("Click Scan Devices to discover laptop targets.")
//            }
//        } else {
//            items(nearbyList) { device ->
//                DeviceRow(
//                    name = device.name ?: "Generic Peripheral Target",
//                    address = device.address,
//                    actions = {
//                        Button(
//                            onClick = { onPairClick(device) },
//                            colors = ButtonDefaults.buttonColors(
//                                containerColor = MaterialTheme.colorScheme.secondary
//                            )
//                        ) {
//                            Text(
//                                text = "Pair",
//                                color = Color.White
//                            )
//                        }
//                    }
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun SectionHeader(title: String) {
//    Text(
//        text = title,
//        fontSize = 15.sp,
//        fontWeight = FontWeight.SemiBold,
//        color = MaterialTheme.colorScheme.primary,
//        modifier = Modifier.padding(
//            top = 12.dp,
//            bottom = 4.dp
//        )
//    )
//}
//
//@Composable
//fun EmptyStateLabel(text: String) {
//    Text(
//        text = text,
//        fontSize = 13.sp,
//        color = Color.Gray,
//        modifier = Modifier.padding(
//            start = 8.dp,
//            bottom = 8.dp
//        )
//    )
//}
//
//@Composable
//fun DeviceRow(
//    name: String,
//    address: String,
//    actions: @Composable () -> Unit
//) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .background(
//                color = MaterialTheme.colorScheme.surfaceVariant,
//                shape = RoundedCornerShape(10.dp)
//            )
//            .padding(12.dp),
//        horizontalArrangement = Arrangement.SpaceBetween,
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Column(
//            modifier = Modifier.weight(1f)
//        ) {
//            Text(
//                text = name,
//                fontWeight = FontWeight.Medium,
//                fontSize = 16.sp,
//                maxLines = 1
//            )
//
//            Text(
//                text = address,
//                fontSize = 12.sp,
//                color = Color.Gray
//            )
//        }
//
//        Spacer(modifier = Modifier.width(8.dp))
//
//        actions()
//    }
//}



//package com.arena.hidcompatibilitytester
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.bluetooth.BluetoothDevice
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.os.Build
//import android.os.Bundle
//import android.util.Log
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.core.content.ContextCompat
//import com.arena.hidcompatibilitytester.ui.theme.HIDCompatibilityTesterTheme
//
//class MainActivity : ComponentActivity(), BluetoothDeviceManager.BluetoothStateListener {
//
//    private lateinit var deviceManager: BluetoothDeviceManager
//
//    // Jetpack Compose reactive state lists
//    private val pairedDevices = mutableStateListOf<BluetoothDevice>()
//    private val connectedDevices = mutableStateListOf<BluetoothDevice>()
//    private val nearbyDevices = mutableStateListOf<BluetoothDevice>()
//
//    // Scan button toggle reactive status tracking
//    private var isScanningState by mutableStateOf(false)
//
//    private val requestPermissionLauncher = registerForActivityResult(
//        ActivityResultContracts.RequestMultiplePermissions()
//    ) { permissions ->
//        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
//            executeBluetoothOperations()
//        } else {
//            Log.e("MAIN", "Location permissions rejected.")
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//
//        // Initialize Device Manager with scan callback handling duplicates
//        deviceManager = BluetoothDeviceManager(this) { newDevice ->
//            val isAlreadyPaired = pairedDevices.any { it.address == newDevice.address }
//            val isAlreadyNearby = nearbyDevices.any { it.address == newDevice.address }
//            if (!isAlreadyPaired && !isAlreadyNearby) {
//                nearbyDevices.add(newDevice)
//            }
//        }
//
//        checkAndRequestPermissions()
//
//        setContent {
//            HIDCompatibilityTesterTheme {
//                Scaffold(
//                    modifier = Modifier.fillMaxSize(),
//                    topBar = {
//                        Box(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .statusBarsPadding()
//                                .padding(16.dp),
//                            contentAlignment = Alignment.CenterStart
//                        ) {
//                            Text(
//                                text = "HID Clone Controller",
//                                fontSize = 22.sp,
//                                fontWeight = FontWeight.Bold
//                            )
//                            // --- ADDED SYSTEM RE-SCAN BUTTON LAYER ---
//                            Button(
//                                onClick = { toggleScanState() }
//                            ) {
//                                Text(if (isScanningState) "Scanning..." else "Scan Devices")
//                            }
//                        }
//                    }
//                ) { innerPadding ->
//                    DeviceManagerScreen(
//                        modifier = Modifier.padding(innerPadding),
//                        connectedList = connectedDevices,
//                        pairedList = pairedDevices,
//                        nearbyList = nearbyDevices,
//                        onPairClick = { deviceManager.pairDevice(it) },
//                        onUnpairClick = {
//                            deviceManager.removePairedDevice(it)
//                            refreshDeviceLists()
//                        },
//                        onConnectClick = {
//                            // SAFETY INJECTION: Force hardware profile declaration to OS before triggering socket binding
//                            deviceManager.registerHidAppConfig()
//                            deviceManager.connectToDevice(it)
//                        },
//                        onDisconnectClick = { deviceManager.disconnectFromDevice(it) }
//                    )
//                }
//            }
//        }
//    }
//
//    private fun checkAndRequestPermissions() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//            == PackageManager.PERMISSION_GRANTED) {
//            executeBluetoothOperations()
//        } else {
//            requestPermissionLauncher.launch(
//                arrayOf(
//                    Manifest.permission.ACCESS_FINE_LOCATION,
//                    Manifest.permission.ACCESS_COARSE_LOCATION
//                )
//            )
//        }
//    }
//
//    private fun executeBluetoothOperations() {
//        startPersistentHidService()
//        deviceManager.registerStateListener(this)
//        refreshDeviceLists()
//        deviceManager.startNearbyScanning()
//    }
//
//    private fun startScanningSequence() {
//        nearbyDevices.clear()
//        deviceManager.startNearbyScanning()
//        isScanningState = true
//    }
//
//    private fun toggleScanState() {
//        if (deviceManager.isCurrentlyScanning()) {
//            deviceManager.stopNearbyScanning()
//            isScanningState = false
//        } else {
//            startScanningSequence()
//        }
//    }
//
//    private fun refreshDeviceLists() {
//        // Sync lists safely from current system memory states
//        pairedDevices.clear()
//        pairedDevices.addAll(deviceManager.getPairedDevices())
//
//        connectedDevices.clear()
//        connectedDevices.addAll(deviceManager.getConnectedDevices())
//    }
//
//    private fun startPersistentHidService() {
//        val serviceIntent = Intent(this, HidInputService::class.java)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(serviceIntent)
//        } else {
//            startService(serviceIntent)
//        }
//    }
//
//    // Bluetooth Broadcast Receiver State Changes Listener Hooks
//    override fun onBondStateChanged(device: BluetoothDevice, state: Int) {
//        runOnUiThread {
//            refreshDeviceLists()
//            // Remove from nearby if it just got paired
//            if (state == BluetoothDevice.BOND_BONDED) {
//                nearbyDevices.removeAll { it.address == device.address }
//            }
//        }
//    }
//
//    override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
//        runOnUiThread {
//            refreshDeviceLists()
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        deviceManager.stopNearbyScanning()
//        deviceManager.unregisterStateListener()
//    }
//}
//
//@SuppressLint("MissingPermission")
//@Composable
//fun DeviceManagerScreen(
//    modifier: Modifier = Modifier,
//    connectedList: List<BluetoothDevice>,
//    pairedList: List<BluetoothDevice>,
//    nearbyList: List<BluetoothDevice>,
//    onPairClick: (BluetoothDevice) -> Unit,
//    onUnpairClick: (BluetoothDevice) -> Unit,
//    onConnectClick: (BluetoothDevice) -> Unit,
//    onDisconnectClick: (BluetoothDevice) -> Unit
//) {
//    LazyColumn(
//        modifier = modifier
//            .fillMaxSize()
//            .padding(horizontal = 16.dp),
//        verticalArrangement = Arrangement.spacedBy(12.dp)
//    ) {
//        // SECTION 1: CONNECTED
//        item { SectionHeader("Active Connections (${connectedList.size})") }
//        if (connectedList.isEmpty()) {
//            item { EmptyStateLabel("No active input connection targets detected.") }
//        } else {
//            items(connectedList) { device ->
//                DeviceRow(
//                    name = device.name ?: "Unknown Device",
//                    address = device.address,
//                    actions = {
//                        Button(
//                            onClick = { onDisconnectClick(device) },
//                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
//                        ) {
//                            Text("Disconnect", color = Color.White)
//                        }
//                    }
//                )
//            }
//        }
//
//        // SECTION 2: SAVED/PAIRED
//        item { SectionHeader("Saved / Paired Devices") }
//        if (pairedList.isEmpty()) {
//            item { EmptyStateLabel("No paired host systems found.") }
//        } else {
//            items(pairedList) { device ->
//                val isConnected = connectedList.any { it.address == device.address }
//                DeviceRow(
//                    name = device.name ?: "Unknown Device",
//                    address = device.address,
//                    actions = {
//                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
//                            if (!isConnected) {
//                                Button(onClick = { onConnectClick(device) }) {
//                                    Text("Connect")
//                                }
//                            }
//                            OutlinedButton(onClick = { onUnpairClick(device) }) {
//                                Text("Forget")
//                            }
//                        }
//                    }
//                )
//            }
//        }
//
//        // SECTION 3: NEARBY
//        item { SectionHeader("Scanned Nearby Targets") }
//        if (nearbyList.isEmpty()) {
//            item { EmptyStateLabel("Scanning for active hardware components...") }
//        } else {
//            items(nearbyList) { device ->
//                DeviceRow(
//                    name = device.name ?: "Generic Peripheral Target",
//                    address = device.address,
//                    actions = {
//                        Button(
//                            onClick = { onPairClick(device) },
//                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
//                        ) {
//                            Text("Pair Device", color = Color.White)
//                        }
//                    }
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun SectionHeader(title: String) {
//    Text(
//        text = title,
//        fontSize = 15.sp,
//        fontWeight = FontWeight.SemiBold,
//        color = MaterialTheme.colorScheme.primary,
//        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
//    )
//}
//
//@Composable
//fun EmptyStateLabel(text: String) {
//    Text(
//        text = text,
//        fontSize = 13.sp,
//        color = Color.Gray,
//        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
//    )
//}
//
//@Composable fun DeviceRow(name: String,address: String,actions: @Composable () -> Unit) {
//    Row(modifier = Modifier.fillMaxWidth()
//                    .background(MaterialTheme
//                        .colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp))
//                    .padding(12.dp),
//        horizontalArrangement = Arrangement.SpaceBetween,
//        verticalAlignment = Alignment.CenterVertically) {
//        Column(modifier = Modifier.weight(1f)) {
//            Text(text = name, fontWeight = FontWeight.Medium, fontSize = 16.sp, maxLines = 1)
//            Text(text = address, fontSize = 12.sp, color = Color.Gray)
//        }
//        Spacer(modifier = Modifier.width(8.dp))
//        actions()
//    }
//}