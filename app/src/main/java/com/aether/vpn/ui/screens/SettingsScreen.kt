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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.vpn.core.TransportCatalog
import com.aether.vpn.ui.AppState
import com.aether.vpn.ui.components.GlassCard
import com.aether.vpn.ui.components.HairlineDivider
import com.aether.vpn.ui.components.KeyValueRow
import com.aether.vpn.ui.components.Pill
import com.aether.vpn.ui.components.ScreenHeader
import com.aether.vpn.ui.components.SectionLabel
import com.aether.vpn.ui.components.clickableNoRipple
import com.aether.vpn.ui.theme.Amber
import com.aether.vpn.ui.theme.Cyan
import com.aether.vpn.ui.theme.Mint
import com.aether.vpn.ui.theme.Rose
import com.aether.vpn.ui.theme.TextFaint
import com.aether.vpn.ui.theme.TextPrimary
import com.aether.vpn.ui.theme.TextSecondary
import com.aether.vpn.ui.theme.TextTertiary
import com.aether.vpn.ui.theme.Violet

/**
 * Settings.
 *
 * Everything that can break the tunnel — SNI host, payload, endpoint, MTU, DNS, IPv6,
 * failover — is ADMIN-OWNED. A normal user sees the active configuration read-only with
 * a lock badge and cannot change it, so the app can never be put into a state where it
 * connects and then carries nothing. The admin unlocks with a PIN to edit.
 */
