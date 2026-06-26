package com.arena.hidcompatibilitytester

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * HOGP (HID over GATT Profile) peripheral implementation.
 *
 * This class turns the Android device into a BLE HID peripheral (mouse + keyboard).
 * It works on any Android 5.0+ device that supports BLE peripheral mode
 * (isMultipleAdvertisementSupported == true).
 *
 * GATT Service layout:
 *  ├─ Generic Access (0x1800)
 *  │   ├─ Device Name (0x2A00)
 *  │   └─ Appearance (0x2A01)  → HID Mouse = 0x03C2
 *  ├─ Device Information (0x180A)
 *  │   ├─ Manufacturer Name (0x2A29)
 *  │   ├─ Model Number (0x2A24)
 *  │   └─ PnP ID (0x2A50)
 *  ├─ Battery Service (0x180F)
 *  │   └─ Battery Level (0x2A19)  → 100%
 *  └─ HID Service (0x1812)
 *      ├─ HID Information (0x2A4A)
 *      ├─ Report Map (0x2A4B)      → descriptor bytes
 *      ├─ HID Control Point (0x2A4C)
 *      ├─ Protocol Mode (0x2A4E)   → Report Protocol
 *      ├─ Mouse Input Report (0x2A4D) + CCCD + Report Reference (mouse, ID 1)
 *      └─ Keyboard Input Report (0x2A4D) + CCCD + Report Reference (kb, ID 2)
 */
@SuppressLint("MissingPermission")
class BleHidManager(private val context: Context) {

