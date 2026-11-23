package com.aether.vpn.net

import com.aether.vpn.core.Endpoint
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Measures how quickly a WARP edge answers from THIS device's network.
 *
 * WHY THIS IS A TCP PROBE AND NOT A WIREGUARD HANDSHAKE PROBE
 * -----------------------------------------------------------
 * The previous version of this file sent a hand-made WireGuard handshake-initiation
 * packet and waited for a reply. That could never work, and it was the reason the app
 * refused to connect at all:
 *
 *   WireGuard's receive path calls cookieChecker.CheckMAC1(packet) on every
 *   handshake-initiation before it does anything else. mac1 is a BLAKE2s-128 keyed with
 *   the peer's static public key, computed over the first 116 bytes of the packet. A
 *   packet whose mac1 is zero (or random) fails that check, and the peer does
 *   "goto skip" - it drops the datagram silently, with no reply of any kind.
 *
 *   So the probe reported "no reply" for every endpoint on every network, the failover
 *   engine skipped every candidate, and the app showed "No server answered" without ever
 *   bringing a tunnel up. The tunnel itself was fine; the gate in front of it was not.
 *
 * A correct handshake probe would need X25519, ChaCha20-Poly1305 and BLAKE2s to build a
 * packet the peer will actually answer - i.e. it would be a second WireGuard
 * implementation, and any bug in it would produce exactly the same false negative.
 *
 * So the probe is now what it can honestly be: a TCP connect to the same anycast edge on
 * port 443. Cloudflare answers TCP 443 on these addresses from the same data centres that
 * answer WARP on UDP, so it is a real measurement of "can this network reach this edge,
 * and how far away is it". It is used ONLY to sort the server list and to order the
 * failover queue. It is never a gate: whether the tunnel works is decided by the tunnel
 * itself (a completed handshake plus a real request through it), which is the only
 * signal that cannot lie.
 */
object TunnelProbe {

    data class Result(
        val endpoint: Endpoint,
        val reachable: Boolean,
        val latencyMs: Long,
        val error: String? = null
    )

    /**
     * Probes one edge. Blocking - call from Dispatchers.IO.
     * @param timeoutMs how long to wait for the TCP handshake.
     */
    fun probe(endpoint: Endpoint, timeoutMs: Int = 1500): Result {
        val started = System.currentTimeMillis()
        return try {
            Socket().use { socket ->
                socket.tcpNoDelay = true
                socket.connect(InetSocketAddress(endpoint.host, 443), timeoutMs)
                Result(
                    endpoint = endpoint,
                    reachable = true,
                    latencyMs = System.currentTimeMillis() - started
                )
            }
        } catch (t: Throwable) {
            Result(
                endpoint = endpoint,
                reachable = false,
                latencyMs = System.currentTimeMillis() - started,
                error = t.message ?: t.javaClass.simpleName
            )
        }
    }

    /**
     * Probes a list and returns them sorted: reachable first (fastest first), then the
     * rest. Blocking - call from Dispatchers.IO.
     */
    fun probeAll(
        endpoints: List<Endpoint>,
        timeoutMs: Int = 1500,
        onProgress: ((done: Int, total: Int, result: Result) -> Unit)? = null
    ): List<Result> {
        val results = ArrayList<Result>(endpoints.size)
        endpoints.forEachIndexed { index, endpoint ->
            val r = probe(endpoint, timeoutMs)
            results.add(r)
            onProgress?.invoke(index + 1, endpoints.size, r)
        }
        return results.sortedWith(
            compareBy({ if (it.reachable) 0 else 1 }, { it.latencyMs })
        )
    }
}
