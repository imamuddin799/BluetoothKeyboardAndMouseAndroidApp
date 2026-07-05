package com.arena.hidcompatibilitytester.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.arena.hidcompatibilitytester.MainActivity
import com.arena.hidcompatibilitytester.bluetooth.BleHidManager
import com.arena.hidcompatibilitytester.bluetooth.BleHidState

class HidInputService : Service() {

    private val channelId      = "HID_Service_Channel"
    private val notificationId = 101

    lateinit var bleHidManager: BleHidManager
        private set

    private val binder = LocalBinder()

    // Activity-forwarded callbacks
    var activityStateCallback: ((BleHidState) -> Unit)? = null
    var activityDeviceListCallback: ((List<BleHidManager.DeviceInfo>) -> Unit)? = null
    var activityDeviceSubscribedCallback: ((BluetoothDevice) -> Unit)? = null
    var activityPairRequiredCallback: ((String) -> Unit)? = null

    fun clearActivityCallbacks() {
        activityStateCallback = null
        activityDeviceListCallback = null
        activityDeviceSubscribedCallback = null
        activityPairRequiredCallback = null
    }

    inner class LocalBinder : Binder() {
        fun getService(): HidInputService = this@HidInputService
    }

    // ── Internal Bluetooth state receiver ────────────────────────────────────
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_ON -> {
                    // Bluetooth turned on — start HID
                    val state = bleHidManager.getCurrentState()
                    if (state is BleHidState.IDLE || state is BleHidState.ERROR) {
                        bleHidManager.start()
                    }
                }
                BluetoothAdapter.STATE_OFF -> {
                    // Bluetooth turned off — clean up
                    bleHidManager.onBluetoothOff()
                    updateNotification("HID Peripheral", "Bluetooth off")
                }
            }
        }
    }

    // ── Bond state receiver ───────────────────────────────────────────────────
    private val bondStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothDevice.ACTION_BOND_STATE_CHANGED) return
            val device: BluetoothDevice = (
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                else
                    @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            ) ?: return
            val bondState = intent.getIntExtra(
                BluetoothDevice.EXTRA_BOND_STATE,
                BluetoothDevice.BOND_NONE
            )
            bleHidManager.onBondStateChanged(device, bondState)
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ═════════════════════════════════════════════════════════════════════════

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        startForegroundCompat(buildNotification("HID Peripheral", "Starting…"))

        bleHidManager = BleHidManager(this)
        setupServiceCallbacks()

        // Register receivers inside service so they work even when app is closed
        registerReceiverCompat(
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
        registerReceiverCompat(
            bondStateReceiver,
            IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        )
    }

    private fun setupServiceCallbacks() {
        bleHidManager.onStateChanged = { state ->
            val text = when (state) {
                is BleHidState.ADVERTISING -> "📡 Advertising — waiting for host…"
                is BleHidState.CONNECTED   -> "✓ Host connected"
                is BleHidState.ERROR       -> "✗ ${state.message}"
                else                       -> "Idle"
            }
            updateNotification("HID Peripheral", text)
            activityStateCallback?.invoke(state)
        }

        bleHidManager.onDeviceListChanged = { list ->
            val subscribed = list.filter { it.isSubscribed }
            val text = when {
                subscribed.isEmpty() && list.isEmpty() ->
                    "📡 Advertising — waiting for host…"
                subscribed.isEmpty() ->
                    "⏳ Pairing with ${list.size} host(s)…"
                subscribed.size == 1 ->
                    "✓ Connected: ${subscribed.first().name ?: subscribed.first().address}"
                else ->
                    "✓ ${subscribed.size} hosts connected"
            }
            updateNotification("HID Peripheral", text)
            activityDeviceListCallback?.invoke(list)
        }

        bleHidManager.onDeviceSubscribed = { device ->
            val name = try { device.name } catch (_: Exception) { device.address }
            updateNotification("HID Peripheral", "✓ Ready: $name")
            activityDeviceSubscribedCallback?.invoke(device)
        }

        bleHidManager.onPairRequired = { address ->
            activityPairRequiredCallback?.invoke(address)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            bleHidManager.stop()
            // Explicitly cancel the notification before stopping foreground
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(notificationId)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        // Start BLE only if not already running
        if (bleHidManager.isSupported()) {
            val state = bleHidManager.getCurrentState()
            if (state is BleHidState.IDLE || state is BleHidState.ERROR) {
                bleHidManager.start()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Do nothing — keep running
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(bluetoothStateReceiver) } catch (_: Exception) {}
        try { unregisterReceiver(bondStateReceiver) } catch (_: Exception) {}
        bleHidManager.stop()
        // Cancel notification explicitly so nothing lingers
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId)
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Notification
    // ═════════════════════════════════════════════════════════════════════════

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(notificationId, notification)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, HidInputService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, channelId)
        else
            @Suppress("DEPRECATION") Notification.Builder(this)

        return builder
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .also { builder ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    builder.addAction(
                        Notification.Action.Builder(
                            android.graphics.drawable.Icon.createWithResource(
                                this,
                                android.R.drawable.ic_delete
                            ),
                            "Stop HID",
                            stopPending
                        ).build()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    builder.addAction(
                        android.R.drawable.ic_delete,
                        "Stop HID",
                        stopPending
                    )
                }
            }
            .build()
    }

    fun updateNotification(title: String, text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, buildNotification(title, text))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "HID Connection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the background Bluetooth connection alive"
                setShowBadge(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun registerReceiverCompat(receiver: BroadcastReceiver, filter: IntentFilter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            registerReceiver(receiver, filter, RECEIVER_EXPORTED)
        else
            registerReceiver(receiver, filter)
    }

    companion object {
        const val ACTION_STOP = "com.arena.hidcompatibilitytester.ACTION_STOP_HID_SERVICE"
    }
}