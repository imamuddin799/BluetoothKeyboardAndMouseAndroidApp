package com.arena.hidcompatibilitytester.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Build
import android.util.Log
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.Executors

@SuppressLint("MissingPermission")
class BluetoothDeviceManager(
    private val context: Context,
    private val onDiscoveryResult: (BluetoothDevice) -> Unit
) {
    private val TAG = "BT_DeviceManager"

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    // ── Two separate proxies: one for each role ─────────────────────────────
    private var hidDeviceProxy: BluetoothHidDevice? = null       // us acting as mouse/keyboard
    private var hidHostProxyRaw: BluetoothProfile? = null        // us acting as a host (receives HID)

    private val HID_DEVICE_PROFILE_ID = 4  // BluetoothProfile.HID_DEVICE  (API 28)
    private val HID_HOST_PROFILE_ID   = 3  // BluetoothProfile.HID_HOST

    // App‑registration state – this is the REAL indicator that HID device works
    var isAppRegistered = false
        private set

    private var isDiscoveryReceiverRegistered = false
    private var stateListener: BluetoothStateListener? = null

    var onScanFinished: (() -> Unit)? = null
    var onLocationServicesRequired: (() -> Unit)? = null
    var onHidAppRegistered: ((Boolean) -> Unit)? = null   // fires when app reg completes

    // ── Mouse HID report descriptor (3-button relative mouse) ───────────────
    private val MOUSE_REPORT_DESCRIPTOR = byteArrayOf(
        0x05.toByte(), 0x01.toByte(),  // Usage Page (Generic Desktop)
        0x09.toByte(), 0x02.toByte(),  // Usage (Mouse)
        0xa1.toByte(), 0x01.toByte(),  // Collection (Application)
        0x09.toByte(), 0x01.toByte(),  //   Usage (Pointer)
        0xa1.toByte(), 0x00.toByte(),  //   Collection (Physical)
        // Buttons 1–3
        0x05.toByte(), 0x09.toByte(),  //     Usage Page (Button)
        0x19.toByte(), 0x01.toByte(),  //     Usage Minimum (1)
        0x29.toByte(), 0x03.toByte(),  //     Usage Maximum (3)
        0x15.toByte(), 0x00.toByte(),  //     Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(),  //     Logical Maximum (1)
        0x95.toByte(), 0x03.toByte(),  //     Report Count (3)
        0x75.toByte(), 0x01.toByte(),  //     Report Size (1)
        0x81.toByte(), 0x02.toByte(),  //     Input (Data, Variable, Absolute)
        // Padding (5 bits)
        0x95.toByte(), 0x01.toByte(),  //     Report Count (1)
        0x75.toByte(), 0x05.toByte(),  //     Report Size (5)
        0x81.toByte(), 0x03.toByte(),  //     Input (Constant)
        // X, Y relative axes
        0x05.toByte(), 0x01.toByte(),  //     Usage Page (Generic Desktop)
        0x09.toByte(), 0x30.toByte(),  //     Usage (X)
        0x09.toByte(), 0x31.toByte(),  //     Usage (Y)
        0x15.toByte(), 0x81.toByte(),  //     Logical Minimum (-127)
        0x25.toByte(), 0x7f.toByte(),  //     Logical Maximum (127)
        0x75.toByte(), 0x08.toByte(),  //     Report Size (8)
        0x95.toByte(), 0x02.toByte(),  //     Report Count (2)
        0x81.toByte(), 0x06.toByte(),  //     Input (Data, Variable, Relative)
        0xc0.toByte(),                  //   End Collection (Physical)
        0xc0.toByte()                   // End Collection (Application)
    )

    // ── HID Support data class ──────────────────────────────────────────────
    /**
     * [hidDeviceFunctional] = true only when:
     *   1. HID_DEVICE proxy connected, AND
     *   2. App successfully registered (isAppRegistered == true)
     * This eliminates the false-positive from getProfileProxy returning true.
     */
    data class HidSupportStatus(
        val hidHostSupported: Boolean,
        val hidDeviceProxyConnected: Boolean,   // proxy bound (may still be false-positive)
        val hidDeviceFunctional: Boolean,        // app registered = actually works
        val blePeripheralSupported: Boolean,
        val bluetoothEnabled: Boolean
    )

    interface BluetoothStateListener {
        fun onBondStateChanged(device: BluetoothDevice, state: Int)
        fun onConnectionStateChanged(device: BluetoothDevice, state: Int)
    }

    // ── Init ────────────────────────────────────────────────────────────────
    init {
        bindHidDeviceProxy()
        bindHidHostProxy()
    }

    // ── HID Device proxy (us as peripheral) ────────────────────────────────
    private fun bindHidDeviceProxy() {
        if (bluetoothAdapter == null) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Log.w(TAG, "HID_DEVICE requires Android 9+")
            return
        }
        try {
            bluetoothAdapter.getProfileProxy(
                context,
                object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                        if (profile == HID_DEVICE_PROFILE_ID && proxy is BluetoothHidDevice) {
                            hidDeviceProxy = proxy
                            Log.d(TAG, "HID_DEVICE proxy bound — registering app")
                            registerHidApp()
                        }
                    }
                    override fun onServiceDisconnected(profile: Int) {
                        if (profile == HID_DEVICE_PROFILE_ID) {
                            hidDeviceProxy = null
                            isAppRegistered = false
                            Log.d(TAG, "HID_DEVICE proxy disconnected")
                        }
                    }
                },
                HID_DEVICE_PROFILE_ID
            )
        } catch (e: Exception) {
            Log.e(TAG, "bindHidDeviceProxy failed: ${e.message}")
        }
    }

    // ── HID Host proxy (us as host — receives HID input) ───────────────────
    private fun bindHidHostProxy() {
        if (bluetoothAdapter == null) return
        try {
            bluetoothAdapter.getProfileProxy(
                context,
                object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                        if (profile == HID_HOST_PROFILE_ID) {
                            hidHostProxyRaw = proxy
                            Log.d(TAG, "HID_HOST proxy bound")
                        }
                    }
                    override fun onServiceDisconnected(profile: Int) {
                        if (profile == HID_HOST_PROFILE_ID) hidHostProxyRaw = null
                    }
                },
                HID_HOST_PROFILE_ID
            )
        } catch (e: Exception) {
            Log.e(TAG, "bindHidHostProxy failed: ${e.message}")
        }
    }

    // ── Register the HID app (the real gate for functional HID device) ──────
    fun registerHidApp(): Boolean {
        val proxy = hidDeviceProxy ?: run {
            Log.w(TAG, "registerHidApp: proxy not ready")
            return false
        }
        if (isAppRegistered) return true

        return try {
            val sdp = BluetoothHidDeviceAppSdpSettings(
                "HID Mouse",
                "Android Mouse Emulator",
                "Arena",
                BluetoothHidDevice.SUBCLASS1_MOUSE,   // 0x02 — correct mouse subclass
                MOUSE_REPORT_DESCRIPTOR
            )

            val callback = object : BluetoothHidDevice.Callback() {
                override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
                    isAppRegistered = registered
                    Log.d(TAG, "HID App status changed: registered=$registered device=$pluggedDevice")
                    onHidAppRegistered?.invoke(registered)
                }
                override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
                    Log.d(TAG, "HID connection state: $state for ${device?.address}")
                    device?.let { stateListener?.onConnectionStateChanged(it, state) }
                }
                override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
                    // Respond with an empty mouse report so the host doesn't stall
                    proxy.replyReport(device, type, id, byteArrayOf(0, 0, 0))
                }
                override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {}
                override fun onSetProtocol(device: BluetoothDevice?, protocol: Byte) {}
                override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {}
                override fun onVirtualCableUnplug(device: BluetoothDevice?) {}
            }

            val executor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.mainExecutor
            } else {
                Executors.newSingleThreadExecutor()
            }

            val result = proxy.registerApp(sdp, null, null, executor, callback)
            Log.d(TAG, "registerApp() returned: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "registerHidApp failed: ${e.message}")
            false
        }
    }

    // ── Send mouse report ───────────────────────────────────────────────────
    /**
     * Sends a single mouse movement/click report.
     * @param dx  X delta  (-127..127)
     * @param dy  Y delta  (-127..127)
     * @param buttons  bitmask: bit0=left, bit1=right, bit2=middle
     */
    fun sendMouseReport(device: BluetoothDevice, dx: Int, dy: Int, buttons: Int = 0): Boolean {
        val proxy = hidDeviceProxy ?: return false
        if (!isAppRegistered) {
            Log.w(TAG, "sendMouseReport: app not registered yet")
            return false
        }
        return try {
            val report = byteArrayOf(
                buttons.toByte(),
                dx.coerceIn(-127, 127).toByte(),
                dy.coerceIn(-127, 127).toByte()
            )
            proxy.sendReport(device, 0, report)
        } catch (e: Exception) {
            Log.e(TAG, "sendMouseReport failed: ${e.message}")
            false
        }
    }

    // ── HID Support Check ───────────────────────────────────────────────────
    /**
     * Returns a [HidSupportStatus] reflecting ACTUAL capability.
     *
     * [hidDeviceFunctional] == true only when the app is registered.
     * [hidDeviceProxyConnected] == true when the proxy bound (old false-positive value).
     * [hidHostSupported] == true when the HID_HOST proxy bound.
     *
     * This runs synchronously — call from a background thread.
     */
    fun checkHidSupport(): HidSupportStatus {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            return HidSupportStatus(
                hidHostSupported = false,
                hidDeviceProxyConnected = false,
                hidDeviceFunctional = false,
                blePeripheralSupported = false,
                bluetoothEnabled = adapter?.isEnabled ?: false
            )
        }

        val blePeripheral = adapter.isMultipleAdvertisementSupported

        val hidHostSupported = hidHostProxyRaw != null

        val hidDeviceProxyConnected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            hidDeviceProxy != null
        } else false

        // Ground truth: were we able to actually register the HID app?
        val hidDeviceFunctional = isAppRegistered

        return HidSupportStatus(
            hidHostSupported = hidHostSupported,
            hidDeviceProxyConnected = hidDeviceProxyConnected,
            hidDeviceFunctional = hidDeviceFunctional,
            blePeripheralSupported = blePeripheral,
            bluetoothEnabled = true
        )
    }

    // ── Location Services ───────────────────────────────────────────────────
    fun isLocationServicesEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm.isLocationEnabled
        } else {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    // ── Receiver registration helper ────────────────────────────────────────
    private fun registerReceiverCompat(
        receiver: BroadcastReceiver,
        filter: IntentFilter,
        exported: Boolean = true
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val flag = if (exported) Context.RECEIVER_EXPORTED else Context.RECEIVER_NOT_EXPORTED
            context.registerReceiver(receiver, filter, flag)
        } else {
            context.registerReceiver(receiver, filter)
        }
    }

    // ── Device list helpers ─────────────────────────────────────────────────
    fun getPairedDevices(): List<BluetoothDevice> =
        bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()

    fun getConnectedDevices(): List<BluetoothDevice> {
        val proxy = hidDeviceProxy ?: return emptyList()
        return try {
            proxy.getDevicesMatchingConnectionStates(
                intArrayOf(BluetoothProfile.STATE_CONNECTED, BluetoothProfile.STATE_CONNECTING)
            )
        } catch (e: Exception) { emptyList() }
    }

    fun pairDevice(device: BluetoothDevice): Boolean =
        if (device.bondState == BluetoothDevice.BOND_NONE) device.createBond() else true

    fun removePairedDevice(device: BluetoothDevice): Boolean {
        return try {
            device.javaClass.getMethod("removeBond").invoke(device) as Boolean
        } catch (e: Exception) { false }
    }

    // ── Connect / Disconnect ────────────────────────────────────────────────
    fun connectToDevice(device: BluetoothDevice): Boolean {
        val proxy = hidDeviceProxy ?: return false
        if (!isAppRegistered) {
            Log.w(TAG, "connectToDevice: app not registered — attempting registration first")
            registerHidApp()
            // Give the system a moment, then connect
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try { proxy.connect(device) } catch (e: Exception) {
                    Log.e(TAG, "connect (delayed) failed: ${e.message}")
                }
            }, 800)
            return true
        }
        return try {
            proxy.connect(device)
        } catch (e: Exception) {
            Log.e(TAG, "connectToDevice failed: ${e.message}")
            false
        }
    }

    fun disconnectFromDevice(device: BluetoothDevice): Boolean {
        val proxy = hidDeviceProxy ?: return false
        return try {
            proxy.disconnect(device)
        } catch (e: Exception) { false }
    }

    // ── State listener ──────────────────────────────────────────────────────
    fun registerStateListener(listener: BluetoothStateListener) {
        stateListener = listener
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction("android.bluetooth.hiddevice.profile.action.CONNECTION_STATE_CHANGED")
        }
        registerReceiverCompat(statusReceiver, filter, exported = true)
    }

    fun unregisterStateListener() {
        stateListener = null
        try { context.unregisterReceiver(statusReceiver) } catch (e: Exception) {}
    }

    // ── Broadcast Receivers ─────────────────────────────────────────────────
    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                    device?.let { onDiscoveryResult(it) }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    isDiscoveryReceiverRegistered = false
                    try { context?.unregisterReceiver(this) } catch (e: Exception) {}
                    onScanFinished?.invoke()
                }
            }
        }
    }

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val device: BluetoothDevice =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent?.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                } ?: return

            when (intent?.action) {
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val bondState = intent.getIntExtra(
                        BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                    stateListener?.onBondStateChanged(device, bondState)
                }
                "android.bluetooth.hiddevice.profile.action.CONNECTION_STATE_CHANGED" -> {
                    val connState = intent.getIntExtra(
                        BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                    stateListener?.onConnectionStateChanged(device, connState)
                }
            }
        }
    }

    // ── Scanning ────────────────────────────────────────────────────────────
    fun isCurrentlyScanning(): Boolean = bluetoothAdapter?.isDiscovering ?: false

    fun startNearbyScanning(): Boolean {
        if (bluetoothAdapter == null) return false

        if (!isLocationServicesEnabled()) {
            onLocationServicesRequired?.invoke()
            return false
        }

        if (!isDiscoveryReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            registerReceiverCompat(discoveryReceiver, filter, exported = true)
            isDiscoveryReceiverRegistered = true
        }

        if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()

        val started = bluetoothAdapter.startDiscovery()
        if (!started) {
            try { context.unregisterReceiver(discoveryReceiver) } catch (e: Exception) {}
            isDiscoveryReceiverRegistered = false
            onScanFinished?.invoke()
            return false
        }
        return true
    }

    fun stopNearbyScanning() {
        if (bluetoothAdapter?.isDiscovering == true) bluetoothAdapter.cancelDiscovery()
        if (isDiscoveryReceiverRegistered) {
            try { context.unregisterReceiver(discoveryReceiver) } catch (e: Exception) {}
            isDiscoveryReceiverRegistered = false
        }
    }
}


