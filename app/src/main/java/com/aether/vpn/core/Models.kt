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
    val isCustom: Boolean = false
) {
    enum class Tier { FREE, PREMIUM }

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
    }
}

/** One finished connection, kept for the stats screen. */
data class SessionRecord(
    val profileName: String,
    val flag: String,
    val startedAt: Long,
    val endedAt: Long,
    val rxBytes: Long,
    val txBytes: Long
) {
    val durationMs: Long get() = (endedAt - startedAt).coerceAtLeast(0)
}

enum class TunnelState { DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING, ERROR }

data class LiveStats(
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val downBps: Long = 0,
    val upBps: Long = 0,
    val handshakeAgeMs: Long = -1
)
