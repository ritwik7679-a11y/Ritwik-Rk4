package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommandCategory
import com.example.data.model.InstalledAppItem
import com.example.ui.MainViewModel
import com.example.ui.theme.AuraAmber
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
fun DeviceControlsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val installedApps by viewModel.installedApps.collectAsState()
    var appSearchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(installedApps, appSearchQuery) {
        if (appSearchQuery.isBlank()) {
            installedApps.take(15)
        } else {
            installedApps.filter { it.appName.contains(appSearchQuery, ignoreCase = true) }
        }
    }

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
                    text = "डिवाइस कंट्रोल्स (CONTROLS)",
                    color = AuraCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "हार्डवेयर और ऐप कंट्रोल्स",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "टच या वॉयस कमांड देकर फोन के फीचर्स और ऐप्स कंट्रोल करें।",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // Section 1: Quick Hardware Controls Grid
        item {
            Text(
                text = "फोन हार्डवेयर",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = "टॉर्च जलाएं",
                    subtitle = "\"टॉर्च जलाओ\"",
                    icon = Icons.Default.FlashlightOn,
                    tint = AuraCyan,
                    onClick = { viewModel.handleUserInput("टॉर्च जलाओ") },
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    title = "टॉर्च बंद करें",
                    subtitle = "\"टॉर्च बंद करो\"",
                    icon = Icons.Default.FlashlightOn,
                    tint = TextSecondary,
                    onClick = { viewModel.handleUserInput("टॉर्च बंद करो") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = "बैटरी चेक करें",
                    subtitle = "\"बैटरी कितनी है\"",
                    icon = Icons.Default.BatteryFull,
                    tint = AuraEmerald,
                    onClick = { viewModel.handleUserInput("बैटरी कितनी है") },
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    title = "ऐप बंद करें",
                    subtitle = "\"ऐप बंद करो\"",
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    tint = AuraAmber,
                    onClick = { viewModel.handleUserInput("ऐप बंद करो") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Section 2: Volume Presets
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "आवाज़ व वॉल्यूम कंट्रोल्स",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "बोलिए: \"आवाज़ बढ़ाओ\", \"आवाज़ कम करो\", \"म्यूट करो\", या \"आवाज़ 50% करो\"",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresetButton(label = "म्यूट", icon = Icons.AutoMirrored.Filled.VolumeMute) {
                            viewModel.handleUserInput("म्यूट करो")
                        }
                        PresetButton(label = "30%") {
                            viewModel.handleUserInput("आवाज़ 30% करो")
                        }
                        PresetButton(label = "70%") {
                            viewModel.handleUserInput("आवाज़ 70% करो")
                        }
                        PresetButton(label = "100%", icon = Icons.AutoMirrored.Filled.VolumeUp) {
                            viewModel.handleUserInput("आवाज़ 100% करो")
                        }
                    }
                }
            }
        }

        // Section 3: System Settings Shortcuts
        item {
            Text(
                text = "सिस्टम सेटिंग्स",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingsChip(label = "वाईफाई", icon = Icons.Default.Wifi) {
                    viewModel.handleUserInput("वाईफाई खोलो")
                }
                SettingsChip(label = "ब्लूटूथ", icon = Icons.Default.Bluetooth) {
                    viewModel.handleUserInput("ब्लूटूथ खोलो")
                }
                SettingsChip(label = "डिस्प्ले", icon = Icons.Default.DisplaySettings) {
                    viewModel.handleUserInput("डिस्प्ले सेटिंग्स")
                }
                SettingsChip(label = "अलार्म", icon = Icons.Default.Alarm) {
                    viewModel.handleUserInput("अलार्म लगाओ")
                }
            }
        }

        // Section 4: App Launcher Showcase
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "इंस्टॉल किए गए ऐप्स (${installedApps.size})",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "किसी भी ऐप को खोलने के लिए बोलिए \"[ऐप का नाम] खोलो\"।",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = appSearchQuery,
                onValueChange = { appSearchQuery = it },
                placeholder = { Text("ऐप खोजें (जैसे यूट्यूब, मैप्स, व्हाट्सएप)...", color = TextTertiary, fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceCard,
                    unfocusedContainerColor = DarkSurfaceCard,
                    focusedBorderColor = AuraCyan,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_search_input")
            )
        }

        items(filteredApps, key = { it.packageName }) { app ->
            AppLaunchRow(
                app = app,
                onLaunch = {
                    viewModel.handleUserInput("${app.appName} kholo")
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        onClick = onClick,
        modifier = modifier.testTag("action_card_${title.replace(" ", "_")}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f))
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun PresetButton(
    label: String,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AuraCyan),
        modifier = Modifier.height(38.dp)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = label, tint = AuraCyan, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SettingsChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        onClick = onClick,
        modifier = Modifier.height(42.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = AuraCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun AppLaunchRow(
    app: InstalledAppItem,
    onLaunch: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_row_${app.packageName}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AuraCyan.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = "App",
                        tint = AuraCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = app.appName,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = app.packageName,
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }

            Button(
                onClick = onLaunch,
                colors = ButtonDefaults.buttonColors(containerColor = AuraCyan.copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, AuraCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Launch", tint = AuraCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("खोलें", color = AuraCyan, fontSize = 12.sp)
            }
        }
    }
}
