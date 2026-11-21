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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.vpn.ui.AppState
import com.aether.vpn.ui.components.EmptyBlock
import com.aether.vpn.ui.components.GlassCard
import com.aether.vpn.ui.components.Metric
import com.aether.vpn.ui.components.SectionLabel
import com.aether.vpn.ui.components.formatBytes
import com.aether.vpn.ui.components.formatDuration
import com.aether.vpn.ui.theme.Cyan
import com.aether.vpn.ui.theme.Mint
import com.aether.vpn.ui.theme.TextPrimary
import com.aether.vpn.ui.theme.TextSecondary
import com.aether.vpn.ui.theme.TextTertiary
import com.aether.vpn.ui.theme.Violet
import java.text.SimpleDateFormat
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

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(6.dp))
            Text("Usage", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
            Spacer(Modifier.height(3.dp))
            Text(
                "Lifetime totals and session history",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary
            )
            Spacer(Modifier.height(10.dp))
        }

        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    SectionLabel("Lifetime")
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Metric("Time", formatDuration(lifetimeMs))
                        Metric("Data", formatBytes(lifetimeRx + lifetimeTx), alignEnd = true)
                    }
                    Spacer(Modifier.height(18.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.07f))
                    )
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

        if (topServers.isNotEmpty()) {
            item {
                Spacer(Modifier.height(6.dp))
                SectionLabel("Top servers")
                Spacer(Modifier.height(10.dp))
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        topServers.forEachIndexed { i, entry ->
                            if (i > 0) Spacer(Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    entry.key,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.width(120.dp)
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
            Spacer(Modifier.height(6.dp))
            SectionLabel("Session history")
            Spacer(Modifier.height(4.dp))
        }

        if (sessions.isEmpty()) {
            item {
                EmptyBlock(
                    title = "No sessions yet",
                    body = "Connect once and your session will appear here with its data totals."
                )
            }
        } else {
            items(sessions, key = { it.startedAt.toString() + it.profileName }) { s ->
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
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
                                formatDate(s.startedAt),
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

private fun formatDate(ts: Long): String {
    val fmt = SimpleDateFormat("d MMM \u00b7 HH:mm", Locale.getDefault())
    return fmt.format(Date(ts))
}
