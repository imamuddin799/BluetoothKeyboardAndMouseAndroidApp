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
import android.provider.Settings

@SuppressLint("MissingPermission")
class BleHidManager(private val context: Context) {

    companion object {
        private const val TAG = "BleHidManager"
        private const val PREFS_NAME              = "ble_hid_prefs"
        private const val PREFS_KNOWN_HOSTS       = "known_hosts"
        private const val PREFS_LAST_HOST         = "last_host"
        private const val SERVICE_ADD_DELAY_MS    = 600L
        private const val SERVICE_ADD_TIMEOUT     = 5_000L
        private const val RECONNECT_INTERVAL      = 8_000L
        private const val INITIAL_RECONNECT_DELAY = 1_500L
        private const val MAX_CONNECTIONS         = 4

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

        val APPEARANCE_MOUSE     = byteArrayOf(0xC2.toByte(), 0x03)
        val HID_INFORMATION      = byteArrayOf(0x11, 0x01, 0x00, 0x02)
        val PROTOCOL_MODE_REPORT = byteArrayOf(0x01)

        const val REPORT_ID_MOUSE    = 1
        const val REPORT_ID_KEYBOARD = 2
        const val REPORT_ID_CONSUMER = 3

        val REPORT_MAP = byteArrayOf(
            0x05, 0x01,
            0x09, 0x02,
            0xA1.toByte(), 0x01,
            0x85.toByte(), REPORT_ID_MOUSE.toByte(),
            0x09, 0x01,
            0xA1.toByte(), 0x00,
            0x05, 0x09,
            0x19, 0x01,
            0x29, 0x05,
            0x15, 0x00,
            0x25, 0x01,
            0x95.toByte(), 0x05,
            0x75, 0x01,
            0x81.toByte(), 0x02,
            0x95.toByte(), 0x01,
            0x75, 0x03,
            0x81.toByte(), 0x03,
            0x05, 0x01,
            0x09, 0x30,
            0x09, 0x31,
            0x15, 0x81.toByte(),
            0x25, 0x7F,
            0x75, 0x08,
            0x95.toByte(), 0x02,
            0x81.toByte(), 0x06,
            0x09, 0x38,
            0x15, 0x81.toByte(),
            0x25, 0x7F,
            0x75, 0x08,
            0x95.toByte(), 0x01,
            0x81.toByte(), 0x06,
            0xC0.toByte(),
            0xC0.toByte(),

            0x05, 0x01,
            0x09, 0x06,
            0xA1.toByte(), 0x01,
            0x85.toByte(), REPORT_ID_KEYBOARD.toByte(),
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
            0xC0.toByte(),

            0x05, 0x0C,
            0x09, 0x01,
            0xA1.toByte(), 0x01,
            0x85.toByte(), REPORT_ID_CONSUMER.toByte(),
            0x15, 0x00,
            0x26, 0xFF.toByte(), 0x03,
            0x19, 0x00,
            0x2A.toByte(), 0xFF.toByte(), 0x03,
            0x75, 0x10,
            0x95.toByte(), 0x01,
            0x81.toByte(), 0x00,
            0xC0.toByte()
        )

        const val MOD_LEFT_CTRL   = 0x01
        const val MOD_LEFT_SHIFT  = 0x02
        const val MOD_LEFT_ALT    = 0x04
        const val MOD_LEFT_GUI    = 0x08
        const val MOD_RIGHT_CTRL  = 0x10
        const val MOD_RIGHT_SHIFT = 0x20
        const val MOD_RIGHT_ALT   = 0x40
        const val MOD_RIGHT_GUI   = 0x80

        const val CONSUMER_PLAY_PAUSE      = 0x00CD
        const val CONSUMER_NEXT_TRACK      = 0x00B5
        const val CONSUMER_PREV_TRACK      = 0x00B6
        const val CONSUMER_STOP            = 0x00B7
        const val CONSUMER_VOL_UP          = 0x00E9
        const val CONSUMER_VOL_DOWN        = 0x00EA
        const val CONSUMER_MUTE            = 0x00E2
        const val CONSUMER_BRIGHTNESS_UP   = 0x006F
        const val CONSUMER_BRIGHTNESS_DOWN = 0x0070
        const val CONSUMER_SCREENSHOT      = 0x0065
    }

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private val originalName: String = try {
        Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
            ?: adapter?.name
            ?: android.os.Build.MODEL
    } catch (e: Exception) {
        android.os.Build.MODEL
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var gattServer   : BluetoothGattServer? = null
    private var advertiser   : BluetoothLeAdvertiser? = null
    private var isAdvertising = false

    private var mouseInputChar   : BluetoothGattCharacteristic? = null
    private var keyboardInputChar: BluetoothGattCharacteristic? = null
    private var consumerInputChar: BluetoothGattCharacteristic? = null

    data class DeviceInfo(
        val device      : BluetoothDevice,
        val address     : String,
        var name        : String?,
        var isSubscribed: Boolean = false
    )

    private val connectedDeviceMap = mutableMapOf<String, DeviceInfo>()
    private val subscribedDevices  = mutableSetOf<BluetoothDevice>()
    private val knownHostAddresses = mutableSetOf<String>()
    private val bondedDeviceCache  = mutableMapOf<String, String>()

    enum class TargetMode { ALL, SINGLE }

    private var targetMode   : TargetMode = TargetMode.ALL
    private var targetAddress: String?    = null

    var onStateChanged      : ((BleHidState) -> Unit)?     = null
    var onDeviceListChanged : ((List<DeviceInfo>) -> Unit)? = null
    var onDeviceSubscribed  : ((BluetoothDevice) -> Unit)?  = null
    var onPairRequired      : ((String) -> Unit)?           = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var gattThread : HandlerThread? = null
    private var gattHandler: Handler?       = null

    private val serviceQueue    = ArrayDeque<BluetoothGattService>()
    private var timeoutRunnable : Runnable? = null

    private var reconnectRunnable: Runnable? = null
    private var isRunning = false

    private var currentState: BleHidState = BleHidState.IDLE
        set(v) { field = v; mainHandler.post { onStateChanged?.invoke(v) } }

    // ═════════════════════════════════════════════════════════════════════════
    // Public API
    // ═════════════════════════════════════════════════════════════════════════

    fun getCurrentState()            = currentState
    fun getConnectedDeviceInfoList() = connectedDeviceMap.values.toList()
    fun isReadyToSend()              = subscribedDevices.isNotEmpty()
    fun getLastHostAddress()         = prefs.getString(PREFS_LAST_HOST, null)

    fun isSupported() = try {
        adapter != null &&
        adapter.isEnabled &&
        adapter.isMultipleAdvertisementSupported
    } catch (e: Exception) { false }

    // ── Target selection ──────────────────────────────────────────────────────

    fun setTargetAll() {
        targetMode    = TargetMode.ALL
        targetAddress = null
    }

    fun setTargetDevice(address: String) {
        targetMode    = TargetMode.SINGLE
        targetAddress = address
    }

    fun getTargetMode()    = targetMode
    fun getTargetAddress() = targetAddress

    private fun getTargetDevices(): List<BluetoothDevice> = when (targetMode) {
        TargetMode.ALL    -> subscribedDevices.toList()
        TargetMode.SINGLE -> {
            val addr = targetAddress ?: return emptyList()
            subscribedDevices.filter { it.address == addr }
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun start() {
        Log.d(TAG, "start() state=$currentState")
        if (!isSupported()) {
            currentState = BleHidState.ERROR("BLE peripheral not supported")
            return
        }
        if (currentState is BleHidState.ADVERTISING ||
            currentState is BleHidState.CONNECTED   ||
            currentState is BleHidState.STARTING) {
            Log.w(TAG, "start() ignored — already running")
            return
        }
        isRunning = true
        refreshBondedCache()

        // Always reload from prefs — never use stale in-memory set
        knownHostAddresses.clear()
        knownHostAddresses.addAll(loadKnownHosts())
        Log.d(TAG, "Known hosts on start: $knownHostAddresses")

        currentState = BleHidState.STARTING
        try { adapter?.name = originalName } catch (_: Exception) {}
        startGattThread()
        gattHandler?.postDelayed({ openGattServer() }, 500)
    }

    fun stop() {
        isRunning = false
        resetInternalState(restoreName = true)
        targetMode    = TargetMode.ALL
        targetAddress = null
        currentState  = BleHidState.IDLE
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

    fun clearAllKnownHosts() {
        knownHostAddresses.clear()
        prefs.edit()
            .remove(PREFS_KNOWN_HOSTS)
            .remove(PREFS_LAST_HOST)
            .apply()
        Log.d(TAG, "clearAllKnownHosts: prefs cleared")
    }

    fun inviteReconnect(device: BluetoothDevice) {
        try { gattServer?.connect(device, true) }
        catch (e: Exception) { Log.e(TAG, "inviteReconnect: ${e.message}") }
    }

    // ── HID senders ───────────────────────────────────────────────────────────

    fun sendMouseReport(
        dx: Int = 0, dy: Int = 0,
        buttons: Int = 0, wheel: Int = 0
    ): Boolean {
        val char = mouseInputChar ?: return false
        if (subscribedDevices.isEmpty()) return false
        return sendInputReport(
            char, byteArrayOf(
                buttons.and(0x1F).toByte(),
                dx.coerceIn(-127, 127).toByte(),
                dy.coerceIn(-127, 127).toByte(),
                wheel.coerceIn(-127, 127).toByte()
            )
        )
    }

    fun sendKeyboardReport(
        modifiers: Int = 0,
        keyCodes : List<Int> = emptyList()
    ): Boolean {
        val char = keyboardInputChar ?: return false
        if (subscribedDevices.isEmpty()) return false
        val r = ByteArray(8)
        r[0] = modifiers.toByte()
        keyCodes.take(6).forEachIndexed { i, c -> r[2 + i] = c.toByte() }
        return sendInputReport(char, r)
    }

    fun releaseKeys() = sendKeyboardReport()

    fun sendConsumerKey(usage: Int): Boolean {
        val char = consumerInputChar ?: return false
        if (subscribedDevices.isEmpty()) return false
        val press   = byteArrayOf(
            usage.and(0xFF).toByte(),
            usage.shr(8).and(0xFF).toByte()
        )
        val release = byteArrayOf(0, 0)
        val ok = sendInputReport(char, press)
        mainHandler.postDelayed({ sendInputReport(char, release) }, 80)
        return ok
    }

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
        prefs.edit()
            .putString(PREFS_KNOWN_HOSTS, knownHostAddresses.joinToString(","))
            .apply()
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
        val server    = gattServer ?: return
        val connected = connectedDeviceMap.keys
        for (address in knownHostAddresses.toSet()) {
            if (address in connected) continue
            val device = try {
                adapter?.getRemoteDevice(address)
            } catch (e: Exception) { null } ?: continue
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
            it.start()
            gattHandler = Handler(it.looper)
        }
    }

    private fun stopGattThread() {
        gattThread?.quitSafely()
        gattThread  = null
        gattHandler = null
    }

    private fun openGattServer() {
        try {
            gattServer?.close()
            gattServer = null
            gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
            if (gattServer == null) {
                currentState = BleHidState.ERROR("Cannot open GATT server")
                return
            }
            gattHandler?.postDelayed({ beginAddingServices() }, 1_000)
        } catch (e: Exception) {
            currentState = BleHidState.ERROR("GATT: ${e.message}")
        }
    }

    private fun resetInternalState(
        restoreName : Boolean,
        btStackAlive: Boolean = true
    ) {
        cancelReconnectLoop()
        cancelTimeout()
        if (btStackAlive) {
            stopAdvertising()
            connectedDeviceMap.values.toList().forEach {
                try { gattServer?.cancelConnection(it.device) } catch (e: Exception) {}
            }
            try { gattServer?.close() } catch (e: Exception) {}
        }
        gattServer        = null
        advertiser        = null
        isAdvertising     = false
        mouseInputChar    = null
        keyboardInputChar = null
        consumerInputChar = null
        connectedDeviceMap.clear()
        subscribedDevices.clear()
        serviceQueue.clear()
        stopGattThread()
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Service pipeline
    // ═════════════════════════════════════════════════════════════════════════

    private fun beginAddingServices() {
        serviceQueue.clear()
        mouseInputChar    = null
        keyboardInputChar = null
        consumerInputChar = null
        subscribedDevices.clear()

        serviceQueue.addLast(buildGenericAccessService())
        serviceQueue.addLast(buildDeviceInformationService())
        serviceQueue.addLast(buildBatteryService())
        serviceQueue.addLast(buildHidService())
        addNextService()
    }

    private fun addNextService() {
        if (serviceQueue.isEmpty()) {
            mainHandler.postDelayed({ onServicesReady() }, 500)
            return
        }
        val svc = serviceQueue.first()
        armTimeout(svc.uuid.toString())
        val ok = try {
            gattServer?.addService(svc) ?: false
        } catch (e: Exception) { false }
        if (!ok) {
            cancelTimeout()
            gattHandler?.postDelayed({
                val retry = try {
                    gattServer?.addService(svc) ?: false
                } catch (e: Exception) { false }
                if (!retry) {
                    serviceQueue.removeFirst()
                    addNextService()
                } else {
                    armTimeout(svc.uuid.toString())
                }
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
        timeoutRunnable?.let {
            gattHandler?.removeCallbacks(it)
            mainHandler.removeCallbacks(it)
        }
        timeoutRunnable = null
    }

    private fun onServicesReady() {
        startAdvertising()
        connectedDeviceMap.values.toList().forEach { info ->
            if (!knownHostAddresses.contains(info.address)) return@forEach
            // Remove stale, add fresh
            subscribedDevices.removeAll { it.address == info.address }
            subscribedDevices.add(info.device)
            listOf(mouseInputChar, keyboardInputChar, consumerInputChar).forEach { char ->
                char?.getDescriptor(UUID_CCCD)?.value =
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            }
            connectedDeviceMap[info.address] = info.copy(isSubscribed = true)
            mainHandler.post {
                onDeviceSubscribed?.invoke(info.device)
                notifyDeviceListChanged()
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Service builders
    // ═════════════════════════════════════════════════════════════════════════

    private fun buildGenericAccessService() = BluetoothGattService(
        UUID_GENERIC_ACCESS, BluetoothGattService.SERVICE_TYPE_PRIMARY
    ).also {
        it.addCharacteristic(readChar(UUID_DEVICE_NAME, originalName.toByteArray()))
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
        svc.addCharacteristic(
            BluetoothGattCharacteristic(
                UUID_BATTERY_LEVEL,
                BluetoothGattCharacteristic.PROPERTY_READ or
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ
            ).apply {
                value = byteArrayOf(100)
                addDescriptor(cccd())
            }
        )
    }

    private fun buildHidService() = BluetoothGattService(
        UUID_HID_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY
    ).also { svc ->
        svc.addCharacteristic(readChar(
            UUID_HID_INFORMATION, HID_INFORMATION,
            BluetoothGattCharacteristic.PERMISSION_READ
        ))
        svc.addCharacteristic(readChar(
            UUID_REPORT_MAP, REPORT_MAP,
            BluetoothGattCharacteristic.PERMISSION_READ
        ))
        svc.addCharacteristic(BluetoothGattCharacteristic(
            UUID_HID_CONTROL_POINT,
            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        ))
        svc.addCharacteristic(BluetoothGattCharacteristic(
            UUID_PROTOCOL_MODE,
            BluetoothGattCharacteristic.PROPERTY_READ or
            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_READ or
            BluetoothGattCharacteristic.PERMISSION_WRITE
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

    private fun readChar(
        uuid       : java.util.UUID,
        value      : ByteArray,
        permissions: Int = BluetoothGattCharacteristic.PERMISSION_READ
    ) = BluetoothGattCharacteristic(
        uuid,
        BluetoothGattCharacteristic.PROPERTY_READ,
        permissions
    ).apply { this.value = value }

    private fun inputReportChar(reportId: Int, reportType: Int) =
        BluetoothGattCharacteristic(
            UUID_REPORT,
            BluetoothGattCharacteristic.PROPERTY_READ  or
            BluetoothGattCharacteristic.PROPERTY_NOTIFY or
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ or
            BluetoothGattCharacteristic.PERMISSION_WRITE
        ).apply {
            addDescriptor(cccd())
            addDescriptor(
                BluetoothGattDescriptor(
                    UUID_REPORT_REFERENCE,
                    BluetoothGattDescriptor.PERMISSION_READ
                ).apply {
                    value = byteArrayOf(reportId.toByte(), reportType.toByte())
                }
            )
        }

    private fun cccd() = BluetoothGattDescriptor(
        UUID_CCCD,
        BluetoothGattDescriptor.PERMISSION_READ or
        BluetoothGattDescriptor.PERMISSION_WRITE
    ).apply { value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE }

    // ═════════════════════════════════════════════════════════════════════════
    // Advertising
    // ═════════════════════════════════════════════════════════════════════════

    private fun startAdvertising() {
        try {
            advertiser = adapter?.bluetoothLeAdvertiser
            if (advertiser == null) {
                currentState = BleHidState.ERROR("No advertiser")
                return
            }
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build()
            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(android.os.ParcelUuid(UUID_HID_SERVICE))
                .build()
            val scan = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build()
            advertiser!!.startAdvertising(settings, data, scan, advertiseCallback)
        } catch (e: Exception) {
            currentState = BleHidState.ERROR("Adv error: ${e.message}")
        }
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
            currentState  = BleHidState.ADVERTISING
            startReconnectLoop()
        }
        override fun onStartFailure(errorCode: Int) {
            if (errorCode == ADVERTISE_FAILED_ALREADY_STARTED) {
                isAdvertising = true
                currentState  = BleHidState.ADVERTISING
                startReconnectLoop()
                return
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

        override fun onConnectionStateChange(
            device  : BluetoothDevice,
            status  : Int,
            newState: Int
        ) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    if (connectedDeviceMap.size >= MAX_CONNECTIONS) {
                        try { gattServer?.cancelConnection(device) } catch (e: Exception) {}
                        return
                    }
                    refreshBondedCache()
                    val isKnown = knownHostAddresses.contains(device.address)
                    val name    = resolveName(device)

                    if (isKnown) {
                        // Remove stale device object, add fresh one
                        subscribedDevices.removeAll { it.address == device.address }
                        subscribedDevices.add(device)
                        if (mouseInputChar != null) {
                            listOf(mouseInputChar, keyboardInputChar, consumerInputChar)
                                .forEach { char ->
                                    char?.getDescriptor(UUID_CCCD)?.value =
                                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                }
                        }
                    }

                    connectedDeviceMap[device.address] =
                        DeviceInfo(device, device.address, name, isKnown)

                    mainHandler.post {
                        currentState = BleHidState.CONNECTED
                        if (isKnown && mouseInputChar != null)
                            onDeviceSubscribed?.invoke(device)
                        notifyDeviceListChanged()
                    }

                    // Multiple name resolution attempts at 1s, 3s, 6s
                    listOf(1_000L, 3_000L, 6_000L).forEach { delay ->
                        mainHandler.postDelayed({
                            val n = resolveName(device)
                            connectedDeviceMap[device.address]?.let { info ->
                                if (n != info.name && !n.startsWith("Device (")) {
                                    connectedDeviceMap[device.address] = info.copy(name = n)
                                    notifyDeviceListChanged()
                                }
                            }
                        }, delay)
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedDeviceMap.remove(device.address)
                    subscribedDevices.removeAll { it.address == device.address }

                    // Reset target if the targeted device disconnected
                    if (targetMode == TargetMode.SINGLE &&
                        targetAddress == device.address) {
                        targetMode    = TargetMode.ALL
                        targetAddress = null
                    }

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
            device        : BluetoothDevice,
            requestId     : Int,
            offset        : Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            val v   = characteristic.value ?: byteArrayOf()
            val off = offset.coerceAtMost(v.size)
            gattServer?.sendResponse(
                device, requestId,
                BluetoothGatt.GATT_SUCCESS,
                off, v.copyOfRange(off, v.size)
            )
        }

        override fun onCharacteristicWriteRequest(
            device          : BluetoothDevice,
            requestId       : Int,
            characteristic  : BluetoothGattCharacteristic,
            preparedWrite   : Boolean,
            responseNeeded  : Boolean,
            offset          : Int,
            value           : ByteArray?
        ) {
            characteristic.value = value
            if (responseNeeded)
                gattServer?.sendResponse(
                    device, requestId,
                    BluetoothGatt.GATT_SUCCESS, 0, null
                )
        }

        override fun onDescriptorReadRequest(
            device    : BluetoothDevice,
            requestId : Int,
            offset    : Int,
            descriptor: BluetoothGattDescriptor
        ) {
            gattServer?.sendResponse(
                device, requestId,
                BluetoothGatt.GATT_SUCCESS,
                offset, descriptor.value ?: byteArrayOf()
            )
        }

        override fun onDescriptorWriteRequest(
            device        : BluetoothDevice,
            requestId     : Int,
            descriptor    : BluetoothGattDescriptor,
            preparedWrite : Boolean,
            responseNeeded: Boolean,
            offset        : Int,
            value         : ByteArray?
        ) {
            descriptor.value = value

            if (descriptor.uuid == UUID_CCCD) {
                val enabled = value?.contentEquals(
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                ) == true

                if (enabled) {
                    // Remove stale entry with same address, add fresh device
                    subscribedDevices.removeAll { it.address == device.address }
                    subscribedDevices.add(device)
                    connectedDeviceMap[device.address]?.let {
                        connectedDeviceMap[device.address] = it.copy(
                            isSubscribed = true,
                            device = device
                        )
                    }
                    saveKnownHost(device.address)
                    mainHandler.post {
                        onDeviceSubscribed?.invoke(device)
                        notifyDeviceListChanged()
                    }
                } else {
                    subscribedDevices.removeAll { it.address == device.address }
                    connectedDeviceMap[device.address]?.let {
                        connectedDeviceMap[device.address] = it.copy(isSubscribed = false)
                    }
                    mainHandler.post { notifyDeviceListChanged() }
                }
            }

            if (responseNeeded)
                gattServer?.sendResponse(
                    device, requestId,
                    BluetoothGatt.GATT_SUCCESS, 0, null
                )
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS)
                Log.w(TAG, "notif failed $status ${device.address}")
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Report send — routes to target device(s) only
    // ═════════════════════════════════════════════════════════════════════════

    private fun sendInputReport(
        char  : BluetoothGattCharacteristic,
        report: ByteArray
    ): Boolean {
        val server  = gattServer ?: return false
        val targets = getTargetDevices()
        if (targets.isEmpty()) return false
        var sent = false
        for (device in targets) {
            try {
                char.value = report
                if (server.notifyCharacteristicChanged(device, char, false)) sent = true
            } catch (e: Exception) {
                Log.e(TAG, "send: ${e.message}")
            }
        }
        return sent
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═════════════════════════════════════════════════════════════════════════

    private fun refreshBondedCache() {
        bondedDeviceCache.clear()
        try {
            adapter?.bondedDevices?.forEach { d ->
                val n = try { d.name } catch (e: Exception) { null }
                if (!n.isNullOrBlank()) bondedDeviceCache[d.address] = n
            }
        } catch (e: Exception) {}
    }

    private fun resolveName(device: BluetoothDevice): String {
        // Try direct name
        val directName = try { device.name } catch (e: Exception) { null }
        if (!directName.isNullOrBlank()) return directName

        // Try bonded cache
        val cachedName = bondedDeviceCache[device.address]
        if (!cachedName.isNullOrBlank()) return cachedName

        // Try scanning bonded devices again live
        try {
            adapter?.bondedDevices?.forEach { bonded ->
                if (bonded.address == device.address) {
                    val n = try { bonded.name } catch (e: Exception) { null }
                    if (!n.isNullOrBlank()) {
                        bondedDeviceCache[device.address] = n
                        return n
                    }
                }
            }
        } catch (e: Exception) {}

        return "Device (${device.address.takeLast(8)})"
    }

    private fun notifyDeviceListChanged() {
        val list = connectedDeviceMap.values.toList()
        mainHandler.post { onDeviceListChanged?.invoke(list) }
    }

    private fun charToHidKey(ch: Char): Pair<Int, Int> = when (ch) {
        'a'  -> 0 to 0x04; 'b'  -> 0 to 0x05; 'c'  -> 0 to 0x06; 'd'  -> 0 to 0x07
        'e'  -> 0 to 0x08; 'f'  -> 0 to 0x09; 'g'  -> 0 to 0x0A; 'h'  -> 0 to 0x0B
        'i'  -> 0 to 0x0C; 'j'  -> 0 to 0x0D; 'k'  -> 0 to 0x0E; 'l'  -> 0 to 0x0F
        'm'  -> 0 to 0x10; 'n'  -> 0 to 0x11; 'o'  -> 0 to 0x12; 'p'  -> 0 to 0x13
        'q'  -> 0 to 0x14; 'r'  -> 0 to 0x15; 's'  -> 0 to 0x16; 't'  -> 0 to 0x17
        'u'  -> 0 to 0x18; 'v'  -> 0 to 0x19; 'w'  -> 0 to 0x1A; 'x'  -> 0 to 0x1B
        'y'  -> 0 to 0x1C; 'z'  -> 0 to 0x1D
        'A'  -> MOD_LEFT_SHIFT to 0x04; 'B'  -> MOD_LEFT_SHIFT to 0x05
        'C'  -> MOD_LEFT_SHIFT to 0x06; 'D'  -> MOD_LEFT_SHIFT to 0x07
        'E'  -> MOD_LEFT_SHIFT to 0x08; 'F'  -> MOD_LEFT_SHIFT to 0x09
        'G'  -> MOD_LEFT_SHIFT to 0x0A; 'H'  -> MOD_LEFT_SHIFT to 0x0B
        'I'  -> MOD_LEFT_SHIFT to 0x0C; 'J'  -> MOD_LEFT_SHIFT to 0x0D
        'K'  -> MOD_LEFT_SHIFT to 0x0E; 'L'  -> MOD_LEFT_SHIFT to 0x0F
        'M'  -> MOD_LEFT_SHIFT to 0x10; 'N'  -> MOD_LEFT_SHIFT to 0x11
        'O'  -> MOD_LEFT_SHIFT to 0x12; 'P'  -> MOD_LEFT_SHIFT to 0x13
        'Q'  -> MOD_LEFT_SHIFT to 0x14; 'R'  -> MOD_LEFT_SHIFT to 0x15
        'S'  -> MOD_LEFT_SHIFT to 0x16; 'T'  -> MOD_LEFT_SHIFT to 0x17
        'U'  -> MOD_LEFT_SHIFT to 0x18; 'V'  -> MOD_LEFT_SHIFT to 0x19
        'W'  -> MOD_LEFT_SHIFT to 0x1A; 'X'  -> MOD_LEFT_SHIFT to 0x1B
        'Y'  -> MOD_LEFT_SHIFT to 0x1C; 'Z'  -> MOD_LEFT_SHIFT to 0x1D
        '1'  -> 0 to 0x1E; '2'  -> 0 to 0x1F; '3'  -> 0 to 0x20; '4'  -> 0 to 0x21
        '5'  -> 0 to 0x22; '6'  -> 0 to 0x23; '7'  -> 0 to 0x24; '8'  -> 0 to 0x25
        '9'  -> 0 to 0x26; '0'  -> 0 to 0x27
        '\n' -> 0 to 0x28; ' '  -> 0 to 0x2C
        '-'  -> 0 to 0x2D; '='  -> 0 to 0x2E
        '['  -> 0 to 0x2F; ']'  -> 0 to 0x30; '\\' -> 0 to 0x31
        ';'  -> 0 to 0x33; '\'' -> 0 to 0x34; '`'  -> 0 to 0x35
        ','  -> 0 to 0x36; '.'  -> 0 to 0x37; '/'  -> 0 to 0x38
        '!'  -> MOD_LEFT_SHIFT to 0x1E; '@'  -> MOD_LEFT_SHIFT to 0x1F
        '#'  -> MOD_LEFT_SHIFT to 0x20; '$'  -> MOD_LEFT_SHIFT to 0x21
        '%'  -> MOD_LEFT_SHIFT to 0x22; '^'  -> MOD_LEFT_SHIFT to 0x23
        '&'  -> MOD_LEFT_SHIFT to 0x24; '*'  -> MOD_LEFT_SHIFT to 0x25
        '('  -> MOD_LEFT_SHIFT to 0x26; ')'  -> MOD_LEFT_SHIFT to 0x27
        '_'  -> MOD_LEFT_SHIFT to 0x2D; '+'  -> MOD_LEFT_SHIFT to 0x2E
        '{'  -> MOD_LEFT_SHIFT to 0x2F; '}'  -> MOD_LEFT_SHIFT to 0x30
        '|'  -> MOD_LEFT_SHIFT to 0x31; ':'  -> MOD_LEFT_SHIFT to 0x33
        '"'  -> MOD_LEFT_SHIFT to 0x34; '<'  -> MOD_LEFT_SHIFT to 0x36
        '>'  -> MOD_LEFT_SHIFT to 0x37; '?'  -> MOD_LEFT_SHIFT to 0x38
        else -> 0 to 0x00
    }
}