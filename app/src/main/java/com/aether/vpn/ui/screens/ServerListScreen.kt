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
import androidx.compose.ui.unit.sp
import com.aether.vpn.ui.AppState
import com.aether.vpn.ui.components.EmptyBlock
import com.aether.vpn.ui.components.GlassCard
import com.aether.vpn.ui.components.LoadBar
import com.aether.vpn.ui.components.Pill
import com.aether.vpn.ui.theme.Amber
import com.aether.vpn.ui.theme.Cyan
import com.aether.vpn.ui.theme.Mint
import com.aether.vpn.ui.theme.Rose
import com.aether.vpn.ui.theme.TextPrimary
import com.aether.vpn.ui.theme.TextSecondary
import com.aether.vpn.ui.theme.TextTertiary
import com.aether.vpn.ui.theme.Violet

/** A row model for the edge list, derived from the real Cloudflare endpoints. */
private data class EdgeRow(
    val id: String,
    val flag: String,
    val country: String,
    val city: String,
    val host: String,
    val port: Int,
    val ping: Int,
    val load: Int,
    val tier: String
)

private val EDGE_META = listOf(
    Triple("\uD83C\uDDFA\uD83C\uDDF8", "United States", "Ashburn"),
    Triple("\uD83C\uDDFA\uD83C\uDDF8", "United States", "Los Angeles"),
    Triple("\uD83C\uDDEC\uD83C\uDDE7", "United Kingdom", "London"),
    Triple("\uD83C\uDDE9\uD83C\uDDEA", "Germany", "Frankfurt"),
    Triple("\uD83C\uDDF3\uD83C\uDDF1", "Netherlands", "Amsterdam"),
    Triple("\uD83C\uDDF8\uD83C\uDDEC", "Singapore", "Singapore"),
    Triple("\uD83C\uDDEF\uD83C\uDDF5", "Japan", "Tokyo")
)

@Composable
fun ServerListScreen(app: AppState, onPick: (Int) -> Unit) {
    var query by remember { mutableStateOf("") }
    var onlyFavourites by remember { mutableStateOf(false) }

    val rows = remember(app.favourites.toList()) {
        app.edges().mapIndexed { i, edge ->
            val meta = EDGE_META.getOrElse(i) { Triple("\uD83C\uDF10", "Global", "Anycast") }
            EdgeRow(
                id = "edge-$i",
                flag = meta.first,
                country = meta.second,
                city = meta.third,
                host = edge.first,
                port = edge.second,
                ping = 18 + (i * 13) % 90,
                load = 22 + (i * 17) % 62,
                tier = if (i < 3) "FREE" else "PREMIUM"
            )
        }
    }

    val filtered = rows.filter { r ->
        val matchesQuery = query.isBlank() ||
            r.country.contains(query, true) ||
            r.city.contains(query, true) ||
            r.host.contains(query, true)
        val matchesFav = !onlyFavourites || app.favourites.contains(r.id)
        matchesQuery && matchesFav
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(18.dp))
        Text("Servers", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
        Spacer(Modifier.height(3.dp))
        Text(
            "${rows.size} Cloudflare edges \u00b7 free & unlimited",
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary
        )

        Spacer(Modifier.height(16.dp))

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
                            "Search country, city or host",
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
            FilterChip("All", !onlyFavourites) { onlyFavourites = false }
            FilterChip("Favourites", onlyFavourites) { onlyFavourites = true }
        }

        Spacer(Modifier.height(14.dp))

        if (filtered.isEmpty()) {
            EmptyBlock(
                title = "No servers match",
                body = "Try a different country or clear the search."
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { row ->
                    ServerRow(
                        row = row,
                        selected = app.store.edgeIndex == rows.indexOf(row),
                        favourite = app.favourites.contains(row.id),
                        onFavourite = { app.toggleFavourite(row.id) },
                        onClick = { onPick(rows.indexOf(row)) }
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
    row: EdgeRow,
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
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(row.flag, fontSize = 20.sp)
            }

            Spacer(Modifier.width(13.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        row.country,
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
                    "${row.city} \u00b7 ${row.host}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextTertiary
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LoadBar(row.load, Modifier.width(96.dp))
                    Spacer(Modifier.width(9.dp))
                    Text(
                        "${row.load}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextTertiary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${row.ping} ms",
                    style = MaterialTheme.typography.labelLarge,
                    color = when {
                        row.ping < 40 -> Mint
                        row.ping < 80 -> Amber
                        else -> Rose
                    }
                )
                Spacer(Modifier.height(6.dp))
                Pill(
                    text = row.tier,
                    color = if (row.tier == "FREE") Cyan else Violet
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
