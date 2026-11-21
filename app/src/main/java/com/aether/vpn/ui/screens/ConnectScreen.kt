package com.aether.vpn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Upload
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.vpn.core.TunnelState
import com.aether.vpn.ui.AppState
import com.aether.vpn.ui.components.ConnectButton
import com.aether.vpn.ui.components.GlassCard
import com.aether.vpn.ui.components.Pill
import com.aether.vpn.ui.components.formatBytes
import com.aether.vpn.ui.components.formatDuration
import com.aether.vpn.ui.components.formatRate
import com.aether.vpn.ui.theme.Cyan
import com.aether.vpn.ui.theme.Mint
import com.aether.vpn.ui.theme.Rose
import com.aether.vpn.ui.theme.TextPrimary
import com.aether.vpn.ui.theme.TextSecondary
import com.aether.vpn.ui.theme.TextTertiary
import com.aether.vpn.ui.theme.Violet
import kotlinx.coroutines.delay

@Composable
fun ConnectScreen(
    app: AppState,
    onOpenServers: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val connected = app.state == TunnelState.CONNECTED

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
        Spacer(Modifier.height(18.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "AETHER TUNNEL",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    if (connected) "Protected" else "Not protected",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
            }
            Pill(
                text = if (connected) "Live" else "Idle",
                color = if (connected) Mint else TextSecondary,
                filled = true
            )
        }

        Spacer(Modifier.height(26.dp))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ConnectButton(
                state = app.state,
                onClick = { if (connected) onDisconnect() else onConnect() }
            )
        }

        Spacer(Modifier.height(22.dp))

        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                formatDuration(elapsed),
                style = MaterialTheme.typography.displayLarge,
                color = if (connected) Mint else TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (connected) "Session time" else "Tap the button to start a secure session",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary
            )
        }

        Spacer(Modifier.height(24.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ThroughputCell(
                    icon = { Icon(Icons.Rounded.Download, null, tint = Cyan, modifier = Modifier.size(16.dp)) },
                    label = "Download",
                    value = formatRate(app.stats.downBps),
                    total = formatBytes(app.stats.rxBytes)
                )
                Box(
                    Modifier
                        .width(1.dp)
                        .height(46.dp)
                        .background(Color.White.copy(alpha = 0.07f))
                )
                ThroughputCell(
                    icon = { Icon(Icons.Rounded.Upload, null, tint = Violet, modifier = Modifier.size(16.dp)) },
                    label = "Upload",
                    value = formatRate(app.stats.upBps),
                    total = formatBytes(app.stats.txBytes)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        val profile = app.activeProfile ?: app.currentProfile()
        GlassCard(
            Modifier
                .fillMaxWidth()
                .clickable { onOpenServers() }
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
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(profile?.flag ?: "\uD83C\uDF10", fontSize = 19.sp)
                }
                Spacer(Modifier.width(13.dp))
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
                Icon(Icons.Rounded.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(14.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Rounded.Bolt, null, tint = Cyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(11.dp))
                Column {
                    Text(
                        "Real WireGuard tunnel",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Traffic is routed through the peer with the WireGuard protocol. " +
                            "The first connection asks Android for VPN permission.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary
                    )
                }
            }
        }

        app.error?.let { msg ->
            Spacer(Modifier.height(14.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Rose)
                    )
                    Spacer(Modifier.width(11.dp))
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Rose
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun ThroughputCell(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    total: String
) {
    Column(Modifier.width(140.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(7.dp))
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            total,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}
