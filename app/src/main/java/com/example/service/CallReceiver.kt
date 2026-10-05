package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.example.data.model.ActiveCallState
import com.example.voice.CallActionHandler

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            Log.d("CallReceiver", "Phone state changed: $stateStr, number: $incomingNumber")

            val callHandler = CallActionHandler(context)
            val resolvedName = callHandler.resolveContactName(incomingNumber)

            when (stateStr) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    val activeState = ActiveCallState(
                        isRinging = true,
                        isConnected = false,
                        callerName = resolvedName,
                        callerNumber = incomingNumber,
                        isAnnouncing = true,
                        callTimestamp = System.currentTimeMillis()
                    )
                    VoiceAssistantEventBus.updateCallState(activeState)
                }

                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    val activeState = ActiveCallState(
                        isRinging = false,
                        isConnected = true,
                        callerName = resolvedName,
                        callerNumber = incomingNumber,
                        isAnnouncing = false,
                        callTimestamp = System.currentTimeMillis()
                    )
                    VoiceAssistantEventBus.updateCallState(activeState)
                }

                TelephonyManager.EXTRA_STATE_IDLE -> {
                    VoiceAssistantEventBus.updateCallState(
                        ActiveCallState(
                            isRinging = false,
                            isConnected = false,
                            isAnnouncing = false
                        )
                    )
                }
            }
        }
    }
}
