package com.example.service

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.MessageEntity
import com.example.data.model.ActiveCallState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallSimulator(private val context: Context) {

    fun simulateIncomingCall(callerName: String, callerNumber: String) {
        val state = ActiveCallState(
            isRinging = true,
            isConnected = false,
            callerName = callerName,
            callerNumber = callerNumber,
            isAnnouncing = true,
            isSimulated = true,
            callTimestamp = System.currentTimeMillis()
        )
        VoiceAssistantEventBus.updateCallState(state)
    }

    fun endSimulatedCall() {
        VoiceAssistantEventBus.updateCallState(
            ActiveCallState(
                isRinging = false,
                isConnected = false,
                isAnnouncing = false
            )
        )
    }

    fun simulateIncomingMessage(sender: String, message: String, appName: String = "WhatsApp") {
        val entity = MessageEntity(
            senderName = sender,
            senderNumber = "+1 (555) 019-2834",
            sourceApp = appName,
            content = message,
            timestamp = System.currentTimeMillis(),
            isReadAloud = false
        )
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val id = db.messageDao().insertMessage(entity)
                VoiceAssistantEventBus.emitIncomingMessage(entity.copy(id = id))
            } catch (_: Exception) {}
        }
    }
}
