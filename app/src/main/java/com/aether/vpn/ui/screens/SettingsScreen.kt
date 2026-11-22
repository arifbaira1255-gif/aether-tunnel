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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Lock
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.vpn.core.Locations
import com.aether.vpn.ui.AppState
import com.aether.vpn.ui.components.GlassCard
import com.aether.vpn.ui.components.HairlineDivider
import com.aether.vpn.ui.components.KeyValueRow
import com.aether.vpn.ui.components.Pill
import com.aether.vpn.ui.components.ScreenHeader
import com.aether.vpn.ui.components.SectionLabel
import com.aether.vpn.ui.components.clickableNoRipple
import com.aether.vpn.ui.theme.Cyan
import com.aether.vpn.ui.theme.Mint
import com.aether.vpn.ui.theme.TextFaint
import com.aether.vpn.ui.theme.TextPrimary
import com.aether.vpn.ui.theme.TextSecondary
import com.aether.vpn.ui.theme.TextTertiary
import com.aether.vpn.ui.theme.Violet

/**
 * Settings - only the things a person actually decides.
 *
 * Everything technical (SNI host, payload, encryption, endpoint, MTU, DNS) is configured
 * inside the app and never shown here. The two switches below are genuine preferences:
 * they do not touch how the tunnel is built, only whether it starts on its own and what
 * happens to traffic if it drops.
 */
@Composable
fun SettingsScreen(app: AppState) {
    val store = app.store

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
            eyebrow = "Aether Tunnel 1.4.0",
            title = "Settings",
            trailing = {
                Pill(text = "Protected", color = Mint, filled = true)
            }
        )

        Spacer(Modifier.height(18.dp))

        // ---- Protection status ---------------------------------------------
        GlassCard(Modifier.fillMaxWidth(), accent = Cyan) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Cyan.copy(alpha = 0.26f), Violet.copy(alpha = 0.20f))
                                )
                            )
                            .border(1.dp, Cyan.copy(alpha = 0.30f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Shield, null, tint = Cyan, modifier = Modifier.size(21.dp))
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Aether Tunnel",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "WireGuard \u00b7 ChaCha20-Poly1305",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                    Icon(Icons.Rounded.CheckCircle, null, tint = Mint, modifier = Modifier.size(19.dp))
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ---- Preferences ----------------------------------------------------
        SectionLabel("Preferences")

        Spacer(Modifier.height(9.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 4.dp)) {
                ToggleRow(
                    icon = Icons.Rounded.Bolt,
                    title = "Connect on launch",
                    subtitle = "Start the tunnel as soon as the app opens",
                    checked = autoConnect,
                    accent = Cyan
                ) { autoConnect = it; store.autoConnect = it }

                HairlineDivider()

                ToggleRow(
                    icon = Icons.Rounded.Lock,
                    title = "Kill switch",
                    subtitle = "Block all traffic if the tunnel drops",
                    checked = killSwitch,
                    accent = Violet
                ) { killSwitch = it; store.killSwitch = it }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ---- Current server -------------------------------------------------
        SectionLabel("Current server")

        Spacer(Modifier.height(9.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        app.activeServerFlag(),
                        fontSize = 24.sp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            app.activeServerTitle(),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Change it any time on the Servers screen",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextFaint
                        )
                    }
                    Icon(Icons.Rounded.Dns, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ---- About ----------------------------------------------------------
        SectionLabel("About")

        Spacer(Modifier.height(9.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                KeyValueRow("Version", "1.4.0")
                HairlineDivider()
                KeyValueRow("Protocol", "WireGuard (userspace)")
                HairlineDivider()
                KeyValueRow("Encryption", "ChaCha20-Poly1305")
                HairlineDivider()
                KeyValueRow("Handshake", "Noise IKpsk2 \u00b7 Curve25519")
                HairlineDivider()
                KeyValueRow(
                    "Locations",
                    "${Locations.all.size} in ${Locations.all.map { it.country }.distinct().size} countries"
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Aether Tunnel encrypts your connection and routes it through a secure server. " +
                "It does not replace your internet connection, and it cannot create one where " +
                "your device has none.",
            style = MaterialTheme.typography.bodySmall,
            color = TextFaint
        )

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun ToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    accent: Color,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.12f))
                .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextFaint
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accent,
                uncheckedThumbColor = TextTertiary,
                uncheckedTrackColor = Color.White.copy(alpha = 0.09f),
                uncheckedBorderColor = Color.White.copy(alpha = 0.14f)
            )
        )
    }
}
