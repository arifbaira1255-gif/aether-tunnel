package com.aether.vpn.net

import com.aether.vpn.core.Endpoint
import com.aether.vpn.core.TransportCatalog
import com.aether.vpn.core.TransportConfig
import com.aether.vpn.core.TunnelProfile

/**
 * Brings the tunnel up by trying endpoint after endpoint until one answers.
 *
 * The order is: the endpoint the user picked, then every other endpoint in the pool,
 * primary port (2408) before the fallbacks (500 / 1701 / 4500). Each candidate is probed
 * with a real WireGuard handshake first, so a blocked port is skipped in about a second
 * instead of hanging the connect button.
 *
 * On top of that, each reachable endpoint is tried across the MTU ladder. A network that
 * silently drops large packets will accept the handshake and then stall — the classic
 * "connects but nothing loads" failure. Stepping the MTU down from 1420 to 1200 is what
 * gets past it, and the value that worked is remembered for next time.
 *
 * Nothing here is simulated: [run] calls the real tunnel, and success means the
 * WireGuard backend reported UP.
 */
object FailoverEngine {

    /** What one attempt did, for the UI to show. */
    data class Attempt(
        val endpoint: Endpoint,
        val probed: Boolean,
        val probeLatencyMs: Long,
        val connected: Boolean,
        val error: String? = null,
        val mtu: Int = 0
    )

    data class Outcome(
        val connected: Boolean,
        val profile: TunnelProfile?,
        val attempts: List<Attempt>,
        val error: String? = null,
        val mtu: Int = 0
    )

    /**
     * Builds the candidate order for a config.
     * @param preferred the endpoint the user selected, tried first.
     */
    fun candidates(preferred: Endpoint?, pool: List<Endpoint> = TransportCatalog.endpoints): List<Endpoint> {
        val ordered = pool.sortedWith(
            compareBy({ if (it.port == 2408) 0 else 1 }, { it.host }, { it.port })
        )
        if (preferred == null) return ordered
        return listOf(preferred) + ordered.filter { it.key != preferred.key }
    }

    /**
     * The MTU values to try, in order. When [lastGood] is known it goes first, so a
     * returning user connects on the first try instead of walking the ladder again.
     */
    fun mtuLadder(lastGood: Int, autoMtu: Boolean, fixed: Int): List<Int> {
        if (!autoMtu) return listOf(fixed)
        val base = TransportCatalog.mtuLadder
        return if (lastGood in base) listOf(lastGood) + base.filter { it != lastGood } else base
    }

    /**
     * Runs the failover loop. Blocking — call from Dispatchers.IO.
     *
     * @param buildProfile turns an endpoint + transport config + MTU into a connectable profile.
     * @param connect brings the tunnel up; throws on failure.
     * @param disconnect tears a failed attempt down before the next one.
     * @param maxAttempts caps the endpoint loop so a hopeless network fails fast instead of
     *        grinding through the whole pool.
     * @param onAttempt called after each attempt so the UI can show progress.
     */
    fun run(
        config: TransportConfig,
        preferred: Endpoint?,
        lastGoodMtu: Int,
        fixedMtu: Int,
        buildProfile: (Endpoint, TransportConfig, Int) -> TunnelProfile,
        connect: (TunnelProfile) -> Unit,
        disconnect: () -> Unit,
        maxAttempts: Int = 12,
        probeTimeoutMs: Int = 1200,
        onAttempt: ((Attempt) -> Unit)? = null
    ): Outcome {
        val attempts = ArrayList<Attempt>()
        val queue = candidates(preferred).take(maxAttempts)
        val mtus = mtuLadder(lastGoodMtu, config.autoMtu, fixedMtu)

        for (endpoint in queue) {
            // 1. Probe: is this endpoint alive from this network at all?
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

            // 2. It answered — try the real tunnel on it, walking the MTU ladder.
            for (mtu in mtus) {
                val profile = buildProfile(endpoint, config, mtu)
                val result = runCatching { connect(profile) }
                if (result.isSuccess) {
                    val a = Attempt(endpoint, true, probe.latencyMs, true, mtu = mtu)
                    attempts.add(a)
                    onAttempt?.invoke(a)
                    return Outcome(
                        connected = true,
                        profile = profile,
                        attempts = attempts,
                        mtu = mtu
                    )
                }

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
            }
        }

        val reachable = attempts.count { it.connected || (it.probed && it.error == null) }
        val message = when {
            attempts.isEmpty() -> "No endpoints to try."
            reachable == 0 ->
                "No WARP endpoint answered from this network. UDP may be blocked — " +
                    "try a different network, or pick a fallback port in Transport."
            else -> "Endpoints answered but the tunnel would not come up. Last error: " +
                (attempts.lastOrNull { it.error != null }?.error ?: "unknown")
        }
        return Outcome(connected = false, profile = null, attempts = attempts, error = message)
    }
}
