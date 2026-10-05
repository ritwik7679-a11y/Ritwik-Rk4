package com.example.service

import com.example.data.model.ActiveCallState
import com.example.data.local.MessageEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object VoiceAssistantEventBus {

    private val _callState = MutableStateFlow(ActiveCallState())
    val callState: StateFlow<ActiveCallState> = _callState.asStateFlow()

    private val _incomingMessageEvent = MutableSharedFlow<MessageEntity>(extraBufferCapacity = 10)
    val incomingMessageEvent: SharedFlow<MessageEntity> = _incomingMessageEvent.asSharedFlow()

    private val _voiceTriggerRequest = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val voiceTriggerRequest: SharedFlow<String> = _voiceTriggerRequest.asSharedFlow()

    fun updateCallState(state: ActiveCallState) {
        _callState.value = state
    }

    fun emitIncomingMessage(message: MessageEntity) {
        _incomingMessageEvent.tryEmit(message)
    }

    fun requestVoiceCommand(command: String) {
        _voiceTriggerRequest.tryEmit(command)
    }
}
