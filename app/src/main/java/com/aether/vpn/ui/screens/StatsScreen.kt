package com.aether.vpn.ui.screens

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.vpn.ui.AppState
import com.aether.vpn.ui.components.EmptyBlock
import com.aether.vpn.ui.components.GlassCard
import com.aether.vpn.ui.components.HairlineDivider
import com.aether.vpn.ui.components.Metric
import com.aether.vpn.ui.components.Pill
import com.aether.vpn.ui.components.SectionLabel
import com.aether.vpn.ui.components.formatBytes
import com.aether.vpn.ui.components.formatDuration
import com.aether.vpn.ui.theme.Cyan
import com.aether.vpn.ui.theme.Mint
import com.aether.vpn.ui.theme.TextFaint
import com.aether.vpn.ui.theme.TextPrimary
import com.aether.vpn.ui.theme.TextSecondary
import com.aether.vpn.ui.theme.TextTertiary
import com.aether.vpn.ui.theme.Violet
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen(app: AppState) {
    val sessions = app.sessions
    val lifetimeRx = app.lifetimeRx()
    val lifetimeTx = app.lifetimeTx()
    val lifetimeMs = app.lifetimeMs()

    val topServers = sessions
        .groupBy { it.profileName }
        .mapValues { (_, list) -> list.sumOf { it.rxBytes + it.txBytes } }
        .entries
        .sortedByDescending { it.value }
        .take(4)
    val topMax = topServers.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1

    // Last 7 days of data, oldest first.
    val week = lastSevenDays(sessions)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(14.dp))
            Text("Usage", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
            Spacer(Modifier.height(3.dp))
            Text(
                "${sessions.size} session${if (sessions.size == 1) "" else "s"} recorded on this device",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary
            )
            Spacer(Modifier.height(10.dp))
        }

        // ---- Hero totals ---------------------------------------------------
        item {
            GlassCard(Modifier.fillMaxWidth(), accent = Cyan) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionLabel("Lifetime", accent = Cyan)
                        Pill("this device", Violet)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Metric("Time protected", formatDuration(lifetimeMs))
                        Metric(
                            "Total data",
                            formatBytes(lifetimeRx + lifetimeTx),
                            alignEnd = true
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    HairlineDivider()
                    Spacer(Modifier.height(18.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Metric("Downloaded", formatBytes(lifetimeRx), valueColor = Cyan)
                        Metric("Uploaded", formatBytes(lifetimeTx), valueColor = Violet, alignEnd = true)
                    }
                }
            }
        }

        // ---- 7-day chart ---------------------------------------------------
        if (sessions.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                SectionLabel("Last 7 days", accent = Mint)
                Spacer(Modifier.height(10.dp))
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        val peak = week.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
                        Row(
                            Modifier.fillMaxWidth().height(96.dp),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            week.forEach { (label, bytes) ->
                                Column(
                                    Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(
                                                (bytes.toFloat() / peak * 74f)
                                                    .coerceAtLeast(if (bytes > 0) 4f else 2f)
                                                    .dp
                                            )
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(
                                                if (bytes > 0)
                                                    Brush.verticalGradient(listOf(Cyan, Violet))
                                                else
                                                    Brush.verticalGradient(
                                                        listOf(
                                                            Color.White.copy(alpha = 0.05f),
                                                            Color.White.copy(alpha = 0.05f)
                                                        )
                                                    )
                                            )
                                    )
                                    Spacer(Modifier.height(7.dp))
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextFaint
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---- Top servers ---------------------------------------------------
        if (topServers.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                SectionLabel("Top servers")
                Spacer(Modifier.height(10.dp))
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        topServers.forEachIndexed { i, entry ->
                            if (i > 0) Spacer(Modifier.height(15.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    entry.key,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.width(112.dp),
                                    maxLines = 1
                                )
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.06f))
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth((entry.value.toFloat() / topMax).coerceIn(0.05f, 1f))
                                            .height(6.dp)
                                            .clip(CircleShape)
                                            .background(Brush.horizontalGradient(listOf(Cyan, Violet)))
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    formatBytes(entry.value),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            SectionLabel("Session history")
            Spacer(Modifier.height(4.dp))
        }

        if (sessions.isEmpty()) {
            item {
                EmptyBlock(
                    title = "No sessions yet",
                    body = "Connect once and the session appears here with its real data totals."
                )
            }
        } else {
            items(sessions, key = { it.startedAt.toString() + it.profileName }) { s ->
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.045f))
                                .border(1.dp, Color.White.copy(alpha = 0.07f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(s.flag.ifBlank { "\uD83C\uDF10" }, fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                s.profileName,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                formatDate(s.startedAt) +
                                    if (s.mtu > 0) " \u00b7 MTU ${s.mtu}" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextTertiary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                formatDuration(s.durationMs),
                                style = MaterialTheme.typography.titleMedium,
                                color = Mint
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "\u2193 ${formatBytes(s.rxBytes)}  \u2191 ${formatBytes(s.txBytes)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

/** Seven (label, bytes) pairs, oldest first, from the real session records. */
private fun lastSevenDays(sessions: List<com.aether.vpn.core.SessionRecord>): List<Pair<String, Long>> {
    val cal = Calendar.getInstance()
    val fmt = SimpleDateFormat("EEE", Locale.getDefault())
    val out = ArrayList<Pair<String, Long>>(7)
    // Build the day buckets first so the labels line up with the bars.
    val buckets = ArrayList<Pair<Long, String>>(7)
    repeat(7) {
        buckets.add(0, cal.timeInMillis to fmt.format(Date(cal.timeInMillis)))
        cal.add(Calendar.DAY_OF_YEAR, -1)
    }
    buckets.forEach { (dayStart, label) ->
        val dayEnd = dayStart + 24L * 60 * 60 * 1000
        val bytes = sessions
            .filter { it.startedAt in dayStart until dayEnd }
            .sumOf { it.rxBytes + it.txBytes }
        out.add(label to bytes)
    }
    return out
}

private fun formatDate(ts: Long): String {
    val fmt = SimpleDateFormat("d MMM \u00b7 HH:mm", Locale.getDefault())
    return fmt.format(Date(ts))
}
