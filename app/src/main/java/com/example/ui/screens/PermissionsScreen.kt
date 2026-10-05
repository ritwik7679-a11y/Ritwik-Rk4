package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PermissionItem
import com.example.ui.MainViewModel
import com.example.ui.theme.AuraAmber
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraEmerald
import com.example.ui.theme.AuraRose
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun PermissionsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val permissionItems by viewModel.permissionItems.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshPermissions()
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
                    text = "प्राइवेसी और परमिशन (PRIVACY)",
                    color = AuraCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "एंड्रॉयड सिस्टम कंट्रोल",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ऑरा आपकी प्राइवेसी और कंट्रोल को प्राथमिकता देती है। संवेदनशील काम हमेशा आपकी स्पष्ट अनुमति के बाद ही होते हैं।",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // Grant Missing Button
        item {
            val missingPermissions = permissionItems.filter { !it.isGranted }.map { it.permissionId }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (missingPermissions.isNotEmpty()) {
                            permissionLauncher.launch(missingPermissions.toTypedArray())
                        } else {
                            viewModel.refreshPermissions()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("request_all_permissions_button")
                ) {
                    Icon(
                        imageVector = if (missingPermissions.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Security,
                        contentDescription = "Grant",
                        tint = Color(0xFF00363D)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (missingPermissions.isEmpty()) "सभी परमिशन मिल चुकी हैं" else "परमिशन दें (${missingPermissions.size})",
                        color = Color(0xFF00363D),
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Settings", tint = AuraCyan)
                }
            }
        }

        // Permission Items List
        item {
            Text(
                text = "Permission Breakdown",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(permissionItems, key = { it.permissionId }) { item ->
            PermissionItemCard(
                item = item,
                onRequest = {
                    permissionLauncher.launch(arrayOf(item.permissionId))
                }
            )
        }

        // System Restrictions and Sandboxing Explanation (Requirement 12)
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Android System Restrictions FAQ",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            RestrictionExplanationCard(
                title = "Why can't Aura forcibly close other apps?",
                explanation = "Android's security architecture isolates apps in separate sandboxed processes. Standard applications cannot terminate other running apps without specialized Device Owner management or root permissions. Aura safely navigates you to the Home screen instead.",
                icon = Icons.Default.Lock
            )
        }

        item {
            RestrictionExplanationCard(
                title = "How does voice answering work safely?",
                explanation = "Answering calls requires the ANSWER_PHONE_CALLS permission introduced in Android 8.0+. When you say 'Answer the call', Aura communicates directly with Android's TelecomManager to accept the active ringing call without touching the screen.",
                icon = Icons.Default.Info
            )
        }

        item {
            RestrictionExplanationCard(
                title = "Why does Aura ask for confirmation?",
                explanation = "Sensitive actions like making phone calls or sending text messages can incur costs or communicate unintentionally. Aura strictly asks for your voice confirmation ('Say Yes or Cancel') before placing calls or sending messages.",
                icon = Icons.Default.Warning
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PermissionItemCard(
    item: PermissionItem,
    onRequest: () -> Unit
) {
    val statusColor = if (item.isGranted) AuraEmerald else AuraRose

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (item.isGranted) DarkBorder else AuraRose.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("permission_card_${item.permissionId.substringAfterLast('.')}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!item.isGranted) {
                    Button(
                        onClick = onRequest,
                        colors = ButtonDefaults.buttonColors(containerColor = AuraCyan.copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AuraCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Grant", color = AuraCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Granted",
                            tint = AuraEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Granted", color = AuraEmerald, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.description,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Privacy guarantee
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Privacy",
                        tint = AuraCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.privacyNotice,
                        color = TextTertiary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RestrictionExplanationCard(
    title: String,
    explanation: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(AuraCyan.copy(alpha = 0.15f))
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = AuraCyan, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = explanation, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}
