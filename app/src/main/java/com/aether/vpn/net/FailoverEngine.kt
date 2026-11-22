package com.aether.vpn.net

import com.aether.vpn.core.Endpoint
import com.aether.vpn.core.TransportCatalog
import com.aether.vpn.core.TransportConfig
import com.aether.vpn.core.TunnelProfile

/**
 * Brings the tunnel up by trying endpoint after endpoint until one actually carries
 * traffic.
 *
 * The order is: the endpoint the user picked, then every other endpoint in the pool,
 * primary port (2408) before the fallbacks (500 / 1701 / 4500). Each candidate is probed
 * with a real WireGuard handshake first, so a blocked port is skipped in about a second
 * instead of hanging the connect button.
 *
 * Then, and this is the part that matters, each candidate that answers is brought up and
 * VERIFIED: we wait for a completed handshake and then make a real HTTP request through
 * the tunnel. A tunnel that comes up but cannot fetch a page is torn down and the next
 * candidate is tried. That is what stops the app from showing a green light over a dead
 * connection.
 *
 * On top of that, each reachable endpoint is tried across the MTU ladder. A network that
 * silently drops large packets will accept the handshake and then stall — the classic
 * "connects but nothing loads, and what does load is slow". Stepping the MTU down from
 * 1420 to 1200 is what gets past it, and the value that worked is remembered.
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
        val verifyLatencyMs: Long = 0
    )

    data class Outcome(
        val connected: Boolean,
        val profile: TunnelProfile?,
        val attempts: List<Attempt>,
        val error: String? = null,
        val mtu: Int = 0,
        val verified: Boolean = false
    )

    fun candidates(preferred: Endpoint?, pool: List<Endpoint> = TransportCatalog.endpoints): List<Endpoint> {
        val ordered = pool.sortedWith(
            compareBy({ if (it.port == 2408) 0 else 1 }, { it.host }, { it.port })
        )
        if (preferred == null) return ordered
        return listOf(preferred) + ordered.filter { it.key != preferred.key }
    }

    fun mtuLadder(lastGood: Int, autoMtu: Boolean, fixed: Int): List<Int> {
        if (!autoMtu) return listOf(fixed)
        val base = TransportCatalog.mtuLadder
        return if (lastGood in base) listOf(lastGood) + base.filter { it != lastGood } else base
    }

    /**
     * Runs the failover loop. Blocking — call from Dispatchers.IO.
     *
     * @param verify called after the tunnel is up; returns true when real traffic flowed.
     * @param maxAttempts caps the endpoint loop so a hopeless network fails fast.
     */
    fun run(
        config: TransportConfig,
        preferred: Endpoint?,
        lastGoodMtu: Int,
        fixedMtu: Int,
        buildProfile: (Endpoint, TransportConfig, Int) -> TunnelProfile,
        connect: (TunnelProfile) -> Unit,
        disconnect: () -> Unit,
        verify: (() -> Boolean)? = null,
        maxAttempts: Int = 12,
        probeTimeoutMs: Int = 1200,
        onAttempt: ((Attempt) -> Unit)? = null
    ): Outcome {
        val attempts = ArrayList<Attempt>()
        val queue = candidates(preferred).take(maxAttempts)
        val mtus = mtuLadder(lastGoodMtu, config.autoMtu, fixedMtu)

        var lastVerifyError: String? = null

        for (endpoint in queue) {
            val probe = TunnelProbe.probe(endpoint, probeTimeoutMs)
            if (!probe.reachable) {
                val a = Attempt(
                    endpoint = endpoint,
                    probed = true,
                    probeLatencyMs = probe.latencyMs,
                    connected = false,
                    error = probe.error ?: "No handshake reply"
                )
                attempts.add(a)
                onAttempt?.invoke(a)
                continue
            }

            for (mtu in mtus) {
                val profile = buildProfile(endpoint, config, mtu)
                val result = runCatching { connect(profile) }
                if (result.isFailure) {
                    runCatching { disconnect() }
                    val a = Attempt(
                        endpoint = endpoint,
                        probed = true,
                        probeLatencyMs = probe.latencyMs,
                        connected = false,
                        error = result.exceptionOrNull()?.message ?: "Tunnel refused",
                        mtu = mtu
                    )
                    attempts.add(a)
                    onAttempt?.invoke(a)
                    continue
                }

                // The tunnel is up. Now prove it works.
                val verifyStart = System.currentTimeMillis()
                val verified = if (verify == null) true else runCatching { verify() }.getOrElse { false }
                val verifyMs = System.currentTimeMillis() - verifyStart

                if (verified) {
                    val a = Attempt(
                        endpoint = endpoint,
                        probed = true,
                        probeLatencyMs = probe.latencyMs,
                        connected = true,
                        mtu = mtu,
                        verified = true,
                        verifyLatencyMs = verifyMs
                    )
                    attempts.add(a)
                    onAttempt?.invoke(a)
                    return Outcome(
                        connected = true,
                        profile = profile,
                        attempts = attempts,
                        mtu = mtu,
                        verified = true
                    )
                }

                // Up but dead. Tear it down and try the next MTU on this endpoint.
                lastVerifyError = "Tunnel came up but no traffic passed"
                runCatching { disconnect() }
                val a = Attempt(
                    endpoint = endpoint,
                    probed = true,
                    probeLatencyMs = probe.latencyMs,
                    connected = false,
                    error = "No traffic through the tunnel",
                    mtu = mtu,
                    verified = false,
                    verifyLatencyMs = verifyMs
                )
                attempts.add(a)
                onAttempt?.invoke(a)
            }
        }

        val anyProbe = attempts.any { it.probed && it.error == null }
        val anyUp = attempts.any { it.mtu > 0 && it.error == "No traffic through the tunnel" }
        val message = when {
            attempts.isEmpty() -> "No endpoints to try."
            anyUp -> "The tunnel connected but no traffic passed. This network is blocking " +
                "the tunnel's data path — try a different network or a fallback port."
            !anyProbe -> "No WARP endpoint answered from this network. UDP may be blocked — " +
                "try a different network, or pick a fallback port in Transport."
            else -> "Endpoints answered but the tunnel would not come up. Last error: " +
                (lastVerifyError ?: attempts.lastOrNull { it.error != null }?.error ?: "unknown")
        }
        return Outcome(connected = false, profile = null, attempts = attempts, error = message)
    }
}
