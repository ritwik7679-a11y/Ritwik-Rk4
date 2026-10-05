package com.example

import com.example.data.model.ParsedIntent
import com.example.data.model.VolumeAction
import com.example.voice.NluIntentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testHindiAppOpenIntent() {
        val intent1 = NluIntentParser.parse("यूट्यूब खोलो")
        assertTrue(intent1 is ParsedIntent.OpenApp)
        assertEquals("YouTube", (intent1 as ParsedIntent.OpenApp).targetAppName)

        val intent2 = NluIntentParser.parse("whatsapp chalao")
        assertTrue(intent2 is ParsedIntent.OpenApp)
        assertEquals("WhatsApp", (intent2 as ParsedIntent.OpenApp).targetAppName)

        val intent3 = NluIntentParser.parse("kholo chrome")
        assertTrue(intent3 is ParsedIntent.OpenApp)
        assertEquals("Chrome", (intent3 as ParsedIntent.OpenApp).targetAppName)

        val intent4 = NluIntentParser.parse("कैमरा खोल दो")
        assertTrue(intent4 is ParsedIntent.OpenApp)
        assertEquals("Camera", (intent4 as ParsedIntent.OpenApp).targetAppName)
    }

    @Test
    fun testHindiCallAnsweringAndRejection() {
        val answer1 = NluIntentParser.parse("कॉल उठाओ")
        assertEquals(ParsedIntent.AnswerCall, answer1)

        val answer2 = NluIntentParser.parse("phone uthao")
        assertEquals(ParsedIntent.AnswerCall, answer2)

        val answer3 = NluIntentParser.parse("call receive karo")
        assertEquals(ParsedIntent.AnswerCall, answer3)

        val reject1 = NluIntentParser.parse("कॉल काटो")
        assertEquals(ParsedIntent.RejectCall, reject1)

        val reject2 = NluIntentParser.parse("phone cut karo")
        assertEquals(ParsedIntent.RejectCall, reject2)

        val reject3 = NluIntentParser.parse("call mat uthao")
        assertEquals(ParsedIntent.RejectCall, reject3)
    }

    @Test
    fun testHindiMessageReading() {
        val msg1 = NluIntentParser.parse("मैसेज पढ़ो")
        assertTrue(msg1 is ParsedIntent.ReadMessages)

        val msg2 = NluIntentParser.parse("kiska message aaya hai")
        assertTrue(msg2 is ParsedIntent.ReadMessages)

        val msg3 = NluIntentParser.parse("रोहन का मैसेज पढ़ो")
        assertTrue(msg3 is ParsedIntent.ReadMessages)
    }

    @Test
    fun testHindiHardwareControls() {
        val torchOn = NluIntentParser.parse("टॉर्च जलाओ")
        assertEquals(ParsedIntent.ToggleFlashlight(true), torchOn)

        val torchOff = NluIntentParser.parse("light band karo")
        assertEquals(ParsedIntent.ToggleFlashlight(false), torchOff)

        val volUp = NluIntentParser.parse("आवाज़ बढ़ाओ")
        assertEquals(ParsedIntent.SetVolume(VolumeAction.UP), volUp)

        val volDown = NluIntentParser.parse("volume kam karo")
        assertEquals(ParsedIntent.SetVolume(VolumeAction.DOWN), volDown)

        val mute = NluIntentParser.parse("म्यूट करो")
        assertEquals(ParsedIntent.SetVolume(VolumeAction.MUTE), mute)

        val battery = NluIntentParser.parse("बैटरी कितनी है")
        assertEquals(ParsedIntent.CheckBattery, battery)
    }

    @Test
    fun testHindiConfirmationHandling() {
        val confYes1 = NluIntentParser.parse("हाँ", isAwaitingConfirmation = true)
        assertEquals(ParsedIntent.ConfirmAction(true), confYes1)

        val confYes2 = NluIntentParser.parse("theek hai", isAwaitingConfirmation = true)
        assertEquals(ParsedIntent.ConfirmAction(true), confYes2)

        val confNo1 = NluIntentParser.parse("नहीं", isAwaitingConfirmation = true)
        assertEquals(ParsedIntent.ConfirmAction(false), confNo1)

        val confNo2 = NluIntentParser.parse("mat karo", isAwaitingConfirmation = true)
        assertEquals(ParsedIntent.ConfirmAction(false), confNo2)
    }

    @Test
    fun testCloseAppIntent() {
        val close1 = NluIntentParser.parse("ऐप बंद करो")
        assertTrue(close1 is ParsedIntent.CloseApp)

        val close2 = NluIntentParser.parse("bahar niklo")
        assertTrue(close2 is ParsedIntent.CloseApp)
    }
}
