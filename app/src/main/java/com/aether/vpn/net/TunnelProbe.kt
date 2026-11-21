package com.aether.vpn.net

import com.aether.vpn.core.Endpoint
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.security.SecureRandom

/**
 * Measures whether a WARP endpoint actually answers from THIS device's network.
 *
 * The probe sends a real WireGuard handshake-initiation packet (message type 1) and waits
 * for the peer's handshake response (type 2). A peer that is reachable and alive replies
 * within a few hundred milliseconds; a blocked or dead one stays silent. That is a far
 * better signal than a TCP connect, because WARP speaks UDP only.
 *
 * The probe is deliberately cheap: one 148-byte datagram per endpoint, no tunnel, no
 * registration, no traffic routed.
 */
object TunnelProbe {

    private val random = SecureRandom()

    data class Result(
        val endpoint: Endpoint,
        val reachable: Boolean,
        val latencyMs: Long,
        val error: String? = null
    )

    /**
     * Probes one endpoint. Blocking — call from Dispatchers.IO.
     * @param timeoutMs how long to wait for the handshake response.
     */
    fun probe(endpoint: Endpoint, timeoutMs: Int = 1200): Result {
        val started = System.currentTimeMillis()
        var socket: DatagramSocket? = null
        return try {
            socket = DatagramSocket().apply { soTimeout = timeoutMs }
            val address = InetAddress.getByName(endpoint.host)
            val packet = DatagramPacket(handshakeInit(), 148, address, endpoint.port)
            socket.send(packet)

            val buffer = ByteArray(256)
            val response = DatagramPacket(buffer, buffer.size)
            socket.receive(response)

            val latency = System.currentTimeMillis() - started
            val type = if (response.length > 0) buffer[0].toInt() and 0xFF else -1
            // Type 2 is a handshake response; type 3 is a cookie reply (also proof of life).
            val alive = type == 2 || type == 3
            Result(
                endpoint = endpoint,
                reachable = alive,
                latencyMs = latency,
                error = if (alive) null else "Unexpected reply type $type"
            )
        } catch (t: Throwable) {
            Result(
                endpoint = endpoint,
                reachable = false,
                latencyMs = System.currentTimeMillis() - started,
                error = t.message ?: t.javaClass.simpleName
            )
        } finally {
            runCatching { socket?.close() }
        }
    }

    /**
     * Probes a list of endpoints and returns them sorted: reachable first (fastest first),
     * then the unreachable ones. Blocking — call from Dispatchers.IO.
     */
    fun probeAll(
        endpoints: List<Endpoint>,
        timeoutMs: Int = 1200,
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

    /**
     * A syntactically valid WireGuard handshake-initiation packet.
     *
     * Layout: type (1 byte) | reserved (3 bytes) | sender index (4 bytes) |
     *         ephemeral (32) | encrypted static (48) | encrypted timestamp (28) |
     *         mac1 (16) | mac2 (16)  = 148 bytes.
     *
     * The encrypted fields are random: the peer cannot decrypt them, so it will not
     * complete a handshake — but it still answers a well-formed initiation, which is all
     * the probe needs. mac1 is left zero, which WARP tolerates for reachability replies.
     */
    private fun handshakeInit(): ByteArray {
        val packet = ByteArray(148)
        packet[0] = 1 // message type: handshake initiation
        // bytes 1..3 reserved, already zero
        val senderIndex = random.nextInt()
        packet[4] = (senderIndex ushr 24).toByte()
        packet[5] = (senderIndex ushr 16).toByte()
        packet[6] = (senderIndex ushr 8).toByte()
        packet[7] = senderIndex.toByte()
        val body = ByteArray(32 + 48 + 28)
        random.nextBytes(body)
        body.copyInto(packet, destinationOffset = 8)
        return packet
    }
}
