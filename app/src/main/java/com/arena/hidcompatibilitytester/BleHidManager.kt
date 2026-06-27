package com.arena.hidcompatibilitytester

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log

/**
 * BleHidManager — BLE GATT peripheral that exposes a combined Mouse + Keyboard HID service.
 *
 * AUTO-RECONNECT BEHAVIOUR (mirrors "Bluetooth Keyboard & Mouse" by Appground):
 *
 *  1. Every time a host successfully subscribes (enables CCCD notifications) its address
 *     is persisted to SharedPreferences ("known hosts").
 *
 *  2. On ANY disconnect the manager immediately:
 *       a. Continues / restarts BLE advertising  →  new hosts can still find "HID Clone"
 *       b. Calls gattServer.connect(device, autoConnect=true) for EVERY known host
 *          →  Android will reconnect the moment the host is in range, even without
 *             advertising.  This covers the "phone BT was off, now back on" case.
 *
 *  3. A periodic retry loop (every RECONNECT_INTERVAL_MS) repeats step 2b so that
 *     hosts that come back online later are still caught.
 *
 *  4. If the host side removes the bond (bond state → BOND_NONE) we remove that address
 *     from known hosts AND keep advertising so the host can re-pair from scratch.
 *
 *  5. On app cold-start, start() loads known hosts from prefs and immediately fires
 *     autoConnect calls in addition to advertising — no user interaction needed.
 *
 * COLD-START RACE CONDITION FIX:
 *  The OS GATT stack fires autoConnect for known hosts almost immediately on start(),
 *  but our GATT service pipeline (4 services) takes 3–5 seconds to complete. This means
 *  onConnectionStateChange(CONNECTED) fires while mouseInputChar / keyboardInputChar are
 *  still null — AND the host won't re-write its CCCD on reconnect (it's a bonded device).
 *
 *  Fix (three parts):
 *   a. beginAddingServices() clears subscribedDevices so stale phantom subscriptions
 *      from a prior OS GATT session don't linger while chars are null.
 *   b. onConnectionStateChange records the known host in connectedDeviceMap + subscribedDevices
 *      but skips the null char guard gracefully, logging that onServicesReady() will fix it.
 *   c. onServicesReady() — called once ALL services are added — sweeps connectedDeviceMap
 *      for known hosts and re-applies CCCD + subscription on the brand-new char instances.
 */
@SuppressLint("MissingPermission")
class BleHidManager(private val context: Context) {

