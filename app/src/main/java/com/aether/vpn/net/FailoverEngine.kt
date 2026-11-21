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
        val error: String? = null
    )

    data class Outcome(
        val connected: Boolean,
        val profile: TunnelProfile?,
        val attempts: List<Attempt>,
        val error: String? = null
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
     * Runs the failover loop. Blocking — call from Dispatchers.IO.
     *
     * @param buildProfile turns an endpoint + transport config into a connectable profile.
     * @param connect brings the tunnel up; throws on failure.
     * @param disconnect tears a failed attempt down before the next one.
     * @param maxAttempts caps the loop so a hopeless network fails fast instead of
     *        grinding through the whole pool.
     * @param onAttempt called after each attempt so the UI can show progress.
     */
    fun run(
        config: TransportConfig,
        preferred: Endpoint?,
        buildProfile: (Endpoint, TransportConfig) -> TunnelProfile,
        connect: (TunnelProfile) -> Unit,
        disconnect: () -> Unit,
        maxAttempts: Int = 12,
        probeTimeoutMs: Int = 1200,
        onAttempt: ((Attempt) -> Unit)? = null
    ): Outcome {
        val attempts = ArrayList<Attempt>()
        val queue = candidates(preferred).take(maxAttempts)

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

            // 2. It answered — try the real tunnel on it.
            val profile = buildProfile(endpoint, config)
            val result = runCatching { connect(profile) }
            if (result.isSuccess) {
                val a = Attempt(endpoint, true, probe.latencyMs, true)
                attempts.add(a)
                onAttempt?.invoke(a)
                return Outcome(connected = true, profile = profile, attempts = attempts)
            }

            runCatching { disconnect() }
            val a = Attempt(
                endpoint = endpoint,
                probed = true,
                probeLatencyMs = probe.latencyMs,
                connected = false,
                error = result.exceptionOrNull()?.message ?: "Tunnel refused"
            )
            attempts.add(a)
            onAttempt?.invoke(a)
        }

        val reachable = attempts.count { it.connected || it.error == null }
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
