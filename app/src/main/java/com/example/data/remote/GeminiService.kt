package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val temperature: Float? = 0.7f,
    val maxOutputTokens: Int? = 150
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val error: GeminiError? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiError(
    val code: Int? = null,
    val message: String? = null
)

class GeminiService {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val requestAdapter = moshi.adapter(GeminiRequest::class.java)
    private val responseAdapter = moshi.adapter(GeminiResponse::class.java)

    suspend fun askAssistant(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Local conversational fallback
            return@withContext getLocalConversationalFallback(prompt)
        }

        val systemInstruction = GeminiContent(
            parts = listOf(
                GeminiPart(
                    text = "Aap Aura hain, Android phones ke liye ek smart, friendly aur human-like AI voice assistant. " +
                            "Aapki default bhasha Hindi hai. " +
                            "Aap hamesha aasan, natural, aur simple conversational Hindi mein baat karte hain. " +
                            "Aap casual Hindi, Hindi-English mixed speech (Hinglish) aur aam bolchal ko aasaani se samajhte hain chahe grammar thoda idhar udhar ho. " +
                            "Agar user Hindi ya Hinglish bole, to hamesha natural, aasan Hindi mein hi jawab dein. English mein tabhi switch karein jab user specifically English mein bolne ko kahe. " +
                            "Jawab chhota, saaf, dostana aur natural rakhein (1 se 2 sentences maximum). " +
                            "Kabhi bhi robotic, scripted ya zyada formal Hindi mat boliye. " +
                            "Devanagari Hindi ya natural conversational Hindi mein seedha bolne laayak jawab dein. " +
                            "Markdown formatting jaise asterisks (*), hash (#), ya bullets bilkul na use karein kyunki text ko Text-to-Speech se bol kar sunaya jayega."
                )
            )
        )

        val requestPayload = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            systemInstruction = systemInstruction,
            generationConfig = GeminiGenerationConfig(temperature = 0.7f, maxOutputTokens = 120)
        )

        val jsonString = requestAdapter.toJson(requestPayload)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(jsonString.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiService", "API error: ${response.code} $responseBody")
                return@withContext getLocalConversationalFallback(prompt)
            }

            val parsed = responseAdapter.fromJson(responseBody)
            val text = parsed?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            if (!text.isNullOrBlank()) {
                // Strip markdown formatting if any was generated
                text.replace("*", "").replace("#", "").trim()
            } else {
                getLocalConversationalFallback(prompt)
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Gemini request failed: ${e.message}", e)
            getLocalConversationalFallback(prompt)
        }
    }

    private fun getLocalConversationalFallback(prompt: String): String {
        val lower = prompt.lowercase().trim()
        val isEnglishRequested = lower.contains("speak in english") || lower.contains("in english")
        if (isEnglishRequested) {
            return "Sure! I can speak in English as well. How can I assist you?"
        }

        return when {
            lower.contains("who are you") || lower.contains("kaun ho") || lower.contains("naam kya hai") || lower.contains("what is your name") || lower.contains("कौन हो") || lower.contains("नाम क्या है") ->
                "मैं ऑरा हूँ, आपकी AI वॉयस असिस्टेंट। मैं ऐप्स खोल सकती हूँ, कॉल उठा और काट सकती हूँ, मैसेज पढ़ सकती हूँ और फोन के कंट्रोल्स संभाल सकती हूँ।"
            lower.contains("how are you") || lower.contains("kaisi ho") || lower.contains("kaise ho") || lower.contains("कैसी हो") || lower.contains("कैसे हो") ->
                "मैं बिल्कुल ठीक हूँ! कहिए, मैं आपकी क्या मदद करूँ?"
            lower.contains("what can you do") || lower.contains("help") || lower.contains("madad") || lower.contains("kya kar sakti ho") || lower.contains("क्या कर सकती") ->
                "आप मुझसे यूट्यूब या व्हाट्सएप खोलने को कह सकते हैं, कॉल उठा या काट सकते हैं, मैसेज सुन सकते हैं, टॉर्च जला सकते हैं या बैटरी चेक कर सकते हैं।"
            lower.contains("time") || lower.contains("samay") || lower.contains("waqt") || lower.contains("kitne baje") || lower.contains("समय") || lower.contains("कितने बजे") ->
                "अभी समय " + java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date()) + " हुआ है।"
            lower.contains("date") || lower.contains("aaj ka din") || lower.contains("tarikh") || lower.contains("तारीख") ->
                "आज " + java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale("hi", "IN")).format(java.util.Date()) + " है।"
            lower.contains("thank") || lower.contains("shukriya") || lower.contains("dhanyawad") || lower.contains("शुक्रिया") || lower.contains("धन्यवाद") ->
                "आपका स्वागत है! जब भी ज़रूरत हो, बस बोलिए।"
            lower.contains("namaste") || lower.contains("hello") || lower.contains("hi") || lower.contains("hey") || lower.contains("नमस्ते") || lower.contains("हेलो") ->
                "नमस्ते! कहिए, मैं आपकी क्या मदद कर सकती हूँ?"
            else ->
                "मैंने सुन लिया: $prompt। अगर आप कोई ऐप खोलना, कॉल उठाना या मैसेज पढ़ना चाहते हैं तो बस बोलिए।"
        }
    }
}
