package com.flowy.explore.ui.workbench

import android.content.Context
import com.flowy.explore.foundation.AccessibilityStatusReader
import com.flowy.explore.foundation.DevServerReader
import com.flowy.explore.foundation.OverlayPermissionReader
import com.flowy.explore.foundation.WorkbenchPreferenceStore
import com.flowy.explore.runtime.DaemonForegroundService
import com.flowy.explore.runtime.MediaProjectionSessionHolder

class OverlayUiStateStore(private val context: Context) {
  private val accessibilityStatusReader = AccessibilityStatusReader(context)
  private val overlayPermissionReader = OverlayPermissionReader(context)
  private val preferenceStore = WorkbenchPreferenceStore(context)

  fun snapshot(): WorkbenchStatusSnapshot {
    val daemonStatus = DaemonForegroundService.currentStatus
    val config = DevServerReader(context).read()
    return WorkbenchStatusSnapshot(
      daemonStatus = daemonStatus,
      connectionStatus = mapConnectionStatus(daemonStatus),
      accessibilityStatus = accessibilityStatusReader.statusText(),
      projectionStatus = MediaProjectionSessionHolder.statusText(),
      overlayPermissionStatus = overlayPermissionReader.statusText(),
      agentMode = preferenceStore.agentMode(),
      captureModeEnabled = preferenceStore.captureModeEnabled(),
      host = config.host,
      port = config.port,
    )
  }

  private fun mapConnectionStatus(status: String): String {
    return when (status) {
      "connected" -> "connected"
      "connecting", "starting" -> "connecting"
      "closing" -> "reconnecting"
      "error" -> "error"
      else -> "disconnected"
    }
  }
}
