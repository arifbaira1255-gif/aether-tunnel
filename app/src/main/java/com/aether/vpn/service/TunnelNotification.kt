package com.aether.vpn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aether.vpn.MainActivity
import com.aether.vpn.R

/**
 * The ongoing notification shown while the tunnel is up.
 *
 * It is an ongoing (non-dismissible) notification: it shows the live session time and
 * throughput, and tapping it opens the app. The tunnel itself is kept alive by the
 * WireGuard VpnService, not by this notification — this is the status surface, and it is
 * cancelled the moment the tunnel goes down.
 */
object TunnelNotification {

    const val CHANNEL_ID = "aether_tunnel_status"
    const val NOTIFICATION_ID = 4101

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Tunnel status",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows the live VPN session while the tunnel is connected."
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    fun build(
        context: Context,
        connected: Boolean,
        durationLabel: String,
        downLabel: String,
        upLabel: String,
        serverLabel: String
    ): Notification {
        ensureChannel(context)

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (connected) "Aether Tunnel · Protected" else "Aether Tunnel · Connecting"
        val body = if (connected) {
            "$serverLabel  ·  $durationLabel\n↓ $downLabel   ↑ $upLabel"
        } else {
            "Negotiating a secure tunnel…"
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_tunnel)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    fun update(
        context: Context,
        connected: Boolean,
        durationLabel: String,
        downLabel: String,
        upLabel: String,
        serverLabel: String
    ) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        runCatching {
            manager.notify(
                NOTIFICATION_ID,
                build(context, connected, durationLabel, downLabel, upLabel, serverLabel)
            )
        }
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        runCatching { manager.cancel(NOTIFICATION_ID) }
    }
}
