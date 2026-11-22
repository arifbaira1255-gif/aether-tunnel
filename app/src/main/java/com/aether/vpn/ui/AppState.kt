package com.aether.vpn.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aether.vpn.core.Carrier
import com.aether.vpn.core.Endpoint
import com.aether.vpn.core.LiveStats
import com.aether.vpn.core.SessionRecord
import com.aether.vpn.core.SpeedHistory
import com.aether.vpn.core.TransportCatalog
import com.aether.vpn.core.TransportConfig
import com.aether.vpn.core.TunnelProfile
import com.aether.vpn.core.TunnelState
import com.aether.vpn.net.ConnectivityVerifier
import com.aether.vpn.net.FailoverEngine
import com.aether.vpn.net.TunnelProbe
import com.aether.vpn.net.WarpApi
import com.aether.vpn.service.TunnelController
import com.aether.vpn.service.TunnelNotification
import com.aether.vpn.store.AppStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Single source of truth for the UI. Holds the WARP registration, the transport config,
 * the live tunnel state and the session history, and drives the stats poller.
 */
class AppState(private val context: Context) {

    val store = AppStore(context)

    var state by mutableStateOf(TunnelState.DISCONNECTED)
        private set
    var stats by mutableStateOf(LiveStats())
        private set
    var activeProfile by mutableStateOf<TunnelProfile?>(null)
        private set
    var registration by mutableStateOf<WarpApi.Registration?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
    var connectedAt by mutableStateOf(0L)
        private set

    /** The SNI / payload / endpoint selection. Admin-owned. */
    var transport by mutableStateOf(store.transport())
        private set

    var failoverNote by mutableStateOf<String?>(null)
        private set
    var attempts by mutableStateOf<List<FailoverEngine.Attempt>>(emptyList())
        private set

    var probeResults by mutableStateOf(store.probeResults())
        private set
    var probing by mutableStateOf(false)
        private set

    /** True once a real HTTP request has come back through the tunnel. */
    var trafficVerified by mutableStateOf(false)
        private set
    /** Round-trip time of that verification request, in ms. */
    var verifiedLatencyMs by mutableStateOf(0L)
        private set

    val speed = SpeedHistory(60)
    var speedTick by mutableStateOf(0L)
        private set

    var activeMtu by mutableStateOf(0)
        private set

    /** Admin session unlock. Not persisted — a restart re-locks the panel. */
    var adminUnlocked by mutableStateOf(false)
        private set

    val sessions = mutableStateListOf<SessionRecord>()
    val favourites = mutableStateListOf<String>()

    private var pollJob: kotlinx.coroutines.Job? = null
    private var watchdogJob: kotlinx.coroutines.Job? = null
    private var notifyJob: kotlinx.coroutines.Job? = null

    init {
        registration = store.registration()
        sessions.addAll(store.sessions())
        favourites.addAll(store.favourites())
        // First run: seed the admin PIN so the panel is never open by default.
        if (!store.hasAdminPin()) store.setAdminPin(DEFAULT_ADMIN_PIN)
        TunnelNotification.ensureChannel(context)
    }

    fun endpoints(): List<Endpoint> = TransportCatalog.endpoints

    fun refreshFromStore() {
        sessions.clear(); sessions.addAll(store.sessions())
        favourites.clear(); favourites.addAll(store.favourites())
    }

    fun toggleFavourite(id: String) {
        store.toggleFavourite(id)
        refreshFromStore()
    }

    // ---- admin -------------------------------------------------------------

    fun unlockAdmin(pin: String): Boolean {
        val ok = store.checkAdminPin(pin)
        if (ok) adminUnlocked = true
        return ok
    }

    fun lockAdmin() { adminUnlocked = false }

    fun changeAdminPin(current: String, next: String): Boolean {
        if (!store.checkAdminPin(current)) return false
        if (next.length < 4) return false
        store.setAdminPin(next)
        return true
    }

    // ---- transport editing (admin only) ------------------------------------

    fun updateTransport(block: (TransportConfig) -> TransportConfig) {
        if (!adminUnlocked) return
        val next = block(transport)
        transport = next
        store.saveTransport(next)
    }

    fun selectEndpoint(endpoint: Endpoint) {
        updateTransport { it.copy(endpointKey = endpoint.key) }
    }

    /**
     * Remembers the endpoint that actually worked, regardless of who is signed in.
     * The failover engine calls this after a verified connection, so the next connect
     * starts from the edge that is known to work on this network.
     */
    private fun rememberWorkingEndpoint(endpoint: Endpoint) {
        val next = transport.copy(endpointKey = endpoint.key)
        transport = next
        store.saveTransport(next)
    }

    fun selectSni(id: String) = updateTransport { it.copy(sniId = id, customSni = "") }
    fun selectPayload(id: String) = updateTransport { it.copy(payloadId = id) }
    fun setCustomSni(value: String) = updateTransport { it.copy(customSni = value) }
    fun setCustomPayload(value: String) = updateTransport { it.copy(customPayload = value) }
    fun setAutoFailover(value: Boolean) = updateTransport { it.copy(autoFailover = value) }
    fun setAutoMtu(value: Boolean) = updateTransport { it.copy(autoMtu = value) }
    fun setWatchdog(value: Boolean) = updateTransport { it.copy(watchdog = value) }
    fun setVerifyTraffic(value: Boolean) = updateTransport { it.copy(verifyTraffic = value) }
    fun setRouteIpv6(value: Boolean) = updateTransport { it.copy(routeIpv6 = value) }