    companion object {
        private const val TAG = "BleHidManager"

        // ── Standard GATT UUIDs ──────────────────────────────────────────
        private val UUID_GENERIC_ACCESS     = gattUuid(0x1800)
        private val UUID_DEVICE_INFORMATION = gattUuid(0x180A)
        private val UUID_BATTERY_SERVICE    = gattUuid(0x180F)
        private val UUID_HID_SERVICE        = gattUuid(0x1812)

        private val UUID_DEVICE_NAME        = gattUuid(0x2A00)
        private val UUID_APPEARANCE         = gattUuid(0x2A01)
        private val UUID_MANUFACTURER_NAME  = gattUuid(0x2A29)
        private val UUID_MODEL_NUMBER       = gattUuid(0x2A24)
        private val UUID_PNP_ID             = gattUuid(0x2A50)
        private val UUID_BATTERY_LEVEL      = gattUuid(0x2A19)
        private val UUID_HID_INFORMATION    = gattUuid(0x2A4A)
        private val UUID_REPORT_MAP         = gattUuid(0x2A4B)
        private val UUID_HID_CONTROL_POINT  = gattUuid(0x2A4C)
        private val UUID_PROTOCOL_MODE      = gattUuid(0x2A4E)
        private val UUID_REPORT             = gattUuid(0x2A4D)

        private val UUID_CCCD               = gattUuid(0x2902) // Client Characteristic Config
        private val UUID_REPORT_REFERENCE   = gattUuid(0x2908)

        // Appearance: Generic HID = 0x03C0, Mouse = 0x03C2
        private val APPEARANCE_MOUSE = byteArrayOf(0xC2.toByte(), 0x03)

        // HID Information: bcdHID=1.11, bCountryCode=0x00, Flags=0x02 (normally connectable)
        private val HID_INFORMATION = byteArrayOf(0x11, 0x01, 0x00, 0x02)

        // Protocol Mode: Report Protocol (0x01)
        private val PROTOCOL_MODE_REPORT = byteArrayOf(0x01)

        /**
         * Combined Mouse + Keyboard HID Report Descriptor
         *
         * Report ID 1 → Mouse  (3 bytes: buttons | X | Y)
         * Report ID 2 → Keyboard (8 bytes: modifiers | reserved | key[0..5])
         */
        val REPORT_MAP = byteArrayOf(
            // ── Mouse (Report ID 1) ──────────────────────────────────────
            0x05, 0x01,        // Usage Page (Generic Desktop)
            0x09, 0x02,        // Usage (Mouse)
            0xA1.toByte(), 0x01, // Collection (Application)
            0x85.toByte(), 0x01, //   Report ID (1)
            0x09, 0x01,        //   Usage (Pointer)
            0xA1.toByte(), 0x00, //   Collection (Physical)
            // Buttons 1-3
            0x05, 0x09,        //     Usage Page (Button)
            0x19, 0x01,        //     Usage Minimum (1)
            0x29, 0x03,        //     Usage Maximum (3)
            0x15, 0x00,        //     Logical Minimum (0)
            0x25, 0x01,        //     Logical Maximum (1)
            0x95.toByte(), 0x03, //     Report Count (3)
            0x75, 0x01,        //     Report Size (1)
            0x81.toByte(), 0x02, //     Input (Data, Variable, Absolute)
            // Padding
            0x95.toByte(), 0x01, //     Report Count (1)
            0x75, 0x05,        //     Report Size (5)
            0x81.toByte(), 0x03, //     Input (Constant)
            // X, Y
            0x05, 0x01,        //     Usage Page (Generic Desktop)
            0x09, 0x30,        //     Usage (X)
            0x09, 0x31,        //     Usage (Y)
            0x15, 0x81.toByte(), //   Logical Minimum (-127)
            0x25, 0x7F,        //     Logical Maximum (127)
            0x75, 0x08,        //     Report Size (8)
            0x95.toByte(), 0x02, //     Report Count (2)
            0x81.toByte(), 0x06, //     Input (Data, Variable, Relative)
            0xC0.toByte(),     //   End Collection (Physical)
            0xC0.toByte(),     // End Collection (Application)

            // ── Keyboard (Report ID 2) ───────────────────────────────────
            0x05, 0x01,        // Usage Page (Generic Desktop)
            0x09, 0x06,        // Usage (Keyboard)
            0xA1.toByte(), 0x01, // Collection (Application)
            0x85.toByte(), 0x02, //   Report ID (2)
            // Modifier keys
            0x05, 0x07,        //   Usage Page (Key Codes)
            0x19, 0xE0.toByte(), // Usage Minimum (Left Ctrl)
            0x29, 0xE7.toByte(), // Usage Maximum (Right GUI)
            0x15, 0x00,        //   Logical Minimum (0)
            0x25, 0x01,        //   Logical Maximum (1)
            0x75, 0x01,        //   Report Size (1)
            0x95.toByte(), 0x08, //   Report Count (8)
            0x81.toByte(), 0x02, //   Input (Data, Variable, Absolute)
            // Reserved byte
            0x95.toByte(), 0x01, //   Report Count (1)
            0x75, 0x08,        //   Report Size (8)
            0x81.toByte(), 0x01, //   Input (Constant)
            // Key array (6 keys)
            0x95.toByte(), 0x06, //   Report Count (6)
            0x75, 0x08,        //   Report Size (8)
            0x15, 0x00,        //   Logical Minimum (0)
            0x25, 0x65,        //   Logical Maximum (101)
            0x05, 0x07,        //   Usage Page (Key Codes)
            0x19, 0x00,        //   Usage Minimum (0)
            0x29, 0x65,        //   Usage Maximum (101)
            0x81.toByte(), 0x00, //   Input (Data, Array, Absolute)
            0xC0.toByte()      // End Collection
        )

        private fun gattUuid(short: Int) =
            java.util.UUID.fromString("0000%04x-0000-1000-8000-00805f9b34fb".format(short))
    }

    // ── State ────────────────────────────────────────────────────────────────
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter

    private var gattServer: BluetoothGattServer? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var isAdvertising = false

    // Characteristics we need to hold refs to for sending notifications
    private var mouseInputChar: BluetoothGattCharacteristic? = null
    private var keyboardInputChar: BluetoothGattCharacteristic? = null

    // Connected central devices (hosts that subscribed to our reports)
    private val subscribedDevices = mutableSetOf<BluetoothDevice>()

    // Public state callbacks
    var onStateChanged: ((BleHidState) -> Unit)? = null
    var onDeviceConnected: ((BluetoothDevice) -> Unit)? = null
    var onDeviceDisconnected: ((BluetoothDevice) -> Unit)? = null

    private val originalName: String? = adapter?.name

    private val mainHandler = Handler(Looper.getMainLooper())

    // ✅ CORRECT — Explicitly declare the type as the sealed class
    private var currentState: BleHidState = BleHidState.IDLE
        set(value) {
            field = value
            mainHandler.post { onStateChanged?.invoke(value) }
        }

    // ── Public API ───────────────────────────────────────────────────────────

