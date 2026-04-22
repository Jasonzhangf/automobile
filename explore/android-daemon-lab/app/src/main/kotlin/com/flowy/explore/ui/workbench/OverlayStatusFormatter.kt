package com.flowy.explore.ui.workbench

object OverlayStatusFormatter {
  fun bubbleLabel(snapshot: WorkbenchStatusSnapshot): String {
    if (snapshot.captureModeEnabled) return "capture"
    return when (snapshot.daemonStatus) {
      "connected" -> "idle"
      "connecting", "starting" -> "starting"
      "error" -> "error"
      "stopped", "stopping" -> "stopped"
      else -> snapshot.daemonStatus.ifBlank { "idle" }
    }
  }

  fun bubbleStateColor(snapshot: WorkbenchStatusSnapshot): Int {
    return when (snapshot.connectionStatus) {
      "connected" -> 0xFF607D8B.toInt()
      "connecting", "reconnecting" -> 0xFF90A4AE.toInt()
      "error" -> 0xFFB0BEC5.toInt()
      else -> 0xFF9E9E9E.toInt()
    }
  }

  fun statusSummary(snapshot: WorkbenchStatusSnapshot): String {
    return buildString {
      append("daemon=")
      append(snapshot.daemonStatus)
      append("\nconn=")
      append(snapshot.connectionStatus)
      append("\naccess=")
      append(snapshot.accessibilityStatus)
      append("\nprojection=")
      append(snapshot.projectionStatus)
      append("\nmode=")
      append(snapshot.agentMode.name.lowercase())
      append("\ncapture=")
      append(if (snapshot.captureModeEnabled) "active" else "inactive")
    }
  }
}
