// BleHidManager.kt
package com.arena.hidcompatibilitytester.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log

@SuppressLint("MissingPermission")
class BleHidManager(private val context: Context) {

    companion object {
        private const val TAG = "BleHidManager"
        private const val PREFS_NAME           = "ble_hid_prefs"
        private const val PREFS_KNOWN_HOSTS    = "known_hosts"
        private const val PREFS_LAST_HOST      = "last_host"
        private const val SERVICE_ADD_DELAY_MS = 600L
        private const val SERVICE_ADD_TIMEOUT  = 5_000L
        private const val RECONNECT_INTERVAL   = 8_000L
        private const val INITIAL_RECONNECT_DELAY = 1_500L
        private const val MAX_CONNECTIONS      = 4

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

        val APPEARANCE_MOUSE    = byteArrayOf(0xC2.toByte(), 0x03)
        val HID_INFORMATION     = byteArrayOf(0x11, 0x01, 0x00, 0x02)
        val PROTOCOL_MODE_REPORT = byteArrayOf(0x01)

        // Report IDs
        const val REPORT_ID_MOUSE    = 1
        const val REPORT_ID_KEYBOARD = 2
        const val REPORT_ID_CONSUMER = 3   // Media keys

        /**
         * Combined descriptor:
         *   Report ID 1 → Mouse (buttons + X + Y + wheel)
         *   Report ID 2 → Keyboard (modifiers + 6 keycodes)
         *   Report ID 3 → Consumer / Media keys (16-bit usage)
         */
        val REPORT_MAP = byteArrayOf(
            // ── Mouse ─────────────────────────────────────────────────────────
            0x05, 0x01,                   // Usage Page (Generic Desktop)
            0x09, 0x02,                   // Usage (Mouse)
            0xA1.toByte(), 0x01,          // Collection (Application)
            0x85.toByte(), REPORT_ID_MOUSE.toByte(), // Report ID 1
            0x09, 0x01,                   // Usage (Pointer)
            0xA1.toByte(), 0x00,          // Collection (Physical)
            // Buttons 1-5
            0x05, 0x09,                   // Usage Page (Buttons)
            0x19, 0x01,                   // Usage Minimum (1)
            0x29, 0x05,                   // Usage Maximum (5)
            0x15, 0x00,                   // Logical Minimum (0)
            0x25, 0x01,                   // Logical Maximum (1)
            0x95.toByte(), 0x05,          // Report Count (5)
            0x75, 0x01,                   // Report Size (1)
            0x81.toByte(), 0x02,          // Input (Data, Variable, Absolute)
            // Padding 3 bits
            0x95.toByte(), 0x01,          // Report Count (1)
            0x75, 0x03,                   // Report Size (3)
            0x81.toByte(), 0x03,          // Input (Constant)
            // X, Y movement
            0x05, 0x01,                   // Usage Page (Generic Desktop)
            0x09, 0x30,                   // Usage (X)
            0x09, 0x31,                   // Usage (Y)
            0x15, 0x81.toByte(),          // Logical Minimum (-127)
            0x25, 0x7F,                   // Logical Maximum (127)
            0x75, 0x08,                   // Report Size (8)
            0x95.toByte(), 0x02,          // Report Count (2)
            0x81.toByte(), 0x06,          // Input (Data, Variable, Relative)
            // Wheel
            0x09, 0x38,                   // Usage (Wheel)
            0x15, 0x81.toByte(),          // Logical Minimum (-127)
            0x25, 0x7F,                   // Logical Maximum (127)
            0x75, 0x08,                   // Report Size (8)
            0x95.toByte(), 0x01,          // Report Count (1)
            0x81.toByte(), 0x06,          // Input (Data, Variable, Relative)
            0xC0.toByte(),                // End Collection (Physical)
            0xC0.toByte(),                // End Collection (Application)

            // ── Keyboard ───────────────────────────────────────────────────────
            0x05, 0x01,                   // Usage Page (Generic Desktop)
            0x09, 0x06,                   // Usage (Keyboard)
            0xA1.toByte(), 0x01,          // Collection (Application)
            0x85.toByte(), REPORT_ID_KEYBOARD.toByte(), // Report ID 2
            // Modifier keys
            0x05, 0x07,                   // Usage Page (Key Codes)
            0x19, 0xE0.toByte(),          // Usage Minimum (224 = L-Ctrl)
            0x29, 0xE7.toByte(),          // Usage Maximum (231 = R-GUI)
            0x15, 0x00,                   // Logical Minimum (0)
            0x25, 0x01,                   // Logical Maximum (1)
            0x75, 0x01,                   // Report Size (1)
            0x95.toByte(), 0x08,          // Report Count (8)
            0x81.toByte(), 0x02,          // Input (Data, Variable, Absolute)
            // Reserved byte
            0x95.toByte(), 0x01,          // Report Count (1)
            0x75, 0x08,                   // Report Size (8)
            0x81.toByte(), 0x01,          // Input (Constant)
            // Key array (6 keys)
            0x95.toByte(), 0x06,          // Report Count (6)
            0x75, 0x08,                   // Report Size (8)
            0x15, 0x00,                   // Logical Minimum (0)
            0x25, 0x65,                   // Logical Maximum (101)
            0x05, 0x07,                   // Usage Page (Key Codes)
            0x19, 0x00,                   // Usage Minimum (0)
            0x29, 0x65,                   // Usage Maximum (101)
            0x81.toByte(), 0x00,          // Input (Data, Array)
            0xC0.toByte(),                // End Collection

            // ── Consumer / Media ───────────────────────────────────────────────
            0x05, 0x0C,                   // Usage Page (Consumer)
            0x09, 0x01,                   // Usage (Consumer Control)
            0xA1.toByte(), 0x01,          // Collection (Application)
            0x85.toByte(), REPORT_ID_CONSUMER.toByte(), // Report ID 3
            0x15, 0x00,                   // Logical Minimum (0)
            0x26, 0xFF.toByte(), 0x03,    // Logical Maximum (1023)
            0x19, 0x00,                   // Usage Minimum (0)
            0x2A.toByte(), 0xFF.toByte(), 0x03, // Usage Maximum (1023)
            0x75, 0x10,                   // Report Size (16)
            0x95.toByte(), 0x01,          // Report Count (1)
            0x81.toByte(), 0x00,          // Input (Data, Array)
            0xC0.toByte()                 // End Collection
        )

        // ── Keyboard modifier bitmasks ────────────────────────────────────────
        const val MOD_LEFT_CTRL   = 0x01
        const val MOD_LEFT_SHIFT  = 0x02
        const val MOD_LEFT_ALT    = 0x04
        const val MOD_LEFT_GUI    = 0x08   // Windows / Command
        const val MOD_RIGHT_CTRL  = 0x10
        const val MOD_RIGHT_SHIFT = 0x20
        const val MOD_RIGHT_ALT   = 0x40
        const val MOD_RIGHT_GUI   = 0x80

        // ── Consumer key codes (16-bit) ───────────────────────────────────────
        const val CONSUMER_PLAY_PAUSE   = 0x00CD
        const val CONSUMER_NEXT_TRACK   = 0x00B5
        const val CONSUMER_PREV_TRACK   = 0x00B6
        const val CONSUMER_STOP         = 0x00B7
        const val CONSUMER_VOL_UP       = 0x00E9
        const val CONSUMER_VOL_DOWN     = 0x00EA
        const val CONSUMER_MUTE         = 0x00E2
        const val CONSUMER_BRIGHTNESS_UP   = 0x006F
        const val CONSUMER_BRIGHTNESS_DOWN = 0x0070
        const val CONSUMER_SCREENSHOT   = 0x0065
    }

