package com.aether.vpn.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Icon
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
import com.aether.vpn.core.SessionRecord
import com.aether.vpn.ui.AppState
import com.aether.vpn.ui.components.EmptyBlock
import com.aether.vpn.ui.components.GlassCard
import com.aether.vpn.ui.components.GradientBorderCard
import com.aether.vpn.ui.components.HairlineDivider
import com.aether.vpn.ui.components.ScreenHeader
import com.aether.vpn.ui.components.SectionLabel
import com.aether.vpn.ui.components.StatTile
import com.aether.vpn.ui.components.formatBytes
import com.aether.vpn.ui.components.formatDuration
import com.aether.vpn.ui.theme.Cyan
import com.aether.vpn.ui.theme.Indigo
import com.aether.vpn.ui.theme.Mint
import com.aether.vpn.ui.theme.TextFaint
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
    val rx = app.lifetimeRx()
    val tx = app.lifetimeTx()
    val totalMs = app.lifetimeMs()

    // Top endpoints by bytes moved.
    val byEndpoint = sessions
        .groupBy { it.endpoint.ifBlank { "auto" } }
        .mapValues { (_, list) -> list.sumOf { it.rxBytes + it.txBytes } }
        .toList()
        .sortedByDescending { it.second }
        .take(4)
    val peakEndpoint = byEndpoint.firstOrNull()?.second ?: 1L

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))

        ScreenHeader(
            eyebrow = "${sessions.size} session${if (sessions.size == 1) "" else "s"} recorded",
            title = "Usage"
        )

        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // ---- Hero totals ------------------------------------------------
            item {
                GradientBorderCard(Modifier.fillMaxWidth(), colors = listOf(Cyan, Indigo, Violet)) {
                    Column(Modifier.padding(20.dp)) {
                        SectionLabel("Lifetime data", accent = Cyan)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            formatBytes(rx + tx),
                            style = MaterialTheme.typography.displayMedium,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(14.dp))
                        HairlineDivider()
                        Spacer(Modifier.height(14.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconStat(Icons.Rounded.Download, "Downloaded", formatBytes(rx), Cyan)
                            IconStat(Icons.Rounded.Upload, "Uploaded", formatBytes(tx), Violet)
                            IconStat(Icons.Rounded.Schedule, "Connected", formatDuration(totalMs), Mint)
                        }
                    }
                }
            }

            // ---- Tiles ------------------------------------------------------
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        "Sessions",
                        sessions.size.toString(),
                        Modifier.weight(1f),
                        accent = Cyan
                    )
                    StatTile(
                        "Avg session",
                        if (sessions.isEmpty()) "—"
                        else formatDuration(totalMs / sessions.size),
                        Modifier.weight(1f),
                        accent = Violet
                    )
                }
            }

            // ---- Top endpoints ----------------------------------------------
            if (byEndpoint.isNotEmpty()) {
                item {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp)) {
                            SectionLabel("Top servers", accent = Mint)
                            Spacer(Modifier.height(14.dp))
                            byEndpoint.forEach { (endpoint, bytes) ->
                                Column(Modifier.padding(bottom = 12.dp)) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            endpoint,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                        Text(
                                            formatBytes(bytes),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.06f))
                                    ) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth(
                                                    (bytes.toFloat() / peakEndpoint.toFloat())
                                                        .coerceIn(0.03f, 1f)
                                                )
                                                .height(4.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.horizontalGradient(listOf(Cyan, Violet))
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---- History ----------------------------------------------------
            item {
                Spacer(Modifier.height(4.dp))
                SectionLabel("Session history", accent = Violet)
            }

            if (sessions.isEmpty()) {
                item {
                    EmptyBlock(
                        title = "No sessions yet",
                        body = "Connect once and your session time, data and server will appear here."
                    )
                }
            } else {
                items(sessions, key = { "${it.startedAt}-${it.endpoint}" }) { s ->
                    SessionRow(s)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun IconStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextFaint
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SessionRow(s: SessionRecord) {
    val fmt = SimpleDateFormat("d MMM · HH:mm", Locale.getDefault())
    GlassCard(Modifier.fillMaxWidth(), corner = 18) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Cyan.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    s.flag,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    s.endpoint.ifBlank { "Auto edge" },
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    fmt.format(Date(s.startedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextFaint
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatDuration(s.durationMs),
                    style = MaterialTheme.typography.titleMedium,
                    color = Mint,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "↓ ${formatBytes(s.rxBytes)}  ↑ ${formatBytes(s.txBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }
    }
}
