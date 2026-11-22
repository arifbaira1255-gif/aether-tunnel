package com.aether.vpn.core

/** A WireGuard peer profile the app can connect to. */
data class TunnelProfile(
    val id: String,
    val name: String,
    val country: String,
    val city: String,
    val flag: String,
    val endpointHost: String,
    val endpointPort: Int,
    val peerPublicKey: String,
    val privateKey: String,
    val addressV4: String,
    val addressV6: String?,
    val dns: List<String>,
    val mtu: Int,
    val allowedIps: List<String>,
    val persistentKeepalive: Int,
    val tier: Tier,
    val sni: String? = null,
    val hostHeader: String? = null,
    val payload: String? = null,
    val sniId: String? = null,
    val payloadId: String? = null,
    val isCustom: Boolean = false,
    /** Which [Location] this profile came from, so the UI can name it. */
    val locationId: String = Locations.AUTO_ID
) {
    enum class Tier { FREE, PREMIUM }

    /**
     * What the user sees. Never an address: a name and a place.
     * "United States · New York".
     */
    val serverTitle: String get() = if (city.isBlank()) country else "$country · $city"

    /** The wg-quick text form, so the user can export/share the profile. */
    fun toWgQuick(): String = buildString {
        appendLine("[Interface]")
        appendLine("PrivateKey = $privateKey")
        appendLine("Address = $addressV4")
        addressV6?.let { appendLine("Address = $it") }
        if (dns.isNotEmpty()) appendLine("DNS = ${dns.joinToString(", ")}")
        appendLine("MTU = $mtu")
        appendLine()
        appendLine("[Peer]")
        appendLine("PublicKey = $peerPublicKey")
        appendLine("AllowedIPs = ${allowedIps.joinToString(", ")}")
        appendLine("Endpoint = $endpointHost:$endpointPort")
        appendLine("PersistentKeepalive = $persistentKeepalive")
        // Transport hints are not part of the WireGuard spec, so they go in comments:
        // a stock wg-quick still parses the file, and a TLS-wrapping relay can read them.
        sni?.let { appendLine("# SNI = $it") }
        hostHeader?.let { appendLine("# Host = $it") }
        payload?.takeIf { it.isNotBlank() }?.let {
            appendLine("# Payload = ${it.replace("\r\n", "\\n")}")
        }
    }

}

/**
 * Internal transport settings. The app ships with a working configuration already in
 * place and maintains it itself; there is no settings panel for any of it. The user
 * chooses a location on the Servers screen, and nothing else.
 */
data class TransportConfig(
    val sniId: String = "cf-engage",
    val payloadId: String = "pl-none",
    val customSni: String = "",
    val customPayload: String = "",
    /** The location the user picked on the Servers screen. */
    val locationId: String = Locations.AUTO_ID,
    /** The edge the failover engine last proved works. Internal; never shown as an IP. */
    val endpointKey: String = "162.159.192.1:2408",
    val autoFailover: Boolean = true,
    val carrier: Carrier = Carrier.AUTO,
    val autoMtu: Boolean = true,
    val watchdog: Boolean = true,
    val dnsId: String = "dns-cf",
    /** Verify real traffic flows before declaring the tunnel connected. */
    val verifyTraffic: Boolean = true,
    /** Route IPv6 through the tunnel. Off by default: a broken IPv6 path is the
     *  single most common cause of "connected but nothing loads". */
    val routeIpv6: Boolean = false
) {
    val effectiveSni: String
        get() = customSni.ifBlank {
            TransportCatalog.sniById(sniId)?.host ?: "engage.cloudflareclient.com"
        }

    val effectivePayloadTemplate: String
        get() = if (payloadId == "pl-custom") customPayload
        else TransportCatalog.payloadById(payloadId)?.template ?: ""
}

/** The Pakistani carriers the app can tune itself for. */
enum class Carrier(val title: String, val short: String, val flag: String) {
    AUTO("Auto-detect", "Auto", "\uD83C\uDF10"),
    JAZZ("Jazz", "Jazz", "\uD83C\uDFE1"),
    TELENOR("Telenor", "Telenor", "\uD83D\uDD35"),
    UFONE("Ufone", "Ufone", "\uD83C\uDFE0"),
    ZONG("Zong", "Zong", "\uD83C\uDFE3"),
    OTHER("Other network", "Other", "\uD83C\uDF10");

    val defaultSniId: String
        get() = when (this) {
            JAZZ -> "pkj-selfcare"
            TELENOR -> "pkt-easypaisa"
            UFONE -> "pku-upaisa"
            ZONG -> "pkz-www"
            AUTO, OTHER -> "cf-engage"
        }

    val defaultPayloadId: String
        get() = when (this) {
            JAZZ -> "pl-jazz-selfcare"
            TELENOR -> "pl-telenor-easypaisa"
            UFONE -> "pl-ufone-upaisa"
            ZONG -> "pl-zong-portal"
            AUTO, OTHER -> "pl-none"
        }
}

/** One finished connection, kept for the stats screen. */
data class SessionRecord(
    val profileName: String,
    val flag: String,
    val startedAt: Long,
    val endedAt: Long,
    val rxBytes: Long,
    val txBytes: Long,
    val mtu: Int = 0,
    val endpoint: String = ""
) {
    val durationMs: Long get() = (endedAt - startedAt).coerceAtLeast(0)
}

enum class TunnelState { DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING, ERROR }

data class LiveStats(
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val downBps: Long = 0,
    val upBps: Long = 0,
    val handshakeAgeMs: Long = -1,
    /** Peak download seen this session, for the "top speed" readout. */
    val peakDownBps: Long = 0,
    val peakUpBps: Long = 0
)

/**
 * A rolling window of throughput samples, so the connect screen can draw a live graph
 * instead of a single number. Fixed capacity, oldest sample dropped.
 */
class SpeedHistory(val capacity: Int = 60) {
    private val down = LongArray(capacity)
    private val up = LongArray(capacity)
    private var count = 0
    private var head = 0

    fun push(downBps: Long, upBps: Long) {
        down[head] = downBps
        up[head] = upBps
        head = (head + 1) % capacity
        if (count < capacity) count++
    }

    fun clear() {
        count = 0
        head = 0
        down.fill(0)
        up.fill(0)
    }

    val size: Int get() = count

    fun downSeries(): List<Long> = series(down)
    fun upSeries(): List<Long> = series(up)

    private fun series(buf: LongArray): List<Long> {
        if (count == 0) return emptyList()
        val out = ArrayList<Long>(count)
        val start = if (count < capacity) 0 else head
        for (i in 0 until count) out.add(buf[(start + i) % capacity])
        return out
    }

    fun peakDown(): Long = downSeries().maxOrNull() ?: 0L
    fun peakUp(): Long = upSeries().maxOrNull() ?: 0L
}
