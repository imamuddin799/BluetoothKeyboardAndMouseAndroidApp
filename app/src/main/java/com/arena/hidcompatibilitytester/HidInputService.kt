package com.arena.hidcompatibilitytester

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log

class HidInputService : Service() {

    private val TAG = "HidInputService"
    private val CHANNEL_ID = "HID_Service_Channel"
    private val NOTIFICATION_ID = 101

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Foreground Service Lifecycle Hook: onCreate")
        createNotificationChannel()

        // FIX: Android 10+ requires foregroundServiceType to be declared when calling
        // startForeground(). Type FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE matches
        // the android:foregroundServiceType="connectedDevice" in AndroidManifest.xml
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                buildServiceNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, buildServiceNotification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Foreground Service Running. Core Connection Thread Locked.")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun buildServiceNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("HID Peripheral Simulator")
            .setContentText("Maintaining stable Bluetooth HID input connection...")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "HID Connection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the background Bluetooth connection alive"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "Foreground Service Lifecycle Hook: onDestroy")
        super.onDestroy()
    }
}

//package com.arena.hidcompatibilitytester
//
//import android.app.Notification
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.Service
//import android.content.Context
//import android.content.Intent
//import android.os.Build
//import android.os.IBinder
//import android.util.Log
//
//class HidInputService : Service() {
//
//    private val TAG = "HidInputService"
//    private val CHANNEL_ID = "HID_Service_Channel"
//    private val NOTIFICATION_ID = 101
//
//    override fun onCreate() {
//        super.onCreate()
//        Log.d(TAG, "Foreground Service Lifecycle Hook: onCreate")
//        createNotificationChannel()
//        startForeground(NOTIFICATION_ID, buildServiceNotification())
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        Log.d(TAG, "Foreground Service Running. Core Connection Thread Locked.")
//        // START_STICKY ensures that if the system kills the service, it will be automatically restarted
//        return START_STICKY
//    }
//
//    override fun onBind(intent: Intent?): IBinder? {
//        return null // We don't need UI-to-Service IPC binding for this implementation
//    }
//
//    private fun buildServiceNotification(): Notification {
//        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            Notification.Builder(this, CHANNEL_ID)
//        } else {
//            @Suppress("DEPRECATION")
//            Notification.Builder(this)
//        }
//
//        return builder
//            .setContentTitle("HID Peripheral Simulator")
//            .setContentText("Maintaining stable Bluetooth HID input connection...")
//            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
//            .setOngoing(true)
//            .build()
//    }
//
//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                CHANNEL_ID,
//                "HID Connection Service",
//                NotificationManager.IMPORTANCE_LOW
//            ).apply {
//                description = "Keeps the background Bluetooth connection alive"
//            }
//            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            manager.createNotificationChannel(channel)
//        }
//    }
//
//    override fun onDestroy() {
//        Log.d(TAG, "Foreground Service Lifecycle Hook: onDestroy")
//        super.onDestroy()
//    }
//}
