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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.vpn.ui.theme.Amber
import com.aether.vpn.ui.theme.Cyan
import com.aether.vpn.ui.theme.Hairline
import com.aether.vpn.ui.theme.HairlineStrong
import com.aether.vpn.ui.theme.HairlineTop
import com.aether.vpn.ui.theme.Ink
import com.aether.vpn.ui.theme.Mint
import com.aether.vpn.ui.theme.Rose
import com.aether.vpn.ui.theme.Surface1
import com.aether.vpn.ui.theme.Surface2
import com.aether.vpn.ui.theme.Surface3
import com.aether.vpn.ui.theme.TextFaint
import com.aether.vpn.ui.theme.TextPrimary
import com.aether.vpn.ui.theme.TextSecondary
import com.aether.vpn.ui.theme.TextTertiary
import com.aether.vpn.ui.theme.Violet

/**
 * The app's signature surface: a glass card with a vertical sheen, a hairline top
 * highlight and a soft inner shadow at the bottom. Three stacked layers is what makes
 * it read as glass rather than as a rounded rectangle.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Int = 24,
    accent: Color? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Surface2.copy(alpha = 0.96f),
                        Surface1.copy(alpha = 0.92f)
                    )
                )
            )
            .border(1.dp, Hairline, RoundedCornerShape(corner.dp))
    ) {
        // Top edge highlight — the light catching the bevel.
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, HairlineTop, Color.Transparent)
                    )
                )
        )
        if (accent != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.11f), Color.Transparent),
                            center = Offset(0f, 0f),
                            radius = 460f
                        )
                    )
            )
        }
        content()
    }
}

/** A card with a gradient hairline border — used for the hero surfaces. */
@Composable
fun GradientBorderCard(
    modifier: Modifier = Modifier,
    corner: Int = 26,
    colors: List<Color> = listOf(Cyan, Violet),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner.dp))
            .background(Brush.linearGradient(colors.map { it.copy(alpha = 0.55f) }))
            .padding(1.dp)
            .clip(RoundedCornerShape((corner - 1).dp))
            .background(
                Brush.verticalGradient(
                    listOf(Surface3.copy(alpha = 0.98f), Surface1.copy(alpha = 0.96f))
                )
            )
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent)
                    )
                )
        )
        content()
    }
}

/** Ambient background: deep ink with three slow-drifting colour blooms. */
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
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    Box(modifier = modifier.background(Ink)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(600.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accentA.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(180f + drift * 160f, 20f),
                        radius = 1000f
                    )
                )
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(760.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accentB.copy(alpha = 0.13f), Color.Transparent),
                        center = Offset(800f - drift * 200f, 500f),
                        radius = 1100f
                    )
                )
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(460.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, accentA.copy(alpha = 0.05f))
                    )
                )
        )
        content()
    }
}

/** Small uppercase section label with a leading rule. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, accent: Color? = null) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (accent != null) {
            Box(
                Modifier
                    .size(3.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(Modifier.width(7.dp))
        }
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )
    }
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
            .background(if (filled) color.copy(alpha = 0.14f) else Color.Transparent)
            .border(1.dp, color.copy(alpha = if (filled) 0.32f else 0.40f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 3.5.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
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
    alignEnd: Boolean = false,
    valueStyle: androidx.compose.ui.text.TextStyle? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )
        Spacer(Modifier.height(5.dp))
        Text(
            value,
            style = valueStyle ?: MaterialTheme.typography.headlineSmall,
            color = valueColor
        )
    }
}

/** Thin progress bar used for server load. */
@Composable
fun LoadBar(load: Int, modifier: Modifier = Modifier) {
    val color = when {
        load < 45 -> Mint
        load < 75 -> Amber
        else -> Rose
    }
    Box(
        modifier
            .height(3.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.07f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(load.coerceIn(4, 100) / 100f)
                .height(3.dp)
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
                    .background(TextSecondary.copy(alpha = 0.30f))
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
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, Hairline, CircleShape)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 28.dp)
        )
    }
}

/**
 * The live throughput graph. Two filled area curves (download above the axis, upload
 * mirrored below) drawn from the real per-second samples the poller collected.
 *
 * When there is no data yet it draws a flat baseline rather than a fake waveform —
 * an empty graph that looks busy would be a lie.
 */
