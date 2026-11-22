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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.aether.vpn.core.SniCategory
import com.aether.vpn.core.SniHost
import com.aether.vpn.core.TransportCatalog
import com.aether.vpn.ui.AppState
import com.aether.vpn.ui.components.Chip
import com.aether.vpn.ui.components.CodeBlock
import com.aether.vpn.ui.components.EmptyBlock
import com.aether.vpn.ui.components.GlassCard
import com.aether.vpn.ui.components.HairlineDivider
import com.aether.vpn.ui.components.KeyValueRow
import com.aether.vpn.ui.components.Pill
import com.aether.vpn.ui.components.SectionLabel
import com.aether.vpn.ui.components.clickableNoRipple
import com.aether.vpn.ui.theme.Amber
import com.aether.vpn.ui.theme.Cyan
import androidx.compose.material.icons.rounded.Lock
import com.aether.vpn.ui.theme.Mint
import com.aether.vpn.ui.theme.Rose
import com.aether.vpn.ui.theme.TextFaint
import com.aether.vpn.ui.theme.TextPrimary
import com.aether.vpn.ui.theme.TextSecondary
import com.aether.vpn.ui.theme.TextTertiary
import com.aether.vpn.ui.theme.Violet

private enum class TransportTab(val title: String) {
    CARRIER("Carrier"), SNI("SNI hosts"), PAYLOAD("Payloads"), ENDPOINTS("Endpoints")
}

