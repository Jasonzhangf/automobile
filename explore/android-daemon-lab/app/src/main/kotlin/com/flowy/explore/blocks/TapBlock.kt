package com.flowy.explore.blocks

import com.flowy.explore.foundation.AccessibilityTargeting
import com.flowy.explore.foundation.OperationBackend
import com.flowy.explore.foundation.RootShellRunner
import com.flowy.explore.runtime.AccessibilitySnapshotStore
import com.flowy.explore.runtime.FlowyAccessibilityService
import org.json.JSONObject

class TapBlock(
  private val rootShellRunner: RootShellRunner = RootShellRunner(),
) {
  fun run(payload: JSONObject): String {
    val point = AccessibilityTargeting.pointFromPayload(payload) ?: resolvePoint(payload)
    return when (OperationBackend.fromPayload(payload)) {
      OperationBackend.ACCESSIBILITY -> {
        check(FlowyAccessibilityService.requireInstance().performTap(point.first, point.second)) { "TAP_FAILED" }
        "tap:accessibility:${point.first},${point.second}"
      }
      OperationBackend.ROOT -> {
        val result = rootShellRunner.run("input tap ${point.first} ${point.second}")
        check(result.exitCode == 0) { "TAP_FAILED" }
        "tap:root:${point.first},${point.second}"
      }
    }
  }

  private fun resolvePoint(payload: JSONObject): Pair<Int, Int> {
    val selector = AccessibilityTargeting.selectorFromPayload(payload)
    val bounds = AccessibilityTargeting.resolveBounds(AccessibilitySnapshotStore.currentJson(), selector)
      ?: error("TARGET_NOT_FOUND")
    return bounds.centerX() to bounds.centerY()
  }
}
