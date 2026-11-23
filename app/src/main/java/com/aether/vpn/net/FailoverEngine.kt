package com.aether.vpn.net

import com.aether.vpn.core.Endpoint
import com.aether.vpn.core.TransportCatalog
import com.aether.vpn.core.TransportConfig
import com.aether.vpn.core.TunnelProfile

/**
 * Brings the tunnel up by trying endpoint after endpoint until one actually carries
 * traffic.
 *
 * WHAT CHANGED AND WHY
 * --------------------
 * The old version probed every candidate with a hand-made WireGuard handshake packet and
 * SKIPPED any endpoint that did not answer. That probe could never get an answer (see
 * [TunnelProbe] for the mac1 reason), so every candidate was skipped and the app never
 * brought a tunnel up at all. The probe is no longer a gate: it only sorts the queue.
 *
 * The only thing that decides success now is the tunnel itself:
 *   1. the backend reports UP,
 *   2. the peer completed a real handshake (WireGuard's own latestHandshakeEpochMillis),
 *   3. a real HTTP request made through the tunnel comes back.
 *
 * If any of those fails, the tunnel is torn down and the next combination is tried -
 * automatically, without the user pressing connect again.
 *
 * The search space is (endpoint x port x MTU). Ports are walked in the order Cloudflare
 * documents for WARP plus the extra ones that survive carrier firewalls. MTU is walked
 * down the ladder, because a network that silently drops large packets accepts the
 * handshake and then stalls.
 *
 * A global deadline keeps a hopeless network from spinning forever: the loop stops when
 * the budget is spent and reports honestly what it saw.
 */
object FailoverEngine {

    data class Attempt(
        val endpoint: Endpoint,
        val probed: Boolean,
        val probeLatencyMs: Long,
        val connected: Boolean,
        val error: String? = null,
        val mtu: Int = 0,
        val verified: Boolean = false,
        val verifyLatencyMs: Long = 0,
        /** Which stage failed: "up", "handshake", "traffic", "ok". */
        val stage: String = ""
    )

    data class Outcome(
        val connected: Boolean,
        val profile: TunnelProfile?,
        val attempts: List<Attempt>,
        val error: String? = null,
        val mtu: Int = 0,
        val verified: Boolean = false,
        /** Machine-readable reason, for the diagnostics screen. */
        val failureKind: FailureKind = FailureKind.NONE
    )

    /**
     * What the verification step saw. Two separate facts, because they call for different
     * next actions: no handshake means this endpoint is blocked (move on), a handshake
     * with no traffic means the path is up but the packet size is wrong (try a smaller
     * MTU on the SAME endpoint).
     */
    data class Verify(
        val handshakeOk: Boolean,
        val trafficOk: Boolean,
        val detail: String = ""
    ) {
        val ok: Boolean get() = handshakeOk && trafficOk
    }

    enum class FailureKind {
        NONE,
        REGISTRATION,
        UNREACHABLE,
        NO_HANDSHAKE,
        NO_TRAFFIC,
        BACKEND,
        TIMEOUT
    }

    /**
     * The candidate queue: the user's chosen edge first, then everything else, ordered by
     * port priority and then by measured latency where we have it.
     */
    fun candidates(
        preferred: Endpoint?,
        pool: List<Endpoint> = TransportCatalog.endpoints,
        latencies: Map<String, Long> = emptyMap()
    ): List<Endpoint> {
        // Group by address, order the addresses by measured latency (unknown last), and
        // within each address walk the ports in priority order.
        //
        // WHY BY ADDRESS AND NOT BY PORT: a carrier blocks a PORT far more often than it
        // blocks an address - UDP 2408 is the usual casualty. Ordering by port first would
        // try all 40 addresses on 2408 (every one failing) before it ever reached port
        // 500, and with a bounded attempt budget it would never reach it at all. Ordering
        // by address means the first eight attempts are one address across all eight
        // ports, so a blocked port is discovered in seconds and the working port is found
        // on the same address.
        val byHost = pool.groupBy { it.host }
        val hosts = byHost.keys.sortedWith(
            compareBy(
                { latencies[byHost[it]!!.first().key] ?: Long.MAX_VALUE },
                { it }
            )
        )
        val ordered = ArrayList<Endpoint>(pool.size)
        hosts.forEach { host ->
            byHost[host]!!.sortedBy { TransportCatalog.portPriority(it.port) }
                .forEach { ordered.add(it) }
        }
        if (preferred == null) return ordered
        return listOf(preferred) + ordered.filter { it.key != preferred.key }
    }

    fun mtuLadder(lastGood: Int, autoMtu: Boolean, fixed: Int): List<Int> {
        if (!autoMtu) return listOf(fixed)
        val base = TransportCatalog.mtuLadder
        return if (lastGood in base) listOf(lastGood) + base.filter { it != lastGood } else base
    }

