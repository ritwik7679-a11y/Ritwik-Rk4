package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantState
import com.example.ui.theme.AuraAmber
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraCyanGlow
import com.example.ui.theme.AuraRose
import com.example.ui.theme.AuraViolet
import com.example.ui.theme.AuraVioletGlow
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VoiceOrbVisualizer(
    assistantState: AssistantState,
    isRinging: Boolean = false,
    size: Dp = 190.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val rippleWave by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_wave"
    )

    val (primaryColor, glowColor) = when {
        isRinging -> Pair(AuraRose, AuraRose.copy(alpha = 0.4f))
        assistantState is AssistantState.Listening -> Pair(AuraCyan, AuraCyanGlow)
        assistantState is AssistantState.Speaking -> Pair(AuraViolet, AuraVioletGlow)
        assistantState is AssistantState.AwaitingConfirmation -> Pair(AuraAmber, AuraAmber.copy(alpha = 0.4f))
        assistantState is AssistantState.Processing -> Pair(AuraCyan, AuraVioletGlow)
        assistantState is AssistantState.Error -> Pair(AuraRose, AuraRose.copy(alpha = 0.3f))
        else -> Pair(AuraCyan, AuraCyanGlow)
    }

    val dynamicBoost = when (assistantState) {
        is AssistantState.Listening -> assistantState.soundLevel * 0.4f
        is AssistantState.Speaking -> assistantState.audioWave * 0.25f
        else -> 0f
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = size / 2),
                onClick = onClick
            )
            .testTag("voice_orb_button")
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2.7f) * (pulseScale + dynamicBoost)

            // Outer animated ripple rings
            if (assistantState is AssistantState.Listening || assistantState is AssistantState.Speaking || isRinging) {
                drawCircle(
                    color = glowColor.copy(alpha = (1.4f - rippleWave).coerceIn(0f, 0.45f)),
                    radius = baseRadius * rippleWave,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = primaryColor.copy(alpha = (1.3f - rippleWave).coerceIn(0f, 0.3f)),
                    radius = baseRadius * (rippleWave * 1.15f).coerceAtMost(1.5f),
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Glow gradient backing
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor,
                        glowColor.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.5f
                ),
                radius = baseRadius * 1.5f,
                center = center
            )

            // Core multi-layered sphere
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(primaryColor, AuraViolet, Color(0xFF0F172A)),
                    start = Offset(center.x - baseRadius, center.y - baseRadius),
                    end = Offset(center.x + baseRadius, center.y + baseRadius)
                ),
                radius = baseRadius,
                center = center
            )

            // Inner high-tech orbital particles
            val particleCount = 8
            for (i in 0 until particleCount) {
                val angle = Math.toRadians((rotationAngle + (i * 360f / particleCount)).toDouble())
                val particleDistance = baseRadius * 0.75f
                val px = center.x + (particleDistance * cos(angle)).toFloat()
                val py = center.y + (particleDistance * sin(angle)).toFloat()

                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = 3.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }

        // Center Icon indicating state
        val icon = when {
            isRinging -> Icons.Default.Call
            assistantState is AssistantState.Listening -> Icons.Default.Stop
            assistantState is AssistantState.Speaking -> Icons.AutoMirrored.Filled.VolumeUp
            assistantState is AssistantState.AwaitingConfirmation -> Icons.Default.Warning
            else -> Icons.Default.Mic
        }

        Icon(
            imageVector = icon,
            contentDescription = "Voice Assistant Control",
            tint = Color.White,
            modifier = Modifier.size(42.dp)
        )
    }
}
