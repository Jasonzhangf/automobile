package com.flowy.explore.runtime

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.flowy.explore.blocks.AppendLogBlock
import com.flowy.explore.flows.DaemonStartupFlow
import com.flowy.explore.flows.ReconnectFlow
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class DaemonForegroundService : Service() {
  private lateinit var logStore: LocalLogStore
  private lateinit var appendLog: AppendLogBlock
  private lateinit var scheduler: ScheduledExecutorService
  private lateinit var startupFlow: DaemonStartupFlow
  private lateinit var reconnectFlow: ReconnectFlow

  override fun onCreate() {
    super.onCreate()
    currentInstance = WeakReference(this)
    logStore = LocalLogStore(this)
    appendLog = AppendLogBlock(logStore)
    scheduler = Executors.newSingleThreadScheduledExecutor()
    reconnectFlow = ReconnectFlow(scheduler)
    startupFlow = DaemonStartupFlow(this, logStore, reconnectFlow)
    currentStatus = "created"
    appendLog.info("daemon_started", "service created")
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP -> stopSelfSafely()
      else -> startRuntime()
    }
    return START_STICKY
  }

  override fun onDestroy() {
    startupFlow.close()
    scheduler.shutdownNow()
    currentInstance?.clear()
    currentInstance = null
    currentStatus = "stopped"
    appendLog.info("daemon_stopped", "service destroyed")
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun startRuntime() {
    startOrRefreshForeground()
    currentStatus = "starting"
    startupFlow.start(
      onStatus = { status -> currentStatus = status },
      onHeartbeat = { heartbeat -> lastHeartbeat = heartbeat },
    )
  }

  private fun startOrRefreshForeground() {
    DaemonNotification.ensureChannel(this)
    val notification = DaemonNotification.build(this, "Phase A runtime active")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
        DaemonNotification.notificationId,
        notification,
        foregroundServiceType(),
      )
      return
    }
    startForeground(DaemonNotification.notificationId, notification)
  }

  private fun foregroundServiceType(): Int {
    var serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
    if (MediaProjectionSessionHolder.hasGrant()) {
      serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
    }
    return serviceType
  }

  private fun stopSelfSafely() {
    currentStatus = "stopping"
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  companion object {
    const val ACTION_START = "com.flowy.explore.action.START"
    const val ACTION_STOP = "com.flowy.explore.action.STOP"

    @Volatile var currentStatus: String = "idle"
    @Volatile var lastHeartbeat: String = "never"
    @Volatile private var currentInstance: WeakReference<DaemonForegroundService>? = null

    fun promoteProjectionIfReady() {
      val service = currentInstance?.get() ?: return
      service.startOrRefreshForeground()
      MediaProjectionSessionHolder.activate(service.applicationContext)
    }
  }
}