    fun isSupported(): Boolean =
        adapter != null &&
                adapter.isEnabled &&
                adapter.isMultipleAdvertisementSupported

    fun getConnectedDevices(): List<BluetoothDevice> = subscribedDevices.toList()

    /**
     * Start the GATT server and begin advertising as a BLE HID peripheral.
     */
    fun start() {
        if (!isSupported()) {
            currentState = BleHidState.ERROR("BLE peripheral not supported on this device")
            return
        }
        if (currentState is BleHidState.ADVERTISING || currentState is BleHidState.CONNECTED) {
            Log.w(TAG, "Already running")
            return
        }

        // Set adapter name so hosts see "HID Clone"
        try {
            adapter?.name = "HID Clone"
        } catch (e: Exception) {
            Log.w(TAG, "Could not set adapter name: ${e.message}")
        }

        currentState = BleHidState.STARTING
        openGattServer()
    }

    /**
     * Stop advertising and close the GATT server.
     */
    fun stop() {
        // Restore original adapter name
        try {
            adapter?.name = originalName
        } catch (e: Exception) {
            Log.w(TAG, "Could not restore adapter name: ${e.message}")
        }

        stopAdvertising()
        gattServer?.close()
        gattServer = null
        subscribedDevices.clear()
        mouseInputChar = null
        keyboardInputChar = null
        currentState = BleHidState.IDLE
        Log.d(TAG, "BLE HID stopped")
    }

    /**
     * Send a mouse movement/click report.
     * @param dx      X delta (-127..127)
     * @param dy      Y delta (-127..127)
     * @param buttons Bitmask: bit0=left, bit1=right, bit2=middle
     */
    fun sendMouseReport(dx: Int, dy: Int, buttons: Int = 0): Boolean {
        val char = mouseInputChar ?: return false
        if (subscribedDevices.isEmpty()) return false

        val report = byteArrayOf(
            buttons.and(0x07).toByte(),
            dx.coerceIn(-127, 127).toByte(),
            dy.coerceIn(-127, 127).toByte()
        )
        return sendInputReport(char, report)
    }

    /**
     * Send a keyboard report.
     * @param modifiers  Modifier bitmask (Ctrl=0x01, Shift=0x02, Alt=0x04, GUI=0x08, etc.)
     * @param keyCodes   Up to 6 HID key codes (USB HID Usage Table page 0x07)
     */
    fun sendKeyboardReport(modifiers: Int = 0, keyCodes: List<Int> = emptyList()): Boolean {
        val char = keyboardInputChar ?: return false
        if (subscribedDevices.isEmpty()) return false

        val report = ByteArray(8)
        report[0] = modifiers.toByte()
        report[1] = 0x00 // reserved
        keyCodes.take(6).forEachIndexed { i, code -> report[2 + i] = code.toByte() }
        return sendInputReport(char, report)
    }

    /** Release all keys (send empty keyboard report). */
    fun releaseKeys(): Boolean = sendKeyboardReport()

    // ── GATT Server Setup ────────────────────────────────────────────────────

    private fun openGattServer() {
        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
        if (gattServer == null) {
            currentState = BleHidState.ERROR("Failed to open GATT server")
            return
        }

        addGenericAccessService()
        // Small delay between service additions to avoid race conditions
        mainHandler.postDelayed({ addDeviceInformationService() }, 100)
        mainHandler.postDelayed({ addBatteryService() }, 200)
        mainHandler.postDelayed({ addHidService() }, 300)
        // Start advertising after services are added
        mainHandler.postDelayed({ startAdvertising() }, 600)
    }

