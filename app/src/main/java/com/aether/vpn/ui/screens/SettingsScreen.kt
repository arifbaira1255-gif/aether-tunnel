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
import androidx.compose.ui.unit.sp
import com.aether.vpn.core.Carrier
import com.aether.vpn.core.TransportCatalog
import com.aether.vpn.ui.AppState
import com.aether.vpn.ui.components.Chip
import com.aether.vpn.ui.components.GlassCard
import com.aether.vpn.ui.components.HairlineDivider
import com.aether.vpn.ui.components.KeyValueRow
import com.aether.vpn.ui.components.Pill
import com.aether.vpn.ui.components.SectionLabel
import com.aether.vpn.ui.components.clickableNoRipple
import com.aether.vpn.ui.theme.Amber
import com.aether.vpn.ui.theme.Cyan
import com.aether.vpn.ui.theme.Mint
import com.aether.vpn.ui.theme.TextFaint
import com.aether.vpn.ui.theme.TextPrimary
import com.aether.vpn.ui.theme.TextSecondary
import com.aether.vpn.ui.theme.TextTertiary
import com.aether.vpn.ui.theme.Violet

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
        Spacer(Modifier.height(14.dp))
        Text("Settings", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
        Spacer(Modifier.height(3.dp))
        Text(
            "Tunnel behaviour and advanced configuration",
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary
        )

        // ---- Carrier -------------------------------------------------------
        Spacer(Modifier.height(20.dp))
        SectionLabel("Your network", accent = Mint)
        Spacer(Modifier.height(10.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(15.dp)) {
                Text(
                    "Sets the SNI host, payload and DNS together",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextTertiary
                )
                Spacer(Modifier.height(11.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Carrier.entries.take(3).forEach { c ->
                        Chip(
                            c.short,
                            app.transport.carrier == c,
                            accent = Mint
                        ) { app.selectCarrier(c) }
                    }
                }
                Spacer(Modifier.height(7.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Carrier.entries.drop(3).forEach { c ->
                        Chip(
                            c.short,
                            app.transport.carrier == c,
                            accent = Mint
                        ) { app.selectCarrier(c) }
                    }
                }
            }
        }

        // ---- Connection ----------------------------------------------------
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

                HairlineDivider(inset = 16)

                ToggleRow(
                    title = "Kill switch",
                    subtitle = "Block traffic if the tunnel drops",
                    checked = killSwitch
                ) { killSwitch = it; store.killSwitch = it }

                HairlineDivider(inset = 16)

                ToggleRow(
                    title = "Auto-failover",
                    subtitle = "Try every endpoint until one answers",
                    checked = app.transport.autoFailover
                ) { app.setAutoFailover(it) }

                HairlineDivider(inset = 16)

                ToggleRow(
                    title = "Auto MTU",
                    subtitle = "Step the MTU down until the tunnel comes up",
                    checked = app.transport.autoMtu
                ) { app.setAutoMtu(it) }

                HairlineDivider(inset = 16)

                ToggleRow(
                    title = "Watchdog",
                    subtitle = "Reconnect when the tunnel stalls",
                    checked = app.transport.watchdog
                ) { app.setWatchdog(it) }
            }
        }

        // ---- Transport summary ---------------------------------------------
        Spacer(Modifier.height(20.dp))
        SectionLabel("Transport \u00b7 SNI, payload & endpoints")
        Spacer(Modifier.height(10.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                KeyValueRow("SNI host", app.activeSni(), mono = true)
                Spacer(Modifier.height(9.dp))
                KeyValueRow(
                    "Payload",
                    TransportCatalog.payloadById(app.transport.payloadId)?.name ?: "Custom",
                    mono = true
                )
                Spacer(Modifier.height(9.dp))
                KeyValueRow("Endpoint", app.transport.endpointKey, mono = true)
                Spacer(Modifier.height(9.dp))
                KeyValueRow(
                    "Last good MTU",
                    store.lastGoodMtu.takeIf { it > 0 }?.toString() ?: "not yet measured"
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Open the Transport tab to pick from " +
                        "${TransportCatalog.sniHosts.size} SNI hosts, " +
                        "${TransportCatalog.payloads.size} payload templates and " +
                        "${TransportCatalog.endpoints.size} endpoints.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextTertiary
                )
            }
        }

        // ---- DNS -----------------------------------------------------------
        Spacer(Modifier.height(20.dp))
        SectionLabel("DNS resolver")
        Spacer(Modifier.height(10.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(15.dp)) {
                TransportCatalog.dnsPresets.chunked(2).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        row.forEach { preset ->
                            Chip(
                                preset.name,
                                app.transport.dnsId == preset.id,
                                accent = Cyan
                            ) { app.setDnsPreset(preset.id) }
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    TransportCatalog.dnsById(app.transport.dnsId)?.servers?.joinToString(", ")
                        ?: dns,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextFaint
                )
            }
        }

        // ---- Tunnel --------------------------------------------------------
        Spacer(Modifier.height(20.dp))
        SectionLabel("Tunnel")
        Spacer(Modifier.height(10.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                FieldRow("MTU", mtu, "1280") { mtu = it; store.mtu = it.toIntOrNull() ?: 1280 }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (app.transport.autoMtu)
                        "Auto MTU is on, so this is only the starting value. The ladder is " +
                            "${TransportCatalog.mtuLadder.joinToString(" \u2192 ")}."
                    else "Auto MTU is off, so this exact value is used.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextFaint
                )
                Spacer(Modifier.height(14.dp))
                FieldRow("DNS", dns, "1.1.1.1, 1.0.0.1") { dns = it; store.dns = it }
            }
        }

        // ---- Protocol ------------------------------------------------------
        Spacer(Modifier.height(20.dp))
        SectionLabel("Protocol & encryption")
        Spacer(Modifier.height(10.dp))

        GlassCard(Modifier.fillMaxWidth(), accent = Mint) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
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
                            "Userspace Go implementation inside Android VpnService",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary
                        )
                    }
                    Pill("Active", Mint, filled = true)
                }
                Spacer(Modifier.height(13.dp))
                HairlineDivider()
                Spacer(Modifier.height(13.dp))
                KeyValueRow("Key agreement", "Curve25519 (X25519)")
                Spacer(Modifier.height(7.dp))
                KeyValueRow("Data channel", "ChaCha20-Poly1305")
                Spacer(Modifier.height(7.dp))
                KeyValueRow("Hashing", "BLAKE2s")
                Spacer(Modifier.height(7.dp))
                KeyValueRow("Rekey", "every 2 minutes")
                Spacer(Modifier.height(7.dp))
                KeyValueRow("Forward secrecy", "ephemeral keypair per handshake")
                Spacer(Modifier.height(12.dp))
                Text(
                    "These are WireGuard's own primitives, enforced by the backend \u2014 there is " +
                        "no weaker mode to fall back to and no setting that turns them off.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextFaint
                )
            }
        }

        // ---- About ---------------------------------------------------------
        Spacer(Modifier.height(20.dp))
        SectionLabel("About")
        Spacer(Modifier.height(10.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                KeyValueRow("App", "Aether Tunnel")
                Spacer(Modifier.height(9.dp))
                KeyValueRow("Version", "1.2.0")
                Spacer(Modifier.height(9.dp))
                KeyValueRow("Server source", "Cloudflare WARP (free, unlimited)")
                Spacer(Modifier.height(9.dp))
                KeyValueRow("Tunnel", "Real WireGuard via Android VpnService")
                Spacer(Modifier.height(9.dp))
                KeyValueRow("Endpoints", "${TransportCatalog.endpoints.size} in the pool")
                Spacer(Modifier.height(9.dp))
                KeyValueRow("SNI hosts", "${TransportCatalog.sniHosts.size} built in")
                Spacer(Modifier.height(9.dp))
                KeyValueRow("Payloads", "${TransportCatalog.payloads.size} templates")
            }
        }

        Spacer(Modifier.height(12.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Amber)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "A VPN encrypts and routes your traffic. It does not create a mobile data " +
                        "allowance: the device still needs a working connection (Wi-Fi or data) " +
                        "for the tunnel to carry anything.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextTertiary
                )
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
        Spacer(Modifier.width(10.dp))
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
                Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = TextFaint)
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
