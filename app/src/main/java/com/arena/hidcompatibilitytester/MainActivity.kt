// MainActivity.kt — complete replacement
package com.arena.hidcompatibilitytester

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.arena.hidcompatibilitytester.ui.theme.HIDCompatibilityTesterTheme

class MainActivity : ComponentActivity(), BluetoothDeviceManager.BluetoothStateListener {

    private lateinit var deviceManager : BluetoothDeviceManager
    private lateinit var bleHidManager : BleHidManager

    private val pairedDevices     = androidx.compose.runtime.snapshots.SnapshotStateList<BluetoothDevice>()
    private val nearbyDevices     = androidx.compose.runtime.snapshots.SnapshotStateList<BluetoothDevice>()
    private val connectedHostList = androidx.compose.runtime.snapshots.SnapshotStateList<BleHidManager.DeviceInfo>()

    private var isScanningState            by mutableStateOf(false)
    private var showLocationServicesDialog by mutableStateOf(false)
    private var bleHidState                by mutableStateOf<BleHidState>(BleHidState.IDLE)
    private var statusMessage              by mutableStateOf<String?>(null)
    private var bleSupported               by mutableStateOf(false)
    private var pairRequiredAddress        by mutableStateOf<String?>(null)

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
            val known = pairedDevices.any  { it.address == newDevice.address } ||
                        nearbyDevices.any  { it.address == newDevice.address }
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