    // ── Bluetooth ─────────────────────────────────────────────────────────────
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private val originalName: String? = try { adapter?.name } catch (e: Exception) { null }

    // ── Persistence ───────────────────────────────────────────────────────────
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── GATT ──────────────────────────────────────────────────────────────────
    private var gattServer   : BluetoothGattServer? = null
    private var advertiser   : BluetoothLeAdvertiser? = null
    private var isAdvertising = false

    private var mouseInputChar   : BluetoothGattCharacteristic? = null
    private var keyboardInputChar: BluetoothGattCharacteristic? = null
    private var consumerInputChar: BluetoothGattCharacteristic? = null

    // ── Device tracking ───────────────────────────────────────────────────────
    data class DeviceInfo(
        val device      : BluetoothDevice,
        val address     : String,
        var name        : String?,
        var isSubscribed: Boolean = false
    )

    private val connectedDeviceMap  = mutableMapOf<String, DeviceInfo>()
    private val subscribedDevices   = mutableSetOf<BluetoothDevice>()
    private val knownHostAddresses  = mutableSetOf<String>()
    private val bondedDeviceCache   = mutableMapOf<String, String>()

    // ── Callbacks ─────────────────────────────────────────────────────────────
    var onStateChanged      : ((BleHidState) -> Unit)? = null
    var onDeviceListChanged : ((List<DeviceInfo>) -> Unit)? = null
    var onDeviceSubscribed  : ((BluetoothDevice) -> Unit)? = null
    var onPairRequired      : ((String) -> Unit)? = null  // address of host that removed bond

