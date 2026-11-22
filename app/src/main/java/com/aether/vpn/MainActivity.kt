package com.aether.vpn

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.aether.vpn.core.TunnelState
import com.aether.vpn.service.TunnelController
import com.aether.vpn.ui.AppState
import com.aether.vpn.ui.components.AmbientBackground
import com.aether.vpn.ui.components.clickableNoRipple
import com.aether.vpn.ui.screens.ConnectScreen
import com.aether.vpn.ui.screens.ServerListScreen
import com.aether.vpn.ui.screens.SettingsScreen
import com.aether.vpn.ui.screens.StatsScreen
import com.aether.vpn.ui.theme.AetherTheme
import com.aether.vpn.ui.theme.Cyan
import com.aether.vpn.ui.theme.Mint
import com.aether.vpn.ui.theme.TextFaint
import com.aether.vpn.ui.theme.TextTertiary
import com.aether.vpn.ui.theme.Violet
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var app: AppState

    private val vpnPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingConnect?.invoke()
        } else {
            app.error = "VPN permission was declined, so the tunnel cannot start."
        }
        pendingConnect = null
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* The tunnel works either way; the notification is just less visible. */ }

    private var pendingConnect: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = AppState(this)

        // Android 13+ needs an explicit grant before the ongoing notification can show.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            AetherTheme {
                val scope = rememberCoroutineScope()
                var tab by remember { mutableStateOf(0) }

                val connected = app.state == TunnelState.CONNECTED
                val accentA = if (connected) Mint else Cyan
                val accentB = if (connected) Cyan else Violet

                AmbientBackground(accentA = accentA, accentB = accentB, modifier = Modifier.fillMaxSize()) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        Box(Modifier.weight(1f)) {
                            when (tab) {
                                0 -> ConnectScreen(
                                    app = app,
                                    onOpenServers = { tab = 1 },
                                    onConnect = { requestConnect(scope) },
                                    onDisconnect = { app.disconnect(scope) }
                                )
                                1 -> ServerListScreen(app) { loc ->
                                    // One tap: remember the location, then connect through the
                                    // normal path so the VPN permission is requested first.
                                    app.selectLocation(loc)
                                    tab = 0
                                    requestConnect(scope)
                                }
                                2 -> StatsScreen(app)
                                else -> SettingsScreen(app)
                            }
                        }
                        BottomBar(tab, connected) { tab = it }
                    }
                }
            }
        }
    }

    private fun requestConnect(scope: kotlinx.coroutines.CoroutineScope) {
        val consent = TunnelController.prepare(this)
        if (consent != null) {
            pendingConnect = { app.connect(scope) }
            vpnPermission.launch(consent)
        } else {
            app.connect(scope)
        }
    }
}

@Composable
private fun BottomBar(selected: Int, connected: Boolean, onSelect: (Int) -> Unit) {
    val items = listOf(
        Triple("Connect", Icons.Rounded.Shield, 0),
        Triple("Servers", Icons.Rounded.Dns, 1),
        Triple("Usage", Icons.Rounded.BarChart, 2),
        Triple("Settings", Icons.Rounded.Settings, 3)
    )
    val accent = if (connected) Mint else Cyan

    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0A0D1C).copy(alpha = 0.96f))
                .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(24.dp))
                .padding(vertical = 7.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (label, icon, index) ->
                TabItem(
                    label = label,
                    icon = icon,
                    active = selected == index,
                    accent = accent,
                    onClick = { onSelect(index) }
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    label: String,
    icon: ImageVector,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val pillAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        label = "pill"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickableNoRipple(onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(width = 34.dp, height = 26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    if (active) {
                        Brush.linearGradient(
                            listOf(accent.copy(alpha = 0.20f), Violet.copy(alpha = 0.14f))
                        )
                    } else {
                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) accent else TextTertiary,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) accent else TextFaint,
            fontSize = 8.5.sp,
            letterSpacing = 0.4.sp
        )
    }
}
