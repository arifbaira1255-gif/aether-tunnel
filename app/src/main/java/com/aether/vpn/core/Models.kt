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
        // Transport hints are not part of the WireGuard spec, so they go in comments:
        // a stock wg-quick still parses the file, and a TLS-wrapping relay can read them.
        sni?.let { appendLine("# SNI = $it") }
        hostHeader?.let { appendLine("# Host = $it") }
        payload?.takeIf { it.isNotBlank() }?.let {
            appendLine("# Payload = ${it.replace("\r\n", "\\n")}")
        }
    }

    /** Short "host:port" for the UI. */
    val endpointLabel: String get() = "$endpointHost:$endpointPort"
}

/**
 * The transport settings the user picked: which SNI host, which payload template and
 * which endpoint. Kept separate from [TunnelProfile] so the UI can show and edit them
 * without a live registration.
 */
data class TransportConfig(
    val sniId: String = "cf-engage",
    val payloadId: String = "pl-none",
    val customSni: String = "",
    val customPayload: String = "",
    val endpointKey: String = "162.159.192.1:2408",
    val autoFailover: Boolean = true
) {
    /** The SNI actually used: the custom one wins when it is set. */
    val effectiveSni: String
        get() = customSni.ifBlank {
            TransportCatalog.sniById(sniId)?.host ?: "engage.cloudflareclient.com"
        }

    /** The payload template actually used. */
    val effectivePayloadTemplate: String
        get() = if (payloadId == "pl-custom") customPayload
        else TransportCatalog.payloadById(payloadId)?.template ?: ""
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
