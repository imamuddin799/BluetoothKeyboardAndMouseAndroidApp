package com.arena.hidcompatibilitytester

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log

class CompatibilityChecker(private val context: Context) {

    private val TAG = "HID_Compatibility Test 9"
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothManager?.adapter
    }

    // Profile constants defined by Android framework
    private val PROFILE_HID_HOST = 3
    private val PROFILE_HID_DEVICE = 4

    interface AdvancedCompatibilityCallback {
        fun onCheckComplete(
            hidHostSupported: Boolean,
            hidDeviceSupported: Boolean,
            blePeripheralSupported: Boolean,
            detailedReport: String
        )
    }

    fun runComprehensiveCheck(callback: AdvancedCompatibilityCallback) {
        val report = StringBuilder()

        if (bluetoothAdapter == null) {
            callback.onCheckComplete(false, false, false, "Device does not support Bluetooth hardware.")
            return
        }

        if (!bluetoothAdapter!!.isEnabled) {
            callback.onCheckComplete(false, false, false, "Bluetooth is disabled. Please enable it to run tests.")
            return
        }

        report.append("--- HARDWARE & SYSTEM REPORT ---\n")
        report.append("OS Version: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")

        // 1. Check BLE Peripheral Capability (Critical for HOGP/BLE Emulation)
        val hasBleFeature = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE)
        val isBlePeripheralSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bluetoothAdapter!!.isMultipleAdvertisementSupported
        } else {
            false
        }
        report.append("BLE Feature Flag Present: $hasBleFeature\n")
        report.append("BLE Peripheral Mode (Multiple Advertisement): $isBlePeripheralSupported\n\n")

        report.append("--- PROFILE EVALUATIONS ---\n")

        // Variables to track statuses across async proxy bindings
        var hidHostResult = false
        var hidDeviceResult = false
        var checkedHost = false
        var checkedDevice = false

        fun checkCompletion() {
            if (checkedHost && checkedDevice) {
                callback.onCheckComplete(hidHostResult, hidDeviceResult, isBlePeripheralSupported, report.toString())
            }
        }

        // ==========================================
        // TEST A: EVALUATING HID_HOST (Value: 3)
        // ==========================================
        try {
            val hostSuccess = bluetoothAdapter!!.getProfileProxy(
                context,
                object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                        if (profile == PROFILE_HID_HOST && proxy != null) {
                            hidHostResult = true
                            report.append("HID_HOST (Standard API Proxy): Supported\n")
                            bluetoothAdapter!!.closeProfileProxy(profile, proxy)
                        }
                        checkedHost = true
                        checkCompletion()
                    }
                    override fun onServiceDisconnected(profile: Int) {}
                },
                PROFILE_HID_HOST
            )

            if (!hostSuccess) {
                // Try Reflection for HID_HOST if Standard fails
                attemptReflectionForProfile(PROFILE_HID_HOST) { supported ->
                    hidHostResult = supported
                    report.append("HID_HOST (Reflection Bypass): ${if (supported) "Supported" else "Unsupported/Blocked"}\n")
                    checkedHost = true
                    checkCompletion()
                }
            }
        } catch (e: Exception) {
            attemptReflectionForProfile(PROFILE_HID_HOST) { supported ->
                hidHostResult = supported
                report.append("HID_HOST (Reflection Fallback due to exception): ${if (supported) "Supported" else "Unsupported"}\n")
                checkedHost = true
                checkCompletion()
            }
        }

        // ==========================================
        // TEST B: EVALUATING HID_DEVICE (Value: 4)
        // ==========================================
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            report.append("HID_DEVICE (Standard API Proxy): Unsupported (Requires Android 9+)\n")
            checkedDevice = true
            checkCompletion()
        } else {
            try {
                val deviceSuccess = bluetoothAdapter!!.getProfileProxy(
                    context,
                    object : BluetoothProfile.ServiceListener {
                        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                            if (profile == PROFILE_HID_DEVICE && proxy != null) {
                                hidDeviceResult = true
                                report.append("HID_DEVICE (Standard API Proxy): Supported\n")
                                bluetoothAdapter!!.closeProfileProxy(profile, proxy)
                            }
                            checkedDevice = true
                            checkCompletion()
                        }
                        override fun onServiceDisconnected(profile: Int) {}
                    },
                    PROFILE_HID_DEVICE
                )

                if (!deviceSuccess) {
                    // Try Reflection for HID_DEVICE if Standard fails
                    attemptReflectionForProfile(PROFILE_HID_DEVICE) { supported ->
                        hidDeviceResult = supported
                        report.append("HID_DEVICE (Reflection Bypass): ${if (supported) "Supported" else "Unsupported/Blocked"}\n")
                        checkedDevice = true
                        checkCompletion()
                    }
                }
            } catch (e: Exception) {
                attemptReflectionForProfile(PROFILE_HID_DEVICE) { supported ->
                    hidDeviceResult = supported
                    report.append("HID_DEVICE (Reflection Fallback due to exception): ${if (supported) "Supported" else "Unsupported"}\n")
                    checkedDevice = true
                    checkCompletion()
                }
            }
        }
    }

    /**
     * Helper method running reflection to explicitly hook into blocked framework components
     */
    private fun attemptReflectionForProfile(profileId: Int, resultCallback: (Boolean) -> Unit) {
        try {
            val getProfileProxyMethod = bluetoothAdapter!!.javaClass.getMethod(
                "getProfileProxy",
                Context::class.java,
                BluetoothProfile.ServiceListener::class.java,
                Int::class.javaPrimitiveType
            )

            val invokedSuccessfully = getProfileProxyMethod.invoke(
                bluetoothAdapter,
                context,
                object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                        if (profile == profileId && proxy != null) {
                            bluetoothAdapter!!.closeProfileProxy(profile, proxy)
                            resultCallback(true)
                        } else {
                            resultCallback(false)
                        }
                    }
                    override fun onServiceDisconnected(profile: Int) {}
                },
                profileId
            ) as Boolean

            if (!invokedSuccessfully) {
                resultCallback(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reflection error on profile $profileId: ${e.message}")
            resultCallback(false)
        }
    }
}