@Composable
fun SettingsScreen(app: AppState) {
    val store = app.store
    val unlocked = app.adminUnlocked

    var pin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var newPin by remember { mutableStateOf("") }
    var pinChanged by remember { mutableStateOf(false) }

    var autoConnect by remember { mutableStateOf(store.autoConnect) }
    var killSwitch by remember { mutableStateOf(store.killSwitch) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        ScreenHeader(
            eyebrow = "Aether Tunnel 1.3.0",
            title = "Settings",
            trailing = {
                Pill(
                    text = if (unlocked) "Admin" else "Locked",
                    color = if (unlocked) Mint else TextSecondary,
                    filled = true
                )
            }
        )

        Spacer(Modifier.height(18.dp))

        // ---- Admin gate ----------------------------------------------------
        if (!unlocked) {
            GlassCard(Modifier.fillMaxWidth(), accent = Amber) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(Amber.copy(alpha = 0.14f))
                                .border(1.dp, Amber.copy(alpha = 0.28f), RoundedCornerShape(11.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Lock, null, tint = Amber, modifier = Modifier.size(17.dp))
                        }
                        Spacer(Modifier.width(11.dp))
                        Column {
                            Text(
                                "Administrator settings",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                "Locked so the tunnel always starts working",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.weight(1f)) {
                            if (pin.isEmpty()) {
                                Text(
                                    "Admin PIN",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextFaint
                                )
                            }
                            BasicTextField(
                                value = pin,
                                onValueChange = {
                                    pin = it.filter { c -> c.isDigit() }.take(8)
                                    pinError = null
                                },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                                cursorBrush = SolidColor(Cyan),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(Cyan.copy(alpha = 0.16f))
                                .border(1.dp, Cyan.copy(alpha = 0.34f), CircleShape)
                                .clickableNoRipple {
                                    if (app.unlockAdmin(pin)) {
                                        pin = ""
                                        pinError = null
                                    } else {
                                        pinError = "Wrong PIN"
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Unlock",
                                style = MaterialTheme.typography.labelLarge,
                                color = Cyan
                            )
                        }
                    }

                    if (pinError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            pinError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Rose
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Default PIN is 2468 — change it below once you are in.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextFaint
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // ---- Active configuration (read-only unless admin) ------------------
        SectionLabel("Active configuration", accent = if (unlocked) Mint else Amber)
        Spacer(Modifier.height(10.dp))

        GlassCard(Modifier.fillMaxWidth(), accent = if (unlocked) Mint else null) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Managed by administrator",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Icon(
                        if (unlocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                        null,
                        tint = if (unlocked) Mint else Amber,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))

                KeyValueRow("Carrier profile", app.transport.carrier.title)
                Spacer(Modifier.height(9.dp))
                KeyValueRow("SNI host", app.transport.effectiveSni, mono = true)
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
                    "MTU",
                    if (app.transport.autoMtu) "auto ladder (1420→1200)" else "${store.mtu} fixed"
                )
                Spacer(Modifier.height(9.dp))
                KeyValueRow(
                    "DNS",
                    TransportCatalog.dnsById(app.transport.dnsId)?.name ?: "Custom"
                )
                Spacer(Modifier.height(9.dp))
                KeyValueRow(
                    "Encryption",
                    "WireGuard · ChaCha20-Poly1305"
                )
                Spacer(Modifier.height(9.dp))
                KeyValueRow(
                    "IPv6 routing",
                    if (app.transport.routeIpv6) "on" else "off (IPv4 only)"
                )
                Spacer(Modifier.height(9.dp))
                KeyValueRow(
                    "Traffic verification",
                    if (app.transport.verifyTraffic) "on" else "off"
                )

                Spacer(Modifier.height(14.dp))
                HairlineDivider()
                Spacer(Modifier.height(12.dp))
                Text(
                    if (unlocked) {
                        "You are the administrator. Open the Transport tab to change any of " +
                            "these values."
                    } else {
                        "These values are fixed by the administrator. They are already set to a " +
                            "working configuration, so you never need to touch them."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextFaint
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // ---- User preferences (always editable) ----------------------------
        SectionLabel("Preferences", accent = Cyan)
        Spacer(Modifier.height(10.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                ToggleRow(
                    title = "Auto-connect on launch",
                    subtitle = "Bring the tunnel up as soon as the app opens",
                    checked = autoConnect,
                    onChecked = {
                        autoConnect = it
                        store.autoConnect = it
                    }
                )
                Spacer(Modifier.height(14.dp))
                HairlineDivider()
                Spacer(Modifier.height(14.dp))
                ToggleRow(
                    title = "Kill switch",
                    subtitle = "Block traffic if the tunnel drops unexpectedly",
                    checked = killSwitch,
                    onChecked = {
                        killSwitch = it
                        store.killSwitch = it
                    }
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // ---- Admin panel (only when unlocked) ------------------------------
        if (unlocked) {
            SectionLabel("Administrator", accent = Mint)
            Spacer(Modifier.height(10.dp))

            GlassCard(Modifier.fillMaxWidth(), accent = Mint) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Change admin PIN",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.weight(1f)) {
                            if (newPin.isEmpty()) {
                                Text(
                                    "New PIN (4–8 digits)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextFaint
                                )
                            }
                            BasicTextField(
                                value = newPin,
                                onValueChange = {
                                    newPin = it.filter { c -> c.isDigit() }.take(8)
                                    pinChanged = false
                                },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                                cursorBrush = SolidColor(Mint),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(Mint.copy(alpha = 0.16f))
                                .border(1.dp, Mint.copy(alpha = 0.34f), CircleShape)
                                .clickableNoRipple {
                                    if (newPin.length >= 4) {
                                        store.setAdminPin(newPin)
                                        newPin = ""
                                        pinChanged = true
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Save", style = MaterialTheme.typography.labelLarge, color = Mint)
                        }
                    }
                    if (pinChanged) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.CheckCircle, null,
                                tint = Mint,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "PIN updated",
                                style = MaterialTheme.typography.bodySmall,
                                color = Mint
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HairlineDivider()
                    Spacer(Modifier.height(14.dp))

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Rose.copy(alpha = 0.10f))
                            .border(1.dp, Rose.copy(alpha = 0.26f), RoundedCornerShape(14.dp))
                            .clickableNoRipple { app.lockAdmin() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Lock admin panel",
                            style = MaterialTheme.typography.labelLarge,
                            color = Rose
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
        }

        // ---- About ---------------------------------------------------------
        SectionLabel("About", accent = Violet)
        Spacer(Modifier.height(10.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(Cyan, Violet))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Shield, null,
                            tint = Color(0xFF05060D),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Aether Tunnel",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            "Version 1.3.0 · WireGuard core",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextFaint
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                HairlineDivider()
                Spacer(Modifier.height(12.dp))
                Text(
                    "The tunnel is a real WireGuard connection running inside Android's " +
                        "VpnService. Encryption is WireGuard's own: Curve25519 key agreement, " +
                        "ChaCha20-Poly1305 data channel, BLAKE2s hashing and a fresh ephemeral " +
                        "keypair per handshake with a 2-minute rekey.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "A VPN encrypts and routes your traffic. It does not create a mobile data " +
                        "allowance — the device still needs a working internet connection for the " +
                        "tunnel to carry anything.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextFaint
                )
            }
        }

        Spacer(Modifier.height(26.dp))
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextFaint
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF05060D),
                checkedTrackColor = Cyan,
                uncheckedThumbColor = TextTertiary,
                uncheckedTrackColor = Color.White.copy(alpha = 0.10f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