@Composable
fun SpeedGraph(
    down: List<Long>,
    up: List<Long>,
    modifier: Modifier = Modifier,
    active: Boolean = true
) {
    val peak = maxOf(down.maxOrNull() ?: 0L, up.maxOrNull() ?: 0L, 1L)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val mid = h / 2f
        val half = h / 2f - 2.dp.toPx()

        // Grid lines
        listOf(0.25f, 0.5f, 0.75f).forEach { f ->
            drawLine(
                color = Color.White.copy(alpha = 0.035f),
                start = Offset(0f, h * f),
                end = Offset(w, h * f),
                strokeWidth = 1.dp.toPx()
            )
        }
        drawLine(
            color = Color.White.copy(alpha = 0.07f),
            start = Offset(0f, mid),
            end = Offset(w, mid),
            strokeWidth = 1.dp.toPx()
        )

        if (down.size < 2 && up.size < 2) return@Canvas

        fun buildPath(series: List<Long>, mirror: Boolean): Path {
            val path = Path()
            if (series.isEmpty()) return path
            val step = w / (series.size - 1).coerceAtLeast(1)
            series.forEachIndexed { i, v ->
                val x = i * step
                val norm = (v.toFloat() / peak.toFloat()).coerceIn(0f, 1f)
                val y = if (mirror) mid + norm * half else mid - norm * half
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            return path
        }

        fun buildFill(series: List<Long>, mirror: Boolean): Path {
            val path = Path()
            if (series.isEmpty()) return path
            val step = w / (series.size - 1).coerceAtLeast(1)
            path.moveTo(0f, mid)
            series.forEachIndexed { i, v ->
                val x = i * step
                val norm = (v.toFloat() / peak.toFloat()).coerceIn(0f, 1f)
                val y = if (mirror) mid + norm * half else mid - norm * half
                path.lineTo(x, y)
            }
            path.lineTo((series.size - 1) * step, mid)
            path.close()
            return path
        }

        val a = if (active) 1f else 0.35f

        drawPath(
            buildFill(down, false),
            brush = Brush.verticalGradient(
                listOf(Cyan.copy(alpha = 0.32f * a), Color.Transparent),
                startY = mid - half,
                endY = mid
            )
        )
        drawPath(
            buildPath(down, false),
            color = Cyan.copy(alpha = a),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        drawPath(
            buildFill(up, true),
            brush = Brush.verticalGradient(
                listOf(Color.Transparent, Violet.copy(alpha = 0.28f * a)),
                startY = mid,
                endY = mid + half
            )
        )
        drawPath(
            buildPath(up, true),
            color = Violet.copy(alpha = a),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
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
    if (kbps < 1) return "0 KB/s"
    if (kbps < 1000) return String.format("%.0f KB/s", kbps)
    return String.format("%.1f MB/s", kbps / 1024.0)
}

/** Formats a bitrate as a bare number + unit, for the big readouts. */
fun formatRateParts(bps: Long): Pair<String, String> {
    val kbps = bps / 1024.0
    return when {
        kbps < 1 -> "0" to "KB/s"
        kbps < 1000 -> String.format("%.0f", kbps) to "KB/s"
        else -> String.format("%.1f", kbps / 1024.0) to "MB/s"
    }
}

/** Formats a duration as 00:12:34 or 12:34. */
fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}

/** A tiny status dot with an optional pulse. */
@Composable
fun StatusDot(color: Color, size: Int = 8, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color)
    )
}

/** A labelled key/value row used in the settings and transport summaries. */
@Composable
fun KeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary,
    mono: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary
        )
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            style = if (mono) MaterialTheme.typography.labelMedium
            else MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End
        )
    }
}

/** Hairline divider that respects card padding. */
@Composable
fun HairlineDivider(modifier: Modifier = Modifier, inset: Int = 0) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = inset.dp)
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.055f))
    )
}

/** A small monospace-ish code block for payload previews. */
@Composable
fun CodeBlock(text: String, modifier: Modifier = Modifier, maxLines: Int = 6) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.34f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(11.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            fontSize = 10.sp,
            lineHeight = 15.sp,
            maxLines = maxLines
        )
    }
}

/** A selectable chip. */
@Composable
fun Chip(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    accent: Color = Cyan,
    onClick: () -> Unit
) {
    Box(
        modifier
            .clip(CircleShape)
            .background(if (active) accent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.035f))
            .border(
                1.dp,
                if (active) accent.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.07f),
                CircleShape
            )
            .clickableNoRipple(onClick)
            .padding(horizontal = 14.dp, vertical = 7.5.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (active) accent else TextSecondary
        )
    }
}

/** Clickable without the ripple, for the app's flat premium feel. */
@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val interaction = androidx.compose.runtime.remember {
        androidx.compose.foundation.interaction.MutableInteractionSource()
    }
    return this.then(
        Modifier.clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick
        )
    )
}

/** A faint "no data" hint used inside graphs and lists. */
@Composable
fun FaintHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = TextFaint,
        modifier = modifier
    )
}

/** A screen header: eyebrow, title, optional trailing slot. */
@Composable
fun ScreenHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    accent: Color = Cyan,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = accent.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary
            )
        }
        trailing?.invoke()
    }
}

/** A stat tile: big number, small label, optional accent. */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = Cyan,
    sub: String? = null
) {
    GlassCard(modifier, corner = 20, accent = accent) {
        Column(Modifier.padding(15.dp)) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            if (sub != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = accent.copy(alpha = 0.85f)
                )
            }
        }
    }
}