    // ── Handlers ──────────────────────────────────────────────────────────────
    private val mainHandler  = Handler(Looper.getMainLooper())
    private var gattThread  : HandlerThread? = null
    private var gattHandler : Handler? = null

    // ── Service queue ─────────────────────────────────────────────────────────
    private val serviceQueue    = ArrayDeque<BluetoothGattService>()
    private var timeoutRunnable : Runnable? = null

    // ── Reconnect ─────────────────────────────────────────────────────────────
    private var reconnectRunnable : Runnable? = null
    private var isRunning = false

    // ── State ─────────────────────────────────────────────────────────────────
    private var currentState: BleHidState = BleHidState.IDLE
        set(v) { field = v; mainHandler.post { onStateChanged?.invoke(v) } }

    // ═════════════════════════════════════════════════════════════════════════
    // Public API
    // ═════════════════════════════════════════════════════════════════════════

    fun isSupported() = try {
        adapter != null && adapter.isEnabled && adapter.isMultipleAdvertisementSupported
    } catch (e: Exception) { false }

    fun getConnectedDeviceInfoList() = connectedDeviceMap.values.toList()
    fun isReadyToSend()             = subscribedDevices.isNotEmpty()
    fun getLastHostAddress()        = prefs.getString(PREFS_LAST_HOST, null)

    fun start() {
        Log.d(TAG, "start() state=$currentState")
        if (!isSupported()) { currentState = BleHidState.ERROR("BLE peripheral not supported"); return }
        if (currentState is BleHidState.ADVERTISING ||
            currentState is BleHidState.CONNECTED   ||
            currentState is BleHidState.STARTING) {
            Log.w(TAG, "start() ignored — already running"); return
        }
        isRunning = true
        refreshBondedCache()
        if (knownHostAddresses.isEmpty()) knownHostAddresses.addAll(loadKnownHosts())
        Log.d(TAG, "Known hosts: $knownHostAddresses")

        currentState = BleHidState.STARTING
        startGattThread()
        gattHandler?.postDelayed({ openGattServer() }, 500)
    }

    fun stop() {
        isRunning = false
        resetInternalState(restoreName = true)
        currentState = BleHidState.IDLE
        notifyDeviceListChanged()
    }

    fun onBluetoothOff() {
        resetInternalState(restoreName = false, btStackAlive = false)
        currentState = BleHidState.IDLE
        notifyDeviceListChanged()
    }

    fun disconnectDevice(address: String) {
        val info = connectedDeviceMap[address] ?: return
        try { gattServer?.cancelConnection(info.device) } catch (e: Exception) {}
    }

    fun forgetDevice(address: String) {
        removeKnownHost(address)
        if (prefs.getString(PREFS_LAST_HOST, null) == address)
            prefs.edit().remove(PREFS_LAST_HOST).apply()
        disconnectDevice(address)
    }

    fun inviteReconnect(device: BluetoothDevice) {
        try { gattServer?.connect(device, true) }
        catch (e: Exception) { Log.e(TAG, "inviteReconnect: ${e.message}") }
    }

    // ── HID senders ───────────────────────────────────────────────────────────

    /**
     * Mouse report: buttons bitmask (1=L,2=R,4=M,8=Back,16=Fwd), dx, dy, wheel
     */
    fun sendMouseReport(
        dx: Int = 0, dy: Int = 0,
        buttons: Int = 0, wheel: Int = 0
    ): Boolean {
        val char = mouseInputChar ?: return false
        if (subscribedDevices.isEmpty()) return false
        return sendInputReport(char, byteArrayOf(
            buttons.and(0x1F).toByte(),
            dx.coerceIn(-127, 127).toByte(),
            dy.coerceIn(-127, 127).toByte(),
            wheel.coerceIn(-127, 127).toByte()
        ))
    }

