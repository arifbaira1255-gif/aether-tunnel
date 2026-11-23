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
 * The tunnel is the WireGuard project's Go userspace implementation running inside
 * Android's VpnService (GoBackend). Nothing here is simulated: when [connect] returns
 * normally, the device's traffic is being routed through the peer.
 *
 * Encryption is WireGuard's own: Curve25519 key agreement, ChaCha20-Poly1305 data
 * channel, BLAKE2s hashing, fresh ephemeral keypair per handshake, 2-minute rekey.
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
    private var lastPollAt: Long = 0L
    private var peakDown: Long = 0L
    private var peakUp: Long = 0L

    fun connectedSince(): Long = connectedAt

    /** The VpnService consent intent, or null when the user has already granted it. */
    fun prepare(context: Context): Intent? = VpnService.prepare(context)

    private fun backendFor(context: Context): Backend =
        backend ?: GoBackend(context.applicationContext).also { backend = it }

    private fun tunnelInstance(): Tunnel = tunnel ?: object : Tunnel {
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
        // Only add the v6 address when v6 is actually routed. Adding an address without
        // routing it makes Android prefer IPv6 for DNS and then blackhole it.
        if (profile.allowedIps.any { it.contains(":") }) {
            profile.addressV6?.let { ifaceBuilder.addAddress(InetNetwork.parse(it)) }
        }
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
            backendFor(context).setState(tunnelInstance(), Tunnel.State.UP, cfg)
            _activeProfile.value = profile
            connectedAt = System.currentTimeMillis()
            lastPollAt = connectedAt
            peakDown = 0L
            peakUp = 0L
            _stats.value = LiveStats()
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
            backendFor(context).setState(tunnelInstance(), Tunnel.State.DOWN, null)
        } catch (_: Throwable) {
            // Already down is fine.
        } finally {
            _activeProfile.value = null
            connectedAt = 0L
            lastPollAt = 0L
            peakDown = 0L
            peakUp = 0L
            _stats.value = LiveStats()
            _state.value = TunnelState.DISCONNECTED
        }
    }

    /** Raw counters straight from the backend, or null when they cannot be read. */
    private fun readCounters(context: Context): Triple<Long, Long, Long>? = runCatching {
        val s: Statistics = backendFor(context).getStatistics(tunnelInstance())
        val rx = s.totalRx()
        val tx = s.totalTx()
        // peers() can be empty while the backend's statistics are still stale (it
        // refreshes on a timer). An empty peer list is NOT proof that no handshake
        // happened, so report -1 and let the caller decide.
        val handshake = s.peers().firstOrNull()?.let { key ->
            s.peer(key)?.latestHandshakeEpochMillis() ?: -1L
        } ?: -1L
        Triple(rx, tx, handshake)
    }.getOrNull()

    /**
     * Reads live counters from the running tunnel. Safe to call on a timer.
     *
     * The rate is computed against the real elapsed time between polls, not against an
     * assumed one second, so a slow frame does not inflate the number.
     */
    fun pollStats(context: Context) {
        if (_state.value != TunnelState.CONNECTED) return
        val counters = readCounters(context) ?: return
        val (rx, tx, handshakeEpoch) = counters
        val prev = _stats.value
        val now = System.currentTimeMillis()
        val elapsedMs = if (lastPollAt > 0) (now - lastPollAt).coerceAtLeast(1) else 1000L
        lastPollAt = now

        val dRx = (rx - prev.rxBytes).coerceAtLeast(0)
        val dTx = (tx - prev.txBytes).coerceAtLeast(0)
        val downBps = dRx * 1000L / elapsedMs
        val upBps = dTx * 1000L / elapsedMs
        if (downBps > peakDown) peakDown = downBps
        if (upBps > peakUp) peakUp = upBps

        val handshakeAge = if (handshakeEpoch > 0) now - handshakeEpoch else -1L

        _stats.value = LiveStats(
            rxBytes = rx,
            txBytes = tx,
            downBps = downBps,
            upBps = upBps,
            handshakeAgeMs = handshakeAge,
            peakDownBps = peakDown,
            peakUpBps = peakUp
        )
    }

    /**
     * Waits until the peer has completed a handshake, or the timeout expires.
     *
     * A completed handshake is the strongest proof that the key exchange worked end to
     * end. But it is not the ONLY proof, and treating it as one was a bug: the backend's
     * statistics refresh on a timer, so `peers()` can be empty for a second or two after
     * a perfectly good handshake. Waiting only for the handshake field therefore reported
     * "no handshake" on tunnels that were working.
     *
     * So this accepts EITHER signal:
     *   - a handshake timestamp within the last 20 seconds, or
     *   - any byte moving in either direction (a peer that has not handshaked cannot
     *     send or receive a single byte, so movement is proof the handshake completed).
     */
    fun awaitHandshake(context: Context, timeoutMs: Long = 3000): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        var firstRx = -1L
        var firstTx = -1L
        while (System.currentTimeMillis() < deadline) {
            val c = readCounters(context)
            if (c != null) {
                val (rx, tx, handshakeEpoch) = c
                if (handshakeEpoch > 0) {
                    val age = System.currentTimeMillis() - handshakeEpoch
                    if (age in 0..20_000) return true
                }
                if (firstRx < 0) {
                    firstRx = rx
                    firstTx = tx
                } else if (rx > firstRx || tx > firstTx) {
                    // Bytes moved: the handshake must have completed.
                    return true
                }
            }
            Thread.sleep(120)
        }
        return false
    }

    /**
     * True when the tunnel claims to be up but is not actually carrying traffic.
     *
     * Two independent signals, because either alone gives false positives:
     *  - the peer's handshake is older than [staleHandshakeMs] (WireGuard rekeys every
     *    2 minutes, so a live peer is never that stale), or
     *  - the tunnel has been up past the grace period and not a single byte has moved.
     */
    fun looksStalled(
        idleMs: Long = 45_000,
        staleHandshakeMs: Long = 180_000
    ): Boolean {
        if (_state.value != TunnelState.CONNECTED) return false
        val s = _stats.value
        val upFor = if (connectedAt > 0) System.currentTimeMillis() - connectedAt else 0L

        if (s.handshakeAgeMs > staleHandshakeMs) return true
        if (s.rxBytes + s.txBytes == 0L && upFor > idleMs) return true
        return false
    }

    /** A one-line snapshot of the backend, for the diagnostics screen. */
    fun backendVersion(context: Context): String =
        runCatching { backendFor(context).version }.getOrElse { "unavailable" }
}
