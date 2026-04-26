package com.flowy.explore.ui.workbench

import com.flowy.explore.foundation.AgentMode
import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayStatusFormatterTest {
  @Test
  fun bubbleLabel_prefersCaptureModeWhenEnabled() {
    assertEquals("capture", OverlayStatusFormatter.bubbleLabel(snapshot(captureModeEnabled = true)))
  }

  @Test
  fun bubbleLabel_mapsConnectedToIdle() {
    assertEquals("idle", OverlayStatusFormatter.bubbleLabel(snapshot(daemonStatus = "connected")))
  }

  @Test
  fun statusSummary_compactsSignals() {
    val summary = OverlayStatusFormatter.statusSummary(
      snapshot(connectionStatus = "connected", projectionStatus = "ready", captureModeEnabled = true),
    )
    assertEquals(true, summary.contains("连接正常"))
    assertEquals(true, summary.contains("截图开"))
    assertEquals(true, summary.contains("捕获开"))
  }

  private fun snapshot(
    daemonStatus: String = "connected",
    connectionStatus: String = "connected",
    accessibilityStatus: String = "enabled",
    projectionStatus: String = "not-ready",
    overlayPermissionStatus: String = "granted",
    agentMode: AgentMode = AgentMode.PASSIVE,
    captureModeEnabled: Boolean = false,
    host: String = "127.0.0.1",
    port: Int = 8787,
  ): WorkbenchStatusSnapshot {
    return WorkbenchStatusSnapshot(
      daemonStatus = daemonStatus,
      connectionStatus = connectionStatus,
      accessibilityStatus = accessibilityStatus,
      projectionStatus = projectionStatus,
      overlayPermissionStatus = overlayPermissionStatus,
      agentMode = agentMode,
      captureModeEnabled = captureModeEnabled,
      host = host,
      port = port,
    )
  }
}
