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
  fun statusSummary_includesModeAndProjection() {
    val summary = OverlayStatusFormatter.statusSummary(
      snapshot(agentMode = AgentMode.ACTIVE, projectionStatus = "ready"),
    )
    assertEquals(true, summary.contains("mode=active"))
    assertEquals(true, summary.contains("projection=ready"))
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
