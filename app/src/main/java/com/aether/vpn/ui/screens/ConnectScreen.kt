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

    // Live session clock
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
        Spacer(Modifier.height(14.dp))

        // ---- Header -------------------------------------------------------
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Brush.linearGradient(listOf(Cyan, Violet))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Shield, null,
                            tint = Color(0xFF04050C),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "AETHER",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        letterSpacing = 2.4.sp
                    )
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    when {
                        connected -> "Protected"
                        connecting -> "Securing\u2026"
                        app.state == TunnelState.ERROR -> "Not connected"
                        else -> "Not protected"
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Pill(
                    text = when {
                        connected -> "Live"
                        connecting -> "Working"
                        app.state == TunnelState.ERROR -> "Error"
                        else -> "Idle"
                    },
                    color = when {
                        connected -> Mint
                        connecting -> Amber
                        app.state == TunnelState.ERROR -> Rose
                        else -> TextSecondary
                    },
                    filled = true
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    app.transport.carrier.short,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextFaint
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // ---- Signature connect button -------------------------------------
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ConnectButton(
                state = app.state,
                onClick = { if (connected) onDisconnect() else onConnect() }
            )
        }

        Spacer(Modifier.height(16.dp))

        // ---- Session timer -------------------------------------------------
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                formatDuration(elapsed),
                style = MaterialTheme.typography.displayLarge,
                color = if (connected) Mint else TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                when {
                    connected -> "Session time \u00b7 MTU ${app.activeMtu}"
                    connecting -> app.failoverNote ?: "Negotiating the tunnel\u2026"
                    else -> "Tap the button to start a secure session"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary
            )
        }

        Spacer(Modifier.height(20.dp))

        // ---- Live throughput + graph ---------------------------------------
        GlassCard(Modifier.fillMaxWidth(), accent = if (connected) Mint else Cyan) {
            Column(Modifier.padding(18.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("Live throughput", accent = if (connected) Mint else Cyan)
                    Text(
                        if (connected) "1s samples" else "idle",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextFaint
                    )
                }

                Spacer(Modifier.height(14.dp))

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
                            .height(52.dp)
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

                Spacer(Modifier.height(14.dp))

                SpeedGraph(
                    down = app.speed.downSeries(),
                    up = app.speed.upSeries(),
                    active = connected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                )

                if (!connected) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "The graph fills with real samples once the tunnel is up.",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextFaint
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---- Current server row --------------------------------------------
        val profile = app.activeProfile ?: app.currentProfile()
        GlassCard(
            Modifier
                .fillMaxWidth()
                .clickableNoRipple { onOpenServers() }
        ) {
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
                        .background(Color.White.copy(alpha = 0.045f))
                        .border(1.dp, Color.White.copy(alpha = 0.07f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(profile?.flag ?: "\uD83C\uDF10", fontSize = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        profile?.name ?: "Aether Auto",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${profile?.country ?: "Global"} \u00b7 ${profile?.city ?: "Anycast edge"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        app.transport.endpointKey,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        app.bestLatency()?.let { "$it ms best" } ?: "not tested",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextFaint
                    )
                }
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Rounded.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---- Transport summary ---------------------------------------------
        GlassCard(
            Modifier
                .fillMaxWidth()
                .clickableNoRipple { onOpenTransport() }
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Bolt, null, tint = Cyan, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Transport",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "SNI ${app.activeSni()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary,
                        maxLines = 1
                    )
                }
                Pill(
                    text = com.aether.vpn.core.TransportCatalog.payloadById(app.transport.payloadId)?.carrier
                        ?.takeIf { it.isNotBlank() } ?: "Generic",
                    color = Violet
                )
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Rounded.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
            }
        }

        // ---- Failover progress ---------------------------------------------
        if (app.attempts.isNotEmpty() && !connected) {
            Spacer(Modifier.height(12.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(15.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (app.busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(13.dp),
                                color = Cyan,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(Rose))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            app.failoverNote ?: "Connection attempts",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Spacer(Modifier.height(9.dp))
                    app.attempts.takeLast(4).forEach { a ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${a.endpoint.key}${if (a.mtu > 0) " \u00b7 ${a.mtu}" else ""}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextTertiary
                            )
                            Text(
                                if (a.connected) "up" else (a.error ?: "failed"),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (a.connected) Mint else TextFaint,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // ---- Error ---------------------------------------------------------
        app.error?.let { msg ->
            Spacer(Modifier.height(12.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Rose)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Rose
                    )
                }
            }
        }

        Spacer(Modifier.height(26.dp))
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
        Modifier.width(132.dp),
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
        Spacer(Modifier.height(7.dp))
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
        Spacer(Modifier.height(1.dp))
        Text(
            total,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}