    companion object {
        private const val TAG = "BleHidManager"

        // SharedPreferences keys
        private const val PREFS_NAME        = "ble_hid_prefs"
        private const val PREFS_KNOWN_HOSTS = "known_hosts"   // comma-separated addresses

        // Timing
        private const val SERVICE_ADD_DELAY_MS    = 600L
        private const val SERVICE_ADD_TIMEOUT_MS  = 5_000L
        private const val RECONNECT_INTERVAL_MS   = 8_000L    // retry autoConnect every 8 s
        private const val INITIAL_RECONNECT_DELAY = 1_500L    // first attempt after this delay

        private const val MAX_CONNECTIONS = 4

        // ── GATT UUIDs ────────────────────────────────────────────────────────
        private fun gattUuid(short: Int) =
            java.util.UUID.fromString("0000%04x-0000-1000-8000-00805f9b34fb".format(short))

        val UUID_GENERIC_ACCESS     = gattUuid(0x1800)
        val UUID_DEVICE_INFORMATION = gattUuid(0x180A)
        val UUID_BATTERY_SERVICE    = gattUuid(0x180F)
        val UUID_HID_SERVICE        = gattUuid(0x1812)
        val UUID_DEVICE_NAME        = gattUuid(0x2A00)
        val UUID_APPEARANCE         = gattUuid(0x2A01)
        val UUID_MANUFACTURER_NAME  = gattUuid(0x2A29)
        val UUID_MODEL_NUMBER       = gattUuid(0x2A24)
        val UUID_PNP_ID             = gattUuid(0x2A50)
        val UUID_BATTERY_LEVEL      = gattUuid(0x2A19)
        val UUID_HID_INFORMATION    = gattUuid(0x2A4A)
        val UUID_REPORT_MAP         = gattUuid(0x2A4B)
        val UUID_HID_CONTROL_POINT  = gattUuid(0x2A4C)
        val UUID_PROTOCOL_MODE      = gattUuid(0x2A4E)
        val UUID_REPORT             = gattUuid(0x2A4D)
        val UUID_CCCD               = gattUuid(0x2902)
        val UUID_REPORT_REFERENCE   = gattUuid(0x2908)

        // ── HID constants ─────────────────────────────────────────────────────
        val APPEARANCE_MOUSE     = byteArrayOf(0xC2.toByte(), 0x03)
        val HID_INFORMATION      = byteArrayOf(0x11, 0x01, 0x00, 0x02)
        val PROTOCOL_MODE_REPORT = byteArrayOf(0x01)

        /** Combined Mouse (Report ID 1) + Keyboard (Report ID 2) descriptor */
        val REPORT_MAP = byteArrayOf(
            // ── Mouse ──────────────────────────────────────────────────────────
            0x05, 0x01,
            0x09, 0x02,
            0xA1.toByte(), 0x01,
            0x85.toByte(), 0x01,         // Report ID 1
            0x09, 0x01,
            0xA1.toByte(), 0x00,
            0x05, 0x09,
            0x19, 0x01,
            0x29, 0x03,
            0x15, 0x00,
            0x25, 0x01,
            0x95.toByte(), 0x03,
            0x75, 0x01,
            0x81.toByte(), 0x02,
            0x95.toByte(), 0x01,
            0x75, 0x05,
            0x81.toByte(), 0x03,
            0x05, 0x01,
            0x09, 0x30,
            0x09, 0x31,
            0x15, 0x81.toByte(),
            0x25, 0x7F,
            0x75, 0x08,
            0x95.toByte(), 0x02,
            0x81.toByte(), 0x06,
            0xC0.toByte(),
            0xC0.toByte(),
            // ── Keyboard ───────────────────────────────────────────────────────
            0x05, 0x01,
            0x09, 0x06,
            0xA1.toByte(), 0x01,
            0x85.toByte(), 0x02,         // Report ID 2
            0x05, 0x07,
            0x19, 0xE0.toByte(),
            0x29, 0xE7.toByte(),
            0x15, 0x00,
            0x25, 0x01,
            0x75, 0x01,
            0x95.toByte(), 0x08,
            0x81.toByte(), 0x02,
            0x95.toByte(), 0x01,
            0x75, 0x08,
            0x81.toByte(), 0x01,
            0x95.toByte(), 0x06,
            0x75, 0x08,
            0x15, 0x00,
            0x25, 0x65,
            0x05, 0x07,
            0x19, 0x00,
            0x29, 0x65,
            0x81.toByte(), 0x00,
            0xC0.toByte()
        )
    }

    // ── Bluetooth system services ─────────────────────────────────────────────
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private val originalName: String? = try { adapter?.name } catch (e: Exception) { null }

    // ── Persistence ───────────────────────────────────────────────────────────
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── GATT + Advertiser ─────────────────────────────────────────────────────
    private var gattServer  : BluetoothGattServer? = null
    private var advertiser  : BluetoothLeAdvertiser? = null
    private var isAdvertising = false

    // ── Report characteristics ────────────────────────────────────────────────
    private var mouseInputChar   : BluetoothGattCharacteristic? = null
    private var keyboardInputChar: BluetoothGattCharacteristic? = null

    // ── Device tracking ───────────────────────────────────────────────────────
    data class DeviceInfo(
        val device      : BluetoothDevice,
        val address     : String,
        var name        : String?,
        var isSubscribed: Boolean = false
    )

    private val connectedDeviceMap = mutableMapOf<String, DeviceInfo>() // address → info
    private val subscribedDevices  = mutableSetOf<BluetoothDevice>()

    /**
     * Persisted set of addresses that have ever successfully subscribed.
     * Used to drive autoConnect on start and after any disconnect.
     */
    private val knownHostAddresses = mutableSetOf<String>()

    private val bondedDeviceCache  = mutableMapOf<String, String>()     // address → name

    // ── Public callbacks ──────────────────────────────────────────────────────
    var onStateChanged      : ((BleHidState) -> Unit)? = null
    var onDeviceListChanged : ((List<DeviceInfo>) -> Unit)? = null
    var onDeviceSubscribed  : ((BluetoothDevice) -> Unit)? = null

    // ── Handlers ──────────────────────────────────────────────────────────────
    private val mainHandler = Handler(Looper.getMainLooper())
    private var gattThread : HandlerThread? = null
    private var gattHandler: Handler? = null

    // ── Service add queue ─────────────────────────────────────────────────────
    private val serviceQueue    = ArrayDeque<BluetoothGattService>()
    private var timeoutRunnable : Runnable? = null

    // ── Reconnect loop ────────────────────────────────────────────────────────
    private var reconnectRunnable: Runnable? = null
    private var isRunning = false          // true between start() and stop()