//package com.arena.hidcompatibilitytester
//
//import android.annotation.SuppressLint
//import android.bluetooth.BluetoothAdapter
//import android.bluetooth.BluetoothDevice
//import android.bluetooth.BluetoothManager
//import android.bluetooth.BluetoothProfile
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.util.Log
//import java.lang.reflect.InvocationHandler
//import java.lang.reflect.Method
//import java.lang.reflect.Proxy
//
//@SuppressLint("MissingPermission")
//class BluetoothDeviceManager(
//    private val context: Context,
//    private val onDiscoveryResult: (BluetoothDevice) -> Unit
//) {
//    private val TAG = "BT_DeviceManager"
//
//    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
//
//    private var hidDeviceProxy: BluetoothProfile? = null
//    private val HID_DEVICE_PROFILE_ID = 4
//    private var isAppRegistered = false
//
//    private var stateListener: BluetoothStateListener? = null
//
//    interface BluetoothStateListener {
//        fun onBondStateChanged(device: BluetoothDevice, state: Int)
//        fun onConnectionStateChanged(device: BluetoothDevice, state: Int)
//    }
//
//    private val MOUSE_REPORT_DESCRIPTOR = byteArrayOf(
//        0x05.toByte(), 0x01.toByte(), // USAGE_PAGE (Generic Desktop)
//        0x09.toByte(), 0x02.toByte(), // USAGE (Mouse)
//        0xa1.toByte(), 0x01.toByte(), // COLLECTION (Application)
//        0x09.toByte(), 0x01.toByte(), //   USAGE (Pointer)
//        0xa1.toByte(), 0x00.toByte(), //   COLLECTION (Physical)
//        0x05.toByte(), 0x09.toByte(), //     USAGE_PAGE (Button)
//        0x19.toByte(), 0x01.toByte(), //     USAGE_MINIMUM (Button 1)
//        0x29.toByte(), 0x03.toByte(), //     USAGE_MAXIMUM (Button 3)
//        0x15.toByte(), 0x00.toByte(), //     LOGICAL_MINIMUM (0)
//        0x25.toByte(), 0x01.toByte(), //     LOGICAL_MAXIMUM (1)
//        0x95.toByte(), 0x03.toByte(), //     REPORT_COUNT (3)
//        0x75.toByte(), 0x01.toByte(), //     REPORT_SIZE (1)
//        0x81.toByte(), 0x02.toByte(), //     INPUT (Data,Var,Abs)
//        0x95.toByte(), 0x01.toByte(), //     REPORT_COUNT (1)
//        0x75.toByte(), 0x05.toByte(), //     REPORT_SIZE (5)
//        0x81.toByte(), 0x03.toByte(), //     INPUT (Cnst,Var,Abs)
//        0x05.toByte(), 0x01.toByte(), //     USAGE_PAGE (Generic Desktop)
//        0x09.toByte(), 0x30.toByte(), //     USAGE (X)
//        0x09.toByte(), 0x31.toByte(), //     USAGE (Y)
//        0x15.toByte(), 0x81.toByte(), //     LOGICAL_MINIMUM (-127)
//        0x25.toByte(), 0x7f.toByte(), //     LOGICAL_MAXIMUM (127)
//        0x75.toByte(), 0x08.toByte(), //     REPORT_SIZE (8)
//        0x95.toByte(), 0x02.toByte(), //     REPORT_COUNT (2)
//        0x81.toByte(), 0x06.toByte(), //     INPUT (Data,Var,Rel)
//        0xc0.toByte(),                //   END_COLLECTION
//        0xc0.toByte()                 // END_COLLECTION
//    )
//
//    init {
//        initializeHidProxy()
//    }
//
//    private fun initializeHidProxy() {
//        if (bluetoothAdapter == null) return
//        try {
//            val getProfileProxyMethod = bluetoothAdapter.javaClass.getMethod(
//                "getProfileProxy",
//                Context::class.java,
//                BluetoothProfile.ServiceListener::class.java,
//                Int::class.javaPrimitiveType
//            )
//            getProfileProxyMethod.invoke(
//                bluetoothAdapter,
//                context,
//                object : BluetoothProfile.ServiceListener {
//                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
//                        if (profile == HID_DEVICE_PROFILE_ID) {
//                            hidDeviceProxy = proxy
//                            Log.d(TAG, "HID Device Proxy successfully bound.")
//                            registerHidAppConfig() // Auto-register profile parameters on load
//                        }
//                    }
//                    override fun onServiceDisconnected(profile: Int) {
//                        if (profile == HID_DEVICE_PROFILE_ID) {
//                            hidDeviceProxy = null
//                            isAppRegistered = false
//                        }
//                    }
//                },
//                HID_DEVICE_PROFILE_ID
//            )
//        } catch (e: Exception) {
//            Log.e(TAG, "Failed to bind HID Proxy via reflection: ${e.message}")
//        }
//    }
//
//    fun getPairedDevices(): List<BluetoothDevice> {
//        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
//    }
//
//    fun getConnectedDevices(): List<BluetoothDevice> {
//        val proxy = hidDeviceProxy ?: return emptyList()
//        return proxy.getDevicesMatchingConnectionStates(
//            intArrayOf(BluetoothProfile.STATE_CONNECTED, BluetoothProfile.STATE_CONNECTING)
//        )
//    }
//
//    fun pairDevice(device: BluetoothDevice): Boolean {
//        return if (device.bondState == BluetoothDevice.BOND_NONE) {
//            Log.d(TAG, "Initiating pairing sequence for: ${device.name}")
//            device.createBond()
//        } else true
//    }
//
//    fun removePairedDevice(device: BluetoothDevice): Boolean {
//        return try {
//            val removeBondMethod = device.javaClass.getMethod("removeBond")
//            removeBondMethod.invoke(device) as Boolean
//        } catch (e: Exception) {
//            false
//        }
//    }
//
//    fun registerHidAppConfig(): Boolean {
//        val proxy = hidDeviceProxy ?: return false
//        if (isAppRegistered) return true
//
//        return try {
//            val sdpSettingsClass = Class.forName("android.bluetooth.BluetoothHidDeviceAppSdpSettings")
//            val sdpConstructor = sdpSettingsClass.getConstructor(
//                String::class.java, String::class.java, String::class.java, Byte::class.javaPrimitiveType, ByteArray::class.java
//            )
//            val sdpSettingsInstance = sdpConstructor.newInstance(
//                "HID Clone Controller", "Android Mouse Emulator", "OpenSource", 0x00.toByte(), MOUSE_REPORT_DESCRIPTOR
//            )
//
//            val callbackClass = Class.forName("android.bluetooth.BluetoothHidDevice\$Callback")
//
//            // Generate a Dynamic Proxy to mimic the hidden Callback class structure natively
//            val callbackProxyInstance = Proxy.newProxyInstance(
//                context.classLoader,
//                arrayOf(callbackClass),
//                object : InvocationHandler {
//                    override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
//                        Log.d(TAG, "HID Framework Event Received: ${method?.name}")
//                        if (method?.name == "onAppStatusChanged") {
//                            isAppRegistered = true
//                            Log.d(TAG, "Application Profile Successfully Registered into Bluetooth OS Stack.")
//                        }
//                        return null
//                    }
//                }
//            )
//
//            val registerAppMethod = proxy.javaClass.getMethod(
//                "registerApp",
//                sdpSettingsClass,
//                Class.forName("android.bluetooth.BluetoothBluetoothDevice"), // Dummy representation placeholders
//                Class.forName("android.bluetooth.BluetoothBluetoothDevice"), // QoS token placeholders
//                java.util.concurrent.Executor::class.java,
//                callbackClass
//            )
//
//            // Dynamic safe platform invocation signature verification fallback
//            val fluidRegisterMethod = proxy.javaClass.methods.firstOrNull { it.name == "registerApp" }
//            fluidRegisterMethod?.invoke(
//                proxy,
//                sdpSettingsInstance,
//                null,
//                null,
//                context.mainExecutor,
//                callbackProxyInstance
//            )
//            true
//        } catch (e: Exception) {
//            Log.e(TAG, "Failed running strict configuration layer validation: ${e.message}")
//            false
//        }
//    }
//
//    fun connectToDevice(device: BluetoothDevice): Boolean {
//        val proxy = hidDeviceProxy ?: return false
//        registerHidAppConfig() // Ensure registration locks are loaded
//        return try {
//            val connectMethod = proxy.javaClass.getMethod("connect", BluetoothDevice::class.java)
//            connectMethod.invoke(proxy, device) as Boolean
//        } catch (e: Exception) {
//            false
//        }
//    }
//
//    fun disconnectFromDevice(device: BluetoothDevice): Boolean {
//        val proxy = hidDeviceProxy ?: return false
//        return try {
//            val disconnectMethod = proxy.javaClass.getMethod("disconnect", BluetoothDevice::class.java)
//            disconnectMethod.invoke(proxy, device) as Boolean
//        } catch (e: Exception) {
//            false
//        }
//    }
//
//    fun registerStateListener(listener: BluetoothStateListener) {
//        this.stateListener = listener
//        val filter = IntentFilter().apply {
//            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
//            addAction("android.bluetooth.hiddevice.profile.action.CONNECTION_STATE_CHANGED")
//        }
//        context.registerReceiver(statusReceiver, filter)
//    }
//
//    fun unregisterStateListener() {
//        this.stateListener = null
//        try { context.unregisterReceiver(statusReceiver) } catch (e: Exception) {}
//    }
//
//    private val discoveryReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent?) {
//            when (intent?.action) {
//                BluetoothDevice.ACTION_FOUND -> {
//                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
//                    device?.let { onDiscoveryResult(it) }
//                }
//            }
//        }
//    }
//
//    private val statusReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent?) {
//            val device: BluetoothDevice = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) ?: return
//            when (intent.action) {
//                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
//                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
//                    stateListener?.onBondStateChanged(device, bondState)}"android.bluetooth.hiddevice.profile.action.CONNECTION_STATE_CHANGED" -> {
//                        val connState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
//                stateListener?.onConnectionStateChanged(device, connState)
//                    }
//            }
//        }
//    }
//    fun isCurrentlyScanning(): Boolean {
//        return bluetoothAdapter?.isDiscovering ?: false
//    }
//    fun startNearbyScanning() {
//        if (bluetoothAdapter == null) return
//        if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
//        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
//        context.registerReceiver(discoveryReceiver, filter)
//        bluetoothAdapter.startDiscovery()
//    }
//    fun stopNearbyScanning() {
//        if (bluetoothAdapter?.isDiscovering == true) bluetoothAdapter.cancelDiscovery()
//        try {
//            context.unregisterReceiver(discoveryReceiver)
//        } catch (e: Exception) {
//
//        }
//    }
//}


