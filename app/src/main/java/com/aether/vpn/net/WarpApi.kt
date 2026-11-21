package com.aether.vpn.net

import com.aether.vpn.core.Endpoint
import com.aether.vpn.core.TransportCatalog
import com.aether.vpn.core.TransportConfig
import com.aether.vpn.core.TunnelProfile
import com.wireguard.crypto.KeyPair
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Registers a free, unlimited Cloudflare WARP account and turns the response into a
 * real WireGuard profile. This is the app's server source: no third-party VPN reseller,
 * no API key, no account of ours in the middle — the device talks straight to
 * Cloudflare's WireGuard edge.
 *
 * The registration is done once per device and cached; the resulting keypair and
 * peer config are what the tunnel actually uses.
 */
object WarpApi {

    private const val REG_URL = "https://api.cloudflareclient.com/v0a2158/reg"
    private const val CLIENT_VERSION = "a-6.30-2158"

    /** Endpoints Cloudflare publishes for WARP; the first that answers is used. */
    private val ENDPOINTS = listOf(
        "162.159.192.1" to 2408,
        "162.159.193.1" to 2408,
        "162.159.195.1" to 2408,
        "188.114.96.1" to 2408,
        "188.114.97.1" to 2408,
        "188.114.98.1" to 2408,
        "188.114.99.1" to 2408
    )

    data class Registration(
        val privateKey: String,
        val publicKey: String,
        val peerPublicKey: String,
        val addressV4: String,
        val addressV6: String?,
        val clientId: String,
        val accountId: String
    )

    /**
     * Registers a new WARP device. Blocking — call from a background dispatcher.
     * @throws Exception when the network is unavailable or Cloudflare refuses.
     */
    fun register(): Registration {
        val keyPair = KeyPair() // WireGuard's own X25519 generator
        val publicKey = keyPair.publicKey.toBase64()

        val body = JSONObject().apply {
            put("key", publicKey)
            put("install_id", "")
            put("fcm_token", "")
            put("tos", isoNow())
            put("model", "PC")
            put("locale", "en_US")
        }.toString()

        val conn = (URL(REG_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("CF-Client-Version", CLIENT_VERSION)
            setRequestProperty("User-Agent", "okhttp/3.12.1")
        }

        try {
            conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.let {
                BufferedReader(InputStreamReader(it)).use { r -> r.readText() }
            } ?: ""
            if (code !in 200..299) {
                throw IllegalStateException("WARP registration failed (HTTP $code): ${text.take(200)}")
            }

            val root = JSONObject(text)
            val config = root.getJSONObject("config")
            val peer = config.getJSONArray("peers").getJSONObject(0)
            val iface = config.getJSONObject("interface")
            val addresses = iface.getJSONObject("addresses")

            return Registration(
                privateKey = keyPair.privateKey.toBase64(),
                publicKey = publicKey,
                peerPublicKey = peer.getString("public_key"),
                addressV4 = addresses.getString("v4"),
                addressV6 = addresses.optString("v6").takeIf { it.isNotBlank() },
                clientId = config.optString("client_id"),
                accountId = root.optJSONObject("account")?.optString("id").orEmpty()
            )
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Builds the profile the tunnel will use from a registration.
     *
     * @param mtu the MTU for this attempt. The failover engine walks the ladder, so this
     *        is not a constant — it is whatever value is being tried right now.
     */
    fun profileFrom(
        reg: Registration,
        endpoint: Endpoint = TransportCatalog.endpoints.first(),
        transport: TransportConfig = TransportConfig(),
        mtu: Int = 1280
    ): TunnelProfile {
        val sni = transport.effectiveSni
        val payload = TransportCatalog.renderPayload(
            template = transport.effectivePayloadTemplate,
            host = sni,
            port = 443,
            sni = sni
        ).ifBlank { null }
        val dns = TransportCatalog.dnsById(transport.dnsId)?.servers
            ?: listOf("1.1.1.1", "1.0.0.1")
        return TunnelProfile(
            id = "warp-auto",
            name = "Aether Auto",
            country = "Global",
            city = "Anycast edge",
            flag = "\uD83C\uDF10",
            endpointHost = endpoint.host,
            endpointPort = endpoint.port,
            peerPublicKey = reg.peerPublicKey,
            privateKey = reg.privateKey,
            addressV4 = "${reg.addressV4}/32",
            addressV6 = reg.addressV6?.let { "$it/128" },
            dns = dns,
            mtu = mtu,
            allowedIps = listOf("0.0.0.0/0", "::/0"),
            persistentKeepalive = 25,
            tier = TunnelProfile.Tier.FREE,
            sni = sni,
            hostHeader = sni,
            payload = payload,
            sniId = transport.sniId,
            payloadId = transport.payloadId
        )
    }

    /** The curated edge list, exposed so the server screen can show real choices. */
    fun edgeList(): List<Pair<String, Int>> = ENDPOINTS

    private fun isoNow(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }
}
