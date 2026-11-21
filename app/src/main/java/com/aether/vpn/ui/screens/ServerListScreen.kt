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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.aether.vpn.core.Endpoint
import com.aether.vpn.ui.AppState
import com.aether.vpn.ui.components.EmptyBlock
import com.aether.vpn.ui.components.GlassCard
import com.aether.vpn.ui.components.Pill
import com.aether.vpn.ui.theme.Amber
import com.aether.vpn.ui.theme.Cyan
import com.aether.vpn.ui.theme.Mint
import com.aether.vpn.ui.theme.Rose
import com.aether.vpn.ui.theme.TextPrimary
import com.aether.vpn.ui.theme.TextSecondary
import com.aether.vpn.ui.theme.TextTertiary
import com.aether.vpn.ui.theme.Violet

/**
 * The endpoint list. Every row is a real Cloudflare WARP ingress address; the latency
 * shown is the measured handshake round-trip from this device, and "not tested" means
 * exactly that — nothing here is invented.
 */
@Composable
fun ServerListScreen(app: AppState, onPick: (Endpoint) -> Unit) {
    var query by remember { mutableStateOf("") }
    var onlyFavourites by remember { mutableStateOf(false) }
    var onlyReachable by remember { mutableStateOf(false) }

    val results = app.probeResults
    val all = app.endpoints()

    val filtered = all.filter { e ->
        val matchesQuery = query.isBlank() ||
            e.host.contains(query, true) || e.port.toString().contains(query)
        val matchesFav = !onlyFavourites || app.favourites.contains(e.key)
        val matchesReachable = !onlyReachable || results.containsKey(e.key)
        matchesQuery && matchesFav && matchesReachable
    }.sortedWith(
        compareBy(
            { results[it.key] ?: Long.MAX_VALUE },
            { if (it.port == 2408) 0 else 1 },
            { it.host }
        )
    )

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(18.dp))
        Text("Servers", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
        Spacer(Modifier.height(3.dp))
        Text(
            "${all.size} WARP endpoints \u00b7 ${results.size} answered",
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary
        )

        Spacer(Modifier.height(16.dp))

        // Search field
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 13.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Search, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            "Search address or port",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextTertiary
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                        cursorBrush = SolidColor(Cyan),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip("All", !onlyFavourites && !onlyReachable) {
                onlyFavourites = false; onlyReachable = false
            }
            FilterChip("Reachable", onlyReachable) { onlyReachable = !onlyReachable }
            FilterChip("Favourites", onlyFavourites) { onlyFavourites = !onlyFavourites }
        }

        Spacer(Modifier.height(14.dp))

        if (filtered.isEmpty()) {
            EmptyBlock(
                title = "No servers match",
                body = if (onlyReachable && results.isEmpty())
                    "Nothing has been tested yet. Open Transport \u2192 Endpoints and tap Test all."
                else "Try a different address or clear the filters."
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.key }) { endpoint ->
                    ServerRow(
                        endpoint = endpoint,
                        latency = results[endpoint.key],
                        tested = results.isNotEmpty(),
                        selected = app.transport.endpointKey == endpoint.key,
                        favourite = app.favourites.contains(endpoint.key),
                        onFavourite = { app.toggleFavourite(endpoint.key) },
                        onClick = { onPick(endpoint) }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, active: Boolean, onClick: () -> Unit) {
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

@Composable
private fun ServerRow(
    endpoint: Endpoint,
    latency: Long?,
    tested: Boolean,
    selected: Boolean,
    favourite: Boolean,
    onFavourite: () -> Unit,
    onClick: () -> Unit
) {
    GlassCard(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reachability dot
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                latency != null -> Mint
                                tested -> Rose.copy(alpha = 0.55f)
                                else -> TextTertiary.copy(alpha = 0.4f)
                            }
                        )
                )
            }

            Spacer(Modifier.width(13.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        endpoint.host,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    if (selected) {
                        Spacer(Modifier.width(8.dp))
                        Pill("Active", Mint, filled = true)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "UDP ${endpoint.port} \u00b7 ${endpoint.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextTertiary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    when {
                        latency != null -> "$latency ms"
                        tested -> "no reply"
                        else -> "\u2014"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = when {
                        latency == null -> TextTertiary
                        latency < 60 -> Mint
                        latency < 150 -> Amber
                        else -> Rose
                    }
                )
                Spacer(Modifier.height(6.dp))
                Pill(
                    text = if (endpoint.port == 2408) "PRIMARY" else "FALLBACK",
                    color = if (endpoint.port == 2408) Cyan else Violet
                )
                Spacer(Modifier.height(6.dp))
                Icon(
                    imageVector = if (favourite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = "Favourite",
                    tint = if (favourite) Amber else TextTertiary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onFavourite)
                )
            }
        }
    }
}
