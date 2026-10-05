package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ParsedIntent
import com.example.data.model.VolumeAction
import com.example.voice.NluIntentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Aura Voice", appName)
    }

    @Test
    fun `test NLU open app intent`() {
        val intent = NluIntentParser.parse("Open YouTube")
        assertTrue(intent is ParsedIntent.OpenApp)
        assertEquals("youtube", (intent as ParsedIntent.OpenApp).targetAppName.lowercase())
    }

    @Test
    fun `test NLU call answer and reject`() {
        val answer = NluIntentParser.parse("Answer the call")
        assertTrue(answer is ParsedIntent.AnswerCall)

        val reject = NluIntentParser.parse("Reject the call")
        assertTrue(reject is ParsedIntent.RejectCall)
    }

    @Test
    fun `test NLU read messages intent`() {
        val intent = NluIntentParser.parse("Read incoming messages")
        assertTrue(intent is ParsedIntent.ReadMessages)
    }

    @Test
    fun `test NLU volume and flashlight`() {
        val vol = NluIntentParser.parse("Volume to 75%")
        assertTrue(vol is ParsedIntent.SetVolume)
        assertEquals(75, (vol as ParsedIntent.SetVolume).percent)

        val flash = NluIntentParser.parse("Turn on flashlight")
        assertTrue(flash is ParsedIntent.ToggleFlashlight)
        assertTrue((flash as ParsedIntent.ToggleFlashlight).enable)
    }

    @Test
    fun `test NLU confirmation handling`() {
        val confirmed = NluIntentParser.parse("Yes proceed", isAwaitingConfirmation = true)
        assertTrue(confirmed is ParsedIntent.ConfirmAction)
        assertTrue((confirmed as ParsedIntent.ConfirmAction).confirmed)

        val cancelled = NluIntentParser.parse("Cancel", isAwaitingConfirmation = true)
        assertTrue(cancelled is ParsedIntent.ConfirmAction)
        assertEquals(false, (cancelled as ParsedIntent.ConfirmAction).confirmed)
    }

    @Test
    fun `test NLU Hindi conversational commands`() {
        // App Launch in Hindi & Hinglish
        val ytIntent = NluIntentParser.parse("YouTube kholo")
        assertTrue(ytIntent is ParsedIntent.OpenApp)
        assertEquals("youtube", (ytIntent as ParsedIntent.OpenApp).targetAppName.lowercase())

        val devYt = NluIntentParser.parse("यूट्यूब खोलो")
        assertTrue(devYt is ParsedIntent.OpenApp)
        assertEquals("youtube", (devYt as ParsedIntent.OpenApp).targetAppName.lowercase())

        val wpIntent = NluIntentParser.parse("WhatsApp chalao")
        assertTrue(wpIntent is ParsedIntent.OpenApp)
        assertEquals("whatsapp", (wpIntent as ParsedIntent.OpenApp).targetAppName.lowercase())

        // Call Answer & Reject in Hindi
        val ansHindi = NluIntentParser.parse("कॉल उठाओ")
        assertTrue(ansHindi is ParsedIntent.AnswerCall)

        val ansHinglish = NluIntentParser.parse("call uthao")
        assertTrue(ansHinglish is ParsedIntent.AnswerCall)

        val rejHindi = NluIntentParser.parse("कॉल काटो")
        assertTrue(rejHindi is ParsedIntent.RejectCall)

        val rejHinglish = NluIntentParser.parse("call kaato")
        assertTrue(rejHinglish is ParsedIntent.RejectCall)

        // Read messages in Hindi
        val msgHindi = NluIntentParser.parse("मैसेज पढ़ो")
        assertTrue(msgHindi is ParsedIntent.ReadMessages)

        val msgHinglish = NluIntentParser.parse("messages padho")
        assertTrue(msgHinglish is ParsedIntent.ReadMessages)

        // Torch in Hindi
        val torchOn = NluIntentParser.parse("टॉर्च जलाओ")
        assertTrue(torchOn is ParsedIntent.ToggleFlashlight)
        assertTrue((torchOn as ParsedIntent.ToggleFlashlight).enable)

        val torchOff = NluIntentParser.parse("टॉर्च बंद करो")
        assertTrue(torchOff is ParsedIntent.ToggleFlashlight)
        assertEquals(false, (torchOff as ParsedIntent.ToggleFlashlight).enable)

        // Battery in Hindi
        val batteryHindi = NluIntentParser.parse("बैटरी कितनी है")
        assertTrue(batteryHindi is ParsedIntent.CheckBattery)

        // Volume in Hindi
        val volUp = NluIntentParser.parse("आवाज़ बढ़ाओ")
        assertTrue(volUp is ParsedIntent.SetVolume)
        assertEquals(VolumeAction.UP, (volUp as ParsedIntent.SetVolume).action)

        // Confirmation in Hindi
        val confHindi = NluIntentParser.parse("हाँ कर दो", isAwaitingConfirmation = true)
        assertTrue(confHindi is ParsedIntent.ConfirmAction)
        assertTrue((confHindi as ParsedIntent.ConfirmAction).confirmed)

        val rejConf = NluIntentParser.parse("नहीं मत करो", isAwaitingConfirmation = true)
        assertTrue(rejConf is ParsedIntent.ConfirmAction)
        assertEquals(false, (rejConf as ParsedIntent.ConfirmAction).confirmed)
    }
}
