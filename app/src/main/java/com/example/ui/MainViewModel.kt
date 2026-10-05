package com.example.ui

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.CommandHistoryEntity
import com.example.data.local.MessageEntity
import com.example.data.model.ActiveCallState
import com.example.data.model.AssistantState
import com.example.data.model.ChatMessage
import com.example.data.model.CommandCategory
import com.example.data.model.InstalledAppItem
import com.example.data.model.ParsedIntent
import com.example.data.model.PermissionItem
import com.example.data.model.SensitiveAction
import com.example.data.model.SettingsType
import com.example.data.remote.GeminiService
import com.example.service.CallSimulator
import com.example.service.VoiceAssistantEventBus
import com.example.voice.CallActionHandler
import com.example.voice.DeviceControlManager
import com.example.voice.NluIntentParser
import com.example.voice.TtsManager
import com.example.voice.VoiceRecognizerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext
    private val database = AppDatabase.getDatabase(context)
    private val geminiService = GeminiService()
    private val ttsManager = TtsManager(context)
    private val callActionHandler = CallActionHandler(context)
    private val deviceControlManager = DeviceControlManager(context)
    val callSimulator = CallSimulator(context)

    // Speech Recognizer helper
    private var voiceRecognizer: VoiceRecognizerHelper? = null

    // UI States
    private val _assistantState = MutableStateFlow<AssistantState>(AssistantState.Idle)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                isUser = false,
                text = "नमस्ते! मैं ऑरा हूँ, आपकी AI वॉयस असिस्टेंट। कहिए, मैं आपकी क्या मदद कर सकती हूँ? आप बोल सकते हैं: \"यूट्यूब खोलो\", \"बैटरी कितनी है\", \"मैसेज पढ़ो\", या कोई भी सवाल पूछिए।",
                category = CommandCategory.GENERAL
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // Continuous hands-free mode
    private val _continuousListening = MutableStateFlow(false)
    val continuousListening: StateFlow<Boolean> = _continuousListening.asStateFlow()

    // Auto announce messages when they arrive
    private val _autoAnnounceMessages = MutableStateFlow(true)
    val autoAnnounceMessages: StateFlow<Boolean> = _autoAnnounceMessages.asStateFlow()

    // Active Call state from EventBus
    val activeCallState: StateFlow<ActiveCallState> = VoiceAssistantEventBus.callState

    // Messages flow from Database
    val messageList = database.messageDao().getAllMessagesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Installed apps
    private val _installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppItem>> = _installedApps.asStateFlow()

    // Permissions list with status
    private val _permissionItems = MutableStateFlow<List<PermissionItem>>(emptyList())
    val permissionItems: StateFlow<List<PermissionItem>> = _permissionItems.asStateFlow()

    init {
        initSpeechRecognizer()
        refreshInstalledApps()
        refreshPermissions()
        observeEventBus()
    }

    private fun initSpeechRecognizer() {
        voiceRecognizer = VoiceRecognizerHelper(
            context = context,
            onResult = { text ->
                handleUserInput(text)
            },
            onPartialResult = { partial ->
                if (_assistantState.value is AssistantState.Listening) {
                    val currentSound = (_assistantState.value as AssistantState.Listening).soundLevel
                    _assistantState.value = AssistantState.Listening(soundLevel = currentSound, partialText = partial)
                }
            },
            onErrorOccurred = { error ->
                Log.e("MainViewModel", "Speech error: $error")
                if (_assistantState.value !is AssistantState.AwaitingConfirmation) {
                    _assistantState.value = AssistantState.Idle
                }
            }
        )

        // Observe voice sound level to animate visualizer
        viewModelScope.launch {
            voiceRecognizer?.soundLevel?.collect { level ->
                if (_assistantState.value is AssistantState.Listening) {
                    val currentPartial = (_assistantState.value as AssistantState.Listening).partialText
                    _assistantState.value = AssistantState.Listening(soundLevel = level, partialText = currentPartial)
                }
            }
        }
    }

    private fun observeEventBus() {
        // Observe Incoming Calls
        viewModelScope.launch(Dispatchers.Main) {
            VoiceAssistantEventBus.callState.collect { call ->
                if (call.isRinging && call.isAnnouncing) {
                    val callerDisplay = call.callerName ?: call.callerNumber ?: "अंज़ान कॉलर"
                    val announcement = "$callerDisplay की कॉल आ रही है। कॉल उठाने के लिए बोलिए 'कॉल उठाओ', या काटने के लिए बोलिए 'कॉल काटो'।"
                    speakResponse(announcement, CommandCategory.CALL) {
                        startListening()
                    }
                }
            }
        }

        // Observe Incoming Messages
        viewModelScope.launch(Dispatchers.Main) {
            VoiceAssistantEventBus.incomingMessageEvent.collect { message ->
                if (_autoAnnounceMessages.value) {
                    val announcement = "${message.senderName} का नया मैसेज आया है: \"${message.content}\""
                    speakResponse(announcement, CommandCategory.MESSAGE)
                }
            }
        }

        // Observe Voice Trigger Requests
        viewModelScope.launch(Dispatchers.Main) {
            VoiceAssistantEventBus.voiceTriggerRequest.collect { command ->
                handleUserInput(command)
            }
        }
    }

    fun toggleContinuousListening() {
        _continuousListening.value = !_continuousListening.value
        val statusText = if (_continuousListening.value) "हैंड्स-फ्री मोड चालू हो गया है।" else "हैंड्स-फ्री मोड बंद कर दिया गया है।"
        speakResponse(statusText, CommandCategory.GENERAL)
        if (_continuousListening.value && _assistantState.value is AssistantState.Idle) {
            startListening()
        }
    }

    fun toggleAutoAnnounceMessages() {
        _autoAnnounceMessages.value = !_autoAnnounceMessages.value
    }

    fun startListening() {
        viewModelScope.launch(Dispatchers.Main) {
            ttsManager.stop()
            _assistantState.value = AssistantState.Listening()
            voiceRecognizer?.startListening()
        }
    }

    fun stopListening() {
        viewModelScope.launch(Dispatchers.Main) {
            voiceRecognizer?.stopListening()
            _assistantState.value = AssistantState.Idle
        }
    }

    fun toggleListening() {
        if (_assistantState.value is AssistantState.Listening) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun handleUserInput(rawText: String) {
        val text = rawText.trim()
        if (text.isBlank()) return

        stopListening()

        // Append user turn to transcript
        val userTurn = ChatMessage(isUser = true, text = text)
        _chatMessages.value = _chatMessages.value + userTurn

        // Check if currently waiting for sensitive confirmation
        val currentState = _assistantState.value
        val isAwaiting = currentState is AssistantState.AwaitingConfirmation

        val parsedIntent = NluIntentParser.parse(text, isAwaitingConfirmation = isAwaiting)
        _assistantState.value = AssistantState.Processing(text)

        executeParsedIntent(parsedIntent, originalText = text)
    }

    private fun executeParsedIntent(intent: ParsedIntent, originalText: String) {
        viewModelScope.launch(Dispatchers.Main) {
            when (intent) {
                // 1. Open App
                is ParsedIntent.OpenApp -> {
                    val result = deviceControlManager.openAppByName(intent.targetAppName)
                    result.onSuccess { msg ->
                        speakResponse(msg, CommandCategory.APP_LAUNCH, success = true)
                    }.onFailure { err ->
                        val explanation = "'${intent.targetAppName}' ऐप आपके फोन में नहीं मिला। कृपया चेक करें कि यह इनस्टॉल है या नहीं।"
                        speakResponse(explanation, CommandCategory.APP_LAUNCH, success = false)
                    }
                }

                // 2. Close App
                is ParsedIntent.CloseApp -> {
                    val (spoken, restriction) = deviceControlManager.closeOrExitApp()
                    speakResponse(
                        text = spoken,
                        category = CommandCategory.SYSTEM_RESTRICTION,
                        restrictionNotice = restriction,
                        success = true
                    )
                }

                // 3. Answer Call
                is ParsedIntent.AnswerCall -> {
                    val activeCall = VoiceAssistantEventBus.callState.value
                    if (activeCall.isRinging) {
                        if (activeCall.isSimulated || (activeCall.callerName != null && activeCall.callerNumber?.startsWith("+1 (555)") == true)) {
                            callSimulator.endSimulatedCall()
                            speakResponse("कॉल उठा ली गई है।", CommandCategory.CALL, success = true)
                        } else {
                            val result = callActionHandler.answerRingingCall()
                            result.onSuccess { msg ->
                                speakResponse(msg, CommandCategory.CALL, success = true)
                            }.onFailure { err ->
                                val explanation = "कॉल नहीं उठ सकी: ${err.message}। कृपया फोन परमिशन चेक करें।"
                                speakResponse(explanation, CommandCategory.CALL, success = false, permission = "ANSWER_PHONE_CALLS")
                            }
                        }
                    } else {
                        speakResponse("अभी कोई इनकमिंग कॉल नहीं आ रही है जिसे उठाया जा सके।", CommandCategory.CALL)
                    }
                }

                // 4. Reject Call
                is ParsedIntent.RejectCall -> {
                    val activeCall = VoiceAssistantEventBus.callState.value
                    if (activeCall.isRinging) {
                        if (activeCall.isSimulated || (activeCall.callerName != null && activeCall.callerNumber?.startsWith("+1 (555)") == true)) {
                            callSimulator.endSimulatedCall()
                            speakResponse("कॉल काट दी गई है।", CommandCategory.CALL, success = true)
                        } else {
                            val result = callActionHandler.rejectRingingCall()
                            result.onSuccess { msg ->
                                speakResponse(msg, CommandCategory.CALL, success = true)
                            }.onFailure { err ->
                                val explanation = "कॉल नहीं कट सकी: ${err.message}।"
                                speakResponse(explanation, CommandCategory.CALL, success = false, permission = "ANSWER_PHONE_CALLS")
                            }
                        }
                    } else {
                        speakResponse("अभी कोई इनकमिंग कॉल नहीं आ रही है जिसे काटा जा सके।", CommandCategory.CALL)
                    }
                }

                // 5. Read Messages
                is ParsedIntent.ReadMessages -> {
                    val messages = kotlinx.coroutines.withContext(Dispatchers.IO) {
                        if (intent.senderQuery != null) {
                            database.messageDao().searchMessagesBySender(intent.senderQuery, intent.count)
                        } else {
                            database.messageDao().getRecentMessages(intent.count)
                        }
                    }

                    if (messages.isEmpty()) {
                        val reply = if (intent.senderQuery != null) {
                            "${intent.senderQuery} की तरफ से कोई नया मैसेज नहीं है।"
                        } else {
                            "आपके पास कोई नए मैसेज नहीं हैं।"
                        }
                        speakResponse(reply, CommandCategory.MESSAGE)
                    } else {
                        val responseBuilder = StringBuilder()
                        responseBuilder.append("आपके पास ${messages.size} नए मैसेज हैं। ")
                        messages.forEachIndexed { index, msg ->
                            responseBuilder.append("${index + 1}: ${msg.senderName} ने ${msg.sourceApp} पर भेजा है: \"${msg.content}\"। ")
                        }
                        speakResponse(responseBuilder.toString(), CommandCategory.MESSAGE, success = true)
                        kotlinx.coroutines.withContext(Dispatchers.IO) {
                            database.messageDao().markAllAsRead()
                        }
                    }
                }

                // 6. Make Phone Call (Sensitive - Requires Confirmation)
                is ParsedIntent.CallContact -> {
                    val contactMatch = callActionHandler.searchContactPhoneNumber(intent.contactQuery)
                    if (contactMatch != null) {
                        val (name, number) = contactMatch
                        val pending = SensitiveAction.CallContact(name, number)
                        val prompt = "क्या आप $name को कॉल लगाना चाहते हैं?"
                        _assistantState.value = AssistantState.AwaitingConfirmation(
                            pendingAction = pending,
                            prompt = prompt,
                            originalCommand = originalText
                        )
                        speakResponse("$prompt बोलिए 'हाँ' या 'कैंसिल'。", CommandCategory.CONFIRMATION) {
                            startListening()
                        }
                    } else {
                        if (intent.contactQuery.filter { it.isDigit() }.length >= 7) {
                            val digits = intent.contactQuery.filter { it.isDigit() || it == '+' }
                            val pending = SensitiveAction.CallContact(digits, digits)
                            val prompt = "क्या आप $digits पर कॉल लगाना चाहते हैं?"
                            _assistantState.value = AssistantState.AwaitingConfirmation(
                                pendingAction = pending,
                                prompt = prompt,
                                originalCommand = originalText
                            )
                            speakResponse("$prompt बोलिए 'हाँ' या 'कैंसिल'।", CommandCategory.CONFIRMATION) {
                                startListening()
                            }
                        } else {
                            speakResponse("'${intent.contactQuery}' आपके सेव किए हुए कॉन्टैक्ट्स में नहीं मिला।", CommandCategory.CALL, success = false)
                        }
                    }
                }

                // 7. Send SMS (Sensitive - Requires Confirmation)
                is ParsedIntent.SendSms -> {
                    val contactMatch = callActionHandler.searchContactPhoneNumber(intent.recipientQuery)
                    val recipientName = contactMatch?.first ?: intent.recipientQuery
                    val recipientNumber = contactMatch?.second ?: intent.recipientQuery

                    val pending = SensitiveAction.SendSms(recipientName, recipientNumber, intent.messageText)
                    val prompt = "क्या मैं $recipientName को यह मैसेज भेज दूँ: \"${intent.messageText}\"?"
                    _assistantState.value = AssistantState.AwaitingConfirmation(
                        pendingAction = pending,
                        prompt = prompt,
                        originalCommand = originalText
                    )
                    speakResponse("$prompt बोलिए 'हाँ भेजो' या 'कैंसिल'।", CommandCategory.CONFIRMATION) {
                        startListening()
                    }
                }

                // 8. Confirm or Cancel Sensitive Action
                is ParsedIntent.ConfirmAction -> {
                    val current = _assistantState.value
                    if (current is AssistantState.AwaitingConfirmation) {
                        if (intent.confirmed) {
                            executeConfirmedAction(current.pendingAction)
                        } else {
                            _assistantState.value = AssistantState.Idle
                            speakResponse("कैंसिल कर दिया गया है।", CommandCategory.CONFIRMATION)
                        }
                    } else {
                        speakResponse("अभी कन्फर्म करने के लिए कोई पेंडिंग काम नहीं है।", CommandCategory.GENERAL)
                    }
                }

                // 9. Volume Controls
                is ParsedIntent.SetVolume -> {
                    val result = deviceControlManager.adjustVolume(intent.action, intent.percent)
                    result.onSuccess { msg ->
                        speakResponse(msg, CommandCategory.DEVICE_CONTROL, success = true)
                    }.onFailure { err ->
                        speakResponse("आवाज़ बदलने में परेशानी हुई: ${err.message}", CommandCategory.DEVICE_CONTROL, success = false)
                    }
                }

                // 10. Flashlight
                is ParsedIntent.ToggleFlashlight -> {
                    val result = deviceControlManager.toggleFlashlight(intent.enable)
                    result.onSuccess { msg ->
                        speakResponse(msg, CommandCategory.DEVICE_CONTROL, success = true)
                    }.onFailure { err ->
                        speakResponse("टॉर्च चालू या बंद नहीं हो सकी: ${err.message}", CommandCategory.DEVICE_CONTROL, success = false)
                    }
                }

                // 11. Battery Check
                is ParsedIntent.CheckBattery -> {
                    val batteryInfo = deviceControlManager.checkBattery()
                    speakResponse(batteryInfo, CommandCategory.DEVICE_CONTROL, success = true)
                }

                // 12. Alarms and Timers
                is ParsedIntent.SetAlarm -> {
                    val res = deviceControlManager.setAlarm(intent.hour, intent.minute, intent.label)
                    res.onSuccess { msg ->
                        speakResponse(msg, CommandCategory.DEVICE_CONTROL, success = true)
                    }.onFailure { err ->
                        speakResponse("अलार्म नहीं लग सका: ${err.message}", CommandCategory.DEVICE_CONTROL, success = false)
                    }
                }
                is ParsedIntent.SetTimer -> {
                    val res = deviceControlManager.setTimer(intent.durationSeconds, intent.label)
                    res.onSuccess { msg ->
                        speakResponse(msg, CommandCategory.DEVICE_CONTROL, success = true)
                    }.onFailure { err ->
                        speakResponse("टाइमर नहीं लग सका: ${err.message}", CommandCategory.DEVICE_CONTROL, success = false)
                    }
                }

                // 13. Settings
                is ParsedIntent.OpenSettings -> {
                    val res = deviceControlManager.openSettings(intent.settingsType)
                    res.onSuccess { msg ->
                        speakResponse(msg, CommandCategory.DEVICE_CONTROL, success = true)
                    }.onFailure { err ->
                        speakResponse("सेटिंग्स नहीं खुल सकी: ${err.message}", CommandCategory.DEVICE_CONTROL, success = false)
                    }
                }

                // 14. Conversational / Gemini AI
                is ParsedIntent.ConversationalQuery -> {
                    val reply = geminiService.askAssistant(intent.userText)
                    speakResponse(reply, CommandCategory.GENERAL, success = true)
                }

                // 15. Unknown
                is ParsedIntent.Unknown -> {
                    val fallback = "मुझे ठीक से समझ नहीं आया। आप मुझसे कोई ऐप खोलने, आवाज़ बढ़ाने या मैसेज पढ़ने को कह सकते हैं।"
                    speakResponse(fallback, CommandCategory.GENERAL)
                }

                else -> {
                    speakResponse("कृपया थोड़ा साफ़ बोलिए, मैं आपके कमांड को समझ नहीं पायी।", CommandCategory.GENERAL)
                }
            }
        }
    }

    private fun executeConfirmedAction(action: SensitiveAction) {
        when (action) {
            is SensitiveAction.CallContact -> {
                val result = callActionHandler.makePhoneCall(action.phoneNumber)
                result.onSuccess { msg ->
                    speakResponse("${action.contactName} को कॉल लगाई जा रही है...", CommandCategory.CALL, success = true)
                }.onFailure { err ->
                    val explanation = "कॉल नहीं लग सकी: ${err.message}। कृपया फोन परमिशन चेक करें।"
                    speakResponse(explanation, CommandCategory.CALL, success = false, permission = "CALL_PHONE")
                }
            }
            is SensitiveAction.SendSms -> {
                try {
                    val smsManager: SmsManager = context.getSystemService(SmsManager::class.java)
                    smsManager.sendTextMessage(action.phoneNumber, null, action.messageBody, null, null)
                    speakResponse("${action.contactName} को मैसेज भेज दिया गया है।", CommandCategory.MESSAGE, success = true)
                } catch (e: Exception) {
                    val msg = "मैसेज नहीं भेजा जा सका: ${e.message}। कृपया SMS परमिशन चेक करें।"
                    speakResponse(msg, CommandCategory.MESSAGE, success = false, permission = "SEND_SMS")
                }
            }
            is SensitiveAction.RejectIncomingCall -> {
                val res = callActionHandler.rejectRingingCall()
                res.onSuccess { msg ->
                    speakResponse(msg, CommandCategory.CALL, success = true)
                }.onFailure { err ->
                    speakResponse("कॉल नहीं कट सकी: ${err.message}", CommandCategory.CALL, success = false)
                }
            }
            is SensitiveAction.SystemAction -> {
                action.onExecute()
                speakResponse("काम पूरा हो गया है।", CommandCategory.DEVICE_CONTROL, success = true)
            }
        }
    }

    fun confirmSensitiveAction(confirmed: Boolean) {
        val current = _assistantState.value
        if (current is AssistantState.AwaitingConfirmation) {
            if (confirmed) {
                executeConfirmedAction(current.pendingAction)
            } else {
                _assistantState.value = AssistantState.Idle
                speakResponse("कैंसिल कर दिया गया है।", CommandCategory.CONFIRMATION)
            }
        }
    }

    fun speakResponse(
        text: String,
        category: CommandCategory,
        success: Boolean? = null,
        permission: String? = null,
        restrictionNotice: String? = null,
        onDone: (() -> Unit)? = null
    ) {
        val assistantMessage = ChatMessage(
            isUser = false,
            text = text,
            category = category,
            actionSuccess = success,
            permissionRequired = permission,
            restrictionNotice = restrictionNotice
        )
        _chatMessages.value = _chatMessages.value + assistantMessage

        // Save command to history
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.commandHistoryDao().insertCommand(
                    CommandHistoryEntity(
                        commandText = _chatMessages.value.findLast { it.isUser }?.text ?: "Voice action",
                        responseText = text,
                        category = category.name,
                        wasSuccessful = success ?: true
                    )
                )
            } catch (_: Exception) {}
        }

        _assistantState.value = AssistantState.Speaking(spokenText = text)
        ttsManager.speak(text) {
            viewModelScope.launch(Dispatchers.Main) {
                if (_assistantState.value !is AssistantState.AwaitingConfirmation) {
                    _assistantState.value = AssistantState.Idle
                }
                onDone?.invoke()

                // If continuous listening is enabled and not waiting for confirmation
                if (_continuousListening.value && _assistantState.value is AssistantState.Idle) {
                    startListening()
                }
            }
        }
    }

    fun answerIncomingCall() {
        val call = VoiceAssistantEventBus.callState.value
        if (call.isRinging) {
            if (call.isSimulated || call.callerNumber?.startsWith("+1 (555)") == true) {
                callSimulator.endSimulatedCall()
                speakResponse("कॉल उठा ली गई है।", CommandCategory.CALL, success = true)
            } else {
                callActionHandler.answerRingingCall().fold(
                    onSuccess = { speakResponse(it, CommandCategory.CALL, success = true) },
                    onFailure = { speakResponse("कॉल नहीं उठ सकी: ${it.message}", CommandCategory.CALL, success = false) }
                )
            }
        }
    }

    fun rejectIncomingCall() {
        val call = VoiceAssistantEventBus.callState.value
        if (call.isRinging) {
            if (call.isSimulated || call.callerNumber?.startsWith("+1 (555)") == true) {
                callSimulator.endSimulatedCall()
                speakResponse("कॉल काट दी गई है।", CommandCategory.CALL, success = true)
            } else {
                callActionHandler.rejectRingingCall().fold(
                    onSuccess = { speakResponse(it, CommandCategory.CALL, success = true) },
                    onFailure = { speakResponse("कॉल नहीं कट सकी: ${it.message}", CommandCategory.CALL, success = false) }
                )
            }
        }
    }

    fun readSpecificMessageAloud(message: MessageEntity) {
        val textToSpeak = "${message.senderName} का नया मैसेज: ${message.content}"
        speakResponse(textToSpeak, CommandCategory.MESSAGE, success = true)
        viewModelScope.launch(Dispatchers.IO) {
            database.messageDao().markAsRead(message.id)
        }
    }

    fun clearAllMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            database.messageDao().clearAll()
        }
    }

    fun refreshInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = deviceControlManager.getInstalledApps()
            _installedApps.value = apps
        }
    }

    fun refreshPermissions() {
        val check = { perm: String ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }

        val items = listOf(
            PermissionItem(
                permissionId = android.Manifest.permission.RECORD_AUDIO,
                title = "माइक्रोफ़ोन (Microphone)",
                description = "बोलकर कमांड देने और हैंड्स-फ्री वॉयस असिस्टेंट चलाने के लिए।",
                privacyNotice = "आपकी आवाज़ केवल कमांड समझने के लिए इस्तेमाल होती है। यह कभी भी आपकी सहमति के बिना रिकॉर्ड या अपलोड नहीं की जाती।",
                isGranted = check(android.Manifest.permission.RECORD_AUDIO),
                isRequiredForCore = true
            ),
            PermissionItem(
                permissionId = android.Manifest.permission.READ_PHONE_STATE,
                title = "फोन स्टेट व कॉलर आईडी (Caller ID)",
                description = "इनकमिंग कॉल आने पर कॉलर का नाम बोलकर सुनाने के लिए।",
                privacyNotice = "इसका इस्तेमाल केवल बजती हुई कॉल का पता लगाने और नंबर जानने के लिए होता है।",
                isGranted = check(android.Manifest.permission.READ_PHONE_STATE),
                isRequiredForCore = true
            ),
            PermissionItem(
                permissionId = android.Manifest.permission.ANSWER_PHONE_CALLS,
                title = "कॉल उठाना व काटना (Answer & Reject Calls)",
                description = "बोलकर 'कॉल उठाओ' या 'कॉल काटो' कमांड से कॉल कंट्रोल करने के लिए।",
                privacyNotice = "जब आप वॉयस कमांड देते हैं, तभी सीधे सिस्टम के ज़रिए कॉल काटी या उठाई जाती है।",
                isGranted = check(android.Manifest.permission.ANSWER_PHONE_CALLS),
                isRequiredForCore = true
            ),
            PermissionItem(
                permissionId = android.Manifest.permission.READ_CONTACTS,
                title = "कॉन्टैक्ट्स (Contacts)",
                description = "फोन नंबर को आपके सेव किए गए कॉन्टैक्ट नाम से मिलाकर बताने के लिए।",
                privacyNotice = "कॉन्टैक्ट्स केवल आपके फोन पर ही प्रोसेस होते हैं, कहीं बाहर नहीं भेजे जाते।",
                isGranted = check(android.Manifest.permission.READ_CONTACTS),
                isRequiredForCore = false
            ),
            PermissionItem(
                permissionId = android.Manifest.permission.CALL_PHONE,
                title = "फोन कॉल लगाना (Make Calls)",
                description = "आपकी मंज़ूरी के बाद बोलकर सीधे किसी कॉन्टैक्ट को फोन लगाने के लिए।",
                privacyNotice = "कॉल लगाने से पहले हमेशा आपसे हाँ या ना की पुष्टि (कन्फर्मेशन) ली जाती है।",
                isGranted = check(android.Manifest.permission.CALL_PHONE),
                isRequiredForCore = false
            ),
            PermissionItem(
                permissionId = android.Manifest.permission.RECEIVE_SMS,
                title = "एसएमएस प्राप्त करना (Receive SMS)",
                description = "नया मैसेज आते ही तुरंत बोलकर सुनाने के लिए।",
                privacyNotice = "मैसेज आपके फोन के सुरक्षित लोकल डेटाबेस में रहते हैं और आपकी इजाज़त पर ही पढ़े जाते हैं।",
                isGranted = check(android.Manifest.permission.RECEIVE_SMS),
                isRequiredForCore = false
            ),
            PermissionItem(
                permissionId = android.Manifest.permission.READ_SMS,
                title = "एसएमएस पढ़ना (Read SMS)",
                description = "जब आप 'मैसेज पढ़ो' कहें तो इनबॉक्स के मैसेज सुनाने के लिए।",
                privacyNotice = "आपके एसएमएस सुरक्षित रूप से सिर्फ आपके फोन पर ही रहते हैं।",
                isGranted = check(android.Manifest.permission.READ_SMS),
                isRequiredForCore = false
            ),
            PermissionItem(
                permissionId = android.Manifest.permission.SEND_SMS,
                title = "एसएमएस भेजना (Send SMS)",
                description = "बोलकर मैसेज भेजने और कन्फर्मेशन के बाद सेंड करने के लिए।",
                privacyNotice = "मैसेज भेजने से पहले हमेशा आपसे स्पष्ट पुष्टि ली जाती है।",
                isGranted = check(android.Manifest.permission.SEND_SMS),
                isRequiredForCore = false
            )
        )
        _permissionItems.value = items
    }

    override fun onCleared() {
        super.onCleared()
        voiceRecognizer?.destroy()
        ttsManager.shutdown()
    }
}