    fun setDnsPreset(id: String) {
        updateTransport { it.copy(dnsId = id) }
        TransportCatalog.dnsById(id)?.let { store.dns = it.servers.joinToString(", ") }
    }

    fun selectCarrier(carrier: Carrier) {
        updateTransport {
            it.copy(
                carrier = carrier,
                sniId = carrier.defaultSniId,
                payloadId = carrier.defaultPayloadId,
                customSni = ""
            )
        }
    }

    fun activeSni(): String = transport.effectiveSni

    fun activePayload(): String = TransportCatalog.renderPayload(
        template = transport.effectivePayloadTemplate,
        host = transport.effectiveSni,
        port = 443,
        sni = transport.effectiveSni
    )

    // ---- probing -----------------------------------------------------------

    fun probeAll(scope: CoroutineScope) {
        if (probing) return
        probing = true
        scope.launch {
            val results = withContext(Dispatchers.IO) {
                TunnelProbe.probeAll(TransportCatalog.endpoints, timeoutMs = 900)
            }
            val reachable = results.filter { it.reachable }
                .associate { it.endpoint.key to it.latencyMs }
            probeResults = reachable
            store.saveProbeResults(reachable)
            probing = false
            if (reachable.isEmpty()) {
                error = "No endpoint answered from this network. UDP may be blocked here."
            } else {
                error = null
                reachable.entries.minByOrNull { it.value }?.let { best ->
                    TransportCatalog.endpoints.firstOrNull { it.key == best.key }
                        ?.let { selectEndpoint(it) }
                }
            }
        }
    }

    // ---- registration ------------------------------------------------------

