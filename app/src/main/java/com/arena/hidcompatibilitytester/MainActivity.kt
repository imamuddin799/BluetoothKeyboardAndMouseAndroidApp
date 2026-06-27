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
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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

    private lateinit var deviceManager : BluetoothDeviceManager
    private lateinit var bleHidManager : BleHidManager

    // ── UI State ──────────────────────────────────────────────────────────────
    private val pairedDevices       = mutableStateListOf<BluetoothDevice>()
    private val nearbyDevices       = mutableStateListOf<BluetoothDevice>()
    private val connectedHostList   = mutableStateListOf<BleHidManager.DeviceInfo>()

    private var isScanningState            by mutableStateOf(false)
    private var showLocationServicesDialog by mutableStateOf(false)
    private var bleHidState                by mutableStateOf<BleHidState>(BleHidState.IDLE)
    private var statusMessage              by mutableStateOf<String?>(null)
    private var bleSupported               by mutableStateOf(false)

    // ── Bluetooth ON/OFF receiver ─────────────────────────────────────────────
    /**
     * When the user turns Bluetooth back on, automatically restart the HID peripheral
     * so it can reconnect without any user action.
     */
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            when (state) {
                BluetoothAdapter.STATE_ON -> {
                    // BT just turned on — auto-start peripheral if it was running before
                    // (or always auto-start if we have known hosts)
                    if (bleHidState is BleHidState.IDLE || bleHidState is BleHidState.ERROR) {
                        statusMessage = "Bluetooth on — reconnecting..."
                        bleHidManager.start()
                    }
                }
                BluetoothAdapter.STATE_OFF -> {
                    // Stack is going down; update UI state cleanly
                    bleHidState   = BleHidState.IDLE
                    statusMessage = "Bluetooth turned off"
                    runOnUiThread {
                        connectedHostList.clear()
                    }
                }
            }
        }
    }

    // ── Bond-state receiver ───────────────────────────────────────────────────
    /**
     * Forward bond-state changes to BleHidManager so it can remove forgotten devices
     * from the known-hosts list.
     */
    private val bondStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothDevice.ACTION_BOND_STATE_CHANGED) return
            val device: BluetoothDevice = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            else
                @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE))
                ?: return

            val bondState = intent.getIntExtra(
                BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)

            // Forward to BleHidManager for known-host maintenance
            bleHidManager.onBondStateChanged(device, bondState)
        }
    }

    // ── Permissions ───────────────────────────────────────────────────────────
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

    // ═════════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ═════════════════════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ── BLE HID Manager setup ──────────────────────────────────────────
        bleHidManager = BleHidManager(this)
        bleSupported  = bleHidManager.isSupported()

        bleHidManager.onStateChanged = { state ->
            bleHidState = state
            when (state) {
                is BleHidState.ADVERTISING ->
                    statusMessage = "📡 Advertising — waiting for host to connect"
                is BleHidState.CONNECTED   ->
                    statusMessage = "✓ Host connected"
                is BleHidState.ERROR       ->
                    statusMessage = "✗ ${state.message}"
                else -> {}
            }
        }

        bleHidManager.onDeviceListChanged = { list ->
            runOnUiThread {
                connectedHostList.clear()
                connectedHostList.addAll(list)
            }
        }

        bleHidManager.onDeviceSubscribed = { device ->
            runOnUiThread {
                statusMessage = "✓ Host ready: ${device.address}"
            }
        }

        // ── Classic BT Device Manager ──────────────────────────────────────
        deviceManager = BluetoothDeviceManager(this) { newDevice ->
            val alreadyKnown =
                pairedDevices.any { it.address == newDevice.address } ||
                        nearbyDevices.any { it.address == newDevice.address }
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

        // ── Register system receivers ──────────────────────────────────────
        registerReceiverCompat(
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
        registerReceiverCompat(
            bondStateReceiver,
            IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        )

        checkAndRequestPermissions()

        // ── UI ─────────────────────────────────────────────────────────────
        setContent {
            HIDCompatibilityTesterTheme {

                if (showLocationServicesDialog) {
                    AlertDialog(
                        onDismissRequest = { showLocationServicesDialog = false },
                        title   = { Text("Location Services Required") },
                        text    = {
                            Text(
                                "Android requires Location Services for Bluetooth scanning.\n\n" +
                                        "Enable Location in Settings, then scan again."
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
                            modifier           = Modifier.padding(innerPadding),
                            bleHidState        = bleHidState,
                            bleSupported       = bleSupported,
                            connectedHostList  = connectedHostList,
                            pairedList         = pairedDevices,
                            nearbyList         = nearbyDevices,
                            isScanningState    = isScanningState,
                            onToggleBleHid     = { toggleBleHid() },
                            onSendMouse        = { dx, dy, buttons ->
                                val ok = bleHidManager.sendMouseReport(dx, dy, buttons)
                                if (!ok) statusMessage =
                                    "✗ No subscribed host — complete pairing first"
                            },
                            onSendKey          = { mod, keys ->
                                bleHidManager.sendKeyboardReport(mod, keys)
                                android.os.Handler(android.os.Looper.getMainLooper())
                                    .postDelayed({ bleHidManager.releaseKeys() }, 100)
                            },
                            onToggleScan       = { toggleScanState() },
                            onPairClick        = { deviceManager.pairDevice(it) },
                            onUnpairClick      = {
                                deviceManager.removePairedDevice(it)
                                // Also remove from BLE known hosts so we don't
                                // auto-reconnect to a device the user deliberately forgot
                                bleHidManager.forgetDevice(it.address)
                                refreshDeviceLists()
                            },
                            onDisconnectHost   = { address ->
                                bleHidManager.disconnectDevice(address)
                            },
                            onReconnectHost    = { device ->
                                bleHidManager.inviteReconnect(device)
                                statusMessage = "Inviting ${device.address} to reconnect..."
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

    // ═════════════════════════════════════════════════════════════════════════
    // Actions
    // ═════════════════════════════════════════════════════════════════════════

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

        // Auto-start HID peripheral on launch if we have known hosts (seamless reconnect)
        if (bleHidManager.isSupported() &&
            (bleHidState is BleHidState.IDLE || bleHidState is BleHidState.ERROR)) {
            bleHidManager.start()
        }
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
        val intent = Intent(this, HidInputService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
    }

    // ── BluetoothStateListener (from DeviceManager bond/connection events) ──

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

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun registerReceiverCompat(receiver: BroadcastReceiver, filter: IntentFilter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't stop bleHidManager — it keeps running via the foreground service.
        // Only unregister Activity-scoped receivers.
        try { unregisterReceiver(bluetoothStateReceiver) } catch (e: Exception) {}
        try { unregisterReceiver(bondStateReceiver) }       catch (e: Exception) {}
        deviceManager.stopNearbyScanning()
        deviceManager.unregisterStateListener()
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Main Screen  (unchanged from original except onUnpairClick wiring above)
// ═════════════════════════════════════════════════════════════════════════════

@SuppressLint("MissingPermission")
@Composable
fun MainScreen(
    modifier          : Modifier,
    bleHidState       : BleHidState,
    bleSupported      : Boolean,
    connectedHostList : List<BleHidManager.DeviceInfo>,
    pairedList        : List<BluetoothDevice>,
    nearbyList        : List<BluetoothDevice>,
    isScanningState   : Boolean,
    onToggleBleHid    : () -> Unit,
    onSendMouse       : (Int, Int, Int) -> Unit,
    onSendKey         : (Int, List<Int>) -> Unit,
    onToggleScan      : () -> Unit,
    onPairClick       : (BluetoothDevice) -> Unit,
    onUnpairClick     : (BluetoothDevice) -> Unit,
    onDisconnectHost  : (String) -> Unit,
    onReconnectHost   : (BluetoothDevice) -> Unit,
) {
    LazyColumn(
        modifier            = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        item {
            BleHidControlCard(
                state             = bleHidState,
                supported         = bleSupported,
                onToggle          = onToggleBleHid,
                onSendMouse       = onSendMouse,
                onSendKey         = onSendKey,
                connectedHostList = connectedHostList,
                onDisconnectHost  = onDisconnectHost,
                onReconnectHost   = onReconnectHost
            )
        }

        item {
            if (bleHidState is BleHidState.ADVERTISING ||
                bleHidState is BleHidState.CONNECTED) {
                InstructionsCard()
            }
        }

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
                ) { Text(if (isScanningState) "Stop Scan" else "Scan") }
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

        item { SectionHeader("Paired Devices") }
        if (pairedList.isEmpty()) {
            item { EmptyStateLabel("No paired devices.") }
        } else {
            items(pairedList) { device ->
                DeviceRow(
                    name    = device.name ?: "Unknown Device",
                    address = device.address,
                    actions = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { onReconnectHost(device) }) {
                                Text("Connect", fontSize = 12.sp)
                            }
                            OutlinedButton(onClick = { onUnpairClick(device) }) {
                                Text("Forget", fontSize = 12.sp)
                            }
                        }
                    }
                )
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// BLE HID Control Card
// ═════════════════════════════════════════════════════════════════════════════

@SuppressLint("MissingPermission")
@Composable
fun BleHidControlCard(
    state             : BleHidState,
    supported         : Boolean,
    onToggle          : () -> Unit,
    onSendMouse       : (Int, Int, Int) -> Unit,
    onSendKey         : (Int, List<Int>) -> Unit,
    connectedHostList : List<BleHidManager.DeviceInfo>,
    onDisconnectHost  : (String) -> Unit,
    onReconnectHost   : (BluetoothDevice) -> Unit,
) {
    val isRunning     = state is BleHidState.ADVERTISING || state is BleHidState.CONNECTED
    val anySubscribed = connectedHostList.any { it.isSubscribed }

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
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("BLE HID Peripheral",
                        fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("HOGP Mouse + Keyboard",
                        fontSize = 12.sp, color = Color.Gray)
                }
                val (badgeText, badgeColor) = when (state) {
                    BleHidState.IDLE        -> "IDLE"        to Color.Gray
                    BleHidState.STARTING    -> "STARTING…"  to Color(0xFFF57F17)
                    BleHidState.ADVERTISING -> "ADVERTISING" to Color(0xFF1565C0)
                    BleHidState.CONNECTED   -> "CONNECTED ✓" to Color(0xFF2E7D32)
                    is BleHidState.ERROR    -> "ERROR"       to MaterialTheme.colorScheme.error
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text       = badgeText,
                        color      = badgeColor,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (!supported) {
                Text("⚠ BLE peripheral not supported on this device.",
                    color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            } else {
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

                if (connectedHostList.isNotEmpty()) {
                    HorizontalDivider()
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text("Connected Hosts",
                            fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("${connectedHostList.size} / 4",
                            fontSize = 12.sp, color = Color.Gray)
                    }
                    connectedHostList.forEach { info ->
                        ConnectedHostRow(
                            info         = info,
                            onDisconnect = { onDisconnectHost(info.address) },
                            onReconnect  = { onReconnectHost(info.device) }
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }

                if (state is BleHidState.CONNECTED && !anySubscribed) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Waiting for host to enable notifications...",
                            fontSize = 12.sp, color = Color.Gray)
                    }
                }

                if (anySubscribed) {
                    HorizontalDivider()
                    Text("Mouse Controls", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Column(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        BigMouseButton("▲") { onSendMouse(0, -40, 0) }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            BigMouseButton("◀") { onSendMouse(-40, 0, 0) }
                            BigMouseButton("●") { onSendMouse(0, 0, 1) }
                            BigMouseButton("▶") { onSendMouse(40, 0, 0) }
                        }
                        BigMouseButton("▼") { onSendMouse(0, 40, 0) }
                    }
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = { onSendMouse(0, 0, 1) },
                            modifier = Modifier.weight(1f)) { Text("Left Click") }
                        Button(onClick = { onSendMouse(0, 0, 2) },
                            modifier = Modifier.weight(1f)) { Text("Right Click") }
                    }
                    HorizontalDivider()
                    Text("Keyboard", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        KeyButton("A")     { onSendKey(0x00, listOf(0x04)) }
                        KeyButton("B")     { onSendKey(0x00, listOf(0x05)) }
                        KeyButton("Space") { onSendKey(0x00, listOf(0x2C)) }
                        KeyButton("Enter") { onSendKey(0x00, listOf(0x28)) }
                    }
                }

                if (state is BleHidState.ERROR) {
                    Text(state.message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Connected Host Row
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun ConnectedHostRow(
    info        : BleHidManager.DeviceInfo,
    onDisconnect: () -> Unit,
    onReconnect : () -> Unit
) {
    var showActions by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (info.isSubscribed) Color(0xFF1B5E20).copy(alpha = 0.10f)
                else Color(0xFFF57F17).copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = info.name ?: "Device (${info.address.takeLast(8)})",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp,
                    maxLines   = 1
                )
                Text(text = info.address, fontSize = 11.sp, color = Color.Gray)
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (info.isSubscribed) Color(0xFF2E7D32).copy(alpha = 0.15f)
                else Color(0xFFF57F17).copy(alpha = 0.15f)
            ) {
                Text(
                    text       = if (info.isSubscribed) "✓ Ready" else "⏳ Pairing...",
                    color      = if (info.isSubscribed) Color(0xFF2E7D32) else Color(0xFFF57F17),
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = { showActions = !showActions }, modifier = Modifier.size(28.dp)) {
                Text(if (showActions) "▲" else "▼", fontSize = 12.sp)
            }
        }
        if (showActions) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick  = { onReconnect(); showActions = false },
                    modifier = Modifier.weight(1f)
                ) { Text("Reconnect", fontSize = 12.sp) }
                Button(
                    onClick  = { onDisconnect(); showActions = false },
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Disconnect", fontSize = 12.sp, color = Color.White) }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Instructions Card
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun InstructionsCard() {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(10.dp),
        colors    = CardDefaults.cardColors(
            containerColor = Color(0xFF0D47A1).copy(alpha = 0.08f))
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("How to connect from host:",
                fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("1. Open Bluetooth settings on host (PC/Mac/Android)", fontSize = 12.sp)
            Text("2. Look for \"HID Clone\" in device list",             fontSize = 12.sp)
            Text("3. Click/Tap to pair — accept on both ends",           fontSize = 12.sp)
            Text("4. Host recognises it as mouse + keyboard",            fontSize = 12.sp)
            Text("5. Next time: app auto-reconnects on BT enable",       fontSize = 12.sp)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Reusable Composables
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun BigMouseButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick        = onClick,
        modifier       = Modifier.size(64.dp),
        contentPadding = PaddingValues(4.dp),
        shape          = RoundedCornerShape(8.dp)
    ) { Text(label, fontSize = 12.sp, textAlign = TextAlign.Center) }
}

@Composable
fun RowScope.KeyButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick        = onClick,
        modifier       = Modifier.weight(1f),
        contentPadding = PaddingValues(4.dp)
    ) { Text(label, fontSize = 11.sp) }
}

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
    Text(
        text     = text,
        fontSize = 13.sp,
        color    = Color.Gray,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceRow(
    name    : String,
    address : String,
    actions : @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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