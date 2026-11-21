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
import com.aether.vpn.core.SniCategory
import com.aether.vpn.core.TransportCatalog
import com.aether.vpn.ui.AppState
import com.aether.vpn.ui.components.EmptyBlock
import com.aether.vpn.ui.components.GlassCard
import com.aether.vpn.ui.components.Pill
import com.aether.vpn.ui.components.SectionLabel
import com.aether.vpn.ui.theme.Amber
import com.aether.vpn.ui.theme.Cyan
import com.aether.vpn.ui.theme.Mint
import com.aether.vpn.ui.theme.Rose
import com.aether.vpn.ui.theme.TextPrimary
import com.aether.vpn.ui.theme.TextSecondary
import com.aether.vpn.ui.theme.TextTertiary
import com.aether.vpn.ui.theme.Violet

private enum class TransportTab(val title: String) {
    SNI("SNI hosts"), PAYLOAD("Payloads"), ENDPOINTS("Endpoints")
}

@Composable
fun TransportScreen(app: AppState) {
    var tab by remember { mutableStateOf(TransportTab.SNI) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(18.dp))
        Text("Transport", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
        Spacer(Modifier.height(3.dp))
        Text(
            "SNI host, handshake payload and endpoint pool",
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary
        )

        Spacer(Modifier.height(16.dp))

        // Active configuration summary
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                SectionLabel("Active configuration")
                Spacer(Modifier.height(10.dp))
                SummaryRow("SNI", app.activeSni())
                Spacer(Modifier.height(7.dp))
                SummaryRow(
                    "Payload",
                    TransportCatalog.payloadById(app.transport.payloadId)?.name ?: "Custom"
                )
                Spacer(Modifier.height(7.dp))
                SummaryRow("Endpoint", app.transport.endpointKey)
                Spacer(Modifier.height(7.dp))
                SummaryRow(
                    "Failover",
                    if (app.transport.autoFailover) "On — tries up to 12 endpoints" else "Off"
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Tabs
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TransportTab.entries.forEach { t ->
                Chip(t.title, tab == t) { tab = t }
            }
        }

        Spacer(Modifier.height(14.dp))

        when (tab) {
            TransportTab.SNI -> SniList(app)
            TransportTab.PAYLOAD -> PayloadList(app)
            TransportTab.ENDPOINTS -> EndpointList(app) { app.probeAll(scope) }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            modifier = Modifier.width(78.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun Chip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (active) Cyan.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.04f))
            .border(1.dp, if (active) Cyan.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (active) Cyan else TextSecondary
        )
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
        SearchField(query, "Search SNI host") { query = it }
        Spacer(Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Chip("All", category == null) { category = null } }
            items(SniCategory.entries.toList()) { c ->
                Chip(c.title, category == c) { category = if (category == c) null else c }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            EmptyBlock("No SNI host matches", "Try a different name or clear the filter.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { host ->
                    val selected = app.transport.sniId == host.id && app.transport.customSni.isBlank()
                    GlassCard(
                        Modifier.fillMaxWidth().clickable { app.selectSni(host.id) }
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    host.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    host.host,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextTertiary
                                )
                                if (host.note.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        host.note,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextTertiary
                                    )
                                }
                            }
                            if (selected) {
                                Box(
                                    Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Mint.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Check, null,
                                        tint = Mint, modifier = Modifier.size(15.dp)
                                    )
                                }
                            } else {
                                Pill(host.category.title.take(9), Violet)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

// ---- Payloads -------------------------------------------------------------

@Composable
private fun PayloadList(app: AppState) {
    var custom by remember { mutableStateOf(app.transport.customPayload) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(TransportCatalog.payloads, key = { it.id }) { preset ->
            val selected = app.transport.payloadId == preset.id
            GlassCard(
                Modifier.fillMaxWidth().clickable { app.selectPayload(preset.id) }
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            preset.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (selected) {
                            Box(
                                Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Mint.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Check, null, tint = Mint, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                    if (preset.note.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            preset.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary
                        )
                    }
                    if (preset.template.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.28f))
                                .padding(10.dp)
                        ) {
                            Text(
                                preset.template.replace("[crlf]", "\u23CE"),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }
        }

        // Custom editor
        item {
            Spacer(Modifier.height(6.dp))
            SectionLabel("Custom payload")
            Spacer(Modifier.height(8.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "Placeholders: [crlf] [host] [sni] [host_port] [ua] [random]",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextTertiary
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        if (custom.isEmpty()) {
                            Text(
                                "GET / HTTP/1.1[crlf]Host: [sni][crlf][crlf]",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextTertiary
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
                Modifier.fillMaxWidth().padding(14.dp),
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
                        if (results.isEmpty()) "Not tested yet — tap Test to probe from this network"
                        else "${results.size} answered from this network",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary
                    )
                }
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(Cyan.copy(alpha = 0.16f))
                        .border(1.dp, Cyan.copy(alpha = 0.5f), CircleShape)
                        .clickable(enabled = !app.probing, onClick = onProbe)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (app.probing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(13.dp),
                                color = Cyan,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Rounded.Refresh, null, tint = Cyan, modifier = Modifier.size(14.dp))
                        }
                        Spacer(Modifier.width(7.dp))
                        Text(
                            if (app.probing) "Testing" else "Test all",
                            style = MaterialTheme.typography.labelLarge,
                            color = Cyan
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TransportCatalog.endpoints, key = { it.key }) { endpoint ->
                val latency = results[endpoint.key]
                val selected = app.transport.endpointKey == endpoint.key
                GlassCard(
                    Modifier.fillMaxWidth().clickable { app.selectEndpoint(endpoint) }
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        latency != null -> Mint
                                        results.isNotEmpty() -> Rose.copy(alpha = 0.5f)
                                        else -> TextTertiary.copy(alpha = 0.4f)
                                    }
                                )
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                endpoint.key,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                endpoint.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextTertiary
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
                                color = TextTertiary
                            )
                        }
                        if (selected) {
                            Spacer(Modifier.width(10.dp))
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
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, null, tint = TextTertiary, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
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
}
