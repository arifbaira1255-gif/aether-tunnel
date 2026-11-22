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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.vpn.core.TunnelState
import com.aether.vpn.ui.AppState
import com.aether.vpn.ui.components.ConnectButton
import com.aether.vpn.ui.components.GlassCard
import com.aether.vpn.ui.components.GradientBorderCard
import com.aether.vpn.ui.components.HairlineDivider
import com.aether.vpn.ui.components.Pill
import com.aether.vpn.ui.components.SectionLabel
import com.aether.vpn.ui.components.SpeedGraph
import com.aether.vpn.ui.components.clickableNoRipple
import com.aether.vpn.ui.components.formatBytes
import com.aether.vpn.ui.components.formatDuration
import com.aether.vpn.ui.components.formatRateParts
import com.aether.vpn.ui.theme.Amber
import com.aether.vpn.ui.theme.Cyan
import com.aether.vpn.ui.theme.Indigo
import com.aether.vpn.ui.theme.Mint
import com.aether.vpn.ui.theme.Rose
import com.aether.vpn.ui.theme.TextFaint
import com.aether.vpn.ui.theme.TextPrimary
import com.aether.vpn.ui.theme.TextSecondary
import com.aether.vpn.ui.theme.TextTertiary
import com.aether.vpn.ui.theme.Violet
import kotlinx.coroutines.delay

