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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aether.vpn.core.TransportCatalog
import com.aether.vpn.ui.AppState
import com.aether.vpn.ui.components.GlassCard
import com.aether.vpn.ui.components.Pill
import com.aether.vpn.ui.components.SectionLabel
import com.aether.vpn.ui.theme.Cyan
import com.aether.vpn.ui.theme.Mint
import com.aether.vpn.ui.theme.TextPrimary
import com.aether.vpn.ui.theme.TextSecondary
import com.aether.vpn.ui.theme.TextTertiary

@Composable
fun SettingsScreen(app: AppState) {
    val store = app.store

    var autoConnect by remember { mutableStateOf(store.autoConnect) }
    var killSwitch by remember { mutableStateOf(store.killSwitch) }
    var mtu by remember { mutableStateOf(store.mtu.toString()) }
    var dns by remember { mutableStateOf(store.dns) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Text("Settings", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
        Spacer(Modifier.height(3.dp))
        Text(
            "Tunnel behaviour and advanced configuration",
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary
        )

        Spacer(Modifier.height(20.dp))
        SectionLabel("Connection")
        Spacer(Modifier.height(10.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 4.dp)) {
                ToggleRow(
                    title = "Auto-connect",
                    subtitle = "Bring the tunnel up when the app opens",
                    checked = autoConnect
                ) { autoConnect = it; store.autoConnect = it }

                Divider()

                ToggleRow(
                    title = "Kill switch",
                    subtitle = "Block traffic if the tunnel drops",
                    checked = killSwitch
                ) { killSwitch = it; store.killSwitch = it }
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("Transport \u00b7 SNI, payload & endpoints")
        Spacer(Modifier.height(10.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                AboutRow("SNI host", app.activeSni())
                Spacer(Modifier.height(10.dp))
                AboutRow(
                    "Payload",
                    TransportCatalog.payloadById(app.transport.payloadId)?.name ?: "Custom"
                )
                Spacer(Modifier.height(10.dp))
                AboutRow("Endpoint", app.transport.endpointKey)
                Spacer(Modifier.height(10.dp))
                AboutRow(
                    "Auto-failover",
                    if (app.transport.autoFailover) "On" else "Off"
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Open the Transport tab to pick from " +
                        "${TransportCatalog.sniHosts.size} SNI hosts, " +
                        "${TransportCatalog.payloads.size} payload templates " +
                        "and ${TransportCatalog.endpoints.size} endpoints.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextTertiary
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("Tunnel")
        Spacer(Modifier.height(10.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                FieldRow("MTU", mtu, "1280") { mtu = it; store.mtu = it.toIntOrNull() ?: 1280 }
                Spacer(Modifier.height(14.dp))
                FieldRow("DNS", dns, "1.1.1.1, 1.0.0.1") { dns = it; store.dns = it }
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("Protocol")
        Spacer(Modifier.height(10.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "WireGuard",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Userspace Go implementation, ChaCha20-Poly1305",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary
                    )
                }
                Pill("Active", Mint, filled = true)
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("About")
        Spacer(Modifier.height(10.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                AboutRow("App", "Aether Tunnel")
                Spacer(Modifier.height(10.dp))
                AboutRow("Version", "1.1.0")
                Spacer(Modifier.height(10.dp))
                AboutRow("Server source", "Cloudflare WARP (free, unlimited)")
                Spacer(Modifier.height(10.dp))
                AboutRow("Tunnel", "Real WireGuard via Android VpnService")
                Spacer(Modifier.height(10.dp))
                AboutRow("Endpoints", "${TransportCatalog.endpoints.size} in the pool")
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Cyan.copy(alpha = 0.7f),
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = Color.White.copy(alpha = 0.08f)
            )
        )
    }
}

@Composable
private fun FieldRow(
    label: String,
    value: String,
    placeholder: String,
    onChange: (String) -> Unit
) {
    Column {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 11.dp)
        ) {
            if (value.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = TextTertiary)
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                cursorBrush = SolidColor(Cyan),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.06f))
    )
}
