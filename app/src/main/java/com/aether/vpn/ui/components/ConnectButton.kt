package com.aether.vpn.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.vpn.core.TunnelState
import com.aether.vpn.ui.theme.Cyan
import com.aether.vpn.ui.theme.Indigo
import com.aether.vpn.ui.theme.Mint
import com.aether.vpn.ui.theme.TextPrimary
import com.aether.vpn.ui.theme.TextSecondary
import com.aether.vpn.ui.theme.Violet
import kotlin.math.cos
import kotlin.math.sin

/**
 * The app's signature control: an oversized circular power button.
 *
 * Idle       — slow breathing glow rings, cyan→violet gradient core, faint orbit dots.
 * Connecting — the conic ring spins fast, core dims, orbit dots accelerate.
 * Connected  — mint core, rotating conic ring, tighter breathing, brighter orbit.
 *
 * Everything is drawn from the real [state]; nothing animates as if connected when it
 * is not.
 */
@Composable
fun ConnectButton(
    state: TunnelState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 252
) {
    val connected = state == TunnelState.CONNECTED
    val busy = state == TunnelState.CONNECTING || state == TunnelState.DISCONNECTING
    val accent = if (connected) Mint else Cyan
    val accent2 = if (connected) Cyan else Violet

    val transition = rememberInfiniteTransition(label = "connect")

    val breathe by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (connected) 2400 else 3800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (busy) 900 else if (connected) 3400 else 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    val orbit by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (connected) 7000 else 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )

    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        // ---- Outer breathing glow -----------------------------------------
        Canvas(Modifier.size(size.dp)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val base = this.size.minDimension / 2f
            repeat(4) { i ->
                val phase = (breathe + i * 0.25f) % 1f
                val radius = base * (0.64f + phase * 0.36f)
                val alpha = (1f - phase) * (if (connected) 0.28f else 0.19f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = alpha), Color.Transparent),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }
        }

        // ---- Orbiting particles -------------------------------------------
        Canvas(Modifier.size(size.dp)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f * 0.87f
            val count = 3
            repeat(count) { i ->
                val angle = Math.toRadians((orbit + i * (360f / count)).toDouble())
                val x = center.x + (cos(angle) * radius).toFloat()
                val y = center.y + (sin(angle) * radius).toFloat()
                val dotAlpha = if (connected) 0.9f else 0.38f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.40f), Color.Transparent),
                        center = Offset(x, y),
                        radius = 10.dp.toPx()
                    ),
                    radius = 10.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = accent.copy(alpha = dotAlpha),
                    radius = 2.4.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        // ---- Rotating conic ring ------------------------------------------
        Canvas(
            Modifier
                .size((size - 22).dp)
                .rotate(spin)
        ) {
            val stroke = 2.6.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color.Transparent,
                        accent.copy(alpha = 0.08f),
                        accent.copy(alpha = 0.90f),
                        accent2,
                        Color.Transparent
                    )
                ),
                startAngle = 0f,
                sweepAngle = 290f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        // ---- Static inner ring --------------------------------------------
        Box(
            Modifier
                .size((size - 22).dp)
                .clip(CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.06f), CircleShape)
        )

        // ---- Core ---------------------------------------------------------
        Box(
            modifier = Modifier
                .size((size - 56).dp)
                .scale(1f + breathe * 0.015f)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = if (connected) 0.28f else 0.19f),
                            accent2.copy(alpha = if (connected) 0.22f else 0.16f)
                        )
                    )
                )
                .border(1.dp, accent.copy(alpha = 0.32f), CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = !busy,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size((size - 56).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.06f), Color.Transparent)
                        )
                    )
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.PowerSettingsNew,
                    contentDescription = "Connect",
                    tint = if (connected) Mint else TextPrimary,
                    modifier = Modifier.size(50.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = when (state) {
                        TunnelState.CONNECTED -> "CONNECTED"
                        TunnelState.CONNECTING -> "CONNECTING"
                        TunnelState.DISCONNECTING -> "STOPPING"
                        TunnelState.ERROR -> "RETRY"
                        else -> "TAP TO CONNECT"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (connected) Mint else TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.7.sp
                )
            }
        }
    }
}
