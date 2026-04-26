package com.flowy.explore.ui.workbench

object OverlaySectionRenderKey {
  fun build(section: WorkbenchOverlayService.Section, snapshot: WorkbenchStatusSnapshot): String {
    return when (section) {
      WorkbenchOverlayService.Section.AGENT_CONTROL -> listOf(
        snapshot.agentMode.name,
        snapshot.host,
        snapshot.port.toString(),
      ).joinToString("|")
      WorkbenchOverlayService.Section.CAPTURE_MODE -> snapshot.captureModeEnabled.toString()
      WorkbenchOverlayService.Section.SYSTEM_SETTINGS -> listOf(
        snapshot.overlayPermissionStatus,
        snapshot.accessibilityStatus,
        snapshot.projectionStatus,
      ).joinToString("|")
    }
  }
}
