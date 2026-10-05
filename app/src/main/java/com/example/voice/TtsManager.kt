package com.example.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsManager(context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _spokenText = MutableStateFlow("")
    val spokenText: StateFlow<String> = _spokenText.asStateFlow()

    private var onSpeechDoneCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    val hindiLocale = Locale("hi", "IN")
                    val result = engine.setLanguage(hindiLocale)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        val fallback = engine.setLanguage(Locale("hi"))
                        if (fallback == TextToSpeech.LANG_MISSING_DATA || fallback == TextToSpeech.LANG_NOT_SUPPORTED) {
                            engine.setLanguage(Locale.getDefault())
                        }
                    }
                    try {
                        val hindiVoice = engine.voices?.firstOrNull { voice ->
                            voice.locale.language == "hi" && !voice.isNetworkConnectionRequired
                        } ?: engine.voices?.firstOrNull { it.locale.language == "hi" }
                        if (hindiVoice != null) {
                            engine.voice = hindiVoice
                        }
                    } catch (_: Exception) {}
                    engine.setPitch(1.0f)
                    engine.setSpeechRate(0.95f)
                    isInitialized = true

                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            mainHandler.post {
                                _isSpeaking.value = true
                            }
                        }

                        override fun onDone(utteranceId: String?) {
                            mainHandler.post {
                                _isSpeaking.value = false
                                _spokenText.value = ""
                                onSpeechDoneCallback?.invoke()
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            mainHandler.post {
                                _isSpeaking.value = false
                                _spokenText.value = ""
                            }
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            mainHandler.post {
                                _isSpeaking.value = false
                                _spokenText.value = ""
                            }
                        }
                    })
                }
            } else {
                Log.e("TtsManager", "TTS initialization failed: status $status")
            }
        }
    }

    fun speak(text: String, flushQueue: Boolean = true, onDone: (() -> Unit)? = null) {
        if (!isInitialized || tts == null) {
            Log.w("TtsManager", "TTS not initialized yet")
            mainHandler.post {
                onDone?.invoke()
            }
            return
        }

        this.onSpeechDoneCallback = onDone
        _spokenText.value = text
        val queueMode = if (flushQueue) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val utteranceId = "utterance_${System.currentTimeMillis()}"

        tts?.speak(text, queueMode, null, utteranceId)
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
        }
        mainHandler.post {
            _isSpeaking.value = false
            _spokenText.value = ""
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("TtsManager", "Error shutting down TTS: ${e.message}")
        }
        tts = null
        isInitialized = false
    }
}
