package com.example.data.model

/**
 * State of the Voice Assistant engine
 */
sealed interface AssistantState {
    data object Idle : AssistantState
    data class Listening(val soundLevel: Float = 0f, val partialText: String = "") : AssistantState
    data class Processing(val recognizedText: String) : AssistantState
    data class Speaking(val spokenText: String, val audioWave: Float = 0.5f) : AssistantState
    data class AwaitingConfirmation(
        val pendingAction: SensitiveAction,
        val prompt: String,
        val originalCommand: String
    ) : AssistantState
    data class Error(val message: String, val canRetry: Boolean = true) : AssistantState
}

/**
 * Categories of commands
 */
enum class CommandCategory {
    APP_LAUNCH,
    CALL,
    MESSAGE,
    DEVICE_CONTROL,
    CONFIRMATION,
    GENERAL,
    SYSTEM_RESTRICTION
}

/**
 * Sensitive actions requiring explicit voice/tap confirmation
 */
sealed interface SensitiveAction {
    data class CallContact(val contactName: String, val phoneNumber: String) : SensitiveAction
    data class SendSms(val contactName: String, val phoneNumber: String, val messageBody: String) : SensitiveAction
    data class RejectIncomingCall(val callerName: String?, val phoneNumber: String?) : SensitiveAction
    data class SystemAction(val title: String, val description: String, val onExecute: () -> Unit) : SensitiveAction
}

/**
 * Parsed intent from user voice or text
 */
sealed interface ParsedIntent {
    data class OpenApp(val targetAppName: String) : ParsedIntent
    data class CloseApp(val targetAppName: String? = null) : ParsedIntent
    data object AnswerCall : ParsedIntent
    data object RejectCall : ParsedIntent
    data class CallContact(val contactQuery: String) : ParsedIntent
    data class SendSms(val recipientQuery: String, val messageText: String) : ParsedIntent
    data class ReadMessages(val senderQuery: String? = null, val count: Int = 3) : ParsedIntent
    data class SetVolume(val action: VolumeAction, val percent: Int? = null) : ParsedIntent
    data class ToggleFlashlight(val enable: Boolean) : ParsedIntent
    data object CheckBattery : ParsedIntent
    data class SetAlarm(val hour: Int, val minute: Int, val label: String = "Voice Alarm") : ParsedIntent
    data class SetTimer(val durationSeconds: Int, val label: String = "Voice Timer") : ParsedIntent
    data class OpenSettings(val settingsType: SettingsType) : ParsedIntent
    data class ConfirmAction(val confirmed: Boolean) : ParsedIntent
    data class ConversationalQuery(val userText: String) : ParsedIntent
    data class SystemRestriction(val explanation: String, val actionTitle: String) : ParsedIntent
    data class Unknown(val rawText: String) : ParsedIntent
}

enum class VolumeAction {
    UP, DOWN, MUTE, UNMUTE, SET
}

enum class SettingsType {
    WIFI, BLUETOOTH, SOUND, DISPLAY, ACCESSIBILITY, GENERAL
}

/**
 * Interactive Chat Message displayed in transcript
 */
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val category: CommandCategory = CommandCategory.GENERAL,
    val actionSuccess: Boolean? = null,
    val permissionRequired: String? = null,
    val restrictionNotice: String? = null
)

/**
 * Incoming phone call details
 */
data class ActiveCallState(
    val isRinging: Boolean = false,
    val isConnected: Boolean = false,
    val callerName: String? = null,
    val callerNumber: String? = null,
    val isAnnouncing: Boolean = false,
    val isSimulated: Boolean = false,
    val callTimestamp: Long = 0L
)

/**
 * Installed Application item for launcher
 */
data class InstalledAppItem(
    val appName: String,
    val packageName: String,
    val isSystemApp: Boolean = false
)

/**
 * Permission information with privacy & restriction reasoning
 */
data class PermissionItem(
    val permissionId: String,
    val title: String,
    val description: String,
    val privacyNotice: String,
    val isGranted: Boolean,
    val isRequiredForCore: Boolean = false
)