    /** Send key-down. Call releaseKeys() after. */
    fun sendKeyboardReport(modifiers: Int = 0, keyCodes: List<Int> = emptyList()): Boolean {
        val char = keyboardInputChar ?: return false
        if (subscribedDevices.isEmpty()) return false
        val r = ByteArray(8)
        r[0] = modifiers.toByte()
        keyCodes.take(6).forEachIndexed { i, c -> r[2 + i] = c.toByte() }
        return sendInputReport(char, r)
    }

    fun releaseKeys() = sendKeyboardReport()

    /** Send a consumer/media key (press + release). */
    fun sendConsumerKey(usage: Int): Boolean {
        val char = consumerInputChar ?: return false
        if (subscribedDevices.isEmpty()) return false
        val press = byteArrayOf(usage.and(0xFF).toByte(), usage.shr(8).and(0xFF).toByte())
        val release = byteArrayOf(0, 0)
        val ok = sendInputReport(char, press)
        mainHandler.postDelayed({ sendInputReport(char, release) }, 80)
        return ok
    }

    // ── Shortcut helpers ──────────────────────────────────────────────────────

    fun sendShortcut(modifiers: Int, keyCode: Int) {
        sendKeyboardReport(modifiers, listOf(keyCode))
        mainHandler.postDelayed({ releaseKeys() }, 100)
    }

