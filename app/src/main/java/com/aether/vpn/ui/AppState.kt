package com.aether.vpn.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aether.vpn.core.LiveStats
import com.aether.vpn.core.SessionRecord
import com.aether.vpn.core.TunnelProfile
import com.aether.vpn.core.TunnelState
import com.aether.vpn.net.WarpApi
import com.aether.vpn.service.TunnelController
import com.aether.vpn.store.AppStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Single source of truth for the UI. Holds the WARP registration, the edge list,
 * the live tunnel state and the session history, and drives the stats poller.
 */
class AppState(private val context: Context) {

    val store = AppStore(context)

    var state by mutableStateOf(TunnelState.DISCONNECTED)
        private set
    var stats by mutableStateOf(LiveStats())
        private set
    var activeProfile by mutableStateOf<TunnelProfile?>(null)
        private set
    var registration by mutableStateOf<WarpApi.Registration?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
    var connectedAt by mutableStateOf(0L)
        private set

    val sessions = mutableStateListOf<SessionRecord>()
    val favourites = mutableStateListOf<String>()

    private var pollJob: kotlinx.coroutines.Job? = null

    init {
        registration = store.registration()
        sessions.addAll(store.sessions())
        favourites.addAll(store.favourites())
    }

    /** The edge list the server screen shows, with real Cloudflare endpoints. */
    fun edges(): List<Pair<String, Int>> = WarpApi.edgeList()

    fun refreshFromStore() {
        sessions.clear(); sessions.addAll(store.sessions())
        favourites.clear(); favourites.addAll(store.favourites())
    }

    fun toggleFavourite(id: String) {
        store.toggleFavourite(id)
        refreshFromStore()
    }

    /** Registers a WARP device if we do not have one yet. */
    suspend fun ensureRegistration(): WarpApi.Registration? {
        registration?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching { WarpApi.register() }
                .onSuccess {
                    store.saveRegistration(it)
                    registration = it
                }
                .onFailure { error = it.message }
                .getOrNull()
        }
    }

    /** The profile the tunnel will use, with the user's preferences applied. */
    fun currentProfile(): TunnelProfile? {
        val reg = registration ?: return null
        val base = WarpApi.profileFrom(reg, store.edgeIndex)
        return store.applyPreferences(base)
    }

    fun connect(scope: CoroutineScope) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            val reg = ensureRegistration()
            if (reg == null) {
                busy = false
                state = TunnelState.ERROR
                return@launch
            }
            val profile = currentProfile()
            if (profile == null) {
                busy = false
                state = TunnelState.ERROR
                return@launch
            }
            state = TunnelState.CONNECTING
            val result = withContext(Dispatchers.IO) {
                runCatching { TunnelController.connect(context, profile) }
            }
            result.onSuccess {
                activeProfile = profile
                connectedAt = TunnelController.connectedSince()
                state = TunnelState.CONNECTED
                startPolling(scope)
            }.onFailure {
                error = it.message ?: "Could not start the tunnel"
                state = TunnelState.ERROR
            }
            busy = false
        }
    }

    fun disconnect(scope: CoroutineScope) {
        if (busy) return
        busy = true
        scope.launch {
            state = TunnelState.DISCONNECTING
            val started = connectedAt
            val finalStats = stats
            withContext(Dispatchers.IO) { runCatching { TunnelController.disconnect(context) } }
            stopPolling()
            if (started > 0) {
                val rec = SessionRecord(
                    profileName = activeProfile?.name ?: "Aether Auto",
                    flag = activeProfile?.flag ?: "\uD83C\uDF10",
                    startedAt = started,
                    endedAt = System.currentTimeMillis(),
                    rxBytes = finalStats.rxBytes,
                    txBytes = finalStats.txBytes
                )
                store.addSession(rec)
                refreshFromStore()
            }
            activeProfile = null
            connectedAt = 0L
            stats = LiveStats()
            state = TunnelState.DISCONNECTED
            busy = false
        }
    }

    private fun startPolling(scope: CoroutineScope) {
        stopPolling()
        pollJob = scope.launch {
            while (true) {
                delay(1000)
                withContext(Dispatchers.IO) { TunnelController.pollStats(context) }
                stats = TunnelController.stats.value
                state = TunnelController.state.value
                if (state == TunnelState.DISCONNECTED) break
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    // ---- lifetime totals ---------------------------------------------------

    fun lifetimeRx(): Long = sessions.sumOf { it.rxBytes }
    fun lifetimeTx(): Long = sessions.sumOf { it.txBytes }
    fun lifetimeMs(): Long = sessions.sumOf { it.durationMs }
}
