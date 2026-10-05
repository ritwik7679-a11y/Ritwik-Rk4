package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.MessageEntity
import com.example.voice.CallActionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val callHandler = CallActionHandler(context)
            val fullBody = StringBuilder()
            var originatingAddress: String? = null

            for (sms in messages) {
                fullBody.append(sms.displayMessageBody)
                if (originatingAddress == null) {
                    originatingAddress = sms.displayOriginatingAddress
                }
            }

            val senderName = callHandler.resolveContactName(originatingAddress) ?: originatingAddress ?: "Unknown"
            val body = fullBody.toString()

            Log.d("SmsReceiver", "SMS received from $senderName: $body")

            val messageEntity = MessageEntity(
                senderName = senderName,
                senderNumber = originatingAddress,
                sourceApp = "SMS",
                content = body,
                timestamp = System.currentTimeMillis(),
                isReadAloud = false
            )

            // Save to database asynchronously and emit event
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val insertedId = db.messageDao().insertMessage(messageEntity)
                    val saved = messageEntity.copy(id = insertedId)
                    VoiceAssistantEventBus.emitIncomingMessage(saved)
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Failed to save received SMS: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
