package com.aether.vpn.service

import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.aether.vpn.core.LiveStats
import com.aether.vpn.core.TunnelProfile
import com.aether.vpn.core.TunnelState
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Statistics
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.config.InetEndpoint
import com.wireguard.config.InetNetwork
import com.wireguard.config.Interface
import com.wireguard.config.Peer
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress

/**
 * Owns the real WireGuard tunnel.
 *
 * The tunnel itself is the WireGuard project's Go userspace implementation running
 * inside Android's VpnService (GoBackend). Nothing here is simulated: when [connect]
 * returns CONNECTED, the device's traffic is being routed through the peer.
 */
object TunnelController {

    private const val TUNNEL_NAME = "aether0"

    private val _state = MutableStateFlow(TunnelState.DISCONNECTED)
    val state: StateFlow<TunnelState> = _state.asStateFlow()

    private val _stats = MutableStateFlow(LiveStats())
    val stats: StateFlow<LiveStats> = _stats.asStateFlow()

    private val _activeProfile = MutableStateFlow<TunnelProfile?>(null)
    val activeProfile: StateFlow<TunnelProfile?> = _activeProfile.asStateFlow()

    private var backend: Backend? = null
    private var tunnel: Tunnel? = null
    private var connectedAt: Long = 0L

    fun connectedSince(): Long = connectedAt

    /** The VpnService consent intent, or null when the user has already granted it. */
    fun prepare(context: Context): Intent? = VpnService.prepare(context)

    private fun backend(context: Context): Backend =
        backend ?: GoBackend(context.applicationContext).also { backend = it }

    private fun tunnel(): Tunnel = tunnel ?: object : Tunnel {
        override fun getName(): String = TUNNEL_NAME
        override fun onStateChange(newState: Tunnel.State) {
            _state.value = when (newState) {
                Tunnel.State.UP -> TunnelState.CONNECTED
                Tunnel.State.DOWN -> TunnelState.DISCONNECTED
                else -> _state.value
            }
        }
    }.also { tunnel = it }

    /** Builds the WireGuard Config from our profile. */
    private fun buildConfig(profile: TunnelProfile): Config {
        val keyPair = KeyPair(Key.fromBase64(profile.privateKey))

        val ifaceBuilder = Interface.Builder()
            .setKeyPair(keyPair)
            .addAddress(InetNetwork.parse(profile.addressV4))
            .setMtu(profile.mtu)
        profile.addressV6?.let { ifaceBuilder.addAddress(InetNetwork.parse(it)) }
        profile.dns.forEach { ifaceBuilder.addDnsServer(InetAddress.getByName(it)) }
        val iface = ifaceBuilder.build()

        val peerBuilder = Peer.Builder()
            .setPublicKey(Key.fromBase64(profile.peerPublicKey))
            .setEndpoint(InetEndpoint.parse("${profile.endpointHost}:${profile.endpointPort}"))
            .setPersistentKeepalive(profile.persistentKeepalive)
        profile.allowedIps.forEach { peerBuilder.addAllowedIp(InetNetwork.parse(it)) }
        val peer = peerBuilder.build()

        return Config.Builder()
            .setInterface(iface)
            .addPeer(peer)
            .build()
    }

    /**
     * Brings the tunnel up. Blocking - call from a background dispatcher.
     * The caller must already hold VpnService consent (see [prepare]).
     */
    fun connect(context: Context, profile: TunnelProfile) {
        _state.value = TunnelState.CONNECTING
        try {
            val cfg = buildConfig(profile)
            backend(context).setState(tunnel(), Tunnel.State.UP, cfg)
            _activeProfile.value = profile
            connectedAt = System.currentTimeMillis()
            _state.value = TunnelState.CONNECTED
        } catch (t: Throwable) {
            _state.value = TunnelState.ERROR
            throw t
        }
    }

    /** Tears the tunnel down. Blocking - call from a background dispatcher. */
    fun disconnect(context: Context) {
        _state.value = TunnelState.DISCONNECTING
        try {
            backend(context).setState(tunnel(), Tunnel.State.DOWN, null)
        } catch (_: Throwable) {
            // Already down is fine.
        } finally {
            _activeProfile.value = null
            connectedAt = 0L
            _stats.value = LiveStats()
            _state.value = TunnelState.DISCONNECTED
        }
    }

    /** Reads live counters from the running tunnel. Safe to call on a timer. */
    fun pollStats(context: Context) {
        if (_state.value != TunnelState.CONNECTED) return
        try {
            val s: Statistics = backend(context).getStatistics(tunnel())
            val rx = s.totalRx()
            val tx = s.totalTx()
            val prev = _stats.value
            _stats.value = LiveStats(
                rxBytes = rx,
                txBytes = tx,
                downBps = (rx - prev.rxBytes).coerceAtLeast(0),
                upBps = (tx - prev.txBytes).coerceAtLeast(0),
                handshakeAgeMs = prev.handshakeAgeMs
            )
        } catch (_: Throwable) {
            // Tunnel may be tearing down; keep the last numbers.
        }
    }
}