@Composable
fun TransportScreen(app: AppState) {
    var tab by remember { mutableStateOf(TransportTab.CARRIER) }
    val scope = rememberCoroutineScope()

    // Admin-only surface. A normal user sees the active profile read-only on the
    // Connect screen; nothing here can be changed without the PIN.
    if (!app.adminUnlocked) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(14.dp))
            Text("Transport", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
            Spacer(Modifier.height(3.dp))
            Text(
                "Advanced tunnel configuration",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary
            )
            Spacer(Modifier.height(20.dp))
            GlassCard(Modifier.fillMaxWidth(), accent = Amber) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Amber.copy(alpha = 0.14f))
                                .border(1.dp, Amber.copy(alpha = 0.28f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Lock, null,
                                tint = Amber,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Administrator only",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "SNI hosts, payloads, endpoints, MTU and DNS are managed by the " +
                            "administrator. The app already ships with a working profile, so " +
                            "you do not need to change anything to connect.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(16.dp))
                    HairlineDivider()
                    Spacer(Modifier.height(14.dp))
                    KeyValueRow("SNI host", app.transport.effectiveSni, mono = true)
                    Spacer(Modifier.height(9.dp))
                    KeyValueRow("Endpoint", app.transport.endpointKey, mono = true)
                    Spacer(Modifier.height(9.dp))
                    KeyValueRow(
                        "MTU",
                        if (app.transport.autoMtu) "auto ladder" else "${app.store.mtu} fixed"
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Unlock with the admin PIN in Settings to edit these.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextFaint
                    )
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(14.dp))
        Text("Transport", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
        Spacer(Modifier.height(3.dp))
        Text(
            "${TransportCatalog.sniHosts.size} SNI hosts \u00b7 " +
                "${TransportCatalog.payloads.size} payloads \u00b7 " +
                "${TransportCatalog.endpoints.size} endpoints",
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary
        )

        Spacer(Modifier.height(14.dp))

        // Active configuration summary
        GlassCard(Modifier.fillMaxWidth(), accent = Cyan) {
            Column(Modifier.padding(15.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("Active configuration", accent = Cyan)
                    Pill(app.transport.carrier.short, Violet, filled = true)
                }
                Spacer(Modifier.height(11.dp))
                KeyValueRow("SNI", app.activeSni(), mono = true)
                Spacer(Modifier.height(6.dp))
                KeyValueRow(
                    "Payload",
                    TransportCatalog.payloadById(app.transport.payloadId)?.name ?: "Custom",
                    mono = true
                )
                Spacer(Modifier.height(6.dp))
                KeyValueRow("Endpoint", app.transport.endpointKey, mono = true)
                Spacer(Modifier.height(6.dp))
                KeyValueRow(
                    "MTU",
                    if (app.transport.autoMtu) "auto ladder" else "${app.store.mtu} fixed"
                )
                Spacer(Modifier.height(6.dp))
                KeyValueRow(
                    "Failover",
                    if (app.transport.autoFailover) "on \u00b7 12 endpoints" else "off"
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(TransportTab.entries.toList()) { t ->
                Chip(t.title, tab == t) { tab = t }
            }
        }

        Spacer(Modifier.height(12.dp))

        when (tab) {
            TransportTab.CARRIER -> Box(Modifier.weight(1f)) { CarrierTab(app) }
            TransportTab.SNI -> Box(Modifier.weight(1f)) { SniList(app) }
            TransportTab.PAYLOAD -> Box(Modifier.weight(1f)) { PayloadList(app) }
            TransportTab.ENDPOINTS -> Box(Modifier.weight(1f)) {
                EndpointList(app) { app.probeAll(scope) }
            }
        }
    }
}

// ---- Carrier profiles -----------------------------------------------------

@Composable
private fun CarrierTab(app: AppState) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(15.dp)) {
                    Text(
                        "Pick your network",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "This sets the SNI host, the handshake payload and the DNS in one tap. " +
                            "You can still override any of them on the other tabs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary
                    )
                }
            }
        }

        items(Carrier.entries.toList()) { carrier ->
            val selected = app.transport.carrier == carrier
            GlassCard(
                Modifier
                    .fillMaxWidth()
                    .clickableNoRipple { app.selectCarrier(carrier) },
                accent = if (selected) Cyan else null
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.045f))
                            .border(1.dp, Color.White.copy(alpha = 0.07f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(carrier.flag, fontSize = 17.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            carrier.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (carrier == Carrier.AUTO) "Cloudflare WARP defaults"
                            else "${TransportCatalog.sniById(carrier.defaultSniId)?.host ?: "\u2014"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary,
                            maxLines = 1
                        )
                    }
                    if (selected) {
                        Box(
                            Modifier
                                .size(23.dp)
                                .clip(CircleShape)
                                .background(Mint.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Check, null, tint = Mint, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(15.dp)) {
                    SectionLabel("Honest note", accent = Amber)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "A WireGuard tunnel is plain UDP: it has no TLS handshake, so it never " +
                            "sends an SNI or an HTTP Host header. These settings are stored on the " +
                            "profile, shown here, and exported with it \u2014 they take effect when the " +
                            "profile is used with a TLS-wrapping relay. Whether a host is zero-rated " +
                            "is decided by your carrier, not by this app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ---- SNI hosts ------------------------------------------------------------

@Composable
private fun SniList(app: AppState) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<SniCategory?>(null) }

    val filtered = TransportCatalog.sniHosts.filter { h ->
        val matchesQuery = query.isBlank() ||
            h.host.contains(query, true) || h.label.contains(query, true)
        val matchesCategory = category == null || h.category == category
        matchesQuery && matchesCategory
    }

    Column(Modifier.fillMaxSize()) {
        SearchField(query, "Search ${TransportCatalog.sniHosts.size} hosts") { query = it }
        Spacer(Modifier.height(9.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            item { Chip("All", category == null) { category = null } }
            items(SniCategory.entries.toList()) { c ->
                Chip(
                    c.short,
                    category == c,
                    accent = if (c.name.startsWith("PAK") || c == SniCategory.ZERO_RATED) Mint else Cyan
                ) { category = if (category == c) null else c }
            }
        }

        Spacer(Modifier.height(11.dp))

        if (filtered.isEmpty()) {
            EmptyBlock("No SNI host matches", "Try a different name or clear the filter.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                items(filtered, key = { it.id }) { host ->
                    SniRow(app, host)
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun SniRow(app: AppState, host: SniHost) {
    val selected = app.transport.sniId == host.id && app.transport.customSni.isBlank()
    GlassCard(
        Modifier
            .fillMaxWidth()
            .clickableNoRipple { app.selectSni(host.id) },
        accent = if (selected) Mint else null
    ) {
        Row(
            Modifier.fillMaxWidth().padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        host.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    if (host.zeroRated) {
                        Spacer(Modifier.width(7.dp))
                        Pill("zero-rated", Mint)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    host.host,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                if (host.note.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        host.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextFaint,
                        maxLines = 2
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            if (selected) {
                Box(
                    Modifier
                        .size(23.dp)
                        .clip(CircleShape)
                        .background(Mint.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Check, null, tint = Mint, modifier = Modifier.size(14.dp))
                }
            } else {
                Pill(host.category.short, Violet)
            }
        }
    }
}

// ---- Payloads -------------------------------------------------------------

@Composable
private fun PayloadList(app: AppState) {
    var custom by remember { mutableStateOf(app.transport.customPayload) }
    var filter by remember { mutableStateOf<String?>(null) }

    val carriers = listOf("Jazz", "Telenor", "Ufone", "Zong", "Any PK", "Zero-rated")
    val shown = TransportCatalog.payloads.filter { p ->
        filter == null || p.carrier.equals(filter, ignoreCase = true)
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                item { Chip("All", filter == null) { filter = null } }
                items(carriers) { c ->
                    Chip(c, filter == c, accent = Mint) { filter = if (filter == c) null else c }
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        items(shown, key = { it.id }) { preset ->
            val selected = app.transport.payloadId == preset.id
            GlassCard(
                Modifier
                    .fillMaxWidth()
                    .clickableNoRipple { app.selectPayload(preset.id) },
                accent = if (selected) Cyan else null
            ) {
                Column(Modifier.padding(13.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            preset.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (preset.carrier.isNotBlank()) {
                            Pill(preset.carrier, Violet)
                            Spacer(Modifier.width(6.dp))
                        }
                        if (selected) {
                            Box(
                                Modifier
                                    .size(23.dp)
                                    .clip(CircleShape)
                                    .background(Mint.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Check, null, tint = Mint, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    if (preset.note.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            preset.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                    if (preset.template.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        CodeBlock(preset.template.replace("[crlf]", "\u23ce"), maxLines = 4)
                    }
                }
            }
        }

        // Custom editor
        item {
            Spacer(Modifier.height(6.dp))
            SectionLabel("Custom payload", accent = Cyan)
            Spacer(Modifier.height(8.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(13.dp)) {
                    Text(
                        "Placeholders: [crlf] [host] [sni] [host_port] [ua] [random]",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextFaint
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(11.dp))
                            .background(Color.White.copy(alpha = 0.035f))
                            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(11.dp))
                            .padding(11.dp)
                    ) {
                        if (custom.isEmpty()) {
                            Text(
                                "GET / HTTP/1.1[crlf]Host: [sni][crlf][crlf]",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextFaint
                            )
                        }
                        BasicTextField(
                            value = custom,
                            onValueChange = {
                                custom = it
                                app.setCustomPayload(it)
                                if (it.isNotBlank()) app.selectPayload("pl-custom")
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                            cursorBrush = SolidColor(Cyan),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (app.transport.payloadId == "pl-custom" && custom.isNotBlank()) {
                        Spacer(Modifier.height(9.dp))
                        SectionLabel("Rendered")
                        Spacer(Modifier.height(6.dp))
                        CodeBlock(app.activePayload(), maxLines = 8)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ---- Endpoints ------------------------------------------------------------

@Composable
private fun EndpointList(app: AppState, onProbe: () -> Unit) {
    val results = app.probeResults

    Column(Modifier.fillMaxSize()) {
        GlassCard(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${TransportCatalog.endpoints.size} endpoints",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (results.isEmpty()) "Not tested \u2014 tap Test to probe from this network"
                        else "${results.size} answered \u00b7 best ${results.values.minOrNull()} ms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary
                    )
                }
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(Cyan.copy(alpha = 0.14f))
                        .border(1.dp, Cyan.copy(alpha = 0.42f), CircleShape)
                        .clickableNoRipple { if (!app.probing) onProbe() }
                        .padding(horizontal = 13.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (app.probing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                color = Cyan,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Rounded.Refresh, null, tint = Cyan, modifier = Modifier.size(13.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (app.probing) "Testing" else "Test all",
                            style = MaterialTheme.typography.labelLarge,
                            color = Cyan
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(11.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            items(TransportCatalog.endpoints, key = { it.key }) { endpoint ->
                val latency = results[endpoint.key]
                val selected = app.transport.endpointKey == endpoint.key
                GlassCard(
                    Modifier
                        .fillMaxWidth()
                        .clickableNoRipple { app.selectEndpoint(endpoint) },
                    accent = if (selected) Mint else null
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        latency != null -> Mint
                                        results.isNotEmpty() -> Rose.copy(alpha = 0.45f)
                                        else -> TextFaint.copy(alpha = 0.5f)
                                    }
                                )
                        )
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                endpoint.key,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                endpoint.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextFaint
                            )
                        }
                        if (latency != null) {
                            Text(
                                "$latency ms",
                                style = MaterialTheme.typography.labelLarge,
                                color = when {
                                    latency < 60 -> Mint
                                    latency < 150 -> Amber
                                    else -> Rose
                                }
                            )
                        } else if (results.isNotEmpty()) {
                            Text(
                                "no reply",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextFaint
                            )
                        }
                        if (selected) {
                            Spacer(Modifier.width(9.dp))
                            Pill("Active", Mint, filled = true)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun SearchField(value: String, placeholder: String, onChange: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(9.dp))
            Box(Modifier.weight(1f)) {
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
}
