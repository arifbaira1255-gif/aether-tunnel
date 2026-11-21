package com.aether.vpn.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aether.vpn.ui.theme.Hairline
import com.aether.vpn.ui.theme.HairlineStrong
import com.aether.vpn.ui.theme.Ink
import com.aether.vpn.ui.theme.Surface1
import com.aether.vpn.ui.theme.TextPrimary
import com.aether.vpn.ui.theme.TextSecondary
import com.aether.vpn.ui.theme.TextTertiary

/** The app's signature surface: a soft glass card with a hairline top highlight. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Int = 24,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A1F3D).copy(alpha = 0.92f),
                        Surface1.copy(alpha = 0.86f)
                    )
                )
            )
            .border(1.dp, Hairline, RoundedCornerShape(corner.dp))
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, HairlineStrong, Color.Transparent)
                    )
                )
        )
        content()
    }
}

/** Ambient background: deep ink with two slow-drifting colour blooms. */
@Composable
fun AmbientBackground(
    accentA: Color,
    accentB: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "ambient")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    Box(modifier = modifier.background(Ink)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(520.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accentA.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(180f + drift * 120f, 60f),
                        radius = 900f
                    )
                )
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(620.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accentB.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(700f - drift * 160f, 420f),
                        radius = 1000f
                    )
                )
        )
        content()
    }
}

/** Small uppercase section label. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        color = TextTertiary,
        modifier = modifier
    )
}

/** A pill badge, e.g. FREE / PREMIUM / CONNECTED. */
@Composable
fun Pill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (filled) color.copy(alpha = 0.16f) else Color.Transparent)
            .border(1.dp, color.copy(alpha = 0.45f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Label + value pair used across the stats and connect screens. */
@Composable
fun Metric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary,
    alignEnd: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Text(
            label.uppercase(),
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            color = valueColor
        )
    }
}

/** Thin progress bar used for server load. */
@Composable
fun LoadBar(load: Int, modifier: Modifier = Modifier) {
    val color = when {
        load < 45 -> com.aether.vpn.ui.theme.Mint
        load < 75 -> com.aether.vpn.ui.theme.Amber
        else -> com.aether.vpn.ui.theme.Rose
    }
    Box(
        modifier
            .height(4.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(load.coerceIn(4, 100) / 100f)
                .height(4.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/** A row of small dots used as a subtle divider ornament. */
@Composable
fun DotDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) {
            Box(
                Modifier
                    .size(3.dp)
                    .clip(CircleShape)
                    .background(TextSecondary.copy(alpha = 0.35f))
            )
        }
    }
}

/** Empty-state block. */
@Composable
fun EmptyBlock(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Hairline, CircleShape)
        )
        Spacer(Modifier.height(14.dp))
        Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = TextTertiary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

/** Formats a byte count the way a premium app does: 1.24 GB, 812 MB, 4.1 KB. */
fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    return String.format("%.2f GB", mb / 1024.0)
}

/** Formats a bitrate. */
fun formatRate(bps: Long): String {
    val kbps = bps / 1024.0
    if (kbps < 1000) return String.format("%.0f KB/s", kbps)
    return String.format("%.1f MB/s", kbps / 1024.0)
}

/** Formats a duration as 00:12:34 or 12:34. */
fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}