    suspend fun ensureRegistration(): WarpApi.Registration? {
        registration?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching { WarpApi.register() }
                .onSuccess {
                    store.saveRegistration(it)
                    registration = it
                }
                .onFailure { error = it.message }
                .getOrNull()
        }
    }

    fun currentProfile(): TunnelProfile? {
        val reg = registration ?: return null
        val endpoint = store.selectedEndpoint() ?: TransportCatalog.endpoints.first()
        val base = WarpApi.profileFrom(reg, endpoint, transport, store.mtu)
        return store.applyPreferences(base)
    }

    // ---- connect / disconnect ---------------------------------------------

    fun connect(scope: CoroutineScope) {
        if (busy) return
        busy = true
        error = null
        attempts = emptyList()
        trafficVerified = false
        verifiedLatencyMs = 0L
        failoverNote = "Registering with the WARP edge…"
        scope.launch {
            val reg = ensureRegistration()
            if (reg == null) {
                busy = false
                state = TunnelState.ERROR
                failoverNote = null
                return@launch
            }

            state = TunnelState.CONNECTING
            failoverNote = "Probing endpoints…"
            val preferred = store.selectedEndpoint()
            val cfg = transport
            val collected = ArrayList<FailoverEngine.Attempt>()

            // Two gates: the peer must have completed a handshake, and a real request
            // must come back. Either alone gives a false green.
            val verifyFn: (() -> Boolean)? = if (cfg.verifyTraffic) {
                {
                    val handshake = TunnelController.awaitHandshake(context, 5000)
                    handshake && ConnectivityVerifier.check(timeoutMs = 6000).ok
                }
            } else {
                null
            }

            val outcome = withContext(Dispatchers.IO) {
                FailoverEngine.run(
                    config = cfg,
                    preferred = preferred,
                    lastGoodMtu = store.lastGoodMtu,
                    fixedMtu = store.mtu,
                    buildProfile = { endpoint, transportCfg, mtu ->
                        store.applyPreferences(
                            WarpApi.profileFrom(reg, endpoint, transportCfg, mtu),
                            mtu
                        )
                    },
                    connect = { profile -> TunnelController.connect(context, profile) },
                    disconnect = { runCatching { TunnelController.disconnect(context) } },
                    verify = verifyFn,
                    maxAttempts = if (cfg.autoFailover) 12 else 1,
                    onAttempt = { attempt ->
                        collected.add(attempt)
                        // Publish progress as it happens, not only at the end.
                        attempts = collected.toList()
                        failoverNote = when {
                            attempt.connected -> "Connected via ${attempt.endpoint.key}"
                            attempt.mtu > 0 -> "Trying ${attempt.endpoint.key} · MTU ${attempt.mtu}"
                            else -> "Probing ${attempt.endpoint.key}"
                        }
                    }
                )
            }

            attempts = collected.toList()
            failoverNote = when {
                outcome.connected && outcome.profile != null ->
                    "Connected via ${outcome.profile.endpointLabel} · MTU ${outcome.mtu}"
                collected.isNotEmpty() ->
                    "Tried ${collected.size} combination(s) — none carried traffic"
                else -> null
            }

            if (outcome.connected && outcome.profile != null) {
                activeProfile = outcome.profile
                activeMtu = outcome.mtu
                connectedAt = TunnelController.connectedSince()
                state = TunnelState.CONNECTED
                trafficVerified = outcome.verified
                verifiedLatencyMs = collected.lastOrNull { it.verified }?.verifyLatencyMs ?: 0L
                store.lastGoodMtu = outcome.mtu
                store.mtu = outcome.mtu
                TransportCatalog.endpoints
                    .firstOrNull { it.key == outcome.profile.endpointLabel }
                    ?.let { rememberWorkingEndpoint(it) }
                speed.clear()
                startPolling(scope)
                startNotification(scope)
                if (cfg.watchdog) startWatchdog(scope)
            } else {
                error = outcome.error
                state = TunnelState.ERROR
                TunnelNotification.cancel(context)
            }
            busy = false
        }
    }

    fun disconnect(scope: CoroutineScope) {
        if (busy) return
        busy = true
        scope.launch {
            state = TunnelState.DISCONNECTING
            val started = connectedAt
            val finalStats = stats
            val mtu = activeMtu
            val endpoint = activeProfile?.endpointLabel ?: ""
            withContext(Dispatchers.IO) { runCatching { TunnelController.disconnect(context) } }
            stopPolling()
            stopWatchdog()
            stopNotification()
            TunnelNotification.cancel(context)
            if (started > 0) {
                val rec = SessionRecord(
                    profileName = activeProfile?.name ?: "Aether Auto",
                    flag = activeProfile?.flag ?: "\uD83C\uDF10",
                    startedAt = started,
                    endedAt = System.currentTimeMillis(),
                    rxBytes = finalStats.rxBytes,
                    txBytes = finalStats.txBytes,
                    mtu = mtu,
                    endpoint = endpoint
                )
                store.addSession(rec)
                refreshFromStore()
            }
            activeProfile = null
            activeMtu = 0
            connectedAt = 0L
            stats = LiveStats()
            speed.clear()
            failoverNote = null
            trafficVerified = false
            verifiedLatencyMs = 0L
            state = TunnelState.DISCONNECTED
            busy = false
        }
    }

    private fun startPolling(scope: CoroutineScope) {
        stopPolling()
        pollJob = scope.launch {
            while (true) {
                delay(1000)
                withContext(Dispatchers.IO) { TunnelController.pollStats(context) }
                val s = TunnelController.stats.value
                stats = s
                speed.push(s.downBps, s.upBps)
                speedTick = System.currentTimeMillis()
                state = TunnelController.state.value
                if (state == TunnelState.DISCONNECTED) break
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    /** Keeps the ongoing notification's numbers in step with the session. */
    private fun startNotification(scope: CoroutineScope) {
        stopNotification()
        notifyJob = scope.launch {
            while (true) {
                val s = stats
                val elapsed = if (connectedAt > 0) System.currentTimeMillis() - connectedAt else 0L
                TunnelNotification.update(
                    context = context,
                    connected = state == TunnelState.CONNECTED,
                    durationLabel = formatClock(elapsed),
                    downLabel = formatSpeed(s.downBps),
                    upLabel = formatSpeed(s.upBps),
                    serverLabel = activeProfile?.endpointLabel ?: "Aether edge"
                )
                delay(2000)
            }
        }
    }

    private fun stopNotification() {
        notifyJob?.cancel()
        notifyJob = null
    }

    private fun startWatchdog(scope: CoroutineScope) {
        stopWatchdog()
        watchdogJob = scope.launch {
            while (true) {
                delay(15_000)
                if (state != TunnelState.CONNECTED) break
                if (TunnelController.looksStalled()) {
                    failoverNote = "Tunnel stalled — reconnecting…"
                    withContext(Dispatchers.IO) { runCatching { TunnelController.disconnect(context) } }
                    stopPolling()
                    stopNotification()
                    state = TunnelState.DISCONNECTED
                    busy = false
                    connect(scope)
                    break
                }
            }
        }
    }

    private fun stopWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = null
    }

    // ---- lifetime totals ---------------------------------------------------

    fun lifetimeRx(): Long = sessions.sumOf { it.rxBytes }
    fun lifetimeTx(): Long = sessions.sumOf { it.txBytes }
    fun lifetimeMs(): Long = sessions.sumOf { it.durationMs }

    fun bestLatency(): Long? = probeResults.values.minOrNull()

    private fun formatClock(ms: Long): String {
        val total = ms / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
        else String.format("%02d:%02d", m, s)
    }

    private fun formatSpeed(bps: Long): String {
        val kbps = bps / 1024.0
        return when {
            kbps < 1 -> "0 KB/s"
            kbps < 1000 -> String.format("%.0f KB/s", kbps)
            else -> String.format("%.1f MB/s", kbps / 1024.0)
        }
    }

    companion object {
        /** Seeded on first run; the admin can change it in Settings → Admin. */
        const val DEFAULT_ADMIN_PIN = "2468"
    }
}