@Composable
fun ConnectScreen(
    app: AppState,
    onOpenServers: () -> Unit,
    onOpenTransport: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val connected = app.state == TunnelState.CONNECTED
    val connecting = app.state == TunnelState.CONNECTING

    var elapsed by remember { mutableStateOf(0L) }
    LaunchedEffect(app.connectedAt, connected) {
        while (connected) {
            elapsed = System.currentTimeMillis() - app.connectedAt
            delay(1000)
        }
        if (!connected) elapsed = 0L
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        // ---- Brand bar -----------------------------------------------------
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(Cyan, Indigo, Violet))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Shield, null,
                        tint = Color(0xFF05060D),
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "AETHER",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        letterSpacing = 2.6.sp
                    )
                    Text(
                        "TUNNEL",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextFaint,
                        letterSpacing = 2.6.sp,
                        fontSize = 8.sp
                    )
                }
            }
            Pill(
                text = when {
                    connected -> "Protected"
                    connecting -> "Securing"
                    app.state == TunnelState.ERROR -> "Offline"
                    else -> "Unprotected"
                },
                color = when {
                    connected -> Mint
                    connecting -> Amber
                    app.state == TunnelState.ERROR -> Rose
                    else -> TextSecondary
                },
                filled = true
            )
        }

        Spacer(Modifier.height(22.dp))

        // ---- Hero status ---------------------------------------------------
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                when {
                    connected -> "You're protected"
                    connecting -> "Securing your link"
                    app.state == TunnelState.ERROR -> "Couldn't connect"
                    else -> "Your connection is open"
                },
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    connected -> "Traffic is encrypted end to end"
                    connecting -> app.failoverNote ?: "Negotiating the tunnel…"
                    app.state == TunnelState.ERROR -> "Tap to try again"
                    else -> "One tap to encrypt everything"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary
            )
        }

        Spacer(Modifier.height(20.dp))

        // ---- Signature connect button --------------------------------------
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ConnectButton(
                state = app.state,
                onClick = { if (connected) onDisconnect() else onConnect() }
            )
        }

        Spacer(Modifier.height(18.dp))

        // ---- Session timer -------------------------------------------------
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                formatDuration(elapsed),
                style = MaterialTheme.typography.displayLarge,
                color = if (connected) Mint else TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (connected && app.trafficVerified) {
                    Icon(
                        Icons.Rounded.Verified, null,
                        tint = Mint,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "Traffic verified · ${app.verifiedLatencyMs} ms",
                        style = MaterialTheme.typography.labelMedium,
                        color = Mint.copy(alpha = 0.9f)
                    )
                } else {
                    Text(
                        when {
                            connected -> "Session time · MTU ${app.activeMtu}"
                            connecting -> "Working through the endpoint pool"
                            else -> "Session time"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = TextFaint
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        // ---- Live throughput -----------------------------------------------
        GradientBorderCard(
            Modifier.fillMaxWidth(),
            colors = if (connected) listOf(Mint, Cyan) else listOf(Cyan, Violet)
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("Live speed", accent = if (connected) Mint else Cyan)
                    Text(
                        if (connected) "1s samples" else "idle",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextFaint
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RateReadout(
                        icon = { Icon(Icons.Rounded.Download, null, tint = Cyan, modifier = Modifier.size(15.dp)) },
                        label = "Download",
                        bps = app.stats.downBps,
                        total = formatBytes(app.stats.rxBytes),
                        color = Cyan
                    )
                    Box(
                        Modifier
                            .width(1.dp)
                            .height(56.dp)
                            .background(Color.White.copy(alpha = 0.06f))
                    )
                    RateReadout(
                        icon = { Icon(Icons.Rounded.Upload, null, tint = Violet, modifier = Modifier.size(15.dp)) },
                        label = "Upload",
                        bps = app.stats.upBps,
                        total = formatBytes(app.stats.txBytes),
                        color = Violet,
                        alignEnd = true
                    )
                }

                Spacer(Modifier.height(16.dp))

                SpeedGraph(
                    down = app.speed.downSeries(),
                    up = app.speed.upSeries(),
                    active = connected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                )

                if (!connected) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "The graph fills with real samples once the tunnel is up.",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextFaint
                    )
                } else {
                    Spacer(Modifier.height(10.dp))
                    HairlineDivider()
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PeakStat("Peak down", app.stats.peakDownBps, Cyan)
                        PeakStat("Peak up", app.stats.peakUpBps, Violet)
                        PeakStat("Total", app.stats.rxBytes + app.stats.txBytes, Mint, isBytes = true)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---- Current server -------------------------------------------------
        val profile = app.activeProfile ?: app.currentProfile()
        GlassCard(
            Modifier
                .fillMaxWidth()
                .clickableNoRipple(onOpenServers),
            accent = Cyan
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Cyan.copy(alpha = 0.20f), Violet.copy(alpha = 0.16f))))
                        .border(1.dp, Cyan.copy(alpha = 0.22f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Bolt, null, tint = Cyan, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Current server",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        profile?.endpointLabel ?: "Auto-select on connect",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        buildString {
                            append("WireGuard · ChaCha20-Poly1305")
                            if (app.activeMtu > 0) append(" · MTU ${app.activeMtu}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextFaint
                    )
                }
                Icon(
                    Icons.Rounded.ChevronRight, null,
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---- Transport summary (read-only for users) ------------------------
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("Active profile", accent = Violet)
                    Text(
                        "Managed",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextFaint
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MiniStat("Carrier", app.transport.carrier.short)
                    MiniStat("SNI", app.transport.effectiveSni.take(22))
                    MiniStat("DNS", app.transport.dnsId.removePrefix("dns-"))
                }
                Spacer(Modifier.height(12.dp))
                HairlineDivider()
                Spacer(Modifier.height(12.dp))
                Text(
                    "These settings are locked by the administrator so the tunnel always " +
                        "starts in a working configuration.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextFaint
                )
            }
        }

        // ---- Error / progress ----------------------------------------------
        if (app.state == TunnelState.ERROR && app.error != null) {
            Spacer(Modifier.height(12.dp))
            GlassCard(Modifier.fillMaxWidth(), accent = Rose) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Rose)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Connection failed",
                            style = MaterialTheme.typography.titleMedium,
                            color = Rose
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        app.error ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        if (connecting && app.attempts.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            GlassCard(Modifier.fillMaxWidth(), accent = Amber) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(13.dp),
                            strokeWidth = 1.6.dp,
                            color = Amber
                        )
                        Spacer(Modifier.width(9.dp))
                        Text(
                            "Trying ${app.attempts.size} combination(s)",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    app.attempts.takeLast(3).forEach { a ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                a.endpoint.key,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextTertiary
                            )
                            Text(
                                when {
                                    a.connected -> "connected"
                                    a.mtu > 0 -> "MTU ${a.mtu} · no traffic"
                                    else -> "no reply"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (a.connected) Mint else TextFaint
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(26.dp))
    }
}

@Composable
private fun PeakStat(label: String, value: Long, color: Color, isBytes: Boolean = false) {
    Column {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextFaint
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (isBytes) formatBytes(value) else formatRateParts(value).let { "${it.first} ${it.second}" },
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextFaint
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RateReadout(
    icon: @Composable () -> Unit,
    label: String,
    bps: Long,
    total: String,
    color: Color,
    alignEnd: Boolean = false
) {
    val (value, unit) = formatRateParts(bps)
    Column(
        Modifier.width(136.dp),
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(6.dp))
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                style = MaterialTheme.typography.displaySmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(4.dp))
            Text(
                unit,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                modifier = Modifier.padding(bottom = 5.dp)
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            total,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}