    // ── Generic Access Service (0x1800) ──────────────────────────────────────
    private fun addGenericAccessService() {
        val service = BluetoothGattService(
            UUID_GENERIC_ACCESS,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        service.addCharacteristic(
            readChar(UUID_DEVICE_NAME, "HID Clone".toByteArray())
        )
        service.addCharacteristic(
            readChar(UUID_APPEARANCE, APPEARANCE_MOUSE)
        )
        gattServer?.addService(service)
    }

    // ── Device Information Service (0x180A) ──────────────────────────────────
    private fun addDeviceInformationService() {
        val service = BluetoothGattService(
            UUID_DEVICE_INFORMATION,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        service.addCharacteristic(readChar(UUID_MANUFACTURER_NAME, "Arena".toByteArray()))
        service.addCharacteristic(readChar(UUID_MODEL_NUMBER, "HIDClone-1".toByteArray()))
        // PnP ID: Vendor ID Source=0x02 (USB), VID=0x046D (Logitech), PID=0xC52B, Version=0x0111
        service.addCharacteristic(
            readChar(UUID_PNP_ID,
                byteArrayOf(0x02, 0x6D, 0x04, 0x2B, 0xC5.toByte(), 0x11, 0x01))
        )
        gattServer?.addService(service)
    }

    // ── Battery Service (0x180F) ─────────────────────────────────────────────
    private fun addBatteryService() {
        val service = BluetoothGattService(
            UUID_BATTERY_SERVICE,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        val battChar = BluetoothGattCharacteristic(
            UUID_BATTERY_LEVEL,
            BluetoothGattCharacteristic.PROPERTY_READ or
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM
        )
        battChar.value = byteArrayOf(100) // 100%
        battChar.addDescriptor(cccd())
        service.addCharacteristic(battChar)
        gattServer?.addService(service)
    }

    // ── HID Service (0x1812) ─────────────────────────────────────────────────
    private fun addHidService() {
        val service = BluetoothGattService(
            UUID_HID_SERVICE,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        // HID Information (read only)
        service.addCharacteristic(
            readChar(UUID_HID_INFORMATION, HID_INFORMATION,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM)
        )

        // Report Map (read only)
        service.addCharacteristic(
            readChar(UUID_REPORT_MAP, REPORT_MAP,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM)
        )

        // HID Control Point (write without response)
        val ctrlPoint = BluetoothGattCharacteristic(
            UUID_HID_CONTROL_POINT,
            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM
        )
        service.addCharacteristic(ctrlPoint)

        // Protocol Mode (read + write without response)
        val protoMode = BluetoothGattCharacteristic(
            UUID_PROTOCOL_MODE,
            BluetoothGattCharacteristic.PROPERTY_READ or
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM or
                    BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM
        )
        protoMode.value = PROTOCOL_MODE_REPORT
        service.addCharacteristic(protoMode)

        // Mouse Input Report (Report ID 1)
        val mouseChar = inputReportChar(reportId = 1, reportType = 0x01)
        mouseInputChar = mouseChar
        service.addCharacteristic(mouseChar)

        // Keyboard Input Report (Report ID 2)
        val kbChar = inputReportChar(reportId = 2, reportType = 0x01)
        keyboardInputChar = kbChar
        service.addCharacteristic(kbChar)

        gattServer?.addService(service)
        Log.d(TAG, "HID Service added")
    }

    // ── Characteristic factory helpers ────────────────────────────────────────

    private fun readChar(
        uuid: java.util.UUID,
        value: ByteArray,
        permissions: Int = BluetoothGattCharacteristic.PERMISSION_READ
    ): BluetoothGattCharacteristic {
        val char = BluetoothGattCharacteristic(
            uuid,
            BluetoothGattCharacteristic.PROPERTY_READ,
            permissions
        )
        char.value = value
        return char
    }

    /**
     * Creates an Input Report characteristic with CCCD (for notifications)
     * and a Report Reference descriptor.
     *
     * @param reportId   HID Report ID (1 = mouse, 2 = keyboard)
     * @param reportType 0x01 = Input, 0x02 = Output, 0x03 = Feature
     */
    private fun inputReportChar(reportId: Int, reportType: Int): BluetoothGattCharacteristic {
        val char = BluetoothGattCharacteristic(
            UUID_REPORT,
            BluetoothGattCharacteristic.PROPERTY_READ or
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                    BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM or
                    BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM
        )

        // CCCD — host writes 0x0001 to enable notifications
        char.addDescriptor(cccd())

        // Report Reference — tells host which report ID this characteristic maps to
        val reportRef = BluetoothGattDescriptor(
            UUID_REPORT_REFERENCE,
            BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM
        )
        reportRef.value = byteArrayOf(reportId.toByte(), reportType.toByte())
        char.addDescriptor(reportRef)

        return char
    }

    private fun cccd(): BluetoothGattDescriptor {
        val descriptor = BluetoothGattDescriptor(
            UUID_CCCD,
            BluetoothGattDescriptor.PERMISSION_READ or
                    BluetoothGattDescriptor.PERMISSION_WRITE
        )
        descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        return descriptor
    }

    // ── Advertising ──────────────────────────────────────────────────────────

    private fun startAdvertising() {
        advertiser = adapter?.bluetoothLeAdvertiser
        if (advertiser == null) {
            currentState = BleHidState.ERROR("BLE advertiser not available")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)           // advertise indefinitely
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .build()

        // Advertise the HID service UUID so hosts know what we are
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(android.os.ParcelUuid(UUID_HID_SERVICE))
            .build()

        advertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    private fun stopAdvertising() {
        if (isAdvertising) {
            advertiser?.stopAdvertising(advertiseCallback)
            isAdvertising = false
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            isAdvertising = true
            currentState = BleHidState.ADVERTISING
            Log.d(TAG, "BLE HID advertising started")
        }

        override fun onStartFailure(errorCode: Int) {
            val reason = when (errorCode) {
                ADVERTISE_FAILED_ALREADY_STARTED       -> "Already started"
                ADVERTISE_FAILED_DATA_TOO_LARGE        -> "Data too large"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED   -> "Feature unsupported"
                ADVERTISE_FAILED_INTERNAL_ERROR        -> "Internal error"
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS  -> "Too many advertisers"
                else -> "Unknown error $errorCode"
            }
            Log.e(TAG, "Advertising failed: $reason")
            currentState = BleHidState.ERROR("Advertising failed: $reason")
        }
    }

    // ── GATT Server Callbacks ────────────────────────────────────────────────

    private val gattServerCallback = object : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Central connected: ${device.address}")
                    // Don't add to subscribed yet — wait for CCCD write
                    mainHandler.post { onDeviceConnected?.invoke(device) }
                    // Update state only if not already connected to others
                    if (currentState is BleHidState.ADVERTISING) {
                        currentState = BleHidState.CONNECTED
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Central disconnected: ${device.address}")
                    subscribedDevices.remove(device)
                    mainHandler.post { onDeviceDisconnected?.invoke(device) }
                    if (subscribedDevices.isEmpty()) {
                        currentState = if (isAdvertising) BleHidState.ADVERTISING
                        else BleHidState.IDLE
                    }
                }
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice, requestId: Int, offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            Log.d(TAG, "Read request: ${characteristic.uuid}")
            gattServer?.sendResponse(
                device, requestId,
                BluetoothGatt.GATT_SUCCESS,
                offset,
                characteristic.value?.copyOfRange(offset, characteristic.value.size)
            )
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice, requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean, responseNeeded: Boolean,
            offset: Int, value: ByteArray?
        ) {
            characteristic.value = value
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            }
        }

        override fun onDescriptorReadRequest(
            device: BluetoothDevice, requestId: Int, offset: Int,
            descriptor: BluetoothGattDescriptor
        ) {
            gattServer?.sendResponse(
                device, requestId,
                BluetoothGatt.GATT_SUCCESS,
                offset,
                descriptor.value
            )
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
                    Log.d(TAG, "Notifications ENABLED by ${device.address} for ${descriptor.characteristic.uuid}")
                    subscribedDevices.add(device)
                } else {
                    Log.d(TAG, "Notifications DISABLED by ${device.address}")
                    // Only remove if no other characteristics are subscribed
                    // (simplification: remove on any disable)
                    subscribedDevices.remove(device)
                }
            }

            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            }
        }

        override fun onServiceAdded(status: Int, service: BluetoothGattService) {
            Log.d(TAG, "Service added: ${service.uuid}, status=$status")
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            // Called after sendNotification completes
        }

        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
            Log.d(TAG, "MTU changed to $mtu for ${device.address}")
        }
    }

    // ── Report sending ────────────────────────────────────────────────────────

    private fun sendInputReport(
        characteristic: BluetoothGattCharacteristic,
        report: ByteArray
    ): Boolean {
        if (gattServer == null) return false
        var success = false
        val targets = subscribedDevices.toList()
        for (device in targets) {
            characteristic.value = report
            val notified = gattServer!!.notifyCharacteristicChanged(device, characteristic, false)
            if (notified) success = true
        }
        return success
    }
}

// ── State sealed class ────────────────────────────────────────────────────────

sealed class BleHidState {
    object IDLE        : BleHidState()
    object STARTING    : BleHidState()
    object ADVERTISING : BleHidState()
    object CONNECTED   : BleHidState()
    data class ERROR(val message: String) : BleHidState()
}