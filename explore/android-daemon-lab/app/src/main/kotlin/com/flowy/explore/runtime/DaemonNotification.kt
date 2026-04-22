package com.flowy.explore.runtime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.flowy.explore.R
import com.flowy.explore.ui.DevPanelActivity

object DaemonNotification {
  const val channelId = "flowy-daemon-lab"
  const val notificationId = 1001

  fun ensureChannel(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java)
    val channel = NotificationChannel(channelId, "Flowy Daemon Lab", NotificationManager.IMPORTANCE_LOW)
    manager.createNotificationChannel(channel)
  }

  fun build(context: Context, content: String): Notification {
    val intent = Intent(context, DevPanelActivity::class.java)
    val pending = PendingIntent.getActivity(
      context,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    return NotificationCompat.Builder(context, channelId)
      .setContentTitle(context.getString(R.string.app_name))
      .setContentText(content)
      .setSmallIcon(android.R.drawable.stat_notify_sync)
      .setContentIntent(pending)
      .setOngoing(true)
      .build()
  }
}
