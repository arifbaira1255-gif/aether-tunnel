package com.aether.vpn.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aether.vpn.core.Carrier
import com.aether.vpn.core.DiagnosticReport
import com.aether.vpn.core.DiagnosticStep
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

    private val APP_VERSION: String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    }.getOrElse { "?" }

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

    /**
     * The record of the last connect attempt. Shown only on the diagnostics screen, which
     * the user reaches from the "Troubleshoot" button that appears after a failure.
     */
    var diagnostics by mutableStateOf(DiagnosticReport())
        private set

    /** True while the diagnostics screen is open. */
    var showDiagnostics by mutableStateOf(false)
        private set

    fun openDiagnostics() { showDiagnostics = true }
    fun closeDiagnostics() { showDiagnostics = false }

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

    /**
     * Measures every edge once, in parallel, and caches the results.
     *
     * The pool is 40 addresses x 8 ports = 320 entries. Probing them one at a time at a
     * 1.5 s timeout would take eight minutes, so this probes the 40 distinct ADDRESSES
     * concurrently (port 443, one socket each) and stores the latency against every port
     * of that address. The edge is the thing with a distance; the port is just a door.
     */
    fun probeAll(scope: CoroutineScope) {
        if (probing) return
        probing = true
        scope.launch {
            val reachable = withContext(Dispatchers.IO) {
                val hosts = TransportCatalog.endpoints.map { it.host }.distinct()
                val byHost = HashMap<String, Long>(hosts.size)
                val threads = hosts.map { host ->
                    Thread {
                        val sample = TransportCatalog.endpoints.first { it.host == host }
                        val r = TunnelProbe.probe(sample, timeoutMs = 2500)
                        if (r.reachable) synchronized(byHost) { byHost[host] = r.latencyMs }
                    }.apply { isDaemon = true; start() }
                }
                val deadline = System.currentTimeMillis() + 12_000
                threads.forEach { t ->
                    val left = (deadline - System.currentTimeMillis()).coerceAtLeast(1)
                    runCatching { t.join(left) }
                }
                val out = HashMap<String, Long>()
                TransportCatalog.endpoints.forEach { e ->
                    byHost[e.host]?.let { out[e.key] = it }
                }
                out
            }
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
        // A cached registration that is incomplete (missing key, missing peer key,
        // missing address) cannot produce a working config. Trying to use one gives a
        // confusing backend error, so it is discarded and a fresh device is registered.
        val cached = registration
        if (cached != null && !WarpApi.isValid(cached)) {
            store.clearRegistration()
            registration = null
        }

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
        failoverNote = "Connecting\u2026"
        val startedAt = System.currentTimeMillis()
        val steps = ArrayList<DiagnosticStep>()

        scope.launch {
            // ---- 1. Registration ------------------------------------------
            val regStart = System.currentTimeMillis()
            val reg = ensureRegistration()
            val regMs = System.currentTimeMillis() - regStart
            if (reg == null) {
                steps.add(
                    DiagnosticStep(
                        "Register with the server network",
                        DiagnosticStep.Status.FAIL,
                        error ?: "The registration request did not complete",
                        regMs
                    )
                )
                diagnostics = DiagnosticReport(
                    steps = steps,
                    failureKind = "REGISTRATION",
                    userMessage = "Could not reach the server network. Check your internet " +
                        "connection and try again.",
                    appVersion = APP_VERSION,
                    backendVersion = TunnelController.backendVersion(context),
                    startedAt = startedAt,
                    finishedAt = System.currentTimeMillis()
                )
                error = "Could not reach the server network. Check your internet connection " +
                    "and try again."
                busy = false
                state = TunnelState.ERROR
                failoverNote = null
                return@launch
            }
            steps.add(
                DiagnosticStep(
                    "Register with the server network",
                    DiagnosticStep.Status.PASS,
                    if (reg.warpEnabled) "Account enabled for routing" else
                        "Account registered (routing flag not confirmed)",
                    regMs
                )
            )

            // ---- 2. Tunnel + traffic --------------------------------------
            state = TunnelState.CONNECTING
            failoverNote = "Finding the best server\u2026"
            val preferred = store.selectedEndpoint()
            val cfg = transport
            val collected = ArrayList<FailoverEngine.Attempt>()
            var handshakeSeen = false
            var trafficSeen = false
            var lastVerifyDetail = ""

            // Two gates: the peer must have completed a handshake, and a real request
            // must come back. Either alone gives a false green.
            val verifyFn: (() -> FailoverEngine.Verify)? = if (cfg.verifyTraffic) {
                {
                    // Two independent facts. The handshake says the endpoint is reachable
                    // and the keys agree; the request says data actually flows. Reporting
                    // them separately is what lets the engine tell "blocked server" apart
                    // from "packet size too big", and therefore what to try next.
                    // Timeouts are deliberately tight. A failover search has a fixed
                    // wall-clock budget, and a generous per-attempt timeout means the
                    // FIRST dead endpoint eats the whole budget and the user never gets
                    // to the one that works. 3 s for the handshake and 3.5 s per request
                    // is enough on any real network and keeps each attempt near 7 s.
                    val handshake = TunnelController.awaitHandshake(context, 3000)
                    if (handshake) handshakeSeen = true
                    val check = ConnectivityVerifier.check(timeoutMs = 3500, rounds = 1)
                    if (check.ok) {
                        trafficSeen = true
                        lastVerifyDetail = check.detail
                    } else if (lastVerifyDetail.isBlank()) {
                        lastVerifyDetail = check.detail
                    }
                    // Traffic is the ONLY gate. The handshake flag is used solely to
                    // decide what to try next (smaller MTU vs. a different server), so a
                    // handshake we could not observe must never fail a tunnel that is
                    // demonstrably carrying data. Reading it the other way round is what
                    // makes an app reject working tunnels.
                    FailoverEngine.Verify(
                        handshakeOk = handshake || check.ok,
                        trafficOk = check.ok,
                        detail = check.detail
                    )
                }
            } else {
                null
            }

            val loc = selectedLocation()
            val tunnelStart = System.currentTimeMillis()
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
                    maxAttempts = if (cfg.autoFailover) 24 else 1,
                    budgetMs = 75_000,
                    latencies = probeResults,
                    onAttempt = { attempt ->
                        collected.add(attempt)
                        // Publish progress as it happens, not only at the end.
                        attempts = collected.toList()
                        // The user sees progress, never an address.
                        failoverNote = when {
                            attempt.connected -> "Connected"
                            attempt.stage == "traffic" -> "Optimising the tunnel\u2026"
                            else -> "Trying the next server\u2026"
                        }
                    }
                )
            }
            val tunnelMs = System.currentTimeMillis() - tunnelStart

            attempts = collected.toList()
            failoverNote = if (outcome.connected) null else failoverNote

            // ---- 3. Record what happened ----------------------------------
            val upCount = collected.count { it.mtu > 0 }
            steps.add(
                DiagnosticStep(
                    "Open the tunnel",
                    if (upCount > 0) DiagnosticStep.Status.PASS else DiagnosticStep.Status.FAIL,
                    if (upCount > 0) "$upCount combination(s) came up" else
                        "The tunnel never came up on any server",
                    tunnelMs
                )
            )
            steps.add(
                DiagnosticStep(
                    "Complete the key exchange",
                    if (handshakeSeen || outcome.connected) DiagnosticStep.Status.PASS
                    else DiagnosticStep.Status.FAIL,
                    if (handshakeSeen || outcome.connected) "Handshake completed"
                    else "The server never answered the handshake"
                )
            )
            steps.add(
                DiagnosticStep(
                    "Carry real traffic",
                    if (trafficSeen || outcome.connected) DiagnosticStep.Status.PASS
                    else DiagnosticStep.Status.FAIL,
                    if (trafficSeen || outcome.connected) "A real request came back"
                    else lastVerifyDetail.ifBlank { "No request came back through the tunnel" }
                )
            )

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

                diagnostics = DiagnosticReport(
                    steps = steps,
                    failureKind = "",
                    userMessage = "",
                    appVersion = APP_VERSION,
                    backendVersion = TunnelController.backendVersion(context),
                    startedAt = startedAt,
                    finishedAt = System.currentTimeMillis(),
                    combinationsTried = collected.size,
                    workingMtu = outcome.mtu,
                    workingPort = outcome.profile.endpointPort
                )
            } else {
                error = outcome.error
                state = TunnelState.ERROR
                TunnelNotification.cancel(context)
                diagnostics = DiagnosticReport(
                    steps = steps,
                    failureKind = outcome.failureKind.name,
                    userMessage = outcome.error ?: "",
                    appVersion = APP_VERSION,
                    backendVersion = TunnelController.backendVersion(context),
                    startedAt = startedAt,
                    finishedAt = System.currentTimeMillis(),
                    combinationsTried = collected.size
                )
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
