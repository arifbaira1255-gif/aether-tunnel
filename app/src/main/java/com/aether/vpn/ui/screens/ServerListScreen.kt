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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.vpn.core.Location
import com.aether.vpn.core.Locations
import com.aether.vpn.core.TunnelState
import com.aether.vpn.ui.AppState
import com.aether.vpn.ui.components.Chip
import com.aether.vpn.ui.components.EmptyBlock
import com.aether.vpn.ui.components.GlassCard
import com.aether.vpn.ui.components.ScreenHeader
import com.aether.vpn.ui.components.clickableNoRipple
import com.aether.vpn.ui.theme.Amber
import com.aether.vpn.ui.theme.Cyan
import com.aether.vpn.ui.theme.Mint
import com.aether.vpn.ui.theme.TextFaint
import com.aether.vpn.ui.theme.TextPrimary
import com.aether.vpn.ui.theme.TextSecondary
import com.aether.vpn.ui.theme.TextTertiary

/**
 * Server picker. Countries and cities only - never an address.
 *
 * A tap selects the location AND connects to it, which is what a person expects from a
 * server list. There is no separate "apply" step to forget.
 */
@Composable
fun ServerListScreen(app: AppState, onPick: (Location) -> Unit) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val scope = rememberCoroutineScope()

    val all = Locations.options()
    val selectedId = app.transport.locationId

    val filtered = all.filter { loc ->
        val q = query.trim()
        val matchesQuery = q.isBlank() ||
            loc.country.contains(q, true) ||
            loc.city.contains(q, true) ||
            loc.region.contains(q, true)
        val matchesFilter = when (filter) {
            "Favourites" -> app.favourites.contains(loc.id)
            "Europe" -> loc.region == "Europe"
            "Asia" -> loc.region.contains("Asia")
            "Americas" -> loc.region == "Americas"
            else -> true
        }
        matchesQuery && matchesFilter
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))

        ScreenHeader(
            eyebrow = "${Locations.all.size} locations \u00b7 ${Locations.all.map { it.country }.distinct().size} countries",
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
                        "Search country or city",
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
            listOf("All", "Europe", "Asia", "Americas", "Favourites").forEach { f ->
                Chip(f, filter == f, accent = Cyan) { filter = f }
            }
        }

        Spacer(Modifier.height(14.dp))

        if (filtered.isEmpty()) {
            EmptyBlock(
                title = "Nothing here",
                body = "No location matches that search. Try another country or city."
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { loc ->
                    LocationRow(
                        loc = loc,
                        selected = loc.id == selectedId,
                        favourite = app.favourites.contains(loc.id),
                        connecting = app.state == TunnelState.CONNECTING &&
                            loc.id == selectedId,
                        onFavourite = { app.toggleFavourite(loc.id) },
                        onClick = { onPick(loc) }
                    )
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun LocationRow(
    loc: Location,
    selected: Boolean,
    favourite: Boolean,
    connecting: Boolean,
    onFavourite: () -> Unit,
    onClick: () -> Unit
) {
    val accent = when {
        selected -> Cyan
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
            // Flag
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(loc.flag, fontSize = 19.sp)
            }

            Spacer(Modifier.width(13.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        loc.country,
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
                    loc.city,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextFaint
                )
            }

            // Connection state
            if (connecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    strokeWidth = 1.6.dp,
                    color = Amber
                )
                Spacer(Modifier.width(8.dp))
            }

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
