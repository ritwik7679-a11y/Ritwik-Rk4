package com.example.voice

import com.example.data.model.ParsedIntent
import com.example.data.model.SettingsType
import com.example.data.model.VolumeAction
import java.util.regex.Pattern

object NluIntentParser {

    /**
     * Parses user utterance into a strongly-typed intent.
     * Supports natural conversational Hindi by default, casual Hinglish,
     * Hindi-English mixed speech, and grammatically imperfect commands.
     */
    fun parse(rawText: String, isAwaitingConfirmation: Boolean = false): ParsedIntent {
        val text = rawText.trim()
        // Retain alphanumeric, Devanagari characters (\u0900-\u097F), colons, percentage, and spaces
        val lower = text.lowercase()
            .replace(Regex("[^a-z0-9\\u0900-\\u097F\\s:%]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        // If currently awaiting confirmation for a sensitive action
        if (isAwaitingConfirmation) {
            return when {
                // Hindi & Hinglish affirmative: haan, haanji, theek hai, bhejo, karo, laga do, kar do, bilkul, sahi hai, etc.
                matchesAny(lower,
                    "haan", "haanji", "han", "hanji", "theek hai", "bhejo", "karo", "laga do", "laga", "sahi hai",
                    "chalega", "kar do", "haan kar do", "haan bhejo", "haan lagao", "bilkul", "bhej do", "call karo",
                    "call lagao", "laga do call", "phone lagao", "yes", "confirm", "sure", "proceed", "do it",
                    "go ahead", "yep", "yeah", "ok", "okay",
                    "हाँ", "हाँजी", "हाँ जी", "हा", "ठीक है", "भेजो", "करो", "लगा दो", "सही है", "कर दो", "बिल्कुल",
                    "हाँ कर दो", "भेज दो", "कॉल करो", "हाँ भेजो") ->
                    ParsedIntent.ConfirmAction(true)

                // Hindi & Hinglish negative: nahi, nahin, mat karo, cancel, ruko, rehne do, na, mat bhejo, etc.
                matchesAny(lower,
                    "nahi", "nahin", "mat karo", "ruko", "rehne do", "na", "mat lagao", "mat bhejo", "mat kar",
                    "ruk jao", "chhod do", "no", "cancel", "stop", "abort", "don't", "nope", "nah", "cancel karo",
                    "drop karo",
                    "नहीं", "मत करो", "कैंसिल", "रुक जाओ", "रहने दो", "ना", "मत लगाओ", "मत भेजो", "रद्द करो",
                    "छोड़ दो", "रुको", "मत कर") ->
                    ParsedIntent.ConfirmAction(false)

                else -> ParsedIntent.Unknown(text)
            }
        }

        // 1. Incoming Call Voice Commands (Answer / Reject)
        // Hindi & Hinglish: call uthao, phone uthao, uthao, receive karo, call pick karo, baat karni hai, उठाओ, कॉल उठाओ
        if (matchesAny(lower,
                "call uthao", "phone uthao", "uthao", "utha lo", "call utha lo", "phone utha lo", "receive karo",
                "call receive karo", "phone receive karo", "pick karo", "call pick karo", "call pick kar",
                "call uthana hai", "baat karo", "baat karni hai", "call connect karo", "call utha do",
                "answer", "pickup", "pick up", "take call", "accept call", "answer the call", "answer call",
                "कॉल उठाओ", "फोन उठाओ", "उठाओ", "उठा लो", "कॉल उठा लो", "रिसीव करो", "कॉल रिसीव करो",
                "आंसर करो", "बात करो", "कॉल एक्सेप्ट करो", "फोन रिसीव करो", "उठा लीजिए")) {
            return ParsedIntent.AnswerCall
        }

        // Hindi & Hinglish: call kaato, call kato, phone kaato, kaato, cut karo, reject karo, mat uthao, काटो, कॉल काटो
        if (matchesAny(lower,
                "call kaato", "call kato", "phone kaato", "phone kato", "kaato", "kato", "kaat do", "kat do",
                "cut call", "call cut", "phone cut", "cut karo", "call cut karo", "phone cut karo", "mat uthao",
                "reject karo", "call reject karo", "reject call", "reject the call", "decline karo",
                "call decline karo", "decline call", "call disconnect karo", "disconnect call", "nahi uthana",
                "nahi baat karni", "drop call", "hang up", "dismiss call",
                "कॉल काटो", "फोन काटो", "काटो", "काट दो", "कट करो", "कॉल कट करो", "रिजेक्ट करो", "कॉल रिजेक्ट करो",
                "मत उठाओ", "डिक्लाइन करो", "कॉल डिसकनेक्ट करो", "फोन कट करो", "काट दीजिए")) {
            return ParsedIntent.RejectCall
        }

        // 2. Close / Exit Application
        // Hindi: app band karo, band karo, band kar do, bahar aao, bahar niklo, home screen par jao, बंद करो, बाहर आओ
        if (matchesAny(lower,
                "app band karo", "band karo", "band kar do", "ise band karo", "ye app band karo", "bahar aao",
                "bahar niklo", "bahar nikal jao", "home screen par jao", "home jao", "home par jao", "home screen",
                "exit karo", "close app", "close the app", "exit app", "exit the app", "quit app", "quit this app",
                "go home", "exit", "hatao ise",
                "ऐप बंद करो", "बंद करो", "बंद कर दो", "इसे बंद करो", "बाहर आओ", "बाहर निकलो", "होम जाओ",
                "होम स्क्रीन पर जाओ", "हटाओ इसे", "एग्जिट करो", "होम पर जाओ")) {
            val target = when {
                lower.contains("youtube") || lower.contains("यूट्यूब") -> "YouTube"
                lower.contains("whatsapp") || lower.contains("व्हाट्सएप") -> "WhatsApp"
                lower.contains("chrome") || lower.contains("क्रोम") -> "Chrome"
                else -> null
            }
            return ParsedIntent.CloseApp(target)
        }

        // 3. Read Incoming Messages / SMS
        // Hindi: messages padho, message sunao, sms padho, kya message aaya hai, kisne message bheja, मैसेज पढ़ो, मैसेज सुनाओ
        val isMessageIntent = (
            lower.contains("message") || lower.contains("sms") || lower.contains("text") ||
            lower.contains("मैसेज") || lower.contains("एसएमएस") || lower.contains("संदेश")
        ) && (
            lower.contains("padho") || lower.contains("sunao") || lower.contains("read") ||
            lower.contains("batao") || lower.contains("check") || lower.contains("kya hai") ||
            lower.contains("padh") || lower.contains("सुनाओ") || lower.contains("पढ़ो") ||
            lower.contains("बताओ") || lower.contains("चेक") || lower.contains("क्या है")
        )

        if (isMessageIntent) {
            // Check if sender query is specified e.g., "Rohan ka message padho" or "read messages from Rohan"
            val fromPattern = Pattern.compile("(?:from\\s+|from\\s+)([a-zA-Z0-9\\u0900-\\u097F]+)")
            val kaPattern = Pattern.compile("([a-zA-Z0-9\\u0900-\\u097F]+)\\s+(?:ka|ke|ki|ne|का|के|की|ने)\\s+(?:message|sms|मैसेज|क्या)")
            val fromMatcher = fromPattern.matcher(text)
            val kaMatcher = kaPattern.matcher(text)

            val sender = when {
                fromMatcher.find() -> fromMatcher.group(1)
                kaMatcher.find() -> kaMatcher.group(1)
                else -> null
            }
            return ParsedIntent.ReadMessages(senderQuery = sender)
        }

        if (matchesAny(lower,
                "who texted me", "who sent message", "who sent me a message", "check messages", "check sms",
                "read my texts", "read last message", "read messages", "read incoming messages",
                "kisne message bheja", "kiska message aaya", "kiska message aaya hai", "message check karo",
                "sms check karo", "naya message kya hai", "koi message aaya hai kya", "messages batao",
                "message padh kar batao", "मैसेज पढ़ो", "मैसेज सुनाओ", "एसएमएस पढ़ो", "किसने मैसेज भेजा",
                "किसका मैसेज आया है", "मैसेज चेक करो", "नया मैसेज बताओ", "कोई मैसेज आया है क्या")) {
            return ParsedIntent.ReadMessages()
        }

        // 4. Open / Launch Applications
        // Natural Hindi: "youtube kholo", "whatsapp chalao", "chrome khol do", "youtube dekhna hai", "kholo youtube", "यूट्यूब खोलो"
        val openAppSuffixPattern = Pattern.compile("(.+?)\\s+(?:kholo|khol do|kholna hai|chalao|chala do|chalu karo|chalu kar do|open karo|open kar do|start karo|laga do|dekhna hai|kholey|खोलो|खोल दो|खोलना है|चलाओ|चला दो|चालू करो|चालू कर दो|ओपन करो|स्टार्ट करो|लगा दो|देखना है)$")
        val suffixMatcher = openAppSuffixPattern.matcher(lower)
        if (suffixMatcher.find()) {
            val appQuery = suffixMatcher.group(1)?.trim() ?: ""
            if (appQuery.isNotEmpty() && !matchesAny(appQuery,
                    "flashlight", "torch", "light", "settings", "wifi", "wi-fi", "bluetooth",
                    "sound", "volume", "टॉर्च", "लाइट", "सेटिंग्स", "वाईफाई", "ब्लूटूथ", "आवाज़")) {
                return ParsedIntent.OpenApp(normalizeAppName(appQuery))
            }
        }

        val openAppPrefixPattern = Pattern.compile("^(?:please\\s+|kripya\\s+|ek baar\\s+|mujhe\\s+)?(?:open|launch|start|run|go to|kholo|chalao|chalu karo|open karo|खोलो|चलाओ|चालू करो)\\s+(?:the\\s+app\\s+)?(.+)")
        val prefixMatcher = openAppPrefixPattern.matcher(lower)
        if (prefixMatcher.find()) {
            val appQuery = prefixMatcher.group(1)?.trim() ?: ""
            if (appQuery.isNotEmpty() && !matchesAny(appQuery,
                    "flashlight", "torch", "camera flash", "light", "settings", "wifi", "wi-fi", "bluetooth",
                    "sound", "volume", "टॉर्च", "लाइट", "सेटिंग्स", "वाईफाई", "ब्लूटूथ", "आवाज़")) {
                return ParsedIntent.OpenApp(normalizeAppName(appQuery))
            }
        }

        // 5. Phone Call to Contact (Sensitive - requires confirmation)
        // Hindi: "[contact] ko call karo", "[contact] ko phone lagao", "[contact] se baat karao", "call [contact]"
        val hindiCallPattern = Pattern.compile("(.+?)\\s+ko\\s+(?:call|phone|dial)\\s+(?:karo|lagao|karna|laga do|milao|kar do)")
        val hindiDevanagariCallPattern = Pattern.compile("(.+?)\\s+को\\s+(?:कॉल|फोन|डायल)\\s+(?:करो|लगाओ|कर दो|लगा दो|मिलाओ)")
        val engCallPattern = Pattern.compile("^(?:please\\s+)?(?:call|phone|dial|ring)\\s+(?:to\\s+)?(.+)")

        val hMatcher = hindiCallPattern.matcher(lower)
        val devMatcher = hindiDevanagariCallPattern.matcher(lower)
        val eMatcher = engCallPattern.matcher(lower)

        val callTarget = when {
            hMatcher.find() -> hMatcher.group(1)?.trim()
            devMatcher.find() -> devMatcher.group(1)?.trim()
            eMatcher.find() -> eMatcher.group(1)?.trim()
            else -> null
        }

        if (!callTarget.isNullOrBlank() && !callTarget.contains("police") && !callTarget.contains("emergency")) {
            return ParsedIntent.CallContact(callTarget)
        }

        // 6. Send SMS to Contact (Sensitive - requires confirmation)
        // Hindi: "[contact] ko message bhejo ki [text]"
        val hindiSmsPattern = Pattern.compile("(.+?)\\s+ko\\s+(?:message|sms|text|bolo|kah do)\\s+(?:bhejo|karo|bhej do|ki|saying)?\\s*(?:ki|that)?\\s*(.+)", Pattern.CASE_INSENSITIVE)
        val devSmsPattern = Pattern.compile("(.+?)\\s+को\\s+(?:मैसेज|एसएमएस|संदेश|बोलो)\\s+(?:भेजो|करो|भेज दो|कि)?\\s*(?:कि)?\\s*(.+)", Pattern.CASE_INSENSITIVE)
        val engSmsPattern = Pattern.compile("^(?:send\\s+(?:a\\s+)?(?:sms|message|text)\\s+to\\s+|text\\s+)(.+?)(?:\\s+saying\\s+|\\s+that\\s+|\\s+message\\s+)(.+)", Pattern.CASE_INSENSITIVE)

        val hsMatcher = hindiSmsPattern.matcher(text)
        val dsMatcher = devSmsPattern.matcher(text)
        val esMatcher = engSmsPattern.matcher(text)

        when {
            hsMatcher.find() -> {
                val r = hsMatcher.group(1)?.trim() ?: ""
                val b = hsMatcher.group(2)?.trim() ?: ""
                if (r.isNotEmpty() && b.isNotEmpty()) return ParsedIntent.SendSms(r, b)
            }
            dsMatcher.find() -> {
                val r = dsMatcher.group(1)?.trim() ?: ""
                val b = dsMatcher.group(2)?.trim() ?: ""
                if (r.isNotEmpty() && b.isNotEmpty()) return ParsedIntent.SendSms(r, b)
            }
            esMatcher.find() -> {
                val r = esMatcher.group(1)?.trim() ?: ""
                val b = esMatcher.group(2)?.trim() ?: ""
                if (r.isNotEmpty() && b.isNotEmpty()) return ParsedIntent.SendSms(r, b)
            }
        }

        // 7. Flashlight / Torch Controls
        // Hindi: torch jalao, light jalao, flashlight chalu karo, torch band karo, टॉर्च जलाओ, टॉर्च बंद करो
        if (matchesAny(lower,
                "turn on flashlight", "flashlight on", "torch on", "enable flashlight", "turn on torch", "light on",
                "torch jalao", "light jalao", "flashlight jalao", "torch chalu karo", "flashlight chalu karo",
                "light on karo", "torch on karo", "flashlight on karo", "light chalu karo", "andhera hai",
                "टॉर्च जलाओ", "लाइट जलाओ", "टॉर्च चालू करो", "फ्लैशलाइट जलाओ", "लाइट चालू करो", "टॉर्च ऑन करो")) {
            return ParsedIntent.ToggleFlashlight(true)
        }
        if (matchesAny(lower,
                "turn off flashlight", "flashlight off", "torch off", "disable flashlight", "turn off torch", "light off",
                "torch band karo", "light band karo", "flashlight band karo", "torch bujhao", "torch bujha do",
                "light off karo", "torch off karo", "flashlight off karo",
                "टॉर्च बंद करो", "लाइट बंद करो", "फ्लैशलाइट बंद करो", "टॉर्च बुझाओ", "टॉर्च बुझा दो", "टॉर्च ऑफ करो")) {
            return ParsedIntent.ToggleFlashlight(false)
        }

        // 8. Volume Controls
        // Hindi: mute karo, aawaz band karo, silent karo, aawaz badhao, volume badhao, aawaz kam karo, volume kam karo
        if (matchesAny(lower,
                "mute", "silence phone", "mute sound", "mute media", "mute audio", "aawaz band karo",
                "silent karo", "mute karo", "आवाज़ बंद करो", "म्यूट करो", "साइलेंट करो")) {
            return ParsedIntent.SetVolume(VolumeAction.MUTE)
        }
        if (matchesAny(lower,
                "unmute", "unmute sound", "turn sound on", "aawaz chalu karo", "aawaz kholo", "unmute karo",
                "आवाज़ चालू करो", "आवाज़ खोलो", "अनम्यूट करो")) {
            return ParsedIntent.SetVolume(VolumeAction.UNMUTE)
        }
        if (matchesAny(lower,
                "volume up", "louder", "turn it up", "increase volume", "raise volume", "aawaz badhao",
                "volume badhao", "aawaz tez karo", "volume tez karo", "thoda tez bolo",
                "आवाज़ बढ़ाओ", "वॉल्यूम बढ़ाओ", "आवाज़ तेज़ करो", "थोड़ा तेज़ बोलो")) {
            return ParsedIntent.SetVolume(VolumeAction.UP)
        }
        if (matchesAny(lower,
                "volume down", "quieter", "turn it down", "lower volume", "decrease volume", "aawaz kam karo",
                "volume kam karo", "dheemi karo", "aawaz dheemi karo", "dheere bolo",
                "आवाज़ कम करो", "वॉल्यूम कम करो", "आवाज़ धीमी करो", "धीमे बोलो")) {
            return ParsedIntent.SetVolume(VolumeAction.DOWN)
        }
        val volPctPattern = Pattern.compile("(?:volume|aawaz|आवाज़|वॉल्यूम)\\s*(?:to|ko|par)?\\s*(\\d+)\\s*(?:percent|%|karo|करो)?")
        val volMatcher = volPctPattern.matcher(lower)
        if (volMatcher.find()) {
            val pct = volMatcher.group(1)?.toIntOrNull()
            return ParsedIntent.SetVolume(VolumeAction.SET, pct)
        }

        // 9. Battery Check
        // Hindi: battery kitni hai, kitna charge hai, battery check karo, battery kitna percent hai, बैटरी कितनी है, चार्ज कितना है
        if (matchesAny(lower,
                "battery", "check battery", "battery status", "battery level", "battery percentage", "how much battery",
                "battery kitni hai", "battery check karo", "kitna charge hai", "charge kitna hai",
                "phone kitna charge hai", "battery kitna percent hai", "phone mein battery kitni hai",
                "बैटरी कितनी है", "चार्ज कितना है", "बैटरी चेक करो", "बैटरी का स्टेटस", "फोन कितना चार्ज है",
                "बैटरी प्रतिशत बताओ")) {
            return ParsedIntent.CheckBattery
        }

        // 10. Alarm & Timers
        if (lower.contains("timer") || lower.contains("टाइमर")) {
            val timerPattern = Pattern.compile("(\\d+)\\s*(minute|second|min|sec|मिनट|सेकंड)")
            val tMatcher = timerPattern.matcher(lower)
            if (tMatcher.find()) {
                val amount = tMatcher.group(1)?.toIntOrNull() ?: 5
                val unit = tMatcher.group(2) ?: "minute"
                val totalSec = if (unit.startsWith("m") || unit.startsWith("मि")) amount * 60 else amount
                return ParsedIntent.SetTimer(totalSec)
            }
            return ParsedIntent.SetTimer(300)
        }

        if (lower.contains("alarm") || lower.contains("अलार्म")) {
            val alarmPattern = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|baje|बजे)?")
            val aMatcher = alarmPattern.matcher(lower)
            if (aMatcher.find()) {
                var hour = aMatcher.group(1)?.toIntOrNull() ?: 7
                val min = aMatcher.group(2)?.toIntOrNull() ?: 0
                val amPm = aMatcher.group(3)
                if (amPm == "pm" && hour < 12) hour += 12
                return ParsedIntent.SetAlarm(hour, min)
            }
            return ParsedIntent.SetAlarm(7, 0)
        }

        // 11. Open Settings
        if (lower.contains("settings") || lower.contains("सेटिंग्स")) {
            return when {
                lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("वाईफाई") -> ParsedIntent.OpenSettings(SettingsType.WIFI)
                lower.contains("bluetooth") || lower.contains("ब्लूटूथ") -> ParsedIntent.OpenSettings(SettingsType.BLUETOOTH)
                lower.contains("sound") || lower.contains("volume") || lower.contains("आवाज़") -> ParsedIntent.OpenSettings(SettingsType.SOUND)
                lower.contains("display") || lower.contains("screen") || lower.contains("डिस्प्ले") -> ParsedIntent.OpenSettings(SettingsType.DISPLAY)
                lower.contains("accessibility") -> ParsedIntent.OpenSettings(SettingsType.ACCESSIBILITY)
                else -> ParsedIntent.OpenSettings(SettingsType.GENERAL)
            }
        }
        if (matchesAny(lower, "wifi kholo", "bluetooth kholo", "wifi chalu karo", "वाईफाई खोलो", "ब्लूटूथ खोलो")) {
            return if (lower.contains("wifi") || lower.contains("वाईफाई")) ParsedIntent.OpenSettings(SettingsType.WIFI) else ParsedIntent.OpenSettings(SettingsType.BLUETOOTH)
        }

        // 12. Conversational / General Knowledge / Gemini AI (natural Hindi by default)
        return ParsedIntent.ConversationalQuery(text)
    }

    private fun normalizeAppName(raw: String): String {
        val lower = raw.lowercase().trim()
        return when {
            lower.contains("youtube") || lower.contains("यूट्यूब") || lower == "yt" -> "YouTube"
            lower.contains("whatsapp") || lower.contains("व्हाट्सएप") || lower.contains("व्हाट्सऐप") || lower == "wp" -> "WhatsApp"
            lower.contains("chrome") || lower.contains("क्रोम") || lower.contains("browser") || lower.contains("ब्राउज़र") -> "Chrome"
            lower.contains("camera") || lower.contains("कैमरा") || lower.contains("photo") || lower.contains("फोटो") -> "Camera"
            lower.contains("maps") || lower.contains("map") || lower.contains("मैप्स") || lower.contains("मैप") -> "Maps"
            lower.contains("gmail") || lower.contains("ईमेल") || lower.contains("mail") || lower.contains("मेल") -> "Gmail"
            lower.contains("settings") || lower.contains("सेटिंग्स") -> "Settings"
            lower.contains("calculator") || lower.contains("कैलकुलेटर") || lower == "calc" || lower.contains("हिसाब") -> "Calculator"
            lower.contains("clock") || lower.contains("घड़ी") -> "Clock"
            lower.contains("play store") || lower.contains("प्ले स्टोर") || lower.contains("playstore") -> "Play Store"
            lower.contains("photos") || lower.contains("gallery") || lower.contains("गैलरी") -> "Photos"
            lower.contains("spotify") || lower.contains("स्पॉटिफाई") || lower.contains("music") || lower.contains("गाना") -> "Spotify"
            lower.contains("instagram") || lower.contains("इंस्टाग्राम") || lower == "insta" -> "Instagram"
            lower.contains("facebook") || lower.contains("फेसबुक") || lower == "fb" -> "Facebook"
            lower.contains("phonepe") || lower.contains("फोन पे") -> "PhonePe"
            lower.contains("gpay") || lower.contains("गूगल पे") || lower.contains("google pay") -> "Google Pay"
            lower.contains("paytm") || lower.contains("पेटीएम") -> "Paytm"
            lower.contains("telegram") || lower.contains("टेलीग्राम") -> "Telegram"
            else -> raw
        }
    }

    private fun matchesAny(text: String, vararg phrases: String): Boolean {
        return phrases.any { phrase ->
            text == phrase || text.startsWith("$phrase ") || text.endsWith(" $phrase") || text.contains(" $phrase ")
        }
    }
}