    /**
     * Runs the failover loop. Blocking - call from Dispatchers.IO.
     *
     * @param verify called after the tunnel is up; returns true when real traffic flowed.
     * @param maxAttempts caps the endpoint loop so a hopeless network fails fast.
     * @param budgetMs overall wall-clock budget for the whole search.
     */
    fun run(
        config: TransportConfig,
        preferred: Endpoint?,
        lastGoodMtu: Int,
        fixedMtu: Int,
        buildProfile: (Endpoint, TransportConfig, Int) -> TunnelProfile,
        connect: (TunnelProfile) -> Unit,
        disconnect: () -> Unit,
        verify: (() -> Verify)? = null,
        maxAttempts: Int = 40,
        budgetMs: Long = 75_000,
        latencies: Map<String, Long> = emptyMap(),
        onAttempt: ((Attempt) -> Unit)? = null
    ): Outcome {
        val startedAt = System.currentTimeMillis()
        val attempts = ArrayList<Attempt>()
        val queue = candidates(preferred, latencies = latencies).take(maxAttempts)
        val mtus = mtuLadder(lastGoodMtu, config.autoMtu, fixedMtu)

        var sawReachable = false
        var sawUp = false
        var sawHandshake = false
        var lastError: String? = null

        fun outOfTime() = System.currentTimeMillis() - startedAt > budgetMs

        for (endpoint in queue) {
            if (outOfTime()) break

            // No probe here on purpose. The queue is already ordered by the cached
            // latencies, and a fresh TCP connect per candidate would spend ~1.2 s of a
            // fixed budget on every endpoint - which is the difference between reaching
            // the working server and timing out before it. Reachability is read from the
            // cache instead.
            val probeReachable = latencies.containsKey(endpoint.key)
            if (probeReachable) sawReachable = true
            val probeLatency = latencies[endpoint.key] ?: 0L

            for (mtu in mtus) {
                if (outOfTime()) break

                val profile = buildProfile(endpoint, config, mtu)
                val result = runCatching { connect(profile) }
                if (result.isFailure) {
                    runCatching { disconnect() }
                    val a = Attempt(
                        endpoint = endpoint,
                        probed = probeReachable,
                        probeLatencyMs = probeLatency,
                        connected = false,
                        error = result.exceptionOrNull()?.message ?: "Tunnel refused",
                        mtu = mtu,
                        stage = "up"
                    )
                    attempts.add(a)
                    onAttempt?.invoke(a)
                    lastError = a.error
                    continue
                }

                sawUp = true

                // The tunnel is up. Now prove it works.
                val verifyStart = System.currentTimeMillis()
                val check = if (verify == null) Verify(true, true)
                else runCatching { verify() }.getOrElse { Verify(false, false, "verify threw") }
                val verifyMs = System.currentTimeMillis() - verifyStart

                if (check.handshakeOk) sawHandshake = true

                if (check.ok) {
                    val a = Attempt(
                        endpoint = endpoint,
                        probed = probeReachable,
                        probeLatencyMs = probeLatency,
                        connected = true,
                        mtu = mtu,
                        verified = true,
                        verifyLatencyMs = verifyMs,
                        stage = "ok"
                    )
                    attempts.add(a)
                    onAttempt?.invoke(a)
                    return Outcome(
                        connected = true,
                        profile = profile,
                        attempts = attempts,
                        mtu = mtu,
                        verified = true,
                        failureKind = FailureKind.NONE
                    )
                }

                // Up but dead. Tear it down.
                runCatching { disconnect() }
                val stage = if (check.handshakeOk) "traffic" else "handshake"
                val msg = if (check.handshakeOk) {
                    "The tunnel opened but no data came through"
                } else {
                    "The server never answered the handshake"
                }
                val a = Attempt(
                    endpoint = endpoint,
                    probed = probeReachable,
                    probeLatencyMs = probeLatency,
                    connected = false,
                    error = msg,
                    mtu = mtu,
                    verified = false,
                    verifyLatencyMs = verifyMs,
                    stage = stage
                )
                attempts.add(a)
                onAttempt?.invoke(a)
                lastError = msg

                // KEY: only a completed handshake says anything about packet size. When
                // the handshake itself fails, the endpoint is blocked and walking the MTU
                // ladder on it just burns the time budget - go straight to the next
                // endpoint. That is what turns a 24-retry stall into a real failover.
                if (!check.handshakeOk) break
            }
        }

        val kind = when {
            attempts.isEmpty() -> FailureKind.TIMEOUT
            sawHandshake -> FailureKind.NO_TRAFFIC
            sawUp -> FailureKind.NO_HANDSHAKE
            !sawReachable -> FailureKind.UNREACHABLE
            else -> FailureKind.BACKEND
        }

        val message = when (kind) {
            FailureKind.UNREACHABLE ->
                "This network is blocking the tunnel. Try mobile data instead of Wi-Fi, " +
                    "or a different network."
            FailureKind.NO_HANDSHAKE ->
                "The server did not respond. Your network may be blocking VPN traffic."
            FailureKind.NO_TRAFFIC ->
                "The tunnel opened but no data came through. Try another server."
            FailureKind.BACKEND ->
                "The tunnel could not start on this device. Try again."
            FailureKind.TIMEOUT ->
                "The connection timed out. Try again, or pick a different server."
            else -> lastError ?: "Could not connect."
        }

        return Outcome(
            connected = false,
            profile = null,
            attempts = attempts,
            error = message,
            failureKind = kind
        )
    }
}
