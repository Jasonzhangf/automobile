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
    return listOf(
      shortConnection(snapshot.connectionStatus),
      shortAccessibility(snapshot.accessibilityStatus),
      shortProjection(snapshot.projectionStatus),
      if (snapshot.captureModeEnabled) "捕获开" else "捕获关",
    ).joinToString(" · ")
  }

  private fun shortConnection(status: String): String {
    return when (status) {
      "connected" -> "连接正常"
      "connecting", "reconnecting" -> "连接中"
      "error" -> "连接异常"
      else -> "未连接"
    }
  }

  private fun shortAccessibility(status: String): String {
    return if (status == "enabled") "无障碍开" else "无障碍关"
  }

  private fun shortProjection(status: String): String {
    return if (status == "ready") "截图开" else "截图关"
  }
}