    // ── State ─────────────────────────────────────────────────────────────────
    private var currentState: BleHidState = BleHidState.IDLE
        set(value) {
            field = value
            mainHandler.post { onStateChanged?.invoke(value) }
        }

    // ═════════════════════════════════════════════════════════════════════════
    // Public API
    // ═════════════════════════════════════════════════════════════════════════

    fun isSupported(): Boolean = try {
        adapter != null &&
        adapter.isEnabled &&
        adapter.isMultipleAdvertisementSupported
    } catch (e: Exception) { false }

    fun getConnectedDeviceInfoList(): List<DeviceInfo> = connectedDeviceMap.values.toList()
    fun getConnectionCount(): Int  = connectedDeviceMap.size
    fun isReadyToSend(): Boolean   = subscribedDevices.isNotEmpty()

    /** Load known hosts from prefs so callers can inspect them before start(). */
    fun getKnownHostCount(): Int = loadKnownHosts().size

    fun start() {
        Log.d(TAG, "start() — isSupported=${isSupported()}")
        if (!isSupported()) {
            currentState = BleHidState.ERROR("BLE peripheral not supported")
            return
        }
        if (currentState is BleHidState.ADVERTISING ||
            currentState is BleHidState.CONNECTED) {
            Log.w(TAG, "Already running"); return
        }

        isRunning = true
        refreshBondedCache()
        knownHostAddresses.clear()
        knownHostAddresses.addAll(loadKnownHosts())
        Log.d(TAG, "Known hosts loaded: ${knownHostAddresses.size} → $knownHostAddresses")

        try { adapter?.name = "HID Clone" } catch (e: Exception) { Log.w(TAG, "rename: ${e.message}") }

        currentState = BleHidState.STARTING
        startGattThread()
        gattHandler?.postDelayed({ openGattServer() }, 500)
    }

    fun stop() {
        Log.d(TAG, "stop()")
        isRunning = false
        cancelReconnectLoop()
        cancelTimeout()
        stopAdvertising()

        for (info in connectedDeviceMap.values.toList()) {
            try { gattServer?.cancelConnection(info.device) } catch (e: Exception) {}
        }
        try { gattServer?.close() } catch (e: Exception) { Log.e(TAG, "close: ${e.message}") }

        gattServer         = null
        mouseInputChar     = null
        keyboardInputChar  = null
        connectedDeviceMap.clear()
        subscribedDevices.clear()
        serviceQueue.clear()

        stopGattThread()

        try { if (originalName != null) adapter?.name = originalName }
        catch (e: Exception) { Log.w(TAG, "restore name: ${e.message}") }

        currentState = BleHidState.IDLE
        notifyDeviceListChanged()
    }

    /** Disconnect a specific device (user-initiated). Does NOT remove from knownHosts. */
    fun disconnectDevice(address: String) {
        val info = connectedDeviceMap[address] ?: return
        try { gattServer?.cancelConnection(info.device) }
        catch (e: Exception) { Log.e(TAG, "disconnectDevice: ${e.message}") }
        // onConnectionStateChange will clean up the map and trigger reconnect loop
    }

    /**
     * Remove a host from knownHosts AND disconnect it.
     * Call this only when the user explicitly "forgets" a device.
     */
    fun forgetDevice(address: String) {
        removeKnownHost(address)
        disconnectDevice(address)
        Log.d(TAG, "Forgot device $address — will no longer auto-reconnect to it")
    }

    /**
     * Invite a previously bonded device to reconnect.
     * The GATT server will accept the connection as soon as the device is in range.
     */
    fun inviteReconnect(device: BluetoothDevice) {
        try {
            gattServer?.connect(device, true)
            Log.d(TAG, "inviteReconnect (autoConnect=true): ${device.address}")
        } catch (e: Exception) {
            Log.e(TAG, "inviteReconnect: ${e.message}")
        }
    }

    // ── HID report senders ────────────────────────────────────────────────────

    fun sendMouseReport(dx: Int, dy: Int, buttons: Int = 0): Boolean {
        val char = mouseInputChar ?: return false.also { Log.w(TAG, "mouseChar null") }
        if (subscribedDevices.isEmpty()) return false.also { Log.w(TAG, "no subscribers") }
        return sendInputReport(
            char,
            byteArrayOf(
                buttons.and(0x07).toByte(),
                dx.coerceIn(-127, 127).toByte(),
                dy.coerceIn(-127, 127).toByte()
            )
        )
    }

