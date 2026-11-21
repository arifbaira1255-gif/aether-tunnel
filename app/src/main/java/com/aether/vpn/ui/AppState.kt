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
import com.aether.vpn.net.FailoverEngine
import com.aether.vpn.net.TunnelProbe
import com.aether.vpn.net.WarpApi
import com.aether.vpn.service.TunnelController
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

    /** The SNI / payload / endpoint selection. */
    var transport by mutableStateOf(store.transport())
        private set

    /** Live failover progress, so the connect screen can show what it is trying. */
    var failoverNote by mutableStateOf<String?>(null)
        private set
    var attempts by mutableStateOf<List<FailoverEngine.Attempt>>(emptyList())
        private set

    /** Cached endpoint reachability: endpoint key -> latency in ms. */
    var probeResults by mutableStateOf(store.probeResults())
        private set
    var probing by mutableStateOf(false)
        private set

    /** Rolling throughput window for the live graph. */
    val speed = SpeedHistory(60)
    var speedTick by mutableStateOf(0L)
        private set

    /** The MTU the current session came up on, 0 when not connected. */
    var activeMtu by mutableStateOf(0)
        private set

    val sessions = mutableStateListOf<SessionRecord>()
    val favourites = mutableStateListOf<String>()

    private var pollJob: kotlinx.coroutines.Job? = null
    private var watchdogJob: kotlinx.coroutines.Job? = null

    init {
        registration = store.registration()
        sessions.addAll(store.sessions())
        favourites.addAll(store.favourites())
    }

    /** The endpoint pool the server screen shows. */
    fun endpoints(): List<Endpoint> = TransportCatalog.endpoints

    fun refreshFromStore() {
        sessions.clear(); sessions.addAll(store.sessions())
        favourites.clear(); favourites.addAll(store.favourites())
    }

    fun toggleFavourite(id: String) {
        store.toggleFavourite(id)
        refreshFromStore()
    }

    // ---- transport editing -------------------------------------------------

    fun updateTransport(block: (TransportConfig) -> TransportConfig) {
        val next = block(transport)
        transport = next
        store.saveTransport(next)
    }

    fun selectEndpoint(endpoint: Endpoint) {
        updateTransport { it.copy(endpointKey = endpoint.key) }
    }

    fun selectSni(id: String) = updateTransport { it.copy(sniId = id, customSni = "") }

    fun selectPayload(id: String) = updateTransport { it.copy(payloadId = id) }

    fun setCustomSni(value: String) = updateTransport { it.copy(customSni = value) }

    fun setCustomPayload(value: String) = updateTransport { it.copy(customPayload = value) }

    fun setAutoFailover(value: Boolean) = updateTransport { it.copy(autoFailover = value) }

    fun setAutoMtu(value: Boolean) = updateTransport { it.copy(autoMtu = value) }

    fun setWatchdog(value: Boolean) = updateTransport { it.copy(watchdog = value) }

    fun setDnsPreset(id: String) {
        updateTransport { it.copy(dnsId = id) }
        TransportCatalog.dnsById(id)?.let { store.dns = it.servers.joinToString(", ") }
    }

    /**
     * Switches the whole transport profile to a carrier: its SNI host, its payload and
     * its DNS. This is the one-tap "I am on Jazz" action.
     */
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

    /** The SNI host currently in effect, for display. */
    fun activeSni(): String = transport.effectiveSni

    /** The rendered payload currently in effect, for display. */
    fun activePayload(): String = TransportCatalog.renderPayload(
        template = transport.effectivePayloadTemplate,
        host = transport.effectiveSni,
        port = 443,
        sni = transport.effectiveSni
    )

    // ---- probing -----------------------------------------------------------

    /**
     * Probes the whole endpoint pool from this device and caches the results.
     * Blocking work runs on IO; the UI shows [probing] while it happens.
     */
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
                // Move the selection to the fastest endpoint that answered.
                reachable.entries.minByOrNull { it.value }?.let { best ->
                    TransportCatalog.endpoints.firstOrNull { it.key == best.key }
                        ?.let { selectEndpoint(it) }
                }
            }
        }
    }

    // ---- registration ------------------------------------------------------

    /** Registers a WARP device if we do not have one yet. */
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

    /** The profile the tunnel would use right now, with preferences applied. */
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
        failoverNote = "Registering with the WARP edge\u2026"
        scope.launch {
            val reg = ensureRegistration()
            if (reg == null) {
                busy = false
                state = TunnelState.ERROR
                failoverNote = null
                return@launch
            }

            state = TunnelState.CONNECTING
            failoverNote = "Probing endpoints\u2026"
            val preferred = store.selectedEndpoint()
            val cfg = transport
            val collected = ArrayList<FailoverEngine.Attempt>()

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
                    maxAttempts = if (cfg.autoFailover) 12 else 1,
                    onAttempt = { attempt -> collected.add(attempt) }
                )
            }

            // Publish the attempt log on the main thread, once.
            attempts = collected.toList()
            failoverNote = when {
                outcome.connected && outcome.profile != null ->
                    "Connected via ${outcome.profile.endpointLabel} \u00b7 MTU ${outcome.mtu}"
                collected.isNotEmpty() ->
                    "Tried ${collected.size} combination(s) \u2014 none came up"
                else -> null
            }

            if (outcome.connected && outcome.profile != null) {
                activeProfile = outcome.profile
                activeMtu = outcome.mtu
                connectedAt = TunnelController.connectedSince()
                state = TunnelState.CONNECTED
                // Remember what actually worked, so the next connect is instant.
                store.lastGoodMtu = outcome.mtu
                store.mtu = outcome.mtu
                TransportCatalog.endpoints
                    .firstOrNull { it.key == outcome.profile.endpointLabel }
                    ?.let { selectEndpoint(it) }
                speed.clear()
                startPolling(scope)
                if (cfg.watchdog) startWatchdog(scope)
            } else {
                error = outcome.error
                state = TunnelState.ERROR
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

    /**
     * Reconnects when the tunnel claims to be up but nothing is moving. This is the
     * difference between "connected" and "actually working" on a flaky mobile network.
     */
    private fun startWatchdog(scope: CoroutineScope) {
        stopWatchdog()
        watchdogJob = scope.launch {
            while (true) {
                delay(15_000)
                if (state != TunnelState.CONNECTED) break
                if (TunnelController.looksStalled()) {
                    failoverNote = "Tunnel stalled \u2014 reconnecting\u2026"
                    withContext(Dispatchers.IO) { runCatching { TunnelController.disconnect(context) } }
                    stopPolling()
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

    /** The best measured latency in the pool, or null when nothing has been probed. */
    fun bestLatency(): Long? = probeResults.values.minOrNull()
}