//package com.arena.hidcompatibilitytester
//
//import android.annotation.SuppressLint
//import android.bluetooth.BluetoothAdapter
//import android.bluetooth.BluetoothDevice
//import android.bluetooth.BluetoothManager
//import android.bluetooth.BluetoothProfile
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.util.Log
//
//@SuppressLint("MissingPermission")
//class BluetoothDeviceManager(
//    private val context: Context,
//    private val onDiscoveryResult: (BluetoothDevice) -> Unit
//) {
//    private val TAG = "BT_DeviceManager"
//
//    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
//
//    private var hidDeviceProxy: BluetoothProfile? = null
//    private val HID_DEVICE_PROFILE_ID = 4 // Constant for BluetoothProfile.HID_DEVICE
//
//    init {
//        initializeHidProxy()
//    }
//
//    /**
//     * Bypasses OEM restrictions to securely fetch the active HID Device profile proxy
//     */
//    private fun initializeHidProxy() {
//        if (bluetoothAdapter == null) return
//        try {
//            val getProfileProxyMethod = bluetoothAdapter.javaClass.getMethod(
//                "getProfileProxy",
//                Context::class.java,
//                BluetoothProfile.ServiceListener::class.java,
//                Int::class.javaPrimitiveType
//            )
//            getProfileProxyMethod.invoke(
//                bluetoothAdapter,
//                context,
//                object : BluetoothProfile.ServiceListener {
//                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
//                        if (profile == HID_DEVICE_PROFILE_ID) {
//                            hidDeviceProxy = proxy
//                            Log.d(TAG, "HID Device Proxy successfully bound.")
//                        }
//                    }
//                    override fun onServiceDisconnected(profile: Int) {
//                        if (profile == HID_DEVICE_PROFILE_ID) hidDeviceProxy = null
//                    }
//                },
//                HID_DEVICE_PROFILE_ID
//            )
//        } catch (e: Exception) {
//            Log.e(TAG, "Failed to bind HID Proxy via reflection: ${e.message}")
//        }
//    }
//
//    /**
//     * 1. GET PAIRED DEVICES
//     */
//    fun getPairedDevices(): List<BluetoothDevice> {
//        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
//    }
//
//    /**
//     * 2. GET CONNECTED DEVICES
//     * Corrected method signature matching the official Android BluetoothProfile framework definition
//     */
//    fun getConnectedDevices(): List<BluetoothDevice> {
//        val proxy = hidDeviceProxy ?: return emptyList()
//        return proxy.getDevicesMatchingConnectionStates(
//            intArrayOf(
//                BluetoothProfile.STATE_CONNECTED,
//                BluetoothProfile.STATE_CONNECTING
//            )
//        )
//    }
//
//    /**
//     * 3. NEARBY DISCOVERY SCANNING
//     */
//    private val discoveryReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent?) {
//            when (intent?.action) {
//                BluetoothDevice.ACTION_FOUND -> {
//                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
//                    device?.let { onDiscoveryResult(it) }
//                }
//                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
//                    Log.d(TAG, "Nearby device discovery cycle completed.")
//                }
//            }
//        }
//    }
//
//    private var stateListener: BluetoothStateListener? = null
//
//    interface BluetoothStateListener {
//        fun onBondStateChanged(device: BluetoothDevice, state: Int)
//        fun onConnectionStateChanged(device: BluetoothDevice, state: Int)
//    }
//
//    fun registerStateListener(listener: BluetoothStateListener) {
//        this.stateListener = listener
//        val filter = IntentFilter().apply {
//            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
//            // Listen for internal Bluetooth HID profile changes
//            addAction("android.bluetooth.hiddevice.profile.action.CONNECTION_STATE_CHANGED")
//        }
//        context.registerReceiver(statusReceiver, filter)
//    }
//
//    fun unregisterStateListener() {
//        this.stateListener = null
//        try { context.unregisterReceiver(statusReceiver) } catch (e: Exception) {}
//    }
//
//    private val statusReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent?) {
//            val device: BluetoothDevice = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) ?: return
//
//            when (intent.action) {
//                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
//                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
//                    Log.d(TAG, "Bond state changed for ${device.address} -> Status: $bondState")
//                    stateListener?.onBondStateChanged(device, bondState)
//                }
//                "android.bluetooth.hiddevice.profile.action.CONNECTION_STATE_CHANGED" -> {
//                    val connState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
//                    Log.d(TAG, "Connection state changed for ${device.address} -> Status: $connState")
//                    stateListener?.onConnectionStateChanged(device, connState)
//                }
//            }
//        }
//    }
//
//    fun startNearbyScanning() {
//        if (bluetoothAdapter == null) return
//        if (bluetoothAdapter.isDiscovering) {
//            bluetoothAdapter.cancelDiscovery()
//        }
//
//        val filter = IntentFilter().apply {
//            addAction(BluetoothDevice.ACTION_FOUND)
//            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
//        }
//        context.registerReceiver(discoveryReceiver, filter)
//        bluetoothAdapter.startDiscovery()
//        Log.d(TAG, "Started scanning for nearby classic devices...")
//    }
//
//    fun stopNearbyScanning() {
//        if (bluetoothAdapter?.isDiscovering == true) {
//            bluetoothAdapter.cancelDiscovery()
//        }
//        try {
//            context.unregisterReceiver(discoveryReceiver)
//        } catch (e: Exception) {
//            // Receiver already unregistered
//        }
//    }
//
//    /**
//     * 4. PAIR A NEW DISCOVERED DEVICE
//     */
//    fun pairDevice(device: BluetoothDevice): Boolean {
//        return if (device.bondState == BluetoothDevice.BOND_NONE) {
//            Log.d(TAG, "Initiating pairing sequence for: ${device.name} [${device.address}]")
//            device.createBond() // Standard Android public method to pair
//        } else {
//            Log.d(TAG, "Device is already paired or pairing.")
//            true
//        }
//    }
//
//    /**
//     * 5. REMOVE / UNPAIR A DEVICE
//     * Note: Android hides 'removeBond()' from the public SDK, so we execute via reflection
//     */
//    fun removePairedDevice(device: BluetoothDevice): Boolean {
//        return try {
//            Log.d(TAG, "Removing pairing/bond for: ${device.address}")
//            val removeBondMethod = device.javaClass.getMethod("removeBond")
//            val result = removeBondMethod.invoke(device) as Boolean
//            result
//        } catch (e: Exception) {
//            Log.e(TAG, "Failed to remove bond via reflection: ${e.message}")
//            false
//        }
//    }
//
//    /**
//     * 6. CONNECT HID_DEVICE TO A HOST
//     * Note: 'connect()' on the HID proxy is a hidden system API. We invoke it via reflection
//     */
//    fun connectToDevice(device: BluetoothDevice): Boolean {
//        val proxy = hidDeviceProxy
//        if (proxy == null) {
//            Log.e(TAG, "Cannot connect: HID Device Proxy is not ready yet.")
//            return false
//        }
//        return try {
//            Log.d(TAG, "Attempting connection to remote host: ${device.address}")
//            // Fetch the hidden connect method on the BluetoothHidDevice class
//            val connectMethod = proxy.javaClass.getMethod("connect", BluetoothDevice::class.java)
//            val result = connectMethod.invoke(proxy, device) as Boolean
//            result
//        } catch (e: Exception) {
//            Log.e(TAG, "Reflection connection execution failed: ${e.message}")
//            false
//        }
//    }
//
//    /**
//     * 7. DISCONNECT HID_DEVICE FROM A HOST
//     * Note: 'disconnect()' is also hidden behind internal system permissions
//     */
//    fun disconnectFromDevice(device: BluetoothDevice): Boolean {
//        val proxy = hidDeviceProxy
//        if (proxy == null) return false
//        return try {
//            Log.d(TAG, "Disconnecting from host: ${device.address}")
//            val disconnectMethod = proxy.javaClass.getMethod("disconnect", BluetoothDevice::class.java)
//            val result = disconnectMethod.invoke(proxy, device) as Boolean
//            result
//        } catch (e: Exception) {
//            Log.e(TAG, "Reflection disconnect execution failed: ${e.message}")
//            false
//        }
//    }
//
//    // --- ADD THE HID DESCRIPTOR BYTE ARRAY CONSTANT ---
//    private val MOUSE_REPORT_DESCRIPTOR = byteArrayOf(
//        0x05.toByte(), 0x01.toByte(), // USAGE_PAGE (Generic Desktop)
//        0x09.toByte(), 0x02.toByte(), // USAGE (Mouse)
//        0xa1.toByte(), 0x01.toByte(), // COLLECTION (Application)
//        0x09.toByte(), 0x01.toByte(), //   USAGE (Pointer)
//        0xa1.toByte(), 0x00.toByte(), //   COLLECTION (Physical)
//        0x05.toByte(), 0x09.toByte(), //     USAGE_PAGE (Button)
//        0x19.toByte(), 0x01.toByte(), //     USAGE_MINIMUM (Button 1)
//        0x29.toByte(), 0x03.toByte(), //     USAGE_MAXIMUM (Button 3)
//        0x15.toByte(), 0x00.toByte(), //     LOGICAL_MINIMUM (0)
//        0x25.toByte(), 0x01.toByte(), //     LOGICAL_MAXIMUM (1)
//        0x95.toByte(), 0x03.toByte(), //     REPORT_COUNT (3)
//        0x75.toByte(), 0x01.toByte(), //     REPORT_SIZE (1)
//        0x81.toByte(), 0x02.toByte(), //     INPUT (Data,Var,Abs)
//        0x95.toByte(), 0x01.toByte(), //     REPORT_COUNT (1)
//        0x75.toByte(), 0x05.toByte(), //     REPORT_SIZE (5)
//        0x81.toByte(), 0x03.toByte(), //     INPUT (Cnst,Var,Abs)
//        0x05.toByte(), 0x01.toByte(), //     USAGE_PAGE (Generic Desktop)
//        0x09.toByte(), 0x30.toByte(), //     USAGE (X)
//        0x09.toByte(), 0x31.toByte(), //     USAGE (Y)
//        0x15.toByte(), 0x81.toByte(), //     LOGICAL_MINIMUM (-127)
//        0x25.toByte(), 0x7f.toByte(), //     LOGICAL_MAXIMUM (127)
//        0x75.toByte(), 0x08.toByte(), //     REPORT_SIZE (8)
//        0x95.toByte(), 0x02.toByte(), //     REPORT_COUNT (2)
//        0x81.toByte(), 0x06.toByte(), //     INPUT (Data,Var,Rel)
//        0xc0.toByte(),                //   END_COLLECTION
//        0xc0.toByte()                 // END_COLLECTION
//    )
//
//    /**
//     * REGISTERS PHONE AS A REAL HID MOUSE PERIPHERAL
//     * This stops the laptop from automatically disconnecting!
//     */
//    fun registerHidAppConfig(): Boolean {
//        val proxy = hidDeviceProxy ?: return false
//        return try {
//            Log.d(TAG, "Registering custom HID App Descriptor Configuration metadata...")
//
//            // 1. Create SDP Settings Object via Reflection
//            val sdpSettingsClass = Class.forName("android.bluetooth.BluetoothHidDeviceAppSdpSettings")
//            val sdpConstructor = sdpSettingsClass.getConstructor(
//                String::class.java, String::class.java, String::class.java, Byte::class.javaPrimitiveType, ByteArray::class.java
//            )
//            val sdpSettingsInstance = sdpConstructor.newInstance(
//                "HID Clone Controller", "Android Mouse Emulator", "OpenSource", 0x00.toByte(), MOUSE_REPORT_DESCRIPTOR
//            )
//
//            // 2. Locate and invoke the internal registerApp function
//            val registerAppMethod = proxy.javaClass.getMethod(
//                "registerApp",
//                sdpSettingsClass,
//                BluetoothProfile::class.java, // Qos token (can pass null)
//                BluetoothProfile::class.java, // Qos token (can pass null)
//                java.util.concurrent.Executor::class.java,
//                Class.forName("android.bluetooth.BluetoothHidDevice\$Callback") // Internal Hidden Callback context
//            )
//
//            // Create an empty dummy callback handler instance via Proxy wrapper if required,
//            // or pass null/stub if hidden implementation supports direct execution
//            Log.d(TAG, "Invoking internal hidden registration profile methods on current OEM stack.")
//            // On standard AOSP, this function signature expects an executor and an interface stub
//            true
//        } catch (e: Exception) {
//            Log.e(TAG, "Failed to register HID App Descriptor via reflection: ${e.message}")
//            false
//        }
//    }
//
//    /**
//     * HELPER STATUS CHECK TO DETECT IF THE ADAPTER IS BUSY
//     */
//    fun isCurrentlyScanning(): Boolean {
//        return bluetoothAdapter?.isDiscovering ?: false
//    }
//}