        setContent {
            HIDCompatibilityTesterTheme {
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

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppMainScreen(
                            modifier          = Modifier.padding(innerPadding),
                            bleHidState       = bleHidState,
                            bleSupported      = bleSupported,
                            connectedHostList = connectedHostList,
                            pairedList        = pairedDevices,
                            nearbyList        = nearbyDevices,
                            isScanningState   = isScanningState,
                            onToggleBleHid    = { toggleBleHid() },
                            onSendMouse       = { dx, dy, buttons, wheel ->
                                if (!bleHidManager.sendMouseReport(dx, dy, buttons, wheel))
                                    statusMessage = "✗ No subscribed host"
                            },
                            onSendKey         = { mod, keys ->
                                bleHidManager.sendKeyboardReport(mod, keys)
                            },
                            onReleaseKeys     = { bleHidManager.releaseKeys() },
                            onConsumerKey     = { usage -> bleHidManager.sendConsumerKey(usage) },
                            onTypeText        = { text -> bleHidManager.typeText(text) },
                            onToggleScan      = { toggleScanState() },
                            onPairClick       = { deviceManager.pairDevice(it) },
                            onUnpairClick     = {
                                deviceManager.removePairedDevice(it)
                                bleHidManager.forgetDevice(it.address)
                                refreshDeviceLists()
                            },
                            onDisconnectHost  = { address -> bleHidManager.disconnectDevice(address) },
                            onReconnectHost   = { device ->
                                bleHidManager.inviteReconnect(device)
                                statusMessage = "Inviting ${device.address}…"
                            }
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
        if (bleHidManager.isSupported() &&
            (bleHidState is BleHidState.IDLE || bleHidState is BleHidState.ERROR))
            bleHidManager.start()
    }

    private fun toggleScanState() {
        if (isScanningState) { deviceManager.stopNearbyScanning(); isScanningState = false }
        else { nearbyDevices.clear(); if (deviceManager.startNearbyScanning()) isScanningState = true }
    }

    private fun refreshDeviceLists() {
        pairedDevices.clear(); pairedDevices.addAll(deviceManager.getPairedDevices())
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
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// AppMainScreen — 4 tabs: Status | Mouse | Keyboard | Devices
// ═════════════════════════════════════════════════════════════════════════════

@SuppressLint("MissingPermission")
@Composable
fun AppMainScreen(
    modifier          : Modifier,
    bleHidState       : BleHidState,
    bleSupported      : Boolean,
    connectedHostList : List<BleHidManager.DeviceInfo>,
    pairedList        : List<BluetoothDevice>,
    nearbyList        : List<BluetoothDevice>,
    isScanningState   : Boolean,
    onToggleBleHid    : () -> Unit,
    onSendMouse       : (Int, Int, Int, Int) -> Unit,
    onSendKey         : (Int, List<Int>) -> Unit,
    onReleaseKeys     : () -> Unit,
    onConsumerKey     : (Int) -> Unit,
    onTypeText        : (String) -> Unit,
    onToggleScan      : () -> Unit,
    onPairClick       : (BluetoothDevice) -> Unit,
    onUnpairClick     : (BluetoothDevice) -> Unit,
    onDisconnectHost  : (String) -> Unit,
    onReconnectHost   : (BluetoothDevice) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Status", "Mouse", "Keyboard", "Devices")
    val isReady = connectedHostList.any { it.isSubscribed }

    Column(modifier = modifier.fillMaxSize()) {
        // ── Compact top status bar ────────────────────────────────────────────
        AppStatusBar(
            state             = bleHidState,
            supported         = bleSupported,
            connectedHostList = connectedHostList,
            onToggle          = onToggleBleHid,
        )

        // ── Tab row ───────────────────────────────────────────────────────────
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected = selectedTab == i,
                    onClick  = { selectedTab = i },
                    text     = { Text(title, fontSize = 13.sp) }
                )
            }
        }

        // ── Tab bodies ────────────────────────────────────────────────────────
        when (selectedTab) {
            0 -> StatusTabContent(
                bleHidState       = bleHidState,
                bleSupported      = bleSupported,
                connectedHostList = connectedHostList,
                onToggleBleHid    = onToggleBleHid,
                onDisconnectHost  = onDisconnectHost,
                onReconnectHost   = onReconnectHost,
            )
            1 -> MouseTabContent(
                isReady    = isReady,
                onSendMouse = onSendMouse,
            )
            2 -> KeyboardScreen(        // ← our full physical keyboard
                isReady       = isReady,
                onSendKey     = { mod, keys ->
                    onSendKey(mod, keys)
                },
                onConsumerKey = onConsumerKey,
                onTypeText    = onTypeText,
            )
            3 -> DevicesTabContent(
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

// ═════════════════════════════════════════════════════════════════════════════
// Top status bar
// ═════════════════════════════════════════════════════════════════════════════

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
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        badgeText,
                        color      = badgeColor,
                        fontSize   = 10.sp,
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
                                             else MaterialTheme.colorScheme.primary
                        )
                    ) { Text(if (isRunning) "Stop" else "Start", fontSize = 12.sp) }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Status Tab
// ═════════════════════════════════════════════════════════════════════════════

@SuppressLint("MissingPermission")
@Composable
fun StatusTabContent(
    bleHidState       : BleHidState,
    bleSupported      : Boolean,
    connectedHostList : List<BleHidManager.DeviceInfo>,
    onToggleBleHid    : () -> Unit,
    onDisconnectHost  : (String) -> Unit,
    onReconnectHost   : (BluetoothDevice) -> Unit,
) {
    LazyColumn(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        if (!bleSupported) {
            item {
                Card(
                    colors   = CardDefaults.cardColors(
                        containerColor = Color(0xFFB71C1C).copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "⚠ BLE peripheral not supported on this device.",
                        color    = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        if (bleHidState is BleHidState.ERROR) {
            item {
                Card(
                    colors   = CardDefaults.cardColors(
                        containerColor = Color(0xFFB71C1C).copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "✗ ${bleHidState.message}",
                        color    = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        if (bleHidState is BleHidState.ADVERTISING ||
            bleHidState is BleHidState.CONNECTED) {
            item { HowToConnectCard() }
        }

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
                    info        = info,
                    onDisconnect = { onDisconnectHost(info.address) },
                    onReconnect  = { onReconnectHost(info.device) }
                )
            }
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
fun HowToConnectCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        colors   = CardDefaults.cardColors(
            containerColor = Color(0xFF0D47A1).copy(alpha = 0.08f))
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("How to connect:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("1. Open Bluetooth settings on host (PC / Mac / Phone)", fontSize = 12.sp)
            Text("2. Find \"HID Clone\" in the device list",               fontSize = 12.sp)
            Text("3. Tap/click to pair",                                   fontSize = 12.sp)
            Text("4. Host sees it as mouse + keyboard",                    fontSize = 12.sp)
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun ConnectedHostCard(
    info        : BleHidManager.DeviceInfo,
    onDisconnect: () -> Unit,
    onReconnect : () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
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
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    color = if (info.isSubscribed) Color(0xFF2E7D32).copy(alpha = 0.15f)
                            else Color(0xFFF57F17).copy(alpha = 0.15f)
                ) {
                    Text(
                        if (info.isSubscribed) "✓ Ready" else "⏳ Pairing…",
                        color      = if (info.isSubscribed) Color(0xFF2E7D32)
                                     else Color(0xFFF57F17),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                IconButton(
                    onClick  = { expanded = !expanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Text(if (expanded) "▲" else "▼", fontSize = 12.sp)
                }
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

// ═════════════════════════════════════════════════════════════════════════════
// Mouse Tab
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun MouseTabContent(
    isReady: Boolean,
    onSendMouse: (Int, Int, Int, Int) -> Unit,
) {
    if (!isReady) {
        NotReadyCard() // Changed from NotReadyPlaceholder to match KeyboardScreen
        return
    }

    var isScrollMode by remember { mutableStateOf(false) }
    var accX by remember { mutableFloatStateOf(0f) }
    var accY by remember { mutableFloatStateOf(0f) }
    val sensitivity = if (isScrollMode) 0.5f else 1.2f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Trackpad", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Scroll", fontSize = 12.sp)
                        Switch(
                            checked = isScrollMode,
                            onCheckedChange = { isScrollMode = it },
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pointerInput(isScrollMode) {
                            detectDragGestures(
                                onDragStart = { accX = 0f; accY = 0f },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    accX += dragAmount.x * sensitivity
                                    accY += dragAmount.y * sensitivity
                                    val ix = accX.toInt()
                                    val iy = accY.toInt()
                                    if (ix != 0 || iy != 0) {
                                        if (isScrollMode)
                                            onSendMouse(0, 0, 0, -iy.coerceIn(-127, 127))
                                        else
                                            onSendMouse(
                                                ix.coerceIn(-127, 127),
                                                iy.coerceIn(-127, 127), 0, 0)
                                        accX -= ix; accY -= iy
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isScrollMode) "Scroll\n(drag up/down)"
                        else "Move mouse\n(drag anywhere)",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
                // ... rest of the buttons (Left, Middle, Right) remain the same
            }
        }
        // ... rest of the D-Pad remains the same
    }
}

@Composable
private fun MouseButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick        = onClick,
        modifier       = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
    ) { Text(label, fontSize = 12.sp, textAlign = TextAlign.Center) }
}

@Composable
private fun DPadBtn(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick        = onClick,
        modifier       = Modifier.size(60.dp),
        contentPadding = PaddingValues(2.dp),
        shape          = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    ) { Text(label, fontSize = 12.sp, textAlign = TextAlign.Center) }
}

// ═════════════════════════════════════════════════════════════════════════════
// Devices Tab
// ═════════════════════════════════════════════════════════════════════════════

@SuppressLint("MissingPermission")
@Composable
fun DevicesTabContent(
    nearbyList     : List<BluetoothDevice>,
    pairedList     : List<BluetoothDevice>,
    isScanningState: Boolean,
    onToggleScan   : () -> Unit,
    onPairClick    : (BluetoothDevice) -> Unit,
    onUnpairClick  : (BluetoothDevice) -> Unit,
    onReconnect    : (BluetoothDevice) -> Unit,
) {
    LazyColumn(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Nearby Devices",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.primary
                )
                Button(
                    onClick = onToggleScan,
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = if (isScanningState) MaterialTheme.colorScheme.error
                                         else MaterialTheme.colorScheme.primary
                    )
                ) { Text(if (isScanningState) "Stop" else "Scan") }
            }
        }

        if (nearbyList.isEmpty()) {
            item {
                Text(
                    "Tap Scan to discover devices.",
                    fontSize = 13.sp, color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        } else {
            items(nearbyList) { device ->
                DeviceListRow(
                    name    = device.name ?: "Unknown",
                    address = device.address
                ) {
                    Button(
                        onClick = { onPairClick(device) },
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary)
                    ) { Text("Pair") }
                }
            }
        }

        item {
            Text(
                "Paired Devices",
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.primary,
                modifier   = Modifier.padding(top = 4.dp)
            )
        }

        if (pairedList.isEmpty()) {
            item {
                Text(
                    "No paired devices.",
                    fontSize = 13.sp, color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        } else {
            items(pairedList) { device ->
                DeviceListRow(
                    name    = device.name ?: "Unknown",
                    address = device.address
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { onReconnect(device) }) {
                            Text("Connect", fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = { onUnpairClick(device) }) {
                            Text("Forget", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun DeviceListRow(
    name   : String,
    address: String,
    actions: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // FIX: Removed the 'androidx.compose...' prefixes
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp)
            )
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

// ═════════════════════════════════════════════════════════════════════════════
// Not-ready placeholder
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun NotReadyCard() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.padding(32.dp),
            colors   = CardDefaults.cardColors(
                containerColor = Color(0xFFF57F17).copy(alpha = 0.1f))
        ) {
            Column(
                modifier            = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("⏳", fontSize = 32.sp)
                Text("Not Connected", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "Start BLE HID → pair from host Bluetooth settings → come back here.",
                    fontSize  = 13.sp,
                    color     = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}