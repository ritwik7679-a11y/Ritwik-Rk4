package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommandCategory
import com.example.ui.MainViewModel
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraEmerald
import com.example.ui.theme.AuraRose
import com.example.ui.theme.AuraViolet
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun CallSimulatorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeCall by viewModel.activeCallState.collectAsState()

    var testCallerName by remember { mutableStateOf("रोहन शर्मा") }
    var testCallerNumber by remember { mutableStateOf("+91 98765 43210") }

    var testSenderName by remember { mutableStateOf("अमित कुमार") }
    var testMessageBody by remember { mutableStateOf("नमस्ते! क्या आप आज शाम को मिल रहे हैं?") }
    var testAppSource by remember { mutableStateOf("WhatsApp") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "वॉयस टेस्ट लैब (TEST LAB)",
                    color = AuraCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "कॉल व मैसेज सिम्युलेटर",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "हैंड्स-फ्री कॉलर का नाम सुनना, बोलकर कॉल उठाना ('कॉल उठाओ'), काटना ('कॉल काटो') और मैसेज सुनने का परीक्षण करें।",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // Active Call Status banner if currently ringing
        if (activeCall.isRinging) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(2.dp, AuraRose),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "सिम्युलेटेड कॉल चल रही है",
                            color = AuraRose,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = activeCall.callerName ?: "अंज़ान कॉलर",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = activeCall.callerNumber ?: "",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ऑरा ने कहा: \"${activeCall.callerName} की कॉल आ रही है\"। बोलकर देखिए 'कॉल उठाओ' या 'कॉल काटो'।",
                            color = AuraCyan,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.rejectIncomingCall() },
                                colors = ButtonDefaults.buttonColors(containerColor = AuraRose),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CallEnd, contentDescription = "Reject", tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("कॉल काटो")
                            }
                            Button(
                                onClick = { viewModel.answerIncomingCall() },
                                colors = ButtonDefaults.buttonColors(containerColor = AuraEmerald),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Answer", tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("कॉल उठाओ")
                            }
                        }
                    }
                }
            }
        }

        // Section 1: Incoming Call Simulator
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AuraCyan.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.PhoneInTalk, contentDescription = "Call", tint = AuraCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "1. इनकमिंग कॉल टेस्ट (कॉलर नाम घोषणा)",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "कॉलर का नाम बोलकर सुनाएगी और 'कॉल उठाओ' / 'कॉल काटो' सुनेगी",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = testCallerName,
                        onValueChange = { testCallerName = it },
                        label = { Text("कॉन्टैक्ट का नाम") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_caller_name_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = testCallerNumber,
                        onValueChange = { testCallerNumber = it },
                        label = { Text("फोन नंबर") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_caller_number_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset Quick Picks
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                testCallerName = "रोहन शर्मा"
                                testCallerNumber = "+91 98765 43210"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("रोहन (दोस्त)", fontSize = 11.sp, color = TextPrimary)
                        }
                        Button(
                            onClick = {
                                testCallerName = "मम्मी"
                                testCallerNumber = "+91 98111 22334"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("मम्मी (घर)", fontSize = 11.sp, color = TextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            viewModel.callSimulator.simulateIncomingCall(testCallerName, testCallerNumber)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("simulate_incoming_call_button")
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = "Simulate", tint = Color(0xFF00363D))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("$testCallerName से कॉल टेस्ट करें", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 2: Incoming Message Simulator
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AuraViolet.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "Message", tint = AuraViolet, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "2. मैसेज रीडर टेस्ट",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "किसने भेजा है नाम बताएगी और मैसेज बोलकर सुनाएगी",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = testSenderName,
                        onValueChange = { testSenderName = it },
                        label = { Text("भेजने वाले का नाम") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraViolet,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_sender_name_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = testMessageBody,
                        onValueChange = { testMessageBody = it },
                        label = { Text("मैसेज टेक्स्ट") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraViolet,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_message_body_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            viewModel.callSimulator.simulateIncomingMessage(
                                sender = testSenderName,
                                message = testMessageBody,
                                appName = testAppSource
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("simulate_incoming_message_button")
                    ) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = "Simulate", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("मैसेज टेस्ट करें व बोलकर सुनें", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
