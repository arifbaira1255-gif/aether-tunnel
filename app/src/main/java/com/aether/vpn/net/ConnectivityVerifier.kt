package com.aether.vpn.net

import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

/**
 * Proves the tunnel actually carries internet traffic.
 *
 * "The backend reported UP" is not the same as "the internet works". A tunnel can come up
 * and still blackhole every packet: a broken IPv6 path, an MTU the carrier silently drops,
 * a DNS server that is unreachable from inside the tunnel, or a peer that accepted the
 * handshake and then went away. All four look identical to the WireGuard API — UP, no
 * error — and all four are what users describe as "connected but nothing loads".
 *
 * So after the tunnel is up we make real requests through it and only call it connected
 * when one of them comes back. That is the difference between a status light and a
 * working VPN.
 */
object ConnectivityVerifier {

    /** Endpoints that answer a tiny request and are reachable from almost anywhere. */
    private val PROBES = listOf(
        "https://cloudflare.com/cdn-cgi/trace",
        "https://1.1.1.1/cdn-cgi/trace",
        "https://www.gstatic.com/generate_204",
        "https://connectivitycheck.gstatic.com/generate_204"
    )

    data class Result(
        val ok: Boolean,
        val latencyMs: Long,
        val detail: String
    )

    /**
     * Makes one real HTTP request through whatever route the device currently has.
     * Blocking — call from Dispatchers.IO.
     */
    fun check(timeoutMs: Int = 6000): Result {
        val started = System.currentTimeMillis()
        var lastError = "no probe answered"
        for (url in PROBES) {
            val attempt = runCatching { hit(url, timeoutMs) }
            val r = attempt.getOrNull()
            if (r != null && r.ok) {
                return Result(true, System.currentTimeMillis() - started, url)
            }
            lastError = attempt.exceptionOrNull()?.message ?: "HTTP ${r?.code}"
        }
        return Result(false, System.currentTimeMillis() - started, lastError)
    }

    private data class Hit(val ok: Boolean, val code: Int)

    private fun hit(url: String, timeoutMs: Int): Hit {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            instanceFollowRedirects = false
            useCaches = false
            setRequestProperty("User-Agent", "AetherTunnel/1.3")
            setRequestProperty("Cache-Control", "no-cache")
        }
        return try {
            val code = conn.responseCode
            // 204 from generate_204, 200 from the trace endpoint. Anything in 2xx/3xx
            // means packets made the round trip.
            Hit(code in 200..399, code)
        } finally {
            runCatching { conn.disconnect() }
        }
    }

    /**
     * Resolves a name through the tunnel's DNS. A tunnel whose DNS does not answer is
     * unusable even when raw IP traffic works, because every app resolves first.
     */
    fun dnsWorks(host: String = "cloudflare.com", timeoutMs: Int = 4000): Boolean =
        runCatching {
            val task = java.util.concurrent.FutureTask {
                InetAddress.getAllByName(host).isNotEmpty()
            }
            val thread = Thread(task).apply { isDaemon = true }
            thread.start()
            task.get(timeoutMs.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
        }.getOrElse { false }
}