    fun sendKeyboardReport(modifiers: Int = 0, keyCodes: List<Int> = emptyList()): Boolean {
        val char = keyboardInputChar ?: return false
        if (subscribedDevices.isEmpty()) return false
        val report = ByteArray(8).also { r ->
            r[0] = modifiers.toByte()
            keyCodes.take(6).forEachIndexed { i, c -> r[2 + i] = c.toByte() }
        }
        return sendInputReport(char, report)
    }

    fun releaseKeys() = sendKeyboardReport()

    // ═════════════════════════════════════════════════════════════════════════
    // Persistence helpers
    // ═════════════════════════════════════════════════════════════════════════

    private fun loadKnownHosts(): Set<String> {
        val raw = prefs.getString(PREFS_KNOWN_HOSTS, "") ?: ""
        return if (raw.isBlank()) emptySet()
        else raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    private fun saveKnownHost(address: String) {
        knownHostAddresses.add(address)
        prefs.edit().putString(PREFS_KNOWN_HOSTS, knownHostAddresses.joinToString(",")).apply()
        Log.d(TAG, "Persisted known host: $address (total ${knownHostAddresses.size})")
    }

    private fun removeKnownHost(address: String) {
        knownHostAddresses.remove(address)
        prefs.edit().putString(PREFS_KNOWN_HOSTS, knownHostAddresses.joinToString(",")).apply()
        Log.d(TAG, "Removed known host: $address (remaining ${knownHostAddresses.size})")
    }

    private fun clearAllKnownHosts() {
        knownHostAddresses.clear()
        prefs.edit().remove(PREFS_KNOWN_HOSTS).apply()
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Auto-reconnect logic
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Called after advertising is up (new start) OR after any host disconnects.
     *
     * Strategy:
     *   • Keep advertising  →  new hosts can find "HID Clone" and pair fresh
     *   • For every known host call gattServer.connect(device, autoConnect=true)
     *     →  Android BT stack reconnects silently when the device is in range
     *   • Schedule a periodic retry so devices that come online later are covered
     */
    private fun startReconnectLoop() {
        cancelReconnectLoop()
        if (knownHostAddresses.isEmpty()) {
            Log.d(TAG, "No known hosts — skipping reconnect loop")
            return
        }

        Log.d(TAG, "Starting reconnect loop for ${knownHostAddresses.size} known host(s)")

        val runnable = object : Runnable {
            override fun run() {
                if (!isRunning) return
                attemptAutoConnectAll()
                mainHandler.postDelayed(this, RECONNECT_INTERVAL_MS)
            }
        }
        reconnectRunnable = runnable
        // First attempt after a short delay (give GATT server time to settle)
        mainHandler.postDelayed(runnable, INITIAL_RECONNECT_DELAY)
    }

    private fun cancelReconnectLoop() {
        reconnectRunnable?.let { mainHandler.removeCallbacks(it) }
        reconnectRunnable = null
    }

    /**
     * Issue a background autoConnect to every known host that is not already connected.
     * This is low-power: Android will only actually connect when the device is in range.
     */
    private fun attemptAutoConnectAll() {
        val server = gattServer ?: return
        val alreadyConnected = connectedDeviceMap.keys

        for (address in knownHostAddresses.toSet()) {      // snapshot to avoid CME
            if (address in alreadyConnected) continue      // already connected — skip

            val device = try { adapter?.getRemoteDevice(address) } catch (e: Exception) { null }
            if (device == null) {
                Log.w(TAG, "Cannot resolve device for $address"); continue
            }

            try {
                val ok = server.connect(device, true)      // autoConnect = true (background)
                Log.d(TAG, "autoConnect → $address : $ok")
            } catch (e: Exception) {
                Log.w(TAG, "autoConnect error for $address: ${e.message}")
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Bond change notification (called from MainActivity's BroadcastReceiver)
    // ═════════════════════════════════════════════════════════════════════════

    fun onBondStateChanged(device: BluetoothDevice, bondState: Int) {
        when (bondState) {
            BluetoothDevice.BOND_NONE -> {
                if (knownHostAddresses.contains(device.address)) {
                    Log.d(TAG, "Bond removed by host ${device.address} — removing from known hosts")
                    removeKnownHost(device.address)
                }
            }
            BluetoothDevice.BOND_BONDED -> {
                Log.d(TAG, "New bond: ${device.address}")
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Device name resolution
    // ═════════════════════════════════════════════════════════════════════════

    private fun refreshBondedCache() {
        bondedDeviceCache.clear()
        try {
            adapter?.bondedDevices?.forEach { d ->
                val n = try { d.name } catch (e: Exception) { null }
                if (!n.isNullOrBlank()) bondedDeviceCache[d.address] = n
            }
        } catch (e: Exception) { Log.w(TAG, "refreshBondedCache: ${e.message}") }
    }

    private fun resolveName(device: BluetoothDevice): String {
        val btName = try { device.name } catch (e: Exception) { null }
        if (!btName.isNullOrBlank()) return btName
        val cached = bondedDeviceCache[device.address]
        if (!cached.isNullOrBlank()) return cached
        return "Device (${device.address.takeLast(8)})"
    }

    // ═════════════════════════════════════════════════════════════════════════
    // UI notification
    // ═════════════════════════════════════════════════════════════════════════

    private fun notifyDeviceListChanged() {
        val list = connectedDeviceMap.values.toList()
        mainHandler.post { onDeviceListChanged?.invoke(list) }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GATT thread
    // ═════════════════════════════════════════════════════════════════════════

    private fun startGattThread() {
        stopGattThread()
        gattThread = HandlerThread("BleHidGattThread").also {
            it.start()
            gattHandler = Handler(it.looper)
        }
    }

    private fun stopGattThread() {
        gattThread?.quitSafely()
        gattThread  = null
        gattHandler = null
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GATT server open
    // ═════════════════════════════════════════════════════════════════════════

    private fun openGattServer() {
        try {
            gattServer?.close(); gattServer = null
            gattServer = bluetoothManager.openGattServer(context, gattServerCallback)

            if (gattServer == null) {
                currentState = BleHidState.ERROR("Could not open GATT server"); return
            }
            gattHandler?.postDelayed({ beginAddingServices() }, 1_000)

        } catch (e: SecurityException) {
            currentState = BleHidState.ERROR("Permission denied: ${e.message}")
        } catch (e: Exception) {
            currentState = BleHidState.ERROR("GATT error: ${e.message}")
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Service pipeline
    // ═════════════════════════════════════════════════════════════════════════

    private fun beginAddingServices() {
        serviceQueue.clear()
        mouseInputChar    = null
        keyboardInputChar = null

        // FIX (part a): Clear phantom subscriptions from any prior OS GATT session.
        // The host may have reconnected during start() before our characteristics existed.
        // onServicesReady() will re-populate subscribedDevices for connected known hosts
        // once the new characteristic instances are actually ready.
        subscribedDevices.clear()

        serviceQueue.addLast(buildGenericAccessService())
        serviceQueue.addLast(buildDeviceInformationService())
        serviceQueue.addLast(buildBatteryService())
        serviceQueue.addLast(buildHidService())

        addNextService()
    }

    private fun addNextService() {
        if (serviceQueue.isEmpty()) {
            // All services added — call onServicesReady() instead of startAdvertising() directly
            mainHandler.postDelayed({ onServicesReady() }, 500)
            return
        }

        val service = serviceQueue.first()
        armTimeout(service.uuid.toString())

        val result = try { gattServer?.addService(service) ?: false }
                     catch (e: Exception) { false }

        if (!result) {
            cancelTimeout()
            gattHandler?.postDelayed({
                val retry = try { gattServer?.addService(service) ?: false } catch (e: Exception) { false }
                if (!retry) { serviceQueue.removeFirst(); addNextService() }
                else armTimeout(service.uuid.toString())
            }, 1_000)
        }
    }

    private fun armTimeout(uuid: String) {
        cancelTimeout()
        val r = Runnable {
            Log.w(TAG, "TIMEOUT for $uuid — force-advancing")
            if (serviceQueue.isNotEmpty()) serviceQueue.removeFirst()
            gattHandler?.post { addNextService() }
        }
        timeoutRunnable = r
        gattHandler?.postDelayed(r, SERVICE_ADD_TIMEOUT_MS)
    }

    private fun cancelTimeout() {
        timeoutRunnable?.let { gattHandler?.removeCallbacks(it); mainHandler.removeCallbacks(it) }
        timeoutRunnable = null
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FIX (part c): onServicesReady — called once ALL services and characteristics
    // are fully initialised. At this point mouseInputChar and keyboardInputChar are
    // guaranteed non-null. We start advertising and then sweep connectedDeviceMap for
    // any known host that connected during the service setup window, re-applying their
    // CCCD subscription on the brand-new characteristic instances.
    // ═════════════════════════════════════════════════════════════════════════

    private fun onServicesReady() {
        startAdvertising()

        // Re-wire subscriptions for known hosts that connected while services were being added.
        // The host won't re-write its CCCD descriptor on reconnect (it's bonded), so we must
        // restore the subscription state manually on the freshly created characteristics.
        val snapshot = connectedDeviceMap.values.toList()   // snapshot — avoid CME
        for (info in snapshot) {
            if (!knownHostAddresses.contains(info.address)) continue

            Log.d(TAG, "onServicesReady: re-applying subscription for known host ${info.address}")

            subscribedDevices.add(info.device)

            // Update the in-memory CCCD value on the NEW characteristic instances
            listOf(mouseInputChar, keyboardInputChar).forEach { char ->
                char?.getDescriptor(UUID_CCCD)?.value =
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            }

            // Update the map entry so UI shows "Ready"
            connectedDeviceMap[info.address] = info.copy(isSubscribed = true)

            mainHandler.post {
                onDeviceSubscribed?.invoke(info.device)
                notifyDeviceListChanged()
            }
        }

        if (snapshot.any { knownHostAddresses.contains(it.address) }) {
            Log.d(TAG, "onServicesReady: subscription restoration complete for ${
                snapshot.count { knownHostAddresses.contains(it.address) }} host(s)")
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Service builders
    // ═════════════════════════════════════════════════════════════════════════

    private fun buildGenericAccessService() = BluetoothGattService(
        UUID_GENERIC_ACCESS, BluetoothGattService.SERVICE_TYPE_PRIMARY
    ).also {
        it.addCharacteristic(readChar(UUID_DEVICE_NAME, "HID Clone".toByteArray()))
        it.addCharacteristic(readChar(UUID_APPEARANCE,  APPEARANCE_MOUSE))
    }

    private fun buildDeviceInformationService() = BluetoothGattService(
        UUID_DEVICE_INFORMATION, BluetoothGattService.SERVICE_TYPE_PRIMARY
    ).also {
        it.addCharacteristic(readChar(UUID_MANUFACTURER_NAME, "Arena".toByteArray()))
        it.addCharacteristic(readChar(UUID_MODEL_NUMBER,      "HIDClone-1".toByteArray()))
        it.addCharacteristic(
            readChar(UUID_PNP_ID,
                byteArrayOf(0x02, 0x6D, 0x04, 0x2B, 0xC5.toByte(), 0x11, 0x01))
        )
    }

    private fun buildBatteryService() = BluetoothGattService(
        UUID_BATTERY_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY
    ).also { svc ->
        svc.addCharacteristic(
            BluetoothGattCharacteristic(
                UUID_BATTERY_LEVEL,
                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM
            ).apply { value = byteArrayOf(100); addDescriptor(cccd()) }
        )
    }

    private fun buildHidService() = BluetoothGattService(
        UUID_HID_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY
    ).also { svc ->
        svc.addCharacteristic(
            readChar(UUID_HID_INFORMATION, HID_INFORMATION,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM))
        svc.addCharacteristic(
            readChar(UUID_REPORT_MAP, REPORT_MAP,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM))
        svc.addCharacteristic(
            BluetoothGattCharacteristic(UUID_HID_CONTROL_POINT,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM))
        svc.addCharacteristic(
            BluetoothGattCharacteristic(UUID_PROTOCOL_MODE,
                BluetoothGattCharacteristic.PROPERTY_READ or
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM or
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM
            ).apply { value = PROTOCOL_MODE_REPORT })

        mouseInputChar    = inputReportChar(reportId = 1, reportType = 0x01)
        keyboardInputChar = inputReportChar(reportId = 2, reportType = 0x01)
        svc.addCharacteristic(mouseInputChar!!)
        svc.addCharacteristic(keyboardInputChar!!)
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Characteristic / descriptor helpers
    // ═════════════════════════════════════════════════════════════════════════

    private fun readChar(
        uuid       : java.util.UUID,
        value      : ByteArray,
        permissions: Int = BluetoothGattCharacteristic.PERMISSION_READ
    ) = BluetoothGattCharacteristic(uuid, BluetoothGattCharacteristic.PROPERTY_READ, permissions)
            .apply { this.value = value }

    private fun inputReportChar(reportId: Int, reportType: Int) =
        BluetoothGattCharacteristic(
            UUID_REPORT,
            BluetoothGattCharacteristic.PROPERTY_READ or
            BluetoothGattCharacteristic.PROPERTY_NOTIFY or
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM or
            BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM
        ).apply {
            addDescriptor(cccd())
            addDescriptor(
                BluetoothGattDescriptor(
                    UUID_REPORT_REFERENCE,
                    BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM
                ).apply { value = byteArrayOf(reportId.toByte(), reportType.toByte()) }
            )
        }

    private fun cccd() = BluetoothGattDescriptor(
        UUID_CCCD,
        BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
    ).apply { value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE }

    // ═════════════════════════════════════════════════════════════════════════
    // Advertising
    // ═════════════════════════════════════════════════════════════════════════

    private fun startAdvertising() {
        try {
            advertiser = adapter?.bluetoothLeAdvertiser
            if (advertiser == null) {
                currentState = BleHidState.ERROR("BLE advertiser unavailable"); return
            }

            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build()

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(android.os.ParcelUuid(UUID_HID_SERVICE))
                .build()

            val scanResponse = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .build()

            advertiser!!.startAdvertising(settings, data, scanResponse, advertiseCallback)

        } catch (e: SecurityException) {
            currentState = BleHidState.ERROR("Missing BLUETOOTH_ADVERTISE permission")
        } catch (e: Exception) {
            currentState = BleHidState.ERROR("Advertising error: ${e.message}")
        }
    }

    private fun stopAdvertising() {
        if (!isAdvertising) return
        try { advertiser?.stopAdvertising(advertiseCallback) } catch (e: Exception) {}
        isAdvertising = false
    }

    /**
     * Restart advertising after a disconnect so new hosts can still find "HID Clone",
     * and so the host's OS sees us in the scan list and reconnects automatically.
     */
    private fun restartAdvertisingIfNeeded() {
        if (isAdvertising) return          // already running — nothing to do
        if (!isRunning)    return          // manager has been stopped
        Log.d(TAG, "Restarting advertising after disconnect")
        startAdvertising()
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            isAdvertising = true
            Log.d(TAG, "✓ Advertising started")
            currentState = BleHidState.ADVERTISING
            // Kick the reconnect loop NOW that we're visible
            startReconnectLoop()
        }
        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
            val msg = when (errorCode) {
                ADVERTISE_FAILED_ALREADY_STARTED -> {
                    // Already advertising — treat as success
                    isAdvertising = true
                    currentState = BleHidState.ADVERTISING
                    startReconnectLoop()
                    return
                }
                ADVERTISE_FAILED_DATA_TOO_LARGE       -> "Data too large"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED  -> "Unsupported"
                ADVERTISE_FAILED_INTERNAL_ERROR       -> "Internal error"
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers"
                else -> "Error $errorCode"
            }
            Log.e(TAG, "✗ Advertising failed: $msg")
            currentState = BleHidState.ERROR("Advertising failed: $msg")
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GATT Server Callback
    // ═════════════════════════════════════════════════════════════════════════

    private val gattServerCallback = object : BluetoothGattServerCallback() {

        override fun onServiceAdded(status: Int, service: BluetoothGattService) {
            cancelTimeout()
            if (serviceQueue.isNotEmpty() && serviceQueue.first().uuid == service.uuid)
                serviceQueue.removeFirst()
            gattHandler?.postDelayed({ addNextService() }, SERVICE_ADD_DELAY_MS)
        }

        override fun onConnectionStateChange(
            device: BluetoothDevice, status: Int, newState: Int
        ) {
            Log.d(TAG, "connState ${device.address} status=$status new=$newState")
            when (newState) {

                BluetoothProfile.STATE_CONNECTED -> {
                    if (connectedDeviceMap.size >= MAX_CONNECTIONS) {
                        Log.w(TAG, "Max connections — rejecting ${device.address}")
                        try { gattServer?.cancelConnection(device) } catch (e: Exception) {}
                        return
                    }
                    refreshBondedCache()
                    val resolvedName = resolveName(device)
                    val isKnownHost  = knownHostAddresses.contains(device.address)

                    if (isKnownHost) {
                        // Always add to subscribedDevices so sendInputReport() can reach it.
                        subscribedDevices.add(device)

                        // FIX (part b): Only update CCCD descriptors if characteristics exist.
                        // If services are still being added (cold-start race), mouseInputChar
                        // will be null here. onServicesReady() handles that case after setup.
                        if (mouseInputChar != null && keyboardInputChar != null) {
                            listOf(mouseInputChar, keyboardInputChar).forEach { char ->
                                char?.getDescriptor(UUID_CCCD)?.value =
                                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            }
                            Log.d(TAG, "Known host reconnected — subscription restored immediately: ${device.address}")
                        } else {
                            Log.d(TAG, "Known host reconnected before services ready — " +
                                  "onServicesReady() will restore subscription: ${device.address}")
                        }
                    }

                    connectedDeviceMap[device.address] = DeviceInfo(
                        device       = device,
                        address      = device.address,
                        name         = resolvedName,
                        isSubscribed = isKnownHost
                    )
                    Log.d(TAG, "Host connected: $resolvedName (${device.address}) knownHost=$isKnownHost")

                    mainHandler.post {
                        currentState = BleHidState.CONNECTED
                        // Only fire onDeviceSubscribed if chars were ready (not in cold-start race).
                        // onServicesReady() will fire it after setup if chars were null.
                        if (isKnownHost && mouseInputChar != null && keyboardInputChar != null) {
                            onDeviceSubscribed?.invoke(device)
                        }
                        notifyDeviceListChanged()
                    }

                    // Late name resolution
                    mainHandler.postDelayed({
                        val laterName = resolveName(device)
                        connectedDeviceMap[device.address]?.let { info ->
                            if (laterName != info.name) {
                                connectedDeviceMap[device.address] = info.copy(name = laterName)
                                notifyDeviceListChanged()
                            }
                        }
                    }, 2_000)
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    val info = connectedDeviceMap.remove(device.address)
                    subscribedDevices.remove(device)
                    Log.d(TAG, "Host disconnected: ${info?.name ?: device.address}" +
                          " (${connectedDeviceMap.size} remaining)")

                    mainHandler.post {
                        currentState = when {
                            connectedDeviceMap.isNotEmpty() -> BleHidState.CONNECTED
                            isAdvertising                   -> BleHidState.ADVERTISING
                            else                            -> BleHidState.ADVERTISING
                        }
                        notifyDeviceListChanged()

                        // AUTO-RECONNECT: restart advertising + attempt background connect
                        mainHandler.postDelayed({
                            restartAdvertisingIfNeeded()
                            try {
                                gattServer?.connect(device, true)
                                Log.d(TAG, "Direct autoConnect queued for ${device.address}")
                            } catch (e: Exception) {
                                Log.w(TAG, "Direct autoConnect failed: ${e.message}")
                            }
                            startReconnectLoop()
                        }, 800)
                    }
                }
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice, requestId: Int, offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            val value   = characteristic.value ?: byteArrayOf()
            val safeOff = offset.coerceAtMost(value.size)
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                safeOff, value.copyOfRange(safeOff, value.size))
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice, requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean, responseNeeded: Boolean,
            offset: Int, value: ByteArray?
        ) {
            characteristic.value = value
            if (responseNeeded)
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
        }

        override fun onDescriptorReadRequest(
            device: BluetoothDevice, requestId: Int, offset: Int,
            descriptor: BluetoothGattDescriptor
        ) {
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                offset, descriptor.value ?: byteArrayOf())
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice, requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean, responseNeeded: Boolean,
            offset: Int, value: ByteArray?
        ) {
            descriptor.value = value

            if (descriptor.uuid == UUID_CCCD) {
                val enabled = value?.contentEquals(
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == true

                if (enabled) {
                    Log.d(TAG, "✓ Subscribed: ${device.address}")
                    subscribedDevices.add(device)
                    connectedDeviceMap[device.address]?.let { info ->
                        connectedDeviceMap[device.address] = info.copy(isSubscribed = true)
                    }

                    // Persist this host so we auto-reconnect next time
                    saveKnownHost(device.address)

                    mainHandler.post {
                        onDeviceSubscribed?.invoke(device)
                        notifyDeviceListChanged()
                    }
                } else {
                    Log.d(TAG, "✗ Unsubscribed: ${device.address}")
                    subscribedDevices.remove(device)
                    connectedDeviceMap[device.address]?.let { info ->
                        connectedDeviceMap[device.address] = info.copy(isSubscribed = false)
                    }
                    mainHandler.post { notifyDeviceListChanged() }
                }
            }

            if (responseNeeded)
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS)
                Log.w(TAG, "notificationSent failed status=$status for ${device.address}")
        }

        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
            Log.d(TAG, "MTU → $mtu for ${device.address}")
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Report sending
    // ═════════════════════════════════════════════════════════════════════════

    private fun sendInputReport(
        characteristic: BluetoothGattCharacteristic,
        report: ByteArray
    ): Boolean {
        val server = gattServer ?: return false
        var sent = false
        for (device in subscribedDevices.toList()) {
            try {
                characteristic.value = report
                val ok = server.notifyCharacteristicChanged(device, characteristic, false)
                if (ok) sent = true
            } catch (e: Exception) {
                Log.e(TAG, "sendInputReport: ${e.message}")
            }
        }
        return sent
    }
}

// ── State ──────────────────────────────────────────────────────────────────────
sealed class BleHidState {
    object IDLE        : BleHidState()
    object STARTING    : BleHidState()
    object ADVERTISING : BleHidState()
    object CONNECTED   : BleHidState()
    data class ERROR(val message: String) : BleHidState()
}