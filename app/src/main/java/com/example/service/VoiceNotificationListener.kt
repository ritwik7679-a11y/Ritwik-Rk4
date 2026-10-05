package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.MessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VoiceNotificationListener : NotificationListenerService() {

    private val supportedMessagingPackages = setOf(
        "com.whatsapp",
        "org.telegram.messenger",
        "com.google.android.apps.messaging",
        "com.facebook.orca",
        "org.thoughtcrime.securesms", // Signal
        "com.discord",
        "com.Slack"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        // Capture messaging notifications or any notification with messaging style
        val isKnownApp = supportedMessagingPackages.contains(packageName)
        val extras = sbn.notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()

        if (title.isNullOrBlank() || text.isNullOrBlank()) return

        val appName = when (packageName) {
            "com.whatsapp" -> "WhatsApp"
            "org.telegram.messenger" -> "Telegram"
            "com.google.android.apps.messaging" -> "Messages"
            "com.facebook.orca" -> "Messenger"
            "org.thoughtcrime.securesms" -> "Signal"
            "com.discord" -> "Discord"
            "com.Slack" -> "Slack"
            else -> if (isKnownApp) packageName else null
        }

        if (appName != null) {
            Log.d("VoiceNotification", "Captured message notification from $appName: $title -> $text")
            val messageEntity = MessageEntity(
                senderName = title,
                senderNumber = null,
                sourceApp = appName,
                content = text,
                timestamp = System.currentTimeMillis(),
                isReadAloud = false
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(applicationContext)
                    val insertedId = db.messageDao().insertMessage(messageEntity)
                    VoiceAssistantEventBus.emitIncomingMessage(messageEntity.copy(id = insertedId))
                } catch (e: Exception) {
                    Log.e("VoiceNotification", "Error saving notification message: ${e.message}")
                }
            }
        }
    }
}
