package com.aether.vpn.store

import android.content.Context
import com.aether.vpn.core.SessionRecord
import com.aether.vpn.core.TunnelProfile
import com.aether.vpn.net.WarpApi
import org.json.JSONArray
import org.json.JSONObject

/**
 * On-device persistence: the WARP registration, the chosen edge, favourites,
 * session history and preferences. Plain SharedPreferences + JSON - no server,
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
                    txBytes = o.getLong("tx")
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
            })
        }
        prefs.edit().putString(KEY_SESSIONS, arr.toString()).apply()
    }

    fun clearSessions() = prefs.edit().remove(KEY_SESSIONS).apply()

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

    /** Applies the user's preferences on top of a base profile. */
    fun applyPreferences(base: TunnelProfile): TunnelProfile = base.copy(
        mtu = mtu,
        dns = dns.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        sni = customSni.ifBlank { base.sni },
        hostHeader = customHost.ifBlank { base.hostHeader },
        payload = customPayload.ifBlank { null }
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
        const val MAX_SESSIONS = 100
    }
}