    fun typeText(text: String) {
        var delay = 0L
        for (ch in text) {
            val (mod, code) = charToHidKey(ch)
            mainHandler.postDelayed({
                sendKeyboardReport(mod, listOf(code))
                mainHandler.postDelayed({ releaseKeys() }, 50)
            }, delay)
            delay += 80
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Internal — Persistence
    // ═════════════════════════════════════════════════════════════════════════

    private fun loadKnownHosts(): Set<String> {
        val raw = prefs.getString(PREFS_KNOWN_HOSTS, "") ?: ""
        return if (raw.isBlank()) emptySet()
        else raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    private fun saveKnownHost(address: String) {
        knownHostAddresses.add(address)
        prefs.edit()
            .putString(PREFS_KNOWN_HOSTS, knownHostAddresses.joinToString(","))
            .putString(PREFS_LAST_HOST, address)
            .apply()
    }

    private fun removeKnownHost(address: String) {
        knownHostAddresses.remove(address)
        prefs.edit().putString(PREFS_KNOWN_HOSTS, knownHostAddresses.joinToString(",")).apply()
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Internal — Reconnect
    // ═════════════════════════════════════════════════════════════════════════

    private fun startReconnectLoop() {
        cancelReconnectLoop()
        if (knownHostAddresses.isEmpty()) return
        val r = object : Runnable {
            override fun run() {
                if (!isRunning) return
                attemptAutoConnectAll()
                mainHandler.postDelayed(this, RECONNECT_INTERVAL)
            }
        }
        reconnectRunnable = r
        mainHandler.postDelayed(r, INITIAL_RECONNECT_DELAY)
    }

    private fun cancelReconnectLoop() {
        reconnectRunnable?.let { mainHandler.removeCallbacks(it) }
        reconnectRunnable = null
    }

    private fun attemptAutoConnectAll() {
        val server = gattServer ?: return
        val connected = connectedDeviceMap.keys
        for (address in knownHostAddresses.toSet()) {
            if (address in connected) continue
            val device = try { adapter?.getRemoteDevice(address) } catch (e: Exception) { null } ?: continue
            try { server.connect(device, true) } catch (e: Exception) {}
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Bond events
    // ═════════════════════════════════════════════════════════════════════════

    fun onBondStateChanged(device: BluetoothDevice, bondState: Int) {
        when (bondState) {
            BluetoothDevice.BOND_NONE -> {
                if (knownHostAddresses.contains(device.address)) {
                    removeKnownHost(device.address)
                    mainHandler.post { onPairRequired?.invoke(device.address) }
                }
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Internal — GATT thread & server
    // ═════════════════════════════════════════════════════════════════════════

    private fun startGattThread() {
        stopGattThread()
        gattThread = HandlerThread("BleHidGattThread").also {
            it.start(); gattHandler = Handler(it.looper)
        }
    }

    private fun stopGattThread() {
        gattThread?.quitSafely(); gattThread = null; gattHandler = null
    }

    private fun openGattServer() {
        try {
            gattServer?.close(); gattServer = null
            gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
            if (gattServer == null) { currentState = BleHidState.ERROR("Cannot open GATT server"); return }
            gattHandler?.postDelayed({ beginAddingServices() }, 1_000)
        } catch (e: Exception) { currentState = BleHidState.ERROR("GATT: ${e.message}") }
    }

    private fun resetInternalState(restoreName: Boolean, btStackAlive: Boolean = true) {
        cancelReconnectLoop(); cancelTimeout()
        if (btStackAlive) {
            stopAdvertising()
            connectedDeviceMap.values.toList().forEach {
                try { gattServer?.cancelConnection(it.device) } catch (e: Exception) {}
            }
            try { gattServer?.close() } catch (e: Exception) {}
        }
        gattServer = null; advertiser = null; isAdvertising = false
        mouseInputChar = null; keyboardInputChar = null; consumerInputChar = null
        connectedDeviceMap.clear(); subscribedDevices.clear(); serviceQueue.clear()
        stopGattThread()
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Service pipeline
    // ═════════════════════════════════════════════════════════════════════════

    private fun beginAddingServices() {
        serviceQueue.clear()
        mouseInputChar = null; keyboardInputChar = null; consumerInputChar = null
        subscribedDevices.clear()

        serviceQueue.addLast(buildGenericAccessService())
        serviceQueue.addLast(buildDeviceInformationService())
        serviceQueue.addLast(buildBatteryService())
        serviceQueue.addLast(buildHidService())
        addNextService()
    }

    private fun addNextService() {
        if (serviceQueue.isEmpty()) { mainHandler.postDelayed({ onServicesReady() }, 500); return }
        val svc = serviceQueue.first()
        armTimeout(svc.uuid.toString())
        val ok = try { gattServer?.addService(svc) ?: false } catch (e: Exception) { false }
        if (!ok) {
            cancelTimeout()
            gattHandler?.postDelayed({
                val retry = try { gattServer?.addService(svc) ?: false } catch (e: Exception) { false }
                if (!retry) { serviceQueue.removeFirst(); addNextService() }
                else armTimeout(svc.uuid.toString())
            }, 1_000)
        }
    }

    private fun armTimeout(uuid: String) {
        cancelTimeout()
        val r = Runnable {
            Log.w(TAG, "Timeout $uuid — advancing")
            if (serviceQueue.isNotEmpty()) serviceQueue.removeFirst()
            gattHandler?.post { addNextService() }
        }
        timeoutRunnable = r
        gattHandler?.postDelayed(r, SERVICE_ADD_TIMEOUT)
    }

    private fun cancelTimeout() {
        timeoutRunnable?.let { gattHandler?.removeCallbacks(it); mainHandler.removeCallbacks(it) }
        timeoutRunnable = null
    }

    private fun onServicesReady() {
        startAdvertising()
        // Restore subscriptions for known hosts that connected during setup
        connectedDeviceMap.values.toList().forEach { info ->
            if (!knownHostAddresses.contains(info.address)) return@forEach
            subscribedDevices.add(info.device)
            listOf(mouseInputChar, keyboardInputChar, consumerInputChar).forEach { char ->
                char?.getDescriptor(UUID_CCCD)?.value =
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            }
            connectedDeviceMap[info.address] = info.copy(isSubscribed = true)
            mainHandler.post { onDeviceSubscribed?.invoke(info.device); notifyDeviceListChanged() }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Service builders
    // ═════════════════════════════════════════════════════════════════════════

    private fun buildGenericAccessService() = BluetoothGattService(
        UUID_GENERIC_ACCESS, BluetoothGattService.SERVICE_TYPE_PRIMARY
    ).also {
        it.addCharacteristic(readChar(UUID_DEVICE_NAME, (originalName ?: android.os.Build.MODEL).toByteArray()))
        it.addCharacteristic(readChar(UUID_APPEARANCE, APPEARANCE_MOUSE))
    }

    private fun buildDeviceInformationService() = BluetoothGattService(
        UUID_DEVICE_INFORMATION, BluetoothGattService.SERVICE_TYPE_PRIMARY
    ).also {
        it.addCharacteristic(readChar(UUID_MANUFACTURER_NAME, "Arena".toByteArray()))
        it.addCharacteristic(readChar(UUID_MODEL_NUMBER, "HIDClone-1".toByteArray()))
        it.addCharacteristic(readChar(UUID_PNP_ID,
            byteArrayOf(0x02, 0x6D, 0x04, 0x2B, 0xC5.toByte(), 0x11, 0x01)))
    }

    private fun buildBatteryService() = BluetoothGattService(
        UUID_BATTERY_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY
    ).also { svc ->
        svc.addCharacteristic(BluetoothGattCharacteristic(
            UUID_BATTERY_LEVEL,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM
        ).apply { value = byteArrayOf(100); addDescriptor(cccd()) })
    }

    private fun buildHidService() = BluetoothGattService(
        UUID_HID_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY
    ).also { svc ->
        svc.addCharacteristic(readChar(UUID_HID_INFORMATION, HID_INFORMATION,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM))
        svc.addCharacteristic(readChar(UUID_REPORT_MAP, REPORT_MAP,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM))
        svc.addCharacteristic(BluetoothGattCharacteristic(UUID_HID_CONTROL_POINT,
            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM))
        svc.addCharacteristic(BluetoothGattCharacteristic(UUID_PROTOCOL_MODE,
            BluetoothGattCharacteristic.PROPERTY_READ or
            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM or
            BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM
        ).apply { value = PROTOCOL_MODE_REPORT })

        mouseInputChar    = inputReportChar(REPORT_ID_MOUSE,    0x01)
        keyboardInputChar = inputReportChar(REPORT_ID_KEYBOARD, 0x01)
        consumerInputChar = inputReportChar(REPORT_ID_CONSUMER, 0x01)

        svc.addCharacteristic(mouseInputChar!!)
        svc.addCharacteristic(keyboardInputChar!!)
        svc.addCharacteristic(consumerInputChar!!)
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Characteristic helpers
    // ═════════════════════════════════════════════════════════════════════════

    private fun readChar(uuid: java.util.UUID, value: ByteArray,
        permissions: Int = BluetoothGattCharacteristic.PERMISSION_READ) =
        BluetoothGattCharacteristic(uuid, BluetoothGattCharacteristic.PROPERTY_READ, permissions)
            .apply { this.value = value }

    private fun inputReportChar(reportId: Int, reportType: Int) =
        BluetoothGattCharacteristic(UUID_REPORT,
            BluetoothGattCharacteristic.PROPERTY_READ or
            BluetoothGattCharacteristic.PROPERTY_NOTIFY or
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM or
            BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM
        ).apply {
            addDescriptor(cccd())
            addDescriptor(BluetoothGattDescriptor(UUID_REPORT_REFERENCE,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM
            ).apply { value = byteArrayOf(reportId.toByte(), reportType.toByte()) })
        }

    private fun cccd() = BluetoothGattDescriptor(UUID_CCCD,
        BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
    ).apply { value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE }

    // ═════════════════════════════════════════════════════════════════════════
    // Advertising
    // ═════════════════════════════════════════════════════════════════════════

    private fun startAdvertising() {
        try {
            advertiser = adapter?.bluetoothLeAdvertiser
            if (advertiser == null) { currentState = BleHidState.ERROR("No advertiser"); return }
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true).setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM).build()
            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(android.os.ParcelUuid(UUID_HID_SERVICE)).build()
            val scan = AdvertiseData.Builder().setIncludeDeviceName(true).build()
            advertiser!!.startAdvertising(settings, data, scan, advertiseCallback)
        } catch (e: Exception) { currentState = BleHidState.ERROR("Adv error: ${e.message}") }
    }

    private fun stopAdvertising() {
        if (!isAdvertising) return
        try { advertiser?.stopAdvertising(advertiseCallback) } catch (e: Exception) {}
        isAdvertising = false
    }

    private fun restartAdvertisingIfNeeded() {
        if (isAdvertising || !isRunning) return
        startAdvertising()
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            isAdvertising = true
            currentState = BleHidState.ADVERTISING
            startReconnectLoop()
        }
        override fun onStartFailure(errorCode: Int) {
            if (errorCode == ADVERTISE_FAILED_ALREADY_STARTED) {
                isAdvertising = true; currentState = BleHidState.ADVERTISING
                startReconnectLoop(); return
            }
            currentState = BleHidState.ERROR("Adv failed: $errorCode")
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

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    if (connectedDeviceMap.size >= MAX_CONNECTIONS) {
                        try { gattServer?.cancelConnection(device) } catch (e: Exception) {}
                        return
                    }
                    refreshBondedCache()
                    val isKnown = knownHostAddresses.contains(device.address)
                    val name = resolveName(device)

                    if (isKnown) {
                        subscribedDevices.add(device)
                        if (mouseInputChar != null) {
                            listOf(mouseInputChar, keyboardInputChar, consumerInputChar).forEach {
                                it?.getDescriptor(UUID_CCCD)?.value =
                                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            }
                        }
                    }

                    connectedDeviceMap[device.address] = DeviceInfo(
                        device, device.address, name, isKnown)

                    mainHandler.post {
                        currentState = BleHidState.CONNECTED
                        if (isKnown && mouseInputChar != null) onDeviceSubscribed?.invoke(device)
                        notifyDeviceListChanged()
                    }

                    // Late name resolution
                    mainHandler.postDelayed({
                        val n = resolveName(device)
                        connectedDeviceMap[device.address]?.let { info ->
                            if (n != info.name) {
                                connectedDeviceMap[device.address] = info.copy(name = n)
                                notifyDeviceListChanged()
                            }
                        }
                    }, 2_000)
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedDeviceMap.remove(device.address)
                    subscribedDevices.remove(device)
                    mainHandler.post {
                        currentState = if (connectedDeviceMap.isNotEmpty())
                            BleHidState.CONNECTED else BleHidState.ADVERTISING
                        notifyDeviceListChanged()
                        mainHandler.postDelayed({
                            restartAdvertisingIfNeeded()
                            try { gattServer?.connect(device, true) } catch (e: Exception) {}
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
            val v = characteristic.value ?: byteArrayOf()
            val off = offset.coerceAtMost(v.size)
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                off, v.copyOfRange(off, v.size))
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice, requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?
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
            preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?
        ) {
            descriptor.value = value
            if (descriptor.uuid == UUID_CCCD) {
                val enabled = value?.contentEquals(
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == true
                if (enabled) {
                    subscribedDevices.add(device)
                    connectedDeviceMap[device.address]?.let {
                        connectedDeviceMap[device.address] = it.copy(isSubscribed = true)
                    }
                    saveKnownHost(device.address)
                    mainHandler.post { onDeviceSubscribed?.invoke(device); notifyDeviceListChanged() }
                } else {
                    subscribedDevices.remove(device)
                    connectedDeviceMap[device.address]?.let {
                        connectedDeviceMap[device.address] = it.copy(isSubscribed = false)
                    }
                    mainHandler.post { notifyDeviceListChanged() }
                }
            }
            if (responseNeeded)
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS)
                Log.w(TAG, "notif failed $status ${device.address}")
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Report send
    // ═════════════════════════════════════════════════════════════════════════

    private fun sendInputReport(char: BluetoothGattCharacteristic, report: ByteArray): Boolean {
        val server = gattServer ?: return false
        var sent = false
        for (device in subscribedDevices.toList()) {
            try {
                char.value = report
                if (server.notifyCharacteristicChanged(device, char, false)) sent = true
            } catch (e: Exception) { Log.e(TAG, "send: ${e.message}") }
        }
        return sent
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═════════════════════════════════════════════════════════════════════════

    private fun refreshBondedCache() {
        bondedDeviceCache.clear()
        try { adapter?.bondedDevices?.forEach { d ->
            val n = try { d.name } catch (e: Exception) { null }
            if (!n.isNullOrBlank()) bondedDeviceCache[d.address] = n
        }} catch (e: Exception) {}
    }

    private fun resolveName(device: BluetoothDevice): String {
        val n = try { device.name } catch (e: Exception) { null }
        if (!n.isNullOrBlank()) return n
        val c = bondedDeviceCache[device.address]
        if (!c.isNullOrBlank()) return c
        return "Device (${device.address.takeLast(8)})"
    }

    private fun notifyDeviceListChanged() {
        val list = connectedDeviceMap.values.toList()
        mainHandler.post { onDeviceListChanged?.invoke(list) }
    }

    // ── Char-to-HID-key table (basic ASCII) ──────────────────────────────────
    private fun charToHidKey(ch: Char): Pair<Int, Int> = when (ch) {
        'a' -> 0 to 0x04; 'b' -> 0 to 0x05; 'c' -> 0 to 0x06; 'd' -> 0 to 0x07
        'e' -> 0 to 0x08; 'f' -> 0 to 0x09; 'g' -> 0 to 0x0A; 'h' -> 0 to 0x0B
        'i' -> 0 to 0x0C; 'j' -> 0 to 0x0D; 'k' -> 0 to 0x0E; 'l' -> 0 to 0x0F
        'm' -> 0 to 0x10; 'n' -> 0 to 0x11; 'o' -> 0 to 0x12; 'p' -> 0 to 0x13
        'q' -> 0 to 0x14; 'r' -> 0 to 0x15; 's' -> 0 to 0x16; 't' -> 0 to 0x17
        'u' -> 0 to 0x18; 'v' -> 0 to 0x19; 'w' -> 0 to 0x1A; 'x' -> 0 to 0x1B
        'y' -> 0 to 0x1C; 'z' -> 0 to 0x1D
        'A' -> MOD_LEFT_SHIFT to 0x04; 'B' -> MOD_LEFT_SHIFT to 0x05
        'C' -> MOD_LEFT_SHIFT to 0x06; 'D' -> MOD_LEFT_SHIFT to 0x07
        'E' -> MOD_LEFT_SHIFT to 0x08; 'F' -> MOD_LEFT_SHIFT to 0x09
        'G' -> MOD_LEFT_SHIFT to 0x0A; 'H' -> MOD_LEFT_SHIFT to 0x0B
        'I' -> MOD_LEFT_SHIFT to 0x0C; 'J' -> MOD_LEFT_SHIFT to 0x0D
        'K' -> MOD_LEFT_SHIFT to 0x0E; 'L' -> MOD_LEFT_SHIFT to 0x0F
        'M' -> MOD_LEFT_SHIFT to 0x10; 'N' -> MOD_LEFT_SHIFT to 0x11
        'O' -> MOD_LEFT_SHIFT to 0x12; 'P' -> MOD_LEFT_SHIFT to 0x13
        'Q' -> MOD_LEFT_SHIFT to 0x14; 'R' -> MOD_LEFT_SHIFT to 0x15
        'S' -> MOD_LEFT_SHIFT to 0x16; 'T' -> MOD_LEFT_SHIFT to 0x17
        'U' -> MOD_LEFT_SHIFT to 0x18; 'V' -> MOD_LEFT_SHIFT to 0x19
        'W' -> MOD_LEFT_SHIFT to 0x1A; 'X' -> MOD_LEFT_SHIFT to 0x1B
        'Y' -> MOD_LEFT_SHIFT to 0x1C; 'Z' -> MOD_LEFT_SHIFT to 0x1D
        '1' -> 0 to 0x1E; '2' -> 0 to 0x1F; '3' -> 0 to 0x20; '4' -> 0 to 0x21
        '5' -> 0 to 0x22; '6' -> 0 to 0x23; '7' -> 0 to 0x24; '8' -> 0 to 0x25
        '9' -> 0 to 0x26; '0' -> 0 to 0x27
        '\n' -> 0 to 0x28; ' ' -> 0 to 0x2C
        '-' -> 0 to 0x2D; '=' -> 0 to 0x2E
        '[' -> 0 to 0x2F; ']' -> 0 to 0x30; '\\' -> 0 to 0x31
        ';' -> 0 to 0x33; '\'' -> 0 to 0x34; '`' -> 0 to 0x35
        ',' -> 0 to 0x36; '.' -> 0 to 0x37; '/' -> 0 to 0x38
        '!' -> MOD_LEFT_SHIFT to 0x1E; '@' -> MOD_LEFT_SHIFT to 0x1F
        '#' -> MOD_LEFT_SHIFT to 0x20; '$' -> MOD_LEFT_SHIFT to 0x21
        '%' -> MOD_LEFT_SHIFT to 0x22; '^' -> MOD_LEFT_SHIFT to 0x23
        '&' -> MOD_LEFT_SHIFT to 0x24; '*' -> MOD_LEFT_SHIFT to 0x25
        '(' -> MOD_LEFT_SHIFT to 0x26; ')' -> MOD_LEFT_SHIFT to 0x27
        '_' -> MOD_LEFT_SHIFT to 0x2D; '+' -> MOD_LEFT_SHIFT to 0x2E
        '{' -> MOD_LEFT_SHIFT to 0x2F; '}' -> MOD_LEFT_SHIFT to 0x30
        '|' -> MOD_LEFT_SHIFT to 0x31; ':' -> MOD_LEFT_SHIFT to 0x33
        '"' -> MOD_LEFT_SHIFT to 0x34; '<' -> MOD_LEFT_SHIFT to 0x36
        '>' -> MOD_LEFT_SHIFT to 0x37; '?' -> MOD_LEFT_SHIFT to 0x38
        else -> 0 to 0x00
    }
}