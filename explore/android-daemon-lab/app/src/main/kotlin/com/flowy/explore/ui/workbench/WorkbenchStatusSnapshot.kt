package com.flowy.explore.ui.workbench

import com.flowy.explore.foundation.AgentMode

data class WorkbenchStatusSnapshot(
  val daemonStatus: String,
  val connectionStatus: String,
  val accessibilityStatus: String,
  val projectionStatus: String,
  val overlayPermissionStatus: String,
  val agentMode: AgentMode,
  val captureModeEnabled: Boolean,
  val host: String,
  val port: Int,
)
