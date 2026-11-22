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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.vpn.core.Endpoint
import com.aether.vpn.ui.AppState
import com.aether.vpn.ui.components.Chip
import com.aether.vpn.ui.components.EmptyBlock
import com.aether.vpn.ui.components.GlassCard
import com.aether.vpn.ui.components.ScreenHeader
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

@Composable
fun ServerListScreen(app: AppState, onPick: (Endpoint) -> Unit) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val scope = rememberCoroutineScope()

    val all = app.endpoints()
    val selectedKey = app.transport.endpointKey

    val filtered = all.filter { e ->
        val matchesQuery = query.isBlank() ||
            e.host.contains(query, true) ||
            e.label.contains(query, true) ||
            e.port.toString().contains(query)
        val matchesFilter = when (filter) {
            "Reachable" -> app.probeResults.containsKey(e.key)
            "Favourites" -> app.favourites.contains(e.key)
            "Primary" -> e.port == 2408
            else -> true
        }
        matchesQuery && matchesFilter
    }.sortedWith(
        compareBy(
            { if (app.probeResults.containsKey(it.key)) 0 else 1 },
            { app.probeResults[it.key] ?: Long.MAX_VALUE },
            { it.host }
        )
    )

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))

        ScreenHeader(
            eyebrow = "${all.size} edges · Cloudflare anycast",
            title = "Servers",
            trailing = {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(Cyan.copy(alpha = 0.12f))
                        .border(1.dp, Cyan.copy(alpha = 0.30f), CircleShape)
                        .clickableNoRipple { app.probeAll(scope) }
                        .padding(horizontal = 13.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (app.probing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(11.dp),
                                strokeWidth = 1.5.dp,
                                color = Cyan
                            )
                        } else {
                            Icon(Icons.Rounded.Bolt, null, tint = Cyan, modifier = Modifier.size(13.dp))
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
        )

        Spacer(Modifier.height(16.dp))

        // ---- Search --------------------------------------------------------
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.045f))
                .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, null, tint = TextTertiary, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        "Search host or port",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextFaint
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                    cursorBrush = SolidColor(Cyan),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Reachable", "Primary", "Favourites").forEach { f ->
                Chip(f, filter == f, accent = Cyan) { filter = f }
            }
        }

        if (!app.adminUnlocked) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Amber.copy(alpha = 0.09f))
                    .border(1.dp, Amber.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 13.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Lock, null, tint = Amber, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Server selection is managed by the administrator. The app picks the " +
                        "fastest edge that actually carries traffic.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        if (filtered.isEmpty()) {
            EmptyBlock(
                title = "Nothing here",
                body = if (app.probing) "Testing every edge — this takes a few seconds."
                else "No edge matches that filter. Try Test all to measure reachability."
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.key }) { endpoint ->
                    ServerRow(
                        endpoint = endpoint,
                        latency = app.probeResults[endpoint.key],
                        selected = endpoint.key == selectedKey,
                        favourite = app.favourites.contains(endpoint.key),
                        onFavourite = { app.toggleFavourite(endpoint.key) },
                        onClick = { if (app.adminUnlocked) onPick(endpoint) }
                    )
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun ServerRow(
    endpoint: Endpoint,
    latency: Long?,
    selected: Boolean,
    favourite: Boolean,
    onFavourite: () -> Unit,
    onClick: () -> Unit
) {
    val reachable = latency != null
    val accent = when {
        selected -> Cyan
        reachable -> Mint
        else -> TextFaint
    }

    GlassCard(
        Modifier
            .fillMaxWidth()
            .clickableNoRipple(onClick),
        corner = 18,
        accent = if (selected) Cyan else null
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status dot
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        endpoint.host,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (selected) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.Check, null,
                            tint = Cyan,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    "Port ${endpoint.port} · ${endpoint.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextFaint
                )
            }

            // Latency
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    when {
                        latency != null -> "$latency ms"
                        else -> "—"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = when {
                        latency == null -> TextFaint
                        latency < 80 -> Mint
                        latency < 200 -> Amber
                        else -> Rose
                    },
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (reachable) "reachable" else "untested",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextFaint
                )
            }

            Spacer(Modifier.width(6.dp))

            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickableNoRipple(onFavourite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (favourite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = "Favourite",
                    tint = if (favourite) Amber else TextFaint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
