package com.aether.vpn.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aether.vpn.core.Carrier
import com.aether.vpn.core.Endpoint
import com.aether.vpn.core.LiveStats
import com.aether.vpn.core.Location
import com.aether.vpn.core.Locations
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

    /** Internal transport state. The app owns it; there is no settings panel for it. */
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

    val sessions = mutableStateListOf<SessionRecord>()
    val favourites = mutableStateListOf<String>()

    private var pollJob: kotlinx.coroutines.Job? = null
    private var watchdogJob: kotlinx.coroutines.Job? = null
    private var notifyJob: kotlinx.coroutines.Job? = null

    init {
        registration = store.registration()
        sessions.addAll(store.sessions())
        favourites.addAll(store.favourites())
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

    // ---- transport (internal; set by the app, not exposed as a panel) ------

    fun updateTransport(block: (TransportConfig) -> TransportConfig) {
        val next = block(transport)
        transport = next
        store.saveTransport(next)
    }

    // ---- server locations (the user-facing choice) -------------------------

    /** The location the user picked, or null for Automatic. */
    fun selectedLocation(): Location? =
        Locations.byId(transport.locationId).takeIf { transport.locationId != Locations.AUTO_ID }

    /** What to show as the current server: a name and a place, never an address. */
    fun activeServerTitle(): String =
        activeProfile?.serverTitle
            ?: selectedLocation()?.title
            ?: "Automatic · Fastest reachable edge"

    fun activeServerFlag(): String =
        activeProfile?.flag ?: selectedLocation()?.flag ?: Locations.auto.flag

    /**
     * The user picked a location. Persisted immediately, so the choice survives a restart
     * and is what the next connect uses.
     */
    fun selectLocation(loc: Location) {
        store.selectLocation(loc)
        transport = store.transport()
    }

    /**
     * Remembers the edge that actually worked, so the next connect starts from something
     * known-good on this network.
     *
     * Only remembered while the user is on Automatic. When they have pinned a location,
     * its edge is the one they asked for and we do not silently move them somewhere else.
     */
    private fun rememberWorkingEndpoint(endpoint: Endpoint) {
        if (transport.locationId != Locations.AUTO_ID) return
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
                error = "No server answered from this network. The tunnel may be blocked here."
            } else {
                error = null
                // Remember the fastest edge internally for Automatic. A pinned location
                // keeps its own edge - the user asked for that one.
                reachable.entries.minByOrNull { it.value }?.let { best ->
                    TransportCatalog.endpoints.firstOrNull { it.key == best.key }
                        ?.let { rememberWorkingEndpoint(it) }
                }
            }
        }
    }

    // ---- registration ------------------------------------------------------

    suspend fun ensureRegistration(): WarpApi.Registration? {
        // Re-enable WARP routing on every connect. The call is idempotent, and a device
        // that registered on an earlier install (or whose enable call failed) would
        // otherwise stay disabled and drop every packet - the classic "connected but
        // nothing loads".
        registration?.let { existing ->
            return withContext(Dispatchers.IO) {
                runCatching { WarpApi.enable(existing) }
                    .onSuccess {
                        if (it.warpEnabled != existing.warpEnabled) store.saveRegistration(it)
                        registration = it
                    }
                    .getOrDefault(existing)
            }
        }
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
        return store.applyPreferences(base).namedFor(selectedLocation())
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

            val loc = selectedLocation()
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
                        ).namedFor(loc)
                    },
                    connect = { profile -> TunnelController.connect(context, profile) },
                    disconnect = { runCatching { TunnelController.disconnect(context) } },
                    verify = verifyFn,
                    maxAttempts = if (cfg.autoFailover) 12 else 1,
                    onAttempt = { attempt ->
                        collected.add(attempt)
                        // Publish progress as it happens, not only at the end.
                        attempts = collected.toList()
                        // The user sees progress, never an address.
                        failoverNote = when {
                            attempt.connected -> "Connected"
                            attempt.mtu > 0 -> "Optimising the tunnel…"
                            else -> "Trying the next server…"
                        }
                    }
                )
            }

            attempts = collected.toList()
            failoverNote = if (outcome.connected) null else failoverNote

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
                    .firstOrNull {
                        it.host == outcome.profile.endpointHost &&
                            it.port == outcome.profile.endpointPort
                    }
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
            val endpoint = activeProfile?.serverTitle ?: ""
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
                    serverLabel = activeProfile?.serverTitle ?: "Automatic"
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

    /**
     * Names a profile after the chosen location. The profile keeps its real address
     * internally - the UI only ever shows the country and city.
     */
    private fun TunnelProfile.namedFor(loc: Location?): TunnelProfile = copy(
        country = loc?.country ?: "Automatic",
        city = loc?.city ?: "Fastest reachable edge",
        flag = loc?.flag ?: Locations.auto.flag,
        locationId = loc?.id ?: Locations.AUTO_ID,
        name = loc?.title ?: "Automatic"
    )

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

}
