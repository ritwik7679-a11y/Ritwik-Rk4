package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssistantState
import com.example.data.model.CommandCategory
import com.example.ui.MainViewModel
import com.example.ui.components.ConfirmationCard
import com.example.ui.components.IncomingCallBanner
import com.example.ui.components.VoiceOrbVisualizer
import com.example.ui.components.VoiceTranscriptItem
import com.example.ui.theme.AuraAmber
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraEmerald
import com.example.ui.theme.AuraRose
import com.example.ui.theme.AuraViolet
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun VoiceAssistantScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val assistantState by viewModel.assistantState.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val activeCallState by viewModel.activeCallState.collectAsState()
    val continuousListening by viewModel.continuousListening.collectAsState()

    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    var textInput by remember { mutableStateOf("") }

    // Auto-scroll to bottom of conversation
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Top Header: App Branding & Continuous Mode Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (assistantState) {
                                    is AssistantState.Listening -> AuraCyan
                                    is AssistantState.Speaking -> AuraViolet
                                    is AssistantState.AwaitingConfirmation -> AuraAmber
                                    else -> AuraEmerald
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AURA VOICE",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Continuous Listening Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceCard)
                        .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = "Hands-free Mode",
                        tint = if (continuousListening) AuraCyan else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "हैंड्स-फ्री",
                        color = if (continuousListening) AuraCyan else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = continuousListening,
                        onCheckedChange = { viewModel.toggleContinuousListening() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AuraCyan,
                            checkedTrackColor = AuraCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = DarkBorder
                        ),
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("hands_free_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Incoming Call Overlay (if ringing)
            IncomingCallBanner(
                callState = activeCallState,
                onAnswer = { viewModel.answerIncomingCall() },
                onReject = { viewModel.rejectIncomingCall() }
            )

            // Sensitive Action Confirmation Card (if waiting)
            if (assistantState is AssistantState.AwaitingConfirmation) {
                ConfirmationCard(
                    awaitingState = assistantState as AssistantState.AwaitingConfirmation,
                    onConfirm = { viewModel.confirmSensitiveAction(true) },
                    onCancel = { viewModel.confirmSensitiveAction(false) }
                )
            }

            // Central Voice Orb Hero Section
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    VoiceOrbVisualizer(
                        assistantState = assistantState,
                        isRinging = activeCallState.isRinging,
                        onClick = { viewModel.toggleListening() }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status Indicator
                    val (statusColor, statusText) = when (val state = assistantState) {
                        is AssistantState.Listening -> Pair(
                            AuraCyan,
                            if (state.partialText.isNotBlank()) "\"${state.partialText}...\"" else "सुन रही हूँ... कहिए"
                        )
                        is AssistantState.Speaking -> Pair(AuraViolet, "बोल रही हूँ...")
                        is AssistantState.Processing -> Pair(AuraCyan, "समझ रही हूँ: \"${state.recognizedText}\"")
                        is AssistantState.AwaitingConfirmation -> Pair(AuraAmber, "आपकी पुष्टि चाहिए...")
                        is AssistantState.Error -> Pair(AuraRose, state.message)
                        AssistantState.Idle -> Pair(TextSecondary, "माइक दबाकर बोलिए या टाइप करें")
                    }

                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Quick Voice Action Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val commandSuggestions = listOf(
                    "यूट्यूब खोलो",
                    "व्हाट्सएप चलाओ",
                    "मैसेज पढ़ो",
                    "बैटरी कितनी है",
                    "टॉर्च जलाओ",
                    "आवाज़ 50% करो",
                    "ऐप बंद करो",
                    "कॉल टेस्ट करो"
                )

                commandSuggestions.forEach { suggestion ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            if (suggestion == "कॉल टेस्ट करो") {
                                viewModel.callSimulator.simulateIncomingCall("रोहन शर्मा", "+91 98765 43210")
                            } else {
                                viewModel.handleUserInput(suggestion)
                            }
                        },
                        label = { Text(suggestion, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = DarkSurfaceCard,
                            labelColor = AuraCyan
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = DarkBorder,
                            enabled = true,
                            selected = false
                        ),
                        modifier = Modifier.testTag("suggestion_chip_${suggestion.replace(" ", "_")}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Conversation Transcript List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(chatMessages, key = { it.id }) { msg ->
                    VoiceTranscriptItem(
                        message = msg,
                        onSpeakMessage = { text ->
                            viewModel.speakResponse(text, CommandCategory.GENERAL)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Input Bar (Voice or Type)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = DarkSurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.toggleListening() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (assistantState is AssistantState.Listening) AuraRose else AuraCyan.copy(alpha = 0.2f))
                            .testTag("mic_toggle_bottom")
                    ) {
                        Icon(
                            imageVector = if (assistantState is AssistantState.Listening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Microphone",
                            tint = if (assistantState is AssistantState.Listening) Color.White else AuraCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                "ऑरा से कुछ भी बोलिए या टाइप करें...",
                                color = TextTertiary,
                                fontSize = 14.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (textInput.isNotBlank()) {
                                    viewModel.handleUserInput(textInput)
                                    textInput = ""
                                    keyboardController?.hide()
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("text_command_input")
                    )

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.handleUserInput(textInput)
                                textInput = ""
                                keyboardController?.hide()
                            }
                        },
                        enabled = textInput.isNotBlank(),
                        modifier = Modifier.testTag("send_command_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (textInput.isNotBlank()) AuraCyan else TextTertiary
                        )
                    }
                }
            }
        }
    }
}
