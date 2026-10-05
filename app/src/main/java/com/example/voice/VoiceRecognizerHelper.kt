package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceRecognizerHelper(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onPartialResult: (String) -> Unit = {},
    private val onErrorOccurred: (String) -> Unit = {}
) : RecognitionListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    init {
        runOnMain {
            initRecognizer()
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private fun initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("VoiceRecognizer", "Speech recognition not available on device")
            return
        }
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@VoiceRecognizerHelper)
            }
        } catch (e: Exception) {
            Log.e("VoiceRecognizer", "Failed to create SpeechRecognizer: ${e.message}")
            speechRecognizer = null
        }
    }

    private var lastStartListeningTime = 0L

    fun startListening() {
        runOnMain {
            val now = System.currentTimeMillis()
            if (now - lastStartListeningTime < 350) {
                // Debounce rapid taps
                return@runOnMain
            }
            lastStartListeningTime = now

            if (_isListening.value) {
                try {
                    speechRecognizer?.cancel()
                } catch (_: Exception) {}
                _isListening.value = false
            }

            if (speechRecognizer == null) {
                initRecognizer()
            }

            if (speechRecognizer == null) {
                Log.w("VoiceRecognizer", "Speech recognizer unavailable")
                return@runOnMain
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-IN"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            try {
                speechRecognizer?.startListening(intent)
                _isListening.value = true
                _partialText.value = ""
            } catch (e: Exception) {
                Log.e("VoiceRecognizer", "startListening error: ${e.message}")
                _isListening.value = false
                initRecognizer()
            }
        }
    }

    fun stopListening() {
        runOnMain {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e("VoiceRecognizer", "stopListening error: ${e.message}")
            }
            _isListening.value = false
            _soundLevel.value = 0f
        }
    }

    fun cancel() {
        runOnMain {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.e("VoiceRecognizer", "cancel error: ${e.message}")
            }
            _isListening.value = false
            _soundLevel.value = 0f
            _partialText.value = ""
        }
    }

    fun destroy() {
        runOnMain {
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.e("VoiceRecognizer", "destroy error: ${e.message}")
            }
            speechRecognizer = null
            _isListening.value = false
            _soundLevel.value = 0f
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        runOnMain {
            _isListening.value = true
        }
    }

    override fun onBeginningOfSpeech() {
        // User started speaking
    }

    override fun onRmsChanged(rmsdB: Float) {
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        _soundLevel.value = normalized
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        runOnMain {
            _isListening.value = false
            _soundLevel.value = 0f
        }
    }

    override fun onError(error: Int) {
        runOnMain {
            _isListening.value = false
            _soundLevel.value = 0f

            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer is busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input heard"
                else -> "Speech recognition error ($error)"
            }

            Log.w("VoiceRecognizer", "SpeechRecognizer error code $error: $errorMessage")

            // If error was client side or recognizer busy, cleanly re-create the recognizer instance
            if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                initRecognizer()
            }

            // Only propagate major errors to the user UI, avoid spamming on normal timeouts / no-match
            if (error != SpeechRecognizer.ERROR_NO_MATCH &&
                error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT &&
                error != SpeechRecognizer.ERROR_CLIENT
            ) {
                onErrorOccurred(errorMessage)
            }
        }
    }

    override fun onResults(results: Bundle?) {
        runOnMain {
            _isListening.value = false
            _soundLevel.value = 0f
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.trim()
            if (!text.isNullOrBlank()) {
                _partialText.value = text
                onResult(text)
            }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        runOnMain {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.trim()
            if (!text.isNullOrBlank()) {
                _partialText.value = text
                onPartialResult(text)
            }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
