package com.flowy.explore.ui.workbench

import com.flowy.explore.foundation.AgentMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class OverlaySectionRenderKeyTest {
  @Test
  fun agentControlKeyIgnoresTransientStatuses() {
    val base = snapshot()
    val changed = base.copy(connectionStatus = "connected", daemonStatus = "running")
    assertEquals(
      OverlaySectionRenderKey.build(WorkbenchOverlayService.Section.AGENT_CONTROL, base),
      OverlaySectionRenderKey.build(WorkbenchOverlayService.Section.AGENT_CONTROL, changed),
    )
  }

  @Test
  fun agentControlKeyChangesWhenHostChanges() {
    val base = snapshot()
    val changed = base.copy(host = "100.86.84.63")
    assertNotEquals(
      OverlaySectionRenderKey.build(WorkbenchOverlayService.Section.AGENT_CONTROL, base),
      OverlaySectionRenderKey.build(WorkbenchOverlayService.Section.AGENT_CONTROL, changed),
    )
  }

  @Test
  fun systemSettingsKeyTracksPermissionChanges() {
    val base = snapshot()
    val changed = base.copy(projectionStatus = "ready")
    assertNotEquals(
      OverlaySectionRenderKey.build(WorkbenchOverlayService.Section.SYSTEM_SETTINGS, base),
      OverlaySectionRenderKey.build(WorkbenchOverlayService.Section.SYSTEM_SETTINGS, changed),
    )
  }

  private fun snapshot(
    daemonStatus: String = "stopped",
    connectionStatus: String = "idle",
    accessibilityStatus: String = "disabled",
    projectionStatus: String = "missing",
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
