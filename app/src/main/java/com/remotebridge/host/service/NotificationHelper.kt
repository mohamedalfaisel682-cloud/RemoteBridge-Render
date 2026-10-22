package com.remotebridge.host.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.remotebridge.MainActivity
import com.rbstudio.R

/**
 * مساعد لإنشاء الإشعارات وقنواتها بما يتوافق مع أندرويد 8+.
 */
object NotificationHelper {

    const val CHANNEL_ID = "bridge_host_channel"
    const val NOTIFICATION_ID = 1001

    /**
     * ينشئ قناة الإشعارات (مطلوب على أندرويد 8+).
     */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.host_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.host_notification_text)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * يبني إشعار الخدمة الأمامية.
     */
    fun buildHostNotification(context: Context): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.host_notification_title))
            .setContentText(context.getString(R.string.host_notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
