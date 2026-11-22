package com.aether.vpn.store

import android.content.Context
import com.aether.vpn.core.Carrier
import com.aether.vpn.core.Endpoint
import com.aether.vpn.core.SessionRecord
import com.aether.vpn.core.TransportCatalog
import com.aether.vpn.core.TransportConfig
import com.aether.vpn.core.TunnelProfile
import com.aether.vpn.net.WarpApi
import org.json.JSONArray
import org.json.JSONObject

/**
 * On-device persistence: the WARP registration, the chosen edge, favourites,
 * session history and preferences. Plain SharedPreferences + JSON — no server,
 * no account, nothing leaves the phone.
 */
class AppStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("aether_tunnel", Context.MODE_PRIVATE)

    // ---- WARP registration -------------------------------------------------

    fun registration(): WarpApi.Registration? {
        val raw = prefs.getString(KEY_REG, null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            WarpApi.Registration(
                privateKey = o.getString("privateKey"),
                publicKey = o.getString("publicKey"),
                peerPublicKey = o.getString("peerPublicKey"),
                addressV4 = o.getString("addressV4"),
                addressV6 = o.optString("addressV6").takeIf { it.isNotBlank() },
                clientId = o.optString("clientId"),
                accountId = o.optString("accountId")
            )
        }.getOrNull()
    }

    fun saveRegistration(reg: WarpApi.Registration) {
        val o = JSONObject().apply {
            put("privateKey", reg.privateKey)
            put("publicKey", reg.publicKey)
            put("peerPublicKey", reg.peerPublicKey)
            put("addressV4", reg.addressV4)
            put("addressV6", reg.addressV6 ?: "")
            put("clientId", reg.clientId)
            put("accountId", reg.accountId)
        }
        prefs.edit().putString(KEY_REG, o.toString()).apply()
    }

    fun clearRegistration() = prefs.edit().remove(KEY_REG).apply()

    // ---- chosen edge -------------------------------------------------------

    var edgeIndex: Int
        get() = prefs.getInt(KEY_EDGE, 0)
        set(value) = prefs.edit().putInt(KEY_EDGE, value).apply()

    // ---- favourites --------------------------------------------------------

    fun favourites(): Set<String> =
        prefs.getStringSet(KEY_FAV, emptySet())?.toSet() ?: emptySet()

    fun toggleFavourite(id: String): Boolean {
        val current = favourites().toMutableSet()
        val nowFav = if (current.contains(id)) {
            current.remove(id); false
        } else {
            current.add(id); true
        }
        prefs.edit().putStringSet(KEY_FAV, current).apply()
        return nowFav
    }

    // ---- session history ---------------------------------------------------

    fun sessions(): List<SessionRecord> {
        val raw = prefs.getString(KEY_SESSIONS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                SessionRecord(
                    profileName = o.getString("name"),
                    flag = o.optString("flag"),
                    startedAt = o.getLong("start"),
                    endedAt = o.getLong("end"),
                    rxBytes = o.getLong("rx"),
                    txBytes = o.getLong("tx"),
                    mtu = o.optInt("mtu", 0),
                    endpoint = o.optString("endpoint", "")
                )
            }
        }.getOrElse { emptyList() }
    }

    fun addSession(record: SessionRecord) {
        val list = sessions().toMutableList()
        list.add(0, record)
        val trimmed = list.take(MAX_SESSIONS)
        val arr = JSONArray()
        trimmed.forEach { r ->
            arr.put(JSONObject().apply {
                put("name", r.profileName)
                put("flag", r.flag)
                put("start", r.startedAt)
                put("end", r.endedAt)
                put("rx", r.rxBytes)
                put("tx", r.txBytes)
                put("mtu", r.mtu)
                put("endpoint", r.endpoint)
            })
        }
        prefs.edit().putString(KEY_SESSIONS, arr.toString()).apply()
    }

    fun clearSessions() = prefs.edit().remove(KEY_SESSIONS).apply()

    // ---- transport config --------------------------------------------------

    /** The SNI / payload / endpoint selection, persisted as one JSON blob. */
    fun transport(): TransportConfig {
        val raw = prefs.getString(KEY_TRANSPORT, null) ?: return TransportConfig()
        return runCatching {
            val o = JSONObject(raw)
            TransportConfig(
                sniId = o.optString("sniId", "cf-engage"),
                payloadId = o.optString("payloadId", "pl-none"),
                customSni = o.optString("customSni", ""),
                customPayload = o.optString("customPayload", ""),
                endpointKey = o.optString("endpointKey", "162.159.192.1:2408"),
                autoFailover = o.optBoolean("autoFailover", true),
                carrier = runCatching {
                    Carrier.valueOf(o.optString("carrier", Carrier.AUTO.name))
                }.getOrElse { Carrier.AUTO },
                autoMtu = o.optBoolean("autoMtu", true),
                watchdog = o.optBoolean("watchdog", true),
                dnsId = o.optString("dnsId", "dns-cf"),
                verifyTraffic = o.optBoolean("verifyTraffic", true),
                routeIpv6 = o.optBoolean("routeIpv6", false)
            )
        }.getOrElse { TransportConfig() }
    }

    fun saveTransport(config: TransportConfig) {
        val o = JSONObject().apply {
            put("sniId", config.sniId)
            put("payloadId", config.payloadId)
            put("customSni", config.customSni)
            put("customPayload", config.customPayload)
            put("endpointKey", config.endpointKey)
            put("autoFailover", config.autoFailover)
            put("carrier", config.carrier.name)
            put("autoMtu", config.autoMtu)
            put("watchdog", config.watchdog)
            put("dnsId", config.dnsId)
            put("verifyTraffic", config.verifyTraffic)
            put("routeIpv6", config.routeIpv6)
        }
        prefs.edit().putString(KEY_TRANSPORT, o.toString()).apply()
    }

    /** The endpoint the user picked, resolved against the pool. */
    fun selectedEndpoint(): Endpoint? {
        val key = transport().endpointKey
        return TransportCatalog.endpoints.firstOrNull { it.key == key }
    }

    // ---- probe results -----------------------------------------------------

    /** Cached reachability, so the server list can show real latency without re-probing. */
    fun probeResults(): Map<String, Long> {
        val raw = prefs.getString(KEY_PROBES, null) ?: return emptyMap()
        return runCatching {
            val o = JSONObject(raw)
            o.keys().asSequence().associateWith { o.getLong(it) }
        }.getOrElse { emptyMap() }
    }

    fun saveProbeResults(results: Map<String, Long>) {
        val o = JSONObject()
        results.forEach { (k, v) -> o.put(k, v) }
        prefs.edit().putString(KEY_PROBES, o.toString()).apply()
    }

    // ---- admin lock --------------------------------------------------------

    /**
     * The admin PIN. Everything that can break the tunnel (SNI, payload, endpoint, MTU,
     * DNS, IPv6) is behind this, so a normal user cannot put the app into a state where
     * it connects and then carries nothing.
     *
     * The PIN is stored as a salted SHA-256 hash, never in clear text. The default is
     * set on first run and can be changed from the admin panel.
     */
    fun hasAdminPin(): Boolean = prefs.contains(KEY_ADMIN_HASH)

    fun setAdminPin(pin: String) {
        val salt = java.util.UUID.randomUUID().toString()
        prefs.edit()
            .putString(KEY_ADMIN_SALT, salt)
            .putString(KEY_ADMIN_HASH, hashPin(pin, salt))
            .apply()
    }

    fun checkAdminPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_ADMIN_SALT, null) ?: return false
        val stored = prefs.getString(KEY_ADMIN_HASH, null) ?: return false
        return stored == hashPin(pin, salt)
    }

    private fun hashPin(pin: String, salt: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = md.digest((salt + ":" + pin).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** True once the admin has unlocked this session. Not persisted across restarts. */
    var adminUnlocked: Boolean = false

    // ---- preferences -------------------------------------------------------

    var autoConnect: Boolean
        get() = prefs.getBoolean(KEY_AUTO, false)
        set(v) = prefs.edit().putBoolean(KEY_AUTO, v).apply()

    var killSwitch: Boolean
        get() = prefs.getBoolean(KEY_KILL, true)
        set(v) = prefs.edit().putBoolean(KEY_KILL, v).apply()

    var mtu: Int
        get() = prefs.getInt(KEY_MTU, 1280)
        set(v) = prefs.edit().putInt(KEY_MTU, v).apply()

    var dns: String
        get() = prefs.getString(KEY_DNS, "1.1.1.1, 1.0.0.1") ?: "1.1.1.1, 1.0.0.1"
        set(v) = prefs.edit().putString(KEY_DNS, v).apply()

    var customSni: String
        get() = prefs.getString(KEY_SNI, "engage.cloudflareclient.com") ?: "engage.cloudflareclient.com"
        set(v) = prefs.edit().putString(KEY_SNI, v).apply()

    var customHost: String
        get() = prefs.getString(KEY_HOST, "engage.cloudflareclient.com") ?: "engage.cloudflareclient.com"
        set(v) = prefs.edit().putString(KEY_HOST, v).apply()

    var customPayload: String
        get() = prefs.getString(KEY_PAYLOAD, "") ?: ""
        set(v) = prefs.edit().putString(KEY_PAYLOAD, v).apply()

    /** The MTU that last produced a working tunnel, so the next connect starts there. */
    var lastGoodMtu: Int
        get() = prefs.getInt(KEY_GOOD_MTU, 0)
        set(v) = prefs.edit().putInt(KEY_GOOD_MTU, v).apply()

    /** Applies the user's preferences on top of a base profile. */
    fun applyPreferences(base: TunnelProfile): TunnelProfile = base.copy(
        mtu = mtu,
        dns = dns.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    )

    /** Applies preferences with an explicit MTU, used by the MTU ladder. */
    fun applyPreferences(base: TunnelProfile, mtuOverride: Int): TunnelProfile = base.copy(
        mtu = mtuOverride,
        dns = dns.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    )

    private companion object {
        const val KEY_REG = "warp_registration"
        const val KEY_EDGE = "edge_index"
        const val KEY_FAV = "favourites"
        const val KEY_SESSIONS = "sessions"
        const val KEY_AUTO = "auto_connect"
        const val KEY_KILL = "kill_switch"
        const val KEY_MTU = "mtu"
        const val KEY_DNS = "dns"
        const val KEY_SNI = "sni"
        const val KEY_HOST = "host"
        const val KEY_PAYLOAD = "payload"
        const val KEY_TRANSPORT = "transport_config"
        const val KEY_PROBES = "probe_results"
        const val KEY_GOOD_MTU = "last_good_mtu"
        const val KEY_ADMIN_HASH = "admin_hash"
        const val KEY_ADMIN_SALT = "admin_salt"
        const val MAX_SESSIONS = 100
    }
}
