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
 * handshake and then went away. All four look identical to the WireGuard API - UP, no
 * error - and all four are what users describe as "connected but nothing loads".
 *
 * So after the tunnel is up we make real requests through it and only call it connected
 * when one of them comes back.
 *
 * TWO DETAILS THAT MATTER
 * -----------------------
 * 1. IP-FIRST. The first probes are bare IP addresses (1.1.1.1, 8.8.8.8 style), so a
 *    broken DNS path inside the tunnel cannot make a working tunnel look dead. Only after
 *    those fail do we try hostnames.
 * 2. RETRIES. Right after `establish()` the routes are installed but the first packet can
 *    still be dropped while the kernel settles. A single attempt therefore produces false
 *    negatives, which is exactly what makes an app give up on a tunnel that would have
 *    worked. Each probe is retried a few times with a short backoff.
 */
object ConnectivityVerifier {

    /**
     * Probes in order. The IP literals come first on purpose: they need no DNS, so they
     * isolate "is the data path up" from "does DNS work".
     */
    private val PROBES = listOf(
        "https://1.1.1.1/cdn-cgi/trace",
        "https://8.8.8.8/generate_204",
        "https://cloudflare.com/cdn-cgi/trace",
        "https://www.gstatic.com/generate_204",
        "https://connectivitycheck.gstatic.com/generate_204"
    )

    data class Result(
        val ok: Boolean,
        val latencyMs: Long,
        val detail: String,
        /** How many attempts were made before giving up. */
        val attempts: Int = 1
    )

    /**
     * Makes real HTTP requests through whatever route the device currently has.
     * Blocking - call from Dispatchers.IO.
     *
     * @param timeoutMs per-request timeout.
     * @param rounds how many full passes over [PROBES] to make before giving up.
     */
    fun check(timeoutMs: Int = 5000, rounds: Int = 2): Result {
        val started = System.currentTimeMillis()
        var lastError = "no probe answered"
        var made = 0

        for (round in 0 until rounds) {
            for (url in PROBES) {
                made++
                val attempt = runCatching { hit(url, timeoutMs) }
                val r = attempt.getOrNull()
                if (r != null && r.ok) {
                    return Result(
                        ok = true,
                        latencyMs = System.currentTimeMillis() - started,
                        detail = url,
                        attempts = made
                    )
                }
                lastError = attempt.exceptionOrNull()?.message ?: "HTTP ${r?.code}"
            }
            // Short backoff between rounds: the first packet after establish() is often
            // dropped while the routes settle.
            if (round < rounds - 1) runCatching { Thread.sleep(400) }
        }

        return Result(
            ok = false,
            latencyMs = System.currentTimeMillis() - started,
            detail = lastError,
            attempts = made
        )
    }

    private data class Hit(val ok: Boolean, val code: Int)

    private fun hit(url: String, timeoutMs: Int): Hit {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            instanceFollowRedirects = false
            useCaches = false
            setRequestProperty("User-Agent", "AetherTunnel/1.5")
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
